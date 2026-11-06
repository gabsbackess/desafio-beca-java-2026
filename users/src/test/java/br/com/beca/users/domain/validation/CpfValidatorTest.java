package br.com.beca.users.domain.validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CpfValidatorTest {

    @Test
    void shouldAcceptValidCpf() {
        assertTrue(CpfValidator.isValid("52998224725"));
    }
    @Test
    void shouldAcceptFormattedCpf() {
        assertTrue(CpfValidator.isValid("529.982.247-25"));
    }
    @Test
    void shouldRejectRepeatedDigits() {
        assertFalse(CpfValidator.isValid("00000000000"));
    }
    @Test
    void shouldRejectInvalidCpf() {
        assertFalse(CpfValidator.isValid("12345678900"));
    }
    @Test
    void shouldNormalizeFormattedCpf() {
        String result = CpfValidator.normalize("529.982.247-25");
        assertEquals("52998224725", result);
    }
}
