package dio.infrastructure;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.io.File;

@Component
public class BudgetKafkaConsumer {

    private final BudgetAiEngine aiEngine;

    public BudgetKafkaConsumer(BudgetAiEngine aiEngine) {
        this.aiEngine = aiEngine;
    }

    @KafkaListener(topics = "finance.transaction.voice.received", groupId = "budget-ai-group")
    public void consumeVoiceEvent(String filePath) {
        System.out.println("[KAFKA CONSUMER] Evento recebido no broker: " + filePath);
        try {
            File audioFile = new File(filePath);
            if (audioFile.exists()) {
                String status = aiEngine.processarAudioEIntencaoReal(audioFile);
                System.out.println("[KAFKA PIPELINE STATUS]: " + status);
                audioFile.delete();
            }
        } catch (Exception e) {
            System.err.println("[KAFKA PIPELINE ERROR]: " + e.getMessage());
        }
    }
}