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
        String nomeOriginal = file.getOriginalFilename();
        System.out.println("[API-POST] Upload de áudio detectado: " + nomeOriginal);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Erro: O arquivo de áudio enviado está vazio.");
        }

        File arquivoParaProcessar = null;
        File arquivoOriginalNoDisco = null;

        try {
            // Garante que a pasta 'uploads/' exista no disco rígido
            File pastaDestino = new File(UPLOAD_DIR);
            if (!pastaDestino.exists()) {
                pastaDestino.mkdirs();
            }

            // Salva o arquivo original vindo do front (pode ser .webm, .ogg, .mp3)
            arquivoOriginalNoDisco = new File(UPLOAD_DIR + nomeOriginal);
            file.transferTo(arquivoOriginalNoDisco);
            System.out.println("[API-POST] Arquivo original gravado: " + arquivoOriginalNoDisco.getAbsolutePath());

            // Se o arquivo NÃO for MP3, fazemos a conversão via FFmpeg
            if (nomeOriginal != null && !nomeOriginal.toLowerCase().endsWith(".mp3")) {
                System.out.println("[CONVERSOR] Detectado arquivo não-MP3. Iniciando conversão via FFmpeg...");
                
                String nomeSemExtensao = nomeOriginal.substring(0, nomeOriginal.lastIndexOf("."));
                File arquivoMp3Convertido = new File(UPLOAD_DIR + nomeSemExtensao + "_convertido.mp3");

                // Executa a conversão chamando o FFmpeg do sistema operacional
                converterParaMp3(arquivoOriginalNoDisco, arquivoMp3Convertido);
                
                // O arquivo que enviaremos para a IA será o novo MP3 gerado
                arquivoParaProcessar = arquivoMp3Convertido;
            } else {
                // Se já for MP3, segue o fluxo normal
                arquivoParaProcessar = arquivoOriginalNoDisco;
            }

            // Envia o arquivo correto (MP3) para o processamento real da IA
            String resultadoIA = aiEngine.processarAudioEIntencaoReal(arquivoParaProcessar);
            
            if (resultadoIA.startsWith("ERRO_IA:")) {
                String mensagemLimpa = resultadoIA.replace("ERRO_IA:", "").trim();
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mensagemLimpa);
            }

            return ResponseEntity.ok(resultadoIA);

        } catch (Exception e) {
            System.err.println("[API-POST] Falha crítica no processamento/conversão: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno ao processar e converter o arquivo de áudio no servidor: " + e.getMessage());
        } finally {
            // [OPCIONAL] Boa prática para não entupir o HD do servidor: limpa os arquivos temporários se necessário
            // Se quiser guardar o histórico no disco, comente as linhas abaixo
            if (arquivoOriginalNoDisco != null && arquivoOriginalNoDisco.exists() && !arquivoOriginalNoDisco.equals(arquivoParaProcessar)) {
                arquivoOriginalNoDisco.delete();
            }
        }
    }

    /**
     * Método auxiliar que invoca o FFmpeg instalado no Linux para converter qualquer áudio em MP3 padrão.
     */
    private void converterParaMp3(File input, File output) throws IOException, InterruptedException {
        // Remove o arquivo de saída se ele já existir por algum resquício de teste anterior
        if (output.exists()) {
            output.delete();
        }

        // Comando FFmpeg: -i (input), -codec:a libmp3lame (encoder de mp3), -qscale:a 2 (alta qualidade VBR)
        ProcessBuilder pb = new ProcessBuilder(
            "ffmpeg", 
            "-y", // Força a sobrescrever o arquivo de saída caso ele exista
            "-i", input.getAbsolutePath(), 
            "-vn", // Desativa processamento de vídeo (WebM tecnicamente é um contêiner de vídeo, isso ajuda o FFmpeg a focar só no áudio)
            "-acodec", "libmp3lame", 
            "-qscale:a", "2", 
            output.getAbsolutePath()
        );

        // Redireciona mensagens de erro do processo para o console do Spring Boot para debug
        pb.redirectErrorStream(true);
        Process processo = pb.start();

        // Aguarda a finalização da conversão com timeout ou até o fim do processo
        int exitCode = processo.waitFor();
        
        if (exitCode != 0) {
            throw new IOException("O FFmpeg falhou ao converter o áudio. Código de saída: " + exitCode);
        }
        
        System.out.println("[CONVERSOR] Conversão concluída com sucesso: " + output.getName());
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