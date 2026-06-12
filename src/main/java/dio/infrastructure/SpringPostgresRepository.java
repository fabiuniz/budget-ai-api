package dio.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringPostgresRepository extends JpaRepository<TransactionEntity, Long> {
}