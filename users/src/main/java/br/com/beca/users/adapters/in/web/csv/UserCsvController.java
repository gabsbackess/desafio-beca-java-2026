package br.com.beca.users.adapters.in.web.csv;

import br.com.beca.users.application.port.in.ImportUsersCsvResponse;
import br.com.beca.users.application.port.in.ImportUsersCsvUseCase;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
public class UserCsvController {

    private final ImportUsersCsvUseCase importUsersCsvUseCase;

    public UserCsvController(ImportUsersCsvUseCase importUsersCsvUseCase) {
        this.importUsersCsvUseCase = importUsersCsvUseCase;
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ImportUsersCsvResponse> importUsers(
            @RequestPart("file") MultipartFile file) {


        ImportUsersCsvResponse response =
                importUsersCsvUseCase.importUsers(file);

        return ResponseEntity.ok(response);
    }
}






