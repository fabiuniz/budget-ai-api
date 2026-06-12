package dio.application.output;

import dio.domain.Transaction;
import java.util.List;

/**
 * Interface que define a porta de saída (Output Port) para persistência.
 * Essencial para o funcionamento do Service e da Simulação.
 */
public interface TransactionRepository {

    Transaction save(Transaction transaction);

    List<Transaction> findAll();
}