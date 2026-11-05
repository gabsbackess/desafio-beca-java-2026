package br.com.beca.users.application.port.in;

import org.springframework.web.multipart.MultipartFile;

public interface ImportUsersCsvUseCase {
    ImportUsersCsvResponse importUsers(MultipartFile file);
}
