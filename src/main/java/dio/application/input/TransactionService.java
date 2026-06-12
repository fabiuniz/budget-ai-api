package dio.application.input;

import dio.domain.Transaction;
import dio.application.output.TransactionRepository;
import java.util.List;

/**
 * Servico de Aplicacao (Use Case / Caso de Uso).
 * Responsavel por orquestrar a logica de negocio de criacao e listagem
 * de transacoes. Ele nao sabe quem o chamou (pode ser um Controller REST
 * ou uma ferramenta de Tool Calling da IA).
 */
public class TransactionService {

    // Dependencia da nossa porta de saida isolada por inversao de controle
    private final TransactionRepository transactionRepository;

    // Construtor para injetar a dependencia manualmente ou via Spring
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Executa a regra para registrar uma nova transacao financeira.
     */
    public Transaction criarTransacao(Transaction transaction) {
        // Aqui poderiam entrar validacoes de saldo ou limites de auditoria
        System.out.println("[Service] Processando regra de negocio para: " + transaction.getDescription());
        return transactionRepository.save(transaction);
    }

    /**
     * Recupera o historico financeiro para consolidacao de dados.
     */
    public List<Transaction> listarTodas() {
        return transactionRepository.findAll();
    }
}