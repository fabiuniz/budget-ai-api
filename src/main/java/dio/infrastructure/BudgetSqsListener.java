package dio.infrastructure;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;
import java.io.File;

@Component
public class BudgetSqsListener {

    private final BudgetAiEngine aiEngine;

    public BudgetSqsListener(BudgetAiEngine aiEngine) {
        this.aiEngine = aiEngine;
    }

    @SqsListener("fila-audios-processar")
    public void receberMensagem(String caminhoArquivo) {
        System.out.println("[SQS-LISTENER] Mensagem capturada da fila! Iniciando consumo...");
        System.out.println("[SQS-LISTENER] Arquivo a ser processado: " + caminhoArquivo);

        try {
            File arquivoAudio = new File(caminhoArquivo);
            if (arquivoAudio.exists()) {
                // Dispara o motor que você já tem pronto para chamar o Gemini e o Banco
                String resultado = aiEngine.processarAudioEIntencaoReal(arquivoAudio);
                System.out.println("[SQS-LISTENER] Resultado do processamento: " + resultado);
            } else {
                System.err.println("[SQS-LISTENER] Erro: Arquivo físico não foi encontrado no disco: " + caminhoArquivo);
            }
        } catch (Exception e) {
            System.err.println("[SQS-LISTENER] Falha crítica ao processar mensagem da fila: " + e.getMessage());
        }
    }
}