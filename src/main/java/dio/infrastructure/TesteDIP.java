package dio.infrastructure;
import dio.domain.Transaction;

// 1. A ABSTRAÇÃO (Contrato exclusivo do teste para não duplicar com o real)
interface TransactionRepositoryDIP{
    void salvar(Transaction transaction);
}

class TransactionServiceDIP {
    private final TransactionRepositoryDIP repository;

    public TransactionServiceDIP (TransactionRepositoryDIP repository) {
        this.repository = repository;
    }
    public void executar(Transaction t){
        repository.salvar(t);
    }
}

class TransactionPostGresAdapterDIP implements TransactionRepositoryDIP{
    @Override
    public void salvar(Transaction transaction){
        System.out.println("fasfsad.....");
    }

}
public class TesteDIP {
    public static void main(String[] args) {
        // 1. Criamos o adaptador (a infraestrutura/banco)
        TransactionRepositoryDIP bancoFalso = new TransactionPostGresAdapterDIP();

        // 2. FAZEMOS A INJEÇÃO DE DEPENDÊNCIA:
        // Passamos o banco direto no construtor do TransactionServiceDIP
        TransactionServiceDIP servico = new TransactionServiceDIP(bancoFalso);

        // 3. Criamos uma transação qualquer e mandamos executar
        Transaction transacao = new Transaction();
        servico.executar(transacao);
    }
}