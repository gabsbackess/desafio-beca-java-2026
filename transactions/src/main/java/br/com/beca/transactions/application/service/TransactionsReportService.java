package br.com.beca.transactions.application.service;

import br.com.beca.transactions.application.port.in.GenerateTransactionsReportUseCase;
import br.com.beca.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.transactions.domain.model.Category;
import br.com.beca.transactions.domain.model.Currency;
import br.com.beca.transactions.domain.model.ExchangeRate;
import br.com.beca.transactions.domain.model.Transaction;
import br.com.beca.transactions.domain.model.TransactionStatus;
import br.com.beca.transactions.domain.model.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Relatório CSV pensado para abrir direto no Excel em português:
 * separador ponto e vírgula, vírgula decimal, datas dd/MM/aaaa e linhas terminadas em CRLF.
 * O BOM UTF-8 (para os acentos) é acrescentado na hora de montar o arquivo, no controller.
 */
@Service
public class TransactionsReportService implements GenerateTransactionsReportUseCase {

    static final String SEPARATOR = ";";
    static final String NEW_LINE = "\r\n";
    static final String HEADER = String.join(SEPARATOR,
            "data", "tipo", "categoria", "descricao", "valor_brl", "status", "motivo", "valor_usd", "valor_eur");

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Map<TransactionType, String> TYPES = Map.of(
            TransactionType.DEPOSIT, "Depósito",
            TransactionType.WITHDRAWAL, "Retirada",
            TransactionType.TRANSFER, "Transferência",
            TransactionType.PURCHASE, "Compra");

    private static final Map<Category, String> CATEGORIES = Map.of(
            Category.FOOD, "Alimentação",
            Category.TRANSPORT, "Transporte",
            Category.HOUSING, "Moradia",
            Category.HEALTH, "Saúde",
            Category.EDUCATION, "Educação",
            Category.LEISURE, "Lazer",
            Category.SHOPPING, "Compras",
            Category.OTHER, "Outros");

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final Clock clock;

    public TransactionsReportService(TransactionRepositoryPort transactionRepositoryPort, Clock clock) {
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.clock = clock;
    }

    @Override
    public String generateCsv(UUID userId, LocalDate from, LocalDate to) {
        ReportPeriod period = ReportPeriod.resolve(from, to, clock);
        List<Transaction> transactions =
                transactionRepositoryPort.findByUser(userId, period.from(), period.to(), null).stream()
                        .sorted(Comparator.comparing(Transaction::getCreatedAt))
                        .toList();

        StringBuilder csv = new StringBuilder(HEADER).append(NEW_LINE);
        for (Transaction transaction : transactions) {
            csv.append(row(transaction)).append(NEW_LINE);
        }
        return csv.toString();
    }

    private static String row(Transaction transaction) {
        return String.join(SEPARATOR,
                transaction.getCreatedAt().format(DATE_TIME),
                TYPES.get(transaction.getType()),
                CATEGORIES.get(transaction.getCategory()),
                text(transaction.getDescription()),
                money(transaction.getAmount()),
                transaction.getStatus() == TransactionStatus.APPROVED ? "Aprovada" : "Rejeitada",
                text(transaction.getRejectionReason()),
                converted(transaction, Currency.USD),
                converted(transaction, Currency.EUR));
    }

    private static String converted(Transaction transaction, Currency currency) {
        return transaction.getExchangeRates().stream()
                .filter(rate -> rate.currency() == currency)
                .findFirst()
                .map(rate -> money(rate.convert(transaction.getAmount())))
                .orElse("");
    }

    /** Valor com vírgula decimal e sem separador de milhar: 1234,50. */
    static String money(BigDecimal value) {
        return value.toPlainString().replace('.', ',');
    }

    /**
     * Texto livre vindo do usuário. Aspas quando tem separador, aspas ou quebra de linha, e um apóstrofo na frente
     * quando começa com = + - @, para o Excel não executar como fórmula.
     */
    static String text(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String safe = value;
        char first = safe.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            safe = "'" + safe;
        }
        if (safe.contains(SEPARATOR) || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
