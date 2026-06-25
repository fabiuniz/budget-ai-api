package dio.infrastructure;

import dio.application.input.TransactionService;
import dio.domain.Transaction;
import dio.domain.DashboardReport;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.Arrays;


@RestController
@RequestMapping("/api/budget")
@CrossOrigin(origins = "*")
public class BudgetAiController {

    private final TransactionService transactionService;
    private final SqsTemplate sqsTemplate;
    
    // Java 21: Usando Virtual Threads para que a conversão/I/O não bloqueie o Tomcat
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Caminho dinâmico para rodar em qualquer ambiente (Local, Docker, AWS ECS)
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    public BudgetAiController(TransactionService transactionService, SqsTemplate sqsTemplate) {
        this.transactionService = transactionService;
        this.sqsTemplate = sqsTemplate;
    }

    @PostMapping("/voice")
        public ResponseEntity<String> processVoice(@RequestParam("file") MultipartFile file) {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Erro: O arquivo de áudio enviado está vazio.");
            }

            try {
                // 1. Garante que o diretório principal /uploads/ existe
                Files.createDirectories(Paths.get(UPLOAD_DIR));
                
                String rawId = UUID.randomUUID().toString();
                String originalExt = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".") 
                        ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")) : ".tmp";
                
                // Salva o arquivo original temporário na raiz do /uploads/
                Path arquivoOriginal = Paths.get(UPLOAD_DIR, rawId + originalExt);
                file.transferTo(arquivoOriginal.toFile());

                // Processamento Assíncrono com Virtual Threads
                virtualThreadExecutor.submit(() -> {
                    try {
                        File arquivoFinal = arquivoOriginal.toFile();

                        // === ALTERAÇÃO AQUI: Definindo a subpasta "fila" ===
                        Path subpastaFila = Paths.get(UPLOAD_DIR, "fila");
                        // Garante que a subpasta /uploads/fila/ existe antes de converter
                        Files.createDirectories(subpastaFila); 

                        if (!originalExt.equalsIgnoreCase(".mp3")) {
                            // Modificado para salvar dentro de /uploads/fila/
                            File arquivoMp3Convertido = subpastaFila.resolve(rawId + ".mp3").toFile();
                            
                            // Executa a conversão jogando o output para dentro de /uploads/fila/
                            converterParaMp3(arquivoOriginal.toFile(), arquivoMp3Convertido);
                            arquivoFinal = arquivoMp3Convertido;
                            
                            // Limpa o arquivo original da raiz do /uploads/
                            Files.deleteIfExists(arquivoOriginal);
                        } else {
                            // Caso o arquivo JÁ SEJA MP3, movemos ele da raiz para a pasta "fila"
                            File arquivoMovido = subpastaFila.resolve(rawId + ".mp3").toFile();
                            Files.move(arquivoOriginal, arquivoMovido.toPath());
                            arquivoFinal = arquivoMovido;
                        }

                        String caminhoFinal = arquivoFinal.getAbsolutePath();
                        
                        // Envia o caminho absoluto (já dentro da subpasta fila) para o SQS
                        sqsTemplate.send(to -> to.queue("fila-audios-processar").payload(caminhoFinal));
                        System.out.println("[API-POST] Arquivo publicado no barramento de mensageria: " + caminhoFinal);

                    } catch (Exception e) {
                        System.err.println("[ASYNC-PROCESS] Erro ao converter/enfileirar áudio: " + e.getMessage());
                    }
                });

                return ResponseEntity.accepted().body("Áudio recebido com sucesso! O processamento em background foi iniciado.");

            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Erro ao manipular o sistema de arquivos: " + e.getMessage());
            }
        }

    private void converterParaMp3(File input, File output) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "ffmpeg", 
            "-y", // Força a sobrescrever o arquivo de saída caso ele exista
            "-i", input.getAbsolutePath(), 
            "-vn", // Desativa processamento de vídeo (WebM tecnicamente é um contêiner de vídeo, isso ajuda o FFmpeg a focar só no áudio)
            "-acodec", "libmp3lame", 
            "-qscale:a", "2", 
            output.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process processo = pb.start();
        int exitCode = processo.waitFor();
        
        if (exitCode != 0) {
            throw new IOException("O FFmpeg falhou ao converter o áudio. Código de saída: " + exitCode);
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.listarTodas());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardReport> getDashboard() {
        return ResponseEntity.ok(transactionService.obterRelatorioDashboard());
    }

    @GetMapping("/transactions/latest")
    public ResponseEntity<Transaction> buscarUltimaTransacao() {
        // Busca a lista de transações ordenadas pela data de criação decrescente
        List<Transaction> transacoes = transactionService.listarTodas();        
        if (transacoes.isEmpty()) {
            return ResponseEntity.noContent().build();
        }        
        // Pega a mais recente (primeira da lista se ordenado por ID/Data desc, ou última da lista padrão)
        Transaction ultima = transacoes.get(transacoes.size() - 1);        
        return ResponseEntity.ok(ultima);
    }

    @GetMapping("/files")
    public ResponseEntity<List<String>> listUploadedFiles() {
        try {
            Path pastaUpload = Paths.get(UPLOAD_DIR);
            if (!Files.exists(pastaUpload)) {
                return ResponseEntity.ok(List.of()); // Retorna lista vazia se a pasta não existir
            }

            // Lista todos os arquivos da pasta original que terminam com .mp3 ou .webm/.tmp enviados
            List<String> arquivos = Files.list(pastaUpload)
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    // Filtra para exibir apenas arquivos de áudio válidos na lista do usuário
                    .filter(name -> name.endsWith(".mp3") || name.endsWith(".webm")) 
                    .collect(Collectors.toList());

            return ResponseEntity.ok(arquivos);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/reprocess/{filename}")
    public ResponseEntity<String> reprocessFile(@PathVariable String filename) {
        try {
            // Busca o arquivo exatamente onde ele foi gravado originalmente
            Path caminhoArquivo = Paths.get(UPLOAD_DIR, filename);
            File arquivo = caminhoArquivo.toFile();

            if (!arquivo.exists()) {
                return ResponseEntity.badRequest().body("Erro: Arquivo não encontrado no servidor.");
            }

            // Fluxo idêntico: injeta o caminho absoluto direto no barramento SQS
            String caminhoAbsoluto = arquivo.getAbsolutePath();
            sqsTemplate.send(to -> to.queue("fila-audios-processar").payload(caminhoAbsoluto));
            System.out.println("[API-REPROCESS] Arquivo reenviado para processamento: " + caminhoAbsoluto);

            return ResponseEntity.accepted().body("Arquivo enviado para reprocessamento com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao reenviar arquivo: " + e.getMessage());
        }
    }
}