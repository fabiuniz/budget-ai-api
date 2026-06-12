package dio.infrastructure;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Componente do Spring que roda automaticamente logo apos o Spring Boot inicializar.
 * Util para testarmos nossa logica sem precisar de rotas HTTP prontas.
 */
@Component
public class RunnerTesteIa implements CommandLineRunner {

    private final BudgetAiEngine aiEngine;

    public RunnerTesteIa(BudgetAiEngine aiEngine) {
        this.aiEngine = aiEngine;
    }

    @Override
    public void run(String... args) throws Exception {
//        System.out.println("\n=================================================");
//        System.out.println("=== SIMULAÇÃO AUTOMÁTICA DO FLUXO DE VOZ + IA ===");
//        System.out.println("=================================================");
//
//         1. Simula a chegada de um arquivo de audio gravado pelo usuario
//        String audioDoUsuario = "gasto_cafe.mp3";
//        String textoConvertido = aiEngine.transcreverAudio(audioDoUsuario);
//
//         2. Passa o texto para a IA processar a intencao e salvar a transacao
//        aiEngine.processarIntencaoEToolCalling(textoConvertido);
//
//        System.out.println("=================================================\n");
    }
}