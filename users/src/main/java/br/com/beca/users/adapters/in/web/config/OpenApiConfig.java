package br.com.beca.users.adapters.in.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI usersOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MS1 - API de Usuários")
                .version("0.0.1")
                .description("Cadastro, consulta, atualização, exclusão e importação em massa "
                        + "(CSV) de usuários. A senha é armazenada com BCrypt e nunca é devolvida "
                        + "nas respostas. Erros seguem o formato {\"status\", \"message\"}; "
                        + "erros de validação devolvem {\"status\", \"errors\": {campo: mensagem}}."));
    }
}
