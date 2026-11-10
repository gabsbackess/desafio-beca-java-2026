package br.com.beca.users.application.port.in;

import java.util.List;

public record ImportUsersCsvResponse (
    int totalLines,
    int registeredUsers,
    int usersError,
    List<String> error){
}
