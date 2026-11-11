package br.com.beca.transactions.adapters.out.auth;

import br.com.beca.transactions.domain.exception.AuthServiceUnavailableException;
import br.com.beca.transactions.domain.exception.InvalidCredentialsException;
import br.com.beca.transactions.domain.model.AuthenticatedUser;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UsersServiceAuthAdapterTest {

    private MockRestServiceServer server;
    private UsersServiceAuthAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://users");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new UsersServiceAuthAdapter(builder.build());
    }

    @Test
    void shouldReturnAuthenticatedUserWhenMs1AcceptsCredentials() {
        server.expect(requestTo("http://users/users/authenticate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"id":"%s","name":"Maria","email":"maria@email.com","cpf":"52998224725","createdAt":"2026-11-10T10:00:00"}
                        """.formatted(Ids.USER), MediaType.APPLICATION_JSON));

        AuthenticatedUser user = adapter.authenticate("maria@email.com", "Senha@123");

        assertEquals(Ids.USER, user.id());
        assertEquals("Maria", user.name());
        server.verify();
    }

    @Test
    void shouldThrowInvalidCredentialsWhenMs1Returns401() {
        server.expect(requestTo("http://users/users/authenticate"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(InvalidCredentialsException.class, () -> adapter.authenticate("maria@email.com", "errada"));
    }

    @Test
    void shouldThrowInvalidCredentialsWhenMs1Returns400() {
        server.expect(requestTo("http://users/users/authenticate"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(InvalidCredentialsException.class, () -> adapter.authenticate("nao-e-email", "x"));
    }

    @Test
    void shouldThrowUnavailableWhenMs1Fails() {
        server.expect(requestTo("http://users/users/authenticate"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(AuthServiceUnavailableException.class, () -> adapter.authenticate("maria@email.com", "x"));
    }
}
