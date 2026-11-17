package br.com.beca.users.adapters.in.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Responde 401 no mesmo formato de erro dos serviços: {"status", "message"}. */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String UNAUTHORIZED_MESSAGE = "Credenciais inválidas ou ausentes.";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"users\"");
        write(response, HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
    }

    static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
                ("{\"status\":" + status + ",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
    }
}
