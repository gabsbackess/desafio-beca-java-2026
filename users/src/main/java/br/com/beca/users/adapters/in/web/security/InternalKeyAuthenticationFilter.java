package br.com.beca.users.adapters.in.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Autentica o MS2 (outro serviço, não uma pessoa) pelo cabeçalho {@code X-Internal-Key}.
 * Quem chega com a chave certa recebe só o papel SERVICE, que vale apenas para {@code GET /users/{id}}
 * (o MS2 confere se o destino de uma transferência existe). Com a chave vazia no ambiente, o acesso fica desligado.
 */
public class InternalKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Key";

    private final byte[] expectedKey;

    public InternalKeyAuthenticationFilter(String expectedKey) {
        this.expectedKey = expectedKey == null ? new byte[0] : expectedKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String received = request.getHeader(HEADER);
        if (expectedKey.length > 0 && received != null
                && MessageDigest.isEqual(expectedKey, received.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(
                            "internal-service", null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))));
        }
        chain.doFilter(request, response);
    }
}
