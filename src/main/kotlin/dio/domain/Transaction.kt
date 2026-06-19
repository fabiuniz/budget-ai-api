package dio.domain // Em Java: package dio.domain; (Apenas adiciona o ponto e vírgula)

import java.math.BigDecimal // Em Java: Igual
import java.time.LocalDateTime // Em Java: Igual

// Em Java: Não existe 'data class'. Seria uma classe comum: public class Transaction {
// Sem o modificador 'data', o Java NÃO cria automaticamente os métodos toString(), equals(), hashCode() ou copy().
data class Transaction(
    // Em Java: private Long id; (Java exige ponto e vírgula e a tipagem vem antes do nome da variável)
    // O 'Long?' indica que pode ser nulo. No Java, todo Objeto (Long) pode ser nulo por padrão, sem aviso do compilador.
    var id: Long? = null, // Em Java: o valor padrão '= null' no construtor não existe. Exigiria criar múltiplos construtores (Overloading).

    // Em Java: private String description;
    var description: String? = null,

    // Em Java: private BigDecimal amount;
    var amount: BigDecimal? = null,

    // Em Java: private String type;
    var type: String? = null,

    // Em Java: private LocalDateTime createdAt;
    var createdAt: LocalDateTime? = null
)
// Em Java: Aqui começaria o "pesadelo" do código repetitivo (Boilerplate):
// 1. Um construtor vazio: public Transaction() {}
// 2. Um construtor com todos os parâmetros.
// 3. 5 métodos Getter (getId, getDescription...) e 5 métodos Setter (setId, setDescription...) porque as variáveis são 'var' (mutáveis).
// 4. Um método public boolean equals(Object o) {...} complexo para comparar os campos.
// 5. Um método public int hashCode() {...} correspondente.
// 6. Um método public String toString() {...} formatado para conseguir ler os dados no log.