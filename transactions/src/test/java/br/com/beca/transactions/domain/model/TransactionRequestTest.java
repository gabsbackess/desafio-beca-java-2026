package br.com.beca.transactions.domain.model;

import br.com.beca.transactions.support.Ids;
import java.util.UUID;
import br.com.beca.transactions.domain.exception.InvalidTransactionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionRequestTest {

    private TransactionRequest request(TransactionType type, String amount,
                                       Category category, UUID destinationUserId) {
        return new TransactionRequest(Ids.USER, type, new BigDecimal(amount), category, "descricao",
                destinationUserId);
    }

    @Test
    void shouldNormalizeAmountToTwoDecimals() {
        TransactionRequest request = request(TransactionType.DEPOSIT, "10.5", null, null);

        assertEquals(new BigDecimal("10.50"), request.amount());
    }

    @Test
    void shouldDefaultCategoryToOtherWhenNotPurchase() {
        TransactionRequest request = request(TransactionType.WITHDRAWAL, "10", null, null);

        assertEquals(Category.OTHER, request.category());
    }

    @Test
    void shouldKeepCategoryWhenInformed() {
        TransactionRequest request = request(TransactionType.PURCHASE, "10", Category.FOOD, null);

        assertEquals(Category.FOOD, request.category());
    }

    @Test
    void shouldTrimDescriptionAndAcceptNull() {
        TransactionRequest trimmed = new TransactionRequest(
                Ids.USER, TransactionType.DEPOSIT, new BigDecimal("10"), null, "  salario  ", null);
        TransactionRequest withoutDescription = new TransactionRequest(
                Ids.USER, TransactionType.DEPOSIT, new BigDecimal("10"), null, null, null);

        assertEquals("salario", trimmed.description());
        assertEquals("", withoutDescription.description());
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThrows(InvalidTransactionException.class,
                () -> request(TransactionType.DEPOSIT, "0", null, null));
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(InvalidTransactionException.class,
                () -> request(TransactionType.DEPOSIT, "-5", null, null));
    }

    @Test
    void shouldRejectNullAmount() {
        assertThrows(InvalidTransactionException.class,
                () -> new TransactionRequest(Ids.USER, TransactionType.DEPOSIT, null, null, "x", null));
    }

    @Test
    void shouldRejectMoreThanTwoDecimalPlaces() {
        assertThrows(InvalidTransactionException.class,
                () -> request(TransactionType.DEPOSIT, "10.005", null, null));
    }

    @Test
    void shouldAcceptTrailingZerosBeyondTwoDecimalPlaces() {
        TransactionRequest request = request(TransactionType.DEPOSIT, "10.500", null, null);

        assertEquals(new BigDecimal("10.50"), request.amount());
    }

    @Test
    void shouldRejectNullUser() {
        assertThrows(InvalidTransactionException.class,
                () -> new TransactionRequest(null, TransactionType.DEPOSIT, new BigDecimal("10"),
                        null, "x", null));
    }

    @Test
    void shouldRejectNullType() {
        assertThrows(InvalidTransactionException.class,
                () -> new TransactionRequest(Ids.USER, null, new BigDecimal("10"), null, "x", null));
    }

    @Test
    void shouldRequireDestinationOnTransfer() {
        assertThrows(InvalidTransactionException.class,
                () -> request(TransactionType.TRANSFER, "10", null, null));
    }

    @Test
    void shouldRejectTransferToSameUser() {
        assertThrows(InvalidTransactionException.class,
                () -> request(TransactionType.TRANSFER, "10", null, Ids.USER));
    }

    @Test
    void shouldAcceptTransferToAnotherUser() {
        TransactionRequest request = request(TransactionType.TRANSFER, "10", null, Ids.OTHER_USER);

        assertEquals(Ids.OTHER_USER, request.destinationUserId());
    }

    @Test
    void shouldRejectDestinationOutsideTransfer() {
        assertThrows(InvalidTransactionException.class,
                () -> request(TransactionType.WITHDRAWAL, "10", null, Ids.OTHER_USER));
    }

    @Test
    void shouldRequireCategoryOnPurchase() {
        assertThrows(InvalidTransactionException.class,
                () -> request(TransactionType.PURCHASE, "10", null, null));
    }
}
