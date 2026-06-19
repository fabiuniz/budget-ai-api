package dio.infrastructure;

import dio.application.output.TransactionRepository;
import dio.domain.Transaction;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;

@Component
@Primary // Força o Spring Boot a priorizar este Adapter de Banco Real
@Profile("prod")
public class TransactionPostgresAdapter implements TransactionRepository {

    private final SpringPostgresRepository springRepository;

    public TransactionPostgresAdapter(SpringPostgresRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = TransactionEntity.fromDomain(transaction);
        TransactionEntity salvo = springRepository.save(entity);
        return salvo.toDomain();
    }

    @Override
    public List<Transaction> findAll() {
        return springRepository.findAll().stream()
                .map(TransactionEntity::toDomain)
                .collect(Collectors.toList());
    }
}