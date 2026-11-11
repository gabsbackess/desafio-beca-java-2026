package br.com.beca.transactions.adapters.out.account;

import br.com.beca.transactions.domain.exception.AccountNotFoundException;
import br.com.beca.transactions.domain.exception.AccountServiceUnavailableException;
import br.com.beca.transactions.domain.model.AccountLimits;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;

class MockApiAccountAdapterTest {

    private MockRestServiceServer server;
    private MockApiAccountAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://mock");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new MockApiAccountAdapter(builder.build());
    }

    @Test
    void shouldReadBalanceAndLimitsFromMockApi() {
        server.expect(requestTo("http://mock/accounts?userId=" + Ids.USER))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        [{"id":"1","userId":"%s","balance":1500.50,"perTransactionLimit":500,"dailyLimit":1000}]
                        """.formatted(Ids.USER), MediaType.APPLICATION_JSON));

        AccountLimits limits = adapter.findByUserId(Ids.USER);

        assertEquals(0, new BigDecimal("1500.50").compareTo(limits.balance()));
        assertEquals(0, new BigDecimal("500").compareTo(limits.perTransactionLimit()));
        assertEquals(0, new BigDecimal("1000").compareTo(limits.dailyLimit()));
        server.verify();
    }

    @Test
    void shouldIgnoreItemsOfOtherUsersReturnedByContainsFilter() {
        server.expect(requestTo("http://mock/accounts?userId=" + Ids.USER))
                .andRespond(withSuccess("""
                        [{"userId":"%s","balance":1,"perTransactionLimit":1,"dailyLimit":1},
                         {"userId":"%s","balance":900,"perTransactionLimit":300,"dailyLimit":600}]
                        """.formatted(Ids.OTHER_USER, Ids.USER), MediaType.APPLICATION_JSON));

        AccountLimits limits = adapter.findByUserId(Ids.USER);

        assertEquals(0, new BigDecimal("900").compareTo(limits.balance()));
    }

    @Test
    void shouldThrowAccountNotFoundWhenMockApiReturns404() {
        server.expect(requestTo("http://mock/accounts?userId=" + Ids.USER))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("Not found"));

        assertThrows(AccountNotFoundException.class, () -> adapter.findByUserId(Ids.USER));
    }

    @Test
    void shouldThrowAccountNotFoundWhenListHasNoMatchingUser() {
        server.expect(requestTo("http://mock/accounts?userId=" + Ids.USER))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThrows(AccountNotFoundException.class, () -> adapter.findByUserId(Ids.USER));
    }

    @Test
    void shouldThrowUnavailableWhenMockApiFails() {
        server.expect(requestTo("http://mock/accounts?userId=" + Ids.USER))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(AccountServiceUnavailableException.class, () -> adapter.findByUserId(Ids.USER));
    }

    @Test
    void shouldThrowUnavailableWhenAccountIsIncomplete() {
        server.expect(requestTo("http://mock/accounts?userId=" + Ids.USER))
                .andRespond(withSuccess("""
                        [{"userId":"%s","balance":100}]
                        """.formatted(Ids.USER), MediaType.APPLICATION_JSON));

        assertThrows(AccountServiceUnavailableException.class, () -> adapter.findByUserId(Ids.USER));
    }

    @Test
    void shouldUpdateTheBalanceWithAPutUsingTheItemId() {
        server.expect(requestTo("http://mock/accounts?userId=" + Ids.USER))
                .andRespond(withSuccess("""
                        [{"id":"7","userId":"%s","balance":1000,"perTransactionLimit":500,"dailyLimit":800}]
                        """.formatted(Ids.USER), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://mock/accounts/7"))
                .andExpect(method(PUT))
                .andExpect(jsonPath("$.userId").value(Ids.USER.toString()))
                .andExpect(jsonPath("$.balance").value(879.5))
                .andExpect(jsonPath("$.perTransactionLimit").value(500))
                .andExpect(jsonPath("$.dailyLimit").value(800))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        adapter.adjustBalance(Ids.USER, new BigDecimal("-120.50"));

        server.verify();
    }

    @Test
    void shouldCreditTheBalanceWhenTheDeltaIsPositive() {
        server.expect(requestTo("http://mock/accounts?userId=" + Ids.USER))
                .andRespond(withSuccess("""
                        [{"id":"7","userId":"%s","balance":100,"perTransactionLimit":500,"dailyLimit":800}]
                        """.formatted(Ids.USER), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://mock/accounts/7"))
                .andExpect(method(PUT))
                .andExpect(jsonPath("$.balance").value(150.25))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        adapter.adjustBalance(Ids.USER, new BigDecimal("50.25"));

        server.verify();
    }

    @Test
    void shouldThrowUnavailableWhenTheAccountHasNoIdToUpdate() {
        server.expect(requestTo("http://mock/accounts?userId=" + Ids.USER))
                .andRespond(withSuccess("""
                        [{"userId":"%s","balance":1000,"perTransactionLimit":500,"dailyLimit":800}]
                        """.formatted(Ids.USER), MediaType.APPLICATION_JSON));

        assertThrows(AccountServiceUnavailableException.class,
                () -> adapter.adjustBalance(Ids.USER, BigDecimal.TEN));
    }

    @Test
    void shouldThrowUnavailableWhenTheUpdateFails() {
        server.expect(requestTo("http://mock/accounts?userId=" + Ids.USER))
                .andRespond(withSuccess("""
                        [{"id":"7","userId":"%s","balance":1000,"perTransactionLimit":500,"dailyLimit":800}]
                        """.formatted(Ids.USER), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://mock/accounts/7"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(AccountServiceUnavailableException.class,
                () -> adapter.adjustBalance(Ids.USER, BigDecimal.TEN));
    }

    @Test
    void shouldThrowAccountNotFoundWhenUpdatingAUserWithoutAccount() {
        server.expect(requestTo("http://mock/accounts?userId=" + Ids.USER))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("Not found"));

        assertThrows(AccountNotFoundException.class,
                () -> adapter.adjustBalance(Ids.USER, BigDecimal.TEN));
    }
}
