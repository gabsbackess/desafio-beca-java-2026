package br.com.beca.transactions.domain.model;

import br.com.beca.transactions.support.Ids;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionPolicyTest {

    private final TransactionPolicy policy = new TransactionPolicy();

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static TransactionRequest request(TransactionType type, String amount) {
        Category category = type == TransactionType.PURCHASE ? Category.FOOD : null;
        UUID destination = type == TransactionType.TRANSFER ? Ids.OTHER_USER : null;
        return new TransactionRequest(Ids.USER, type, bd(amount), category, "teste", destination);
    }

    @Test
    void shouldAlwaysApproveDeposit() {
        AccountLimits limits = new AccountLimits(bd("0"), bd("1"), bd("1"));

        ApprovalDecision decision = policy.decide(request(TransactionType.DEPOSIT, "5000"), limits,
                bd("9999"));

        assertTrue(decision.isApproved());
        assertNull(decision.rejectionReason());
    }

    @Test
    void shouldApproveWithdrawalWithinBalanceAndLimits() {
        AccountLimits limits = new AccountLimits(bd("1000"), bd("500"), bd("800"));

        ApprovalDecision decision = policy.decide(request(TransactionType.WITHDRAWAL, "100"), limits,
                bd("0"));

        assertEquals(TransactionStatus.APPROVED, decision.status());
    }

    @Test
    void shouldRejectWithdrawalAboveBalance() {
        AccountLimits limits = new AccountLimits(bd("100"), bd("1000"), bd("1000"));

        ApprovalDecision decision = policy.decide(request(TransactionType.WITHDRAWAL, "150"), limits,
                bd("0"));

        assertEquals(TransactionStatus.REJECTED, decision.status());
        assertEquals(TransactionPolicy.INSUFFICIENT_BALANCE, decision.rejectionReason());
    }

    @Test
    void shouldRejectTransferAboveBalance() {
        AccountLimits limits = new AccountLimits(bd("100"), bd("1000"), bd("1000"));

        ApprovalDecision decision = policy.decide(request(TransactionType.TRANSFER, "100.01"), limits,
                bd("0"));

        assertEquals(TransactionPolicy.INSUFFICIENT_BALANCE, decision.rejectionReason());
    }

    @Test
    void shouldRejectPurchaseAbovePerTransactionLimit() {
        AccountLimits limits = new AccountLimits(bd("1000"), bd("500"), bd("2000"));

        ApprovalDecision decision = policy.decide(request(TransactionType.PURCHASE, "600"), limits,
                bd("0"));

        assertEquals(TransactionStatus.REJECTED, decision.status());
        assertEquals(TransactionPolicy.PER_TRANSACTION_LIMIT_EXCEEDED, decision.rejectionReason());
    }

    @Test
    void shouldRejectWhenDailyLimitWouldBeExceeded() {
        AccountLimits limits = new AccountLimits(bd("1000"), bd("500"), bd("800"));

        ApprovalDecision decision = policy.decide(request(TransactionType.PURCHASE, "150"), limits,
                bd("700"));

        assertEquals(TransactionStatus.REJECTED, decision.status());
        assertEquals(TransactionPolicy.DAILY_LIMIT_EXCEEDED, decision.rejectionReason());
    }

    @Test
    void shouldApproveWhenAmountEqualsBalance() {
        AccountLimits limits = new AccountLimits(bd("100"), bd("500"), bd("800"));

        ApprovalDecision decision = policy.decide(request(TransactionType.WITHDRAWAL, "100"), limits,
                bd("0"));

        assertTrue(decision.isApproved());
    }

    @Test
    void shouldApproveWhenAmountEqualsPerTransactionLimit() {
        AccountLimits limits = new AccountLimits(bd("1000"), bd("500"), bd("800"));

        ApprovalDecision decision = policy.decide(request(TransactionType.PURCHASE, "500"), limits,
                bd("0"));

        assertTrue(decision.isApproved());
    }

    @Test
    void shouldApproveWhenDailyTotalEqualsDailyLimit() {
        AccountLimits limits = new AccountLimits(bd("1000"), bd("500"), bd("800"));

        ApprovalDecision decision = policy.decide(request(TransactionType.PURCHASE, "100"), limits,
                bd("700"));

        assertTrue(decision.isApproved());
    }

    @Test
    void shouldReportBalanceFirstWhenSeveralRulesAreBroken() {
        AccountLimits limits = new AccountLimits(bd("10"), bd("20"), bd("30"));

        ApprovalDecision decision = policy.decide(request(TransactionType.PURCHASE, "500"), limits,
                bd("100"));

        assertEquals(TransactionPolicy.INSUFFICIENT_BALANCE, decision.rejectionReason());
    }
}
