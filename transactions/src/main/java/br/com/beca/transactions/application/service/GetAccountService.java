package br.com.beca.transactions.application.service;

import br.com.beca.transactions.application.port.in.GetAccountUseCase;
import br.com.beca.transactions.application.port.out.AccountLimitsPort;
import br.com.beca.transactions.domain.model.AccountLimits;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetAccountService implements GetAccountUseCase {

    private final AccountLimitsPort accountLimitsPort;

    public GetAccountService(AccountLimitsPort accountLimitsPort) {
        this.accountLimitsPort = accountLimitsPort;
    }

    @Override
    public AccountLimits getAccount(UUID userId) {
        return accountLimitsPort.findByUserId(userId);
    }
}
