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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ImportUsersCsvService implements ImportUsersCsvUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportUsersCsvService.class);

    private static final int MAX_RECORDS = 1000;
    private static final String MALFORMED_CSV_MESSAGE = "Arquivo CSV malformado ou inválido.";
    private static final String UNEXPECTED_ERROR_MESSAGE = "erro ao cadastrar usuário.";

    private final CreateUserUseCase createUserUseCase;
    private final Validator validator;

    public ImportUsersCsvService(CreateUserUseCase createUserUseCase, Validator validator) {
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

        // Remove o BOM UTF-8 (EF BB BF), se existir.
        if (fileContent.length >= 3
                && (fileContent[0] & 0xFF) == 0xEF
                && (fileContent[1] & 0xFF) == 0xBB
                && (fileContent[2] & 0xFF) == 0xBF) {
            byte[] withoutBom = new byte[fileContent.length - 3];
            System.arraycopy(fileContent, 3, withoutBom, 0, withoutBom.length);
            fileContent = withoutBom;
        }

        int totalLines = 0;
        int registeredUsers = 0;
        List<String> errors = new ArrayList<>();

        Set<String> expectedHeaders = Set.of("name", "email", "cpf", "password");
        CSVFormat format = CSVFormat.DEFAULT.builder().setTrim(true).get();

        try (CSVParser headerParser = CSVParser.parse(
                new ByteArrayInputStream(fileContent),
                StandardCharsets.UTF_8,
                format.builder().setHeader().setSkipHeaderRecord(true).get())) {

            List<String> normalizedHeaders = headerParser.getHeaderNames().stream()
                    .map(header -> header.trim().toLowerCase(Locale.ROOT))
                    .toList();

            if (normalizedHeaders.size() != expectedHeaders.size()
                    || !Set.copyOf(normalizedHeaders).equals(expectedHeaders)) {
                throw new InvalidCsvException("Cabeçalho inválido. Esperado: name, email, cpf, password.");
            }

            CSVFormat normalizedFormat = format.builder()
                    .setHeader(normalizedHeaders.toArray(new String[0]))
                    .setSkipHeaderRecord(true)
                    .get();

            try (CSVParser parser = CSVParser.parse(
                    new ByteArrayInputStream(fileContent),
                    StandardCharsets.UTF_8,
                    normalizedFormat)) {

                List<CSVRecord> records = parser.getRecords();
                totalLines = records.size();

                if (totalLines > MAX_RECORDS) {
                    throw new InvalidCsvException("O arquivo CSV ultrapassou o limite de 1.000 registros.");
                }

                for (CSVRecord record : records) {
                    long line = record.getRecordNumber() + 1;

                    if (!record.isConsistent()) {
                        errors.add("Linha " + line + ": quantidade de colunas inválida. Esperadas: 4.");
                        continue;
                    }

                    CsvUserData data = new CsvUserData(
                            record.get(normalizedHeaders.indexOf("name")),
                            record.get(normalizedHeaders.indexOf("email")),
                            record.get(normalizedHeaders.indexOf("cpf")),
                            record.get(normalizedHeaders.indexOf("password")));

                    List<String> validationErrors = validate(data);
                    if (!validationErrors.isEmpty()) {
                        errors.add("Linha " + line + ": " + String.join(", ", validationErrors));
                        continue;
                    }

                    try {
                        createUserUseCase.create(
                                new User(data.name(), data.email(), data.cpf(), data.password()));
                        registeredUsers++;
                    } catch (UserAlreadyExistsException exception) {
                        // Mensagem fixa: não repassa o texto da exceção para a resposta.
                        log.warn("Importação CSV: usuário já existe (linha {}).", line, exception);
                        errors.add("Linha " + line + ": usuário já cadastrado.");
                    } catch (RuntimeException exception) {
                        // O detalhe vai só para o log (sem dados do usuário, nem a senha).
                        log.error("Importação CSV: erro inesperado ao cadastrar usuário (linha {}).",
                                line, exception);
                        errors.add("Linha " + line + ": " + UNEXPECTED_ERROR_MESSAGE);
                    }
                }
            }
        } catch (UncheckedIOException exception) {
            // Ex.: aspa não fechada.
            log.warn("Importação CSV: arquivo malformado.", exception);
            throw new InvalidCsvException(MALFORMED_CSV_MESSAGE);
        } catch (IOException exception) {
            log.warn("Importação CSV: não foi possível ler o arquivo.", exception);
            throw new InvalidCsvException(MALFORMED_CSV_MESSAGE);
        } catch (IllegalArgumentException exception) {
            // Ex.: coluna do cabeçalho vazia ou duplicada.
            log.warn("Importação CSV: cabeçalho inválido.", exception);
            throw new InvalidCsvException("Cabeçalho inválido. Esperado: name, email, cpf, password.");
        }

        return new ImportUsersCsvResponse(totalLines, registeredUsers, errors.size(), errors);
    }
}
