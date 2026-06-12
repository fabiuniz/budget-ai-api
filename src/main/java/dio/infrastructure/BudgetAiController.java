package dio.infrastructure;

import dio.application.input.TransactionService;
import dio.domain.Transaction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controlador REST (Input Adapter).
 * Expoe os endpoints HTTP da API para permitir a interacao com o sistema.
 */
@RestController
@RequestMapping("/api/budget")
public class BudgetAiController {

    private final BudgetAiEngine aiEngine;
    private final TransactionService transactionService;

    // Injeção automática via construtor efetuada pelo Spring
    public BudgetAiController(BudgetAiEngine aiEngine, TransactionService transactionService) {
        this.aiEngine = aiEngine;
        this.transactionService = transactionService;
    }

    /**
     * Endpoint para receber um arquivo de áudio e processá-lo com IA.
     * POST http://localhost:8080/api/budget/voice
     */
    @PostMapping("/voice")
    public ResponseEntity<String> enviarComandoVoz(@RequestParam("file") String nomeArquivo) {
        System.out.println("[Controller] HTTP POST recebido para processamento de voz: " + nomeArquivo);

        // 1. Transcreve o áudio para texto
        String textoTranscrevido = aiEngine.transcreverAudio(nomeArquivo);

        // 2. Processa as regras de negócio e executa o Tool Calling
        aiEngine.processarIntencaoEToolCalling(textoTranscrevido);

        return ResponseEntity.ok("Áudio '" + nomeArquivo + "' processado com sucesso e registrado via IA!");
    }

    /**
     * Endpoint para listar todo o extrato consolidado.
     * GET http://localhost:8080/api/budget/transactions
     */
    @GetMapping("/transactions") // <-- Corrigido para "G" maiúsculo aqui!
    public ResponseEntity<List<Transaction>> listarExtrato() {
        System.out.println("[Controller] HTTP GET recebido para listar extrato.");
        List<Transaction> transacoes = transactionService.listarTodas();
        return ResponseEntity.ok(transacoes);
    }
}