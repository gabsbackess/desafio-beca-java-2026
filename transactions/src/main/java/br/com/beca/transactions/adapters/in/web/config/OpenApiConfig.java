package br.com.beca.transactions.adapters.in.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BASIC = "basicAuth";

    @Bean
    public OpenAPI transactionsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Transações (MS2)")
                        .version("1.0")
                        .description("Transações financeiras, saldo e limites e câmbio. "
                                + "Use o botão Authorize com o e-mail e a senha de um usuário do MS1."))
                .components(new Components().addSecuritySchemes(BASIC,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC));
    }
}
