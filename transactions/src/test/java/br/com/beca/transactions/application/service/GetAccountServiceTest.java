package br.com.beca.transactions.application.service;

import br.com.beca.transactions.application.port.out.AccountLimitsPort;
import br.com.beca.transactions.domain.exception.AccountServiceUnavailableException;
import br.com.beca.transactions.domain.model.AccountLimits;
import br.com.beca.transactions.support.Ids;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAccountServiceTest {

    @Mock
    private AccountLimitsPort accountLimitsPort;
    @InjectMocks
    private GetAccountService service;

    @Test
    void shouldReturnLimitsFromAccountPort() {
        AccountLimits limits = new AccountLimits(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN);
        when(accountLimitsPort.findByUserId(Ids.USER)).thenReturn(limits);

        assertEquals(limits, service.getAccount(Ids.USER));
    }

    @Test
    void shouldPropagateUnavailableError() {
        when(accountLimitsPort.findByUserId(Ids.USER)).thenThrow(new AccountServiceUnavailableException("x"));

        assertThrows(AccountServiceUnavailableException.class, () -> service.getAccount(Ids.USER));
    }
}
