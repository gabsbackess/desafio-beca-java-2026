package br.com.beca.users.application.port.in;
public interface ImportUsersCsvUseCase {
    ImportUsersCsvResponse importUsers(byte[] fileContent);
}