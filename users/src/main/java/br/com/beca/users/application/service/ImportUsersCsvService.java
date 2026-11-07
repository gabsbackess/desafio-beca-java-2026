package br.com.beca.users.application.service;

import br.com.beca.users.application.port.in.CreateUserUseCase;
import br.com.beca.users.application.port.in.ImportUsersCsvResponse;
import br.com.beca.users.application.port.in.ImportUsersCsvUseCase;
import br.com.beca.users.domain.exception.UserAlreadyExistsException;
import br.com.beca.users.domain.model.User;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ImportUsersCsvService implements ImportUsersCsvUseCase {

    private final CreateUserUseCase createUserUseCase;
    private final Validator validator;
    public ImportUsersCsvService(CreateUserUseCase createUserUseCase,
                                 Validator validator) {
        this.createUserUseCase = createUserUseCase;
        this.validator = validator;
    }
    private List<String> validate(CsvUserData data) {

        return validator.validate(data)
                .stream()
                .map(violation ->
                        violation.getMessage())
                .toList();
    }
    @Override
    public ImportUsersCsvResponse importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()){
            return new ImportUsersCsvResponse(0,0,1,
            List.of("O arquivo CSV está vazio."));
        }
        if (file.getOriginalFilename() == null ||
                !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".csv")) {

            return new ImportUsersCsvResponse(
                    0,
                    0,
                    1,
                    List.of("Formato de arquivo inválido. Envie um arquivo .csv.")
            );
        }
        int totalLines = 0;
        int registeredUsers = 0;

        List<String> errors = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (CSVParser parser = CSVParser.parse(
                file.getInputStream(),
                StandardCharsets.UTF_8,
                format)) {
            Set<String> expectedHeaders = Set.of("name", "email", "cpf", "password");
            List<String> actualHeaders = parser.getHeaderNames();
            if (actualHeaders.size() != expectedHeaders.size()
                    || !Set.copyOf(actualHeaders).equals(expectedHeaders)) {
                errors.add("Cabeçalho inválido. Esperado: name, email, cpf, password.");
                return new ImportUsersCsvResponse(
                        totalLines, registeredUsers, errors.size(), errors);
            }
            for (CSVRecord record : parser) {
                totalLines++;
                if (!record.isConsistent()) {
                    errors.add("Linha " + (record.getRecordNumber() +1)
                        + ": quantidade de colunas inválida. Esperadas: 4.");
                    continue;
                }
                CsvUserData data = new CsvUserData(
                        record.get("name"),
                        record.get("email"),
                        record.get("cpf"),
                        record.get("password")
                );

                List<String> validationErrors = validate(data);
                if (!validationErrors.isEmpty()) {
                    errors.add("Linha " + (record.getRecordNumber() + 1)
                            + ": " + String.join(", ", validationErrors));
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
                    errors.add("Linha " + (record.getRecordNumber() + 1)
                            + ": " + exception.getMessage());
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
