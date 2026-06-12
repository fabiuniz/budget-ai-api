package dio;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import dio.application.input.TransactionService;
import dio.application.output.TransactionRepository;
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
        // Carrega as variáveis do arquivo .env para a memória do sistema
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        // --- BLOCO DEDO-DURO PARA CERTIFICAÇÃO ---
        String chaveCarregada = System.getProperty("GOOGLE_AI_KEY");
        System.out.println("\n====================================================");
        if (chaveCarregada == null || chaveCarregada.isBlank()) {
            System.err.println("[VALIDAÇÃO] ❌ ERRO CRÍTICO: A variável GOOGLE_AI_KEY está VAZIA!");
        } else {
            String inicio = chaveCarregada.substring(0, Math.min(chaveCarregada.length(), 4));
            String fim = chaveCarregada.substring(Math.max(0, chaveCarregada.length() - 4));
            System.out.println("[VALIDAÇÃO] ✅ Chave carregada com sucesso pelo Dotenv!");
            System.out.println("[VALIDAÇÃO] Início/Fim na memória: " + inicio + "..." + fim);
            System.out.println("[VALIDAÇÃO] Total de caracteres lidos: " + chaveCarregada.length());
        }
        System.out.println("====================================================\n");
        // ------------------------------------------

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