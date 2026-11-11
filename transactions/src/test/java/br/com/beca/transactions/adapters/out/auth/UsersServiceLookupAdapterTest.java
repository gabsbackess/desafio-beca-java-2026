package br.com.beca.transactions.adapters.out.auth;

import br.com.beca.transactions.domain.exception.UserServiceUnavailableException;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UsersServiceLookupAdapterTest {

    private MockRestServiceServer server;
    private UsersServiceLookupAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://users");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new UsersServiceLookupAdapter(builder.build());
    }

    @Test
    void shouldReturnTrueWhenTheUserExists() {
        server.expect(requestTo("http://users/users/" + Ids.OTHER_USER))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"id\":\"" + Ids.OTHER_USER + "\"}", MediaType.APPLICATION_JSON));

        assertTrue(adapter.exists(Ids.OTHER_USER));
        server.verify();
    }

    @Test
    void shouldReturnFalseWhenTheUserDoesNotExist() {
        server.expect(requestTo("http://users/users/" + Ids.UNKNOWN))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertFalse(adapter.exists(Ids.UNKNOWN));
    }

    @Test
    void shouldThrowUnavailableWhenTheUsersServiceFails() {
        server.expect(requestTo("http://users/users/" + Ids.OTHER_USER))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(UserServiceUnavailableException.class, () -> adapter.exists(Ids.OTHER_USER));
    }
}
