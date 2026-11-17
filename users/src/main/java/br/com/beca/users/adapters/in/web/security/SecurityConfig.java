package br.com.beca.users.adapters.in.web.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Autenticação básica no MS1.
 * <ul>
 *   <li>Abertas: cadastro ({@code POST /users}), importação por CSV, conferência de credenciais
 *       ({@code POST /users/authenticate}, usada pelo MS2) e a documentação (Swagger).</li>
 *   <li>Listar ({@code GET /users}), alterar e excluir exigem usuário autenticado; alterar e excluir valem só
 *       para o próprio usuário (conferido no controller).</li>
 *   <li>{@code GET /users/{id}} também aceita o MS2, pela chave interna.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   UsersAuthenticationProvider authenticationProvider,
                                                   @Value("${security.internal-key:}") String internalKey)
            throws Exception {
        JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint();
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(new InternalKeyAuthenticationFilter(internalKey), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/users", "/users/authenticate", "/users/import")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/{id}").hasAnyRole("USER", "SERVICE")
                        .anyRequest().hasRole("USER"))
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(new JsonAccessDeniedHandler()));
        return http.build();
    }
}
