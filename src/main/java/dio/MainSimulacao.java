package dio;

import dio.domain.Transaction;
import dio.application.input.TransactionService;
import dio.infrastructure.TransactionInMemoryAdapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MainSimulacao {

    public static void main(String[] args) {
        System.out.println("=== Testando Adaptador de Infraestrutura Real ===");

        // 1. Instanciando o adaptador oficial de infraestrutura
        TransactionInMemoryAdapter adapter = new TransactionInMemoryAdapter();

        // 2. Injetando o adaptador no Servico (Inversao de Dependencia classica)
        TransactionService service = new TransactionService(adapter);

        // 3. Criando transacoes sem ID (o adaptador deve gerar o ID incremental automaticamente)
        Transaction t1 = new Transaction(null, "Gasolina no Posto Shell", new BigDecimal("220.00"), "EXPENSE", LocalDateTime.now());
        Transaction t2 = new Transaction(null, "Pix Recebido Freela", new BigDecimal("1500.00"), "INCOME", LocalDateTime.now());

        // 4. Executando o fluxo de negocio
        service.criarTransacao(t1);
        service.criarTransacao(t2);

        // 5. Listando o resultado
        System.out.println("\n--- Estado Atual do Banco em Memoria ---");
        service.listarTodas().forEach(System.out::println);
        System.out.println("=================================================");
    }
}