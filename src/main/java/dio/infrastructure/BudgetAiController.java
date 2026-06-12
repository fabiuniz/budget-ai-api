package dio.infrastructure;

import dio.application.input.TransactionService;
import dio.domain.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

import dio.domain.DashboardReport;

@RestController
@RequestMapping("/api/budget")
@CrossOrigin(origins = "*")
public class BudgetAiController {

    private final BudgetAiEngine aiEngine;
    private final TransactionService transactionService;

    // Pasta onde os áudios reais enviados via cURL/Upload vão ficar salvos fisicamente no seu Linux
    private static final String UPLOAD_DIR = "/home/userlnx/docker/script_docker/java-ia/budget-ai-api/uploads/";

    public BudgetAiController(BudgetAiEngine aiEngine, TransactionService transactionService) {
        this.aiEngine = aiEngine;
        this.transactionService = transactionService;
    }

    @PostMapping("/voice")
    public ResponseEntity<String> processVoice(@RequestParam("file") MultipartFile file) {
        System.out.println("[API-POST] Upload de áudio detectado: " + file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Erro: O arquivo de áudio enviado está vazio.");
        }

        try {
            // Garante que a pasta 'uploads/' exista no disco rígido
            File pastaDestino = new File(UPLOAD_DIR);
            if (!pastaDestino.exists()) {
                pastaDestino.mkdirs();
            }

            // Grava o arquivo físico de áudio .mp3 no disco do Linux
            File arquivoNoDisco = new File(UPLOAD_DIR + file.getOriginalFilename());
            file.transferTo(arquivoNoDisco);
            System.out.println("[API-POST] Arquivo gravado com sucesso no disco em: " + arquivoNoDisco.getAbsolutePath());

            // Envia o arquivo gravado para o processamento real da inteligência artificial
            String resultadoIA = aiEngine.processarAudioEIntencaoReal(arquivoNoDisco);
            return ResponseEntity.ok(resultadoIA);

        } catch (IOException e) {
            System.err.println("[API-POST] Falha crítica de I/O ao salvar no disco: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno ao gravar arquivo no disco rígido do servidor.");
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        System.out.println("[API-GET] Listando transações.");
        return ResponseEntity.ok(transactionService.listarTodas());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardReport> getDashboard() {
        System.out.println("[API-GET] Requisição recebida para o painel de controle.");
        DashboardReport relatorio = transactionService.obterRelatorioDashboard();
        return ResponseEntity.ok(relatorio);
    }
}