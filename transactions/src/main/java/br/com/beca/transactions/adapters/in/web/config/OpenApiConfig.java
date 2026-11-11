package br.com.beca.transactions.adapters.in.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API de Transações (MS2)")
                .version("1.0")
                .description("Registro de transações financeiras, saldo e limites."));
    }
}
