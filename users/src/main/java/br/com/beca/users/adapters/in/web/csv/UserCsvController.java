package br.com.beca.users.adapters.in.web.csv;
import br.com.beca.users.application.port.in.ImportUsersCsvResponse;
import br.com.beca.users.application.port.in.ImportUsersCsvUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/users")
@Tag(name = "Importação CSV", description = "Cadastro de usuários em massa")
public class UserCsvController {
    private final ImportUsersCsvUseCase importUsersCsvUseCase;
    public UserCsvController(ImportUsersCsvUseCase importUsersCsvUseCase) {
        this.importUsersCsvUseCase = importUsersCsvUseCase;
    }

    @Operation(summary = "Importar usuários por CSV",
            description = "Colunas obrigatórias: name, email, cpf, password (separador vírgula, UTF-8; "
                    + "o cabeçalho não diferencia maiúsculas). Máximo de 1.000 registros e 5 MB. "
                    + "Linhas inválidas ou duplicadas não interrompem a importação: voltam em 'error', "
                    + "com o número da linha e o motivo. Arquivo de exemplo: users/exemplos/usuarios-exemplo.csv.")
    @ApiResponse(responseCode = "200", description = "Importação processada (veja o resumo por linha)",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"totalLines": 3, "registeredUsers": 2, "usersError": 1,
                             "error": ["Linha 3: email: E-mail inválido."]}
                            """)))
    @ApiResponse(responseCode = "400",
            description = "Arquivo vazio, cabeçalho inválido, CSV malformado ou mais de 1.000 registros",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"status": 400, "message": "Cabeçalho inválido. Esperado: name, email, cpf, password."}
                            """)))
    @ApiResponse(responseCode = "413", description = "Arquivo maior que 5 MB",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"status": 413, "message": "O arquivo enviado ultrapassa o limite permitido de 5 MB."}
                            """)))
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ImportUsersCsvResponse> importUsers(
            @Parameter(description = "Arquivo CSV com as colunas name,email,cpf,password")
            @RequestPart("file") MultipartFile file) throws IOException {
        ImportUsersCsvResponse response =
                importUsersCsvUseCase.importUsers(file.getBytes());
        return ResponseEntity.ok(response);
    }
}
