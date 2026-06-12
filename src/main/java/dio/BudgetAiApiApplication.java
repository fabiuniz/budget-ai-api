package dio;

import dio.application.input.TransactionService;
import dio.application.output.TransactionRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Classe principal de inicializacao do Spring Boot.
 * A anotacao @SpringBootApplication ativa o escaneamento automatico de componentes
 * (Component Scan) a partir do pacote 'dio'.
 */
@SpringBootApplication
public class BudgetAiApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BudgetAiApiApplication.class, args);
    }

    /**
     * Definicao de Bean para o Service de Aplicacao.
     * Como mantivemos o TransactionService limpo de anotacoes de framework
     * (Clean Architecture), nos ensinamos o Spring a instanciá-lo aqui,
     * injetando automaticamente o TransactionRepository (InMemoryAdapter) disponivel.
     */
    @Bean
    public TransactionService transactionService(TransactionRepository repository) {
        System.out.println("[Spring Container] Registrando Bean de TransactionService e injetando dependencias...");
        return new TransactionService(repository);
    }
}