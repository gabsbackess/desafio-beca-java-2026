package br.com.beca.users.application.service;
import br.com.beca.users.application.port.in.CreateUserUseCase;
import br.com.beca.users.application.port.in.ImportUsersCsvResponse;
import br.com.beca.users.application.port.in.ImportUsersCsvUseCase;
import br.com.beca.users.domain.exception.InvalidCsvException;
import br.com.beca.users.domain.exception.UserAlreadyExistsException;
import br.com.beca.users.domain.model.User;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
@Service
public class ImportUsersCsvService implements ImportUsersCsvUseCase {
    private static final int MAX_RECORDS = 1000;
    private final CreateUserUseCase createUserUseCase;
    private final Validator validator;
    public ImportUsersCsvService(
            CreateUserUseCase createUserUseCase,
            Validator validator
    ) {
        this.createUserUseCase = createUserUseCase;
        this.validator = validator;
    }
    private List<String> validate(CsvUserData data) {
        return validator.validate(data)
                .stream()
                .map(violation -> violation.getMessage())
                .toList();
    }
    @Override
    public ImportUsersCsvResponse importUsers(byte[] fileContent) {
        if (fileContent == null || fileContent.length == 0) {
            throw new InvalidCsvException("O arquivo CSV está vazio.");
        }
        // Remove o BOM UTF-8, caso exista.
        if (fileContent.length >= 3
                && fileContent[0] == (byte) 0xEF
                && fileContent[1] == (byte) 0xBB
                && fileContent[2] == (byte) 0xBF) {
            fileContent = Arrays.copyOfRange(
                    fileContent, 3, fileContent.length
            );
        }
        int totalLines = 0;
        int registeredUsers = 0;
        List<String> errors = new ArrayList<>();
        Set<String> expectedHeaders =
                Set.of("name", "email", "cpf", "password");
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setTrim(true)
                .get();
        try (CSVParser headerParser = CSVParser.parse(
                new ByteArrayInputStream(fileContent),
                StandardCharsets.UTF_8,
                format.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .get()
        )) {
            List<String> actualHeaders =
                    headerParser.getHeaderNames();
            List<String> normalizedHeaders = actualHeaders.stream()
                    .map(header -> header.trim().toLowerCase(Locale.ROOT))
                    .toList();
            if (normalizedHeaders.size() != expectedHeaders.size()
                    || !Set.copyOf(normalizedHeaders).equals(expectedHeaders)) {
                throw new InvalidCsvException(
                        "Cabeçalho inválido. Esperado: name, email, cpf, password."
                );
            }
            CSVFormat normalizedFormat = format.builder()
                    .setHeader(normalizedHeaders.toArray(new String[0]))
                    .setSkipHeaderRecord(true)
                    .get();
            try (CSVParser parser = CSVParser.parse(
                    new ByteArrayInputStream(fileContent),
                    StandardCharsets.UTF_8,
                    normalizedFormat
            )) {
                List<CSVRecord> records = parser.getRecords();
                totalLines = records.size();
                if (totalLines > MAX_RECORDS) {
                    throw new InvalidCsvException(
                            "O arquivo CSV ultrapassou o limite de 1.000 registros."
                    );
                }
                for (CSVRecord record : records) {
                    if (!record.isConsistent()) {
                        errors.add(
                                "Linha " + (record.getRecordNumber() + 1)
                                        + ": quantidade de colunas inválida. Esperadas: 4."
                        );
                        continue;
                    }
                    CsvUserData data = new CsvUserData(
                            record.get(normalizedHeaders.indexOf("name")),
                            record.get(normalizedHeaders.indexOf("email")),
                            record.get(normalizedHeaders.indexOf("cpf")),
                            record.get(normalizedHeaders.indexOf("password"))
                    );
                    List<String> validationErrors = validate(data);
                    if (!validationErrors.isEmpty()) {
                        errors.add(
                                "Linha " + (record.getRecordNumber() + 1)
                                        + ": " + String.join(", ", validationErrors)
                        );
                        continue;
                    }
                    try {
                        User user = new User(
                                data.name(),
                                data.email(),
                                data.cpf(),
                                data.password()
                        );
                        createUserUseCase.create(user);
                        registeredUsers++;
                    } catch (UserAlreadyExistsException exception) {
                        errors.add(
                                "Linha " + (record.getRecordNumber() + 1)
                                        + ": " + exception.getMessage()
                        );
                    } catch (RuntimeException exception) {
                        errors.add(
                                "Linha " + (record.getRecordNumber() + 1)
                                        + ": erro ao cadastrar usuário. "
                                        + exception.getMessage()
                        );
                    }
                }
            }
        } catch (UncheckedIOException exception) {
            errors.add("Arquivo CSV malformado ou inválido.");
        } catch (IOException exception) {
            errors.add("Não foi possível ler o arquivo CSV.");
        }
        return new ImportUsersCsvResponse(
                totalLines,
                registeredUsers,
                errors.size(),
                errors
        );
    }
}