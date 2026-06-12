package dio;

import dio.domain.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Classe de simulação inicial ajustada para o pacote base real do projeto.
 */
public class MainSimulacao {
    public static void main(String[] args) {
        System.out.println("=== Iniciando Simulacao do Ecossistema Java + IA ===");

        // Instanciação utilizando o caminho de pacote correto: dio.domain.Transaction
        Transaction fakeTransaction = new Transaction(
                1L,
                "Assinatura Cloud API",
                new BigDecimal("150.00"),
                "EXPENSE",
                LocalDateTime.now()
        );

        System.out.println("Objeto de dominio instanciado com sucesso:");
        System.out.println(fakeTransaction);
        System.out.println("=================================================");
    }
}