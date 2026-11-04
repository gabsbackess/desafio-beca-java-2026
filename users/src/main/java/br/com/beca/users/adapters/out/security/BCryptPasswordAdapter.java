package br.com.beca.users.adapters.out.security;

import br.com.beca.users.application.port.out.PasswordEncoderPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordAdapter implements PasswordEncoderPort {
    private final BCryptPasswordEncoder passwordEncoder;

    public BCryptPasswordAdapter() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }
    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}


