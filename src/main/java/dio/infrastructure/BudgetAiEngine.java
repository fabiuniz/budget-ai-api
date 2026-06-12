package dio.infrastructure;

import dio.application.input.TransactionService;
import dio.domain.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Simulador do Motor de Inteligencia Artificial (AI Engine).
 * Replica o comportamento do Spring AI (Speech-to-Text + Tool Calling)
 * processando comandos de voz ou texto e mapeando para o nosso Service.
 */
@Component
public class BudgetAiEngine {

    private final TransactionService transactionService;

    // O Spring injeta automaticamente o TransactionService aqui
    public BudgetAiEngine(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Simula a transcricao de um arquivo de audio (Speech-to-Text)
     */
    public String transcreverAudio(String nomeArquivoAudio) {
        System.out.println("[AI Engine] Recebendo arquivo de audio: " + nomeArquivoAudio);
        System.out.println("[AI Engine] Simulando processamento via modelo OpenAI Whisper...");

        // Simula comandos com base no nome do arquivo enviado para fins didaticos
        if (nomeArquivoAudio.contains("gasto_cafe")) {
            return "Comprei um cafe gourmet por dezoito reais e cinquenta centavos";
        } else if (nomeArquivoAudio.contains("recebi_salario")) {
            return "Recebi meu salario de cinco mil reais";
        }

        return "Gastei trinta e cinco reais com Uber";
    }

    /**
     * Simula o Processamento de Linguagem Natural (LLM) extraindo dados
     * textuais estruturados e disparando a funcao real do sistema (Tool Calling).
     */
    public void processarIntencaoEToolCalling(String textoTranscrevido) {
        System.out.println("[AI Engine] Analisando texto: \"" + textoTranscrevido + "\"");
        System.out.println("[AI Engine] Executando Named Entity Recognition (NER) para extrair parametros...");

        Transaction transaction = new Transaction();
        transaction.setCreatedAt(LocalDateTime.now());

        // Logica de interpretacao semantica simulada
        if (textoTranscrevido.toLowerCase().contains("cafe")) {
            transaction.setDescription("Café Gourmet (Via Voz)");
            transaction.setAmount(new BigDecimal("18.50"));
            transaction.setType("EXPENSE");
        } else if (textoTranscrevido.toLowerCase().contains("salario")) {
            transaction.setDescription("Salário Mensal (Via Voz)");
            transaction.setAmount(new BigDecimal("5000.00"));
            transaction.setType("INCOME");
        } else {
            transaction.setDescription("Gasto Generico (Via Voz)");
            transaction.setAmount(new BigDecimal("35.00"));
            transaction.setType("EXPENSE");
        }

        System.out.println("[AI Engine] [Tool Calling] Gatilho acionado! Chamando TransactionService...");

        // Aqui acontece a magica: a IA decide disparar a nossa regra de negocio do Java
        Transaction resultado = transactionService.criarTransacao(transaction);
        System.out.println("[AI Engine] Fluxo finalizado com sucesso. Registro ID: " + resultado.getId());
    }
}