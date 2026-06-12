package dio.infrastructure;

import dio.application.output.TransactionRepository;
import dio.domain.Transaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador de Infraestrutura (Output Adapter).
 * Implementa a interface TransactionRepository gerenciando os dados em memoria.
 * A anotacao @Repository registra esta classe no container de Injecao de
 * Dependencias do Spring.
 */
@Repository
public class TransactionInMemoryAdapter implements TransactionRepository {

    // Lista thread-safe simulando a tabela do banco de dados
    private final List<Transaction> database = Collections.synchronizedList(new ArrayList<>());

    // Gerador de IDs incrementais simulando o bando (Identity)
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Transaction save(Transaction transaction) {
        if (transaction.getId() == null) {
            transaction.setId(idGenerator.getAndIncrement());
        }
        database.add(transaction);
        System.out.println("[Infrastructure] Transacao salva em memoria com ID: " + transaction.getId());
        return transaction;
    }

    @Override
    public List<Transaction> findAll() {
        System.out.println("[Infrastructure] Buscando todos os registros do banco em memoria. Total: " + database.size());
        return new ArrayList<>(database);
    }
}