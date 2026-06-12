package dio.infrastructure;

import dio.domain.Transaction;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private BigDecimal amount;
    private String type;
    private LocalDateTime createdAt;

    public TransactionEntity() {}

    // Converte Domínio Puro -> Entidade do Banco
    public static TransactionEntity fromDomain(Transaction domain) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = domain.getId();
        entity.description = domain.getDescription();
        entity.amount = domain.getAmount();
        entity.type = domain.getType();
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }

    // Converte Entidade do Banco -> Domínio Puro
    public Transaction toDomain() {
        return new Transaction(this.id, this.description, this.amount, this.type, this.createdAt);
    }

    // Getters e Setters básicos
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getType() { return type; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}