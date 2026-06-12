package dio.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade de Domínio que representa uma transação financeira no sistema.
 * Mantida limpa de anotações complexas de frameworks nesta etapa inicial
 * para focar na regra de negócio pura (Clean Architecture).
 */
public class Transaction {

    private Long id;
    private String description; // Descrição da transação (ex: "Compra de café")
    private BigDecimal amount;  // Valor monetário da movimentação
    private String type;        // Tipo da transação: "EXPENSE" (Despesa) ou "INCOME" (Receita)
    private LocalDateTime createdAt;

    // Construtor padrão
    public Transaction() {
    }

    // Construtor completo
    public Transaction(Long id, String description, BigDecimal amount, String type, LocalDateTime createdAt) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.createdAt = createdAt;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", type='" + type + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}