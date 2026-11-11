package br.com.beca.transactions.support;

import java.util.UUID;

/** Identificadores fixos para os testes. */
public final class Ids {

    public static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID OTHER_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID TX_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    public static final UUID UNKNOWN = UUID.fromString("42424242-4242-4242-4242-424242424242");

    private Ids() {
    }
}
