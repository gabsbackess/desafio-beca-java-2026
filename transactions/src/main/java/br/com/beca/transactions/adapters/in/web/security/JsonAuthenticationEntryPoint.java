package br.com.beca.transactions.adapters.in.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Responde 401 (ou 503, se o MS1 estiver fora do ar) no mesmo formato de erro dos serviços. */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String UNAUTHORIZED_MESSAGE = "Credenciais inválidas ou ausentes.";
    public static final String UNAVAILABLE_MESSAGE =
            "Serviço de autenticação indisponível no momento. Tente novamente mais tarde.";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        boolean unavailable = exception instanceof AuthenticationServiceException;
        int status = unavailable ? HttpServletResponse.SC_SERVICE_UNAVAILABLE : HttpServletResponse.SC_UNAUTHORIZED;
        if (!unavailable) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"transactions\"");
        }
        write(response, status, unavailable ? UNAVAILABLE_MESSAGE : UNAUTHORIZED_MESSAGE);
    }

    static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
                ("{\"status\":" + status + ",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
    }
}
