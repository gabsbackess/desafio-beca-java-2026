package br.com.beca.users.validation;
import br.com.beca.users.domain.validation.CpfValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
public class CpfConstraintValidator
        implements ConstraintValidator<ValidCpf, String> {
    @Override
    public boolean isValid(
            String cpf,
            ConstraintValidatorContext context) {
        if (cpf == null || cpf.isBlank()) {
            return true;
        }
        return CpfValidator.isValid(cpf);
    }
}