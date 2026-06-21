package dio.infrastructure;

import dio.application.input.TransactionService;
import dio.domain.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import kotlin.coroutines.EmptyCoroutineContext;

@Component
public class BudgetAiEngine {

    private final TransactionService transactionService;
    private final RestTemplate restTemplate;
    private final BudgetAnalysisService budgetAnalysisService;

    @Value("${google.ai.studio.api.key}")
    private String apiKey;

    // URL oficial correta sem expor a chave como parâmetro de query
    // Altere para o modelo correto sem o "1.5"
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    public BudgetAiEngine(TransactionService transactionService, BudgetAnalysisService budgetAnalysisService) {
        this.transactionService = transactionService;
        this.budgetAnalysisService = budgetAnalysisService;
        this.restTemplate = new RestTemplate();
    }

    public String processarAudioEIntencaoReal(File arquivoAudio) {
        System.out.println("[IA-ENGINE] Iniciando processamento real via Google AI Studio para: " + arquivoAudio.getName());

        try {
            byte[] fileContent = Files.readAllBytes(arquivoAudio.toPath());
            String audioBase64 = Base64.getEncoder().encodeToString(fileContent);

            String urlCompleta = GEMINI_API_URL;
            String promptComando = "Analise o áudio anexado que contém uma movimentação financeira falada em português. " +
                    "Extraia as informações e retorne OBRIGATORIAMENTE um objeto JSON puro, sem formatação markdown (sem ```json), " +
                    "contendo os campos: 'description' (uma descrição curta e limpa do gasto/ganho), " +
                    "'amount' (o valor numérico decimal) e 'type' (deve ser 'INCOME' se for entrada/ganho ou 'EXPENSE' se for saída/gasto).";

            Map<String, Object> requestBody = mapiarRequestGemini(audioBase64, promptComando);

            // Passando os Headers idênticos ao cURL oficial que funciona
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-goog-api-key", apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            System.out.println("[IA-ENGINE] Enviando payload multimodal para o Gemini...");
            ResponseEntity<Map> response = restTemplate.postForEntity(urlCompleta, entity, Map.class);

            String textoJsonDaIa = extrairTextoDaResposta(response.getBody());
            System.out.println("[IA-ENGINE] Resposta estruturada recebida da IA: " + textoJsonDaIa);

            return executarToolCalling(textoJsonDaIa);

        } catch (IOException e) {
            System.err.println("[IA-ENGINE] Erro ao ler arquivo de áudio: " + e.getMessage());
            return "ERRO_IA: Falha ao ler arquivo de áudio físico no servidor.";
        } catch (Exception e) {
            System.err.println("[IA-ENGINE] Erro na integração com Google AI Studio: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("503")) {
               return "ERRO_IA: O servidor do Google Gemini está sobrecarregado devido à alta demanda global neste momento. Por favor, tente enviar o áudio novamente em alguns segundos.";
            }
            return "ERRO_IA: Falha ao processar IA no Google AI Studio. Detalhe: " + e.getMessage();
        }
    }

    private Map<String, Object> mapiarRequestGemini(String audioBase64, String prompt) {
        Map<String, Object> partAudio = new HashMap<>();
        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mimeType", "audio/mpeg");
        inlineData.put("data", audioBase64);
        partAudio.put("inlineData", inlineData);

        Map<String, Object> partText = new HashMap<>();
        partText.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Arrays.asList(partAudio, partText));

        Map<String, Object> request = new HashMap<>();
        request.put("contents", Collections.singletonList(content));
        return request;
    }

    private String extrairTextoDaResposta(Map responseBody) {
        try {
            List candidates = (List) responseBody.get("candidates");
            Map firstCandidate = (Map) candidates.get(0);
            Map content = (Map) firstCandidate.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);
            return (String) firstPart.get("text");
        } catch (Exception e) {
            throw new RuntimeException("Não foi possível extrair o texto do payload de resposta do Gemini.", e);
        }
    }

    private String executarToolCalling(String jsonIa) {
        String jsonLimpo = jsonIa.replace("```json", "").replace("```", "").trim();

        String description = buscarChaveJson(jsonLimpo, "description");
        String amountStr = buscarChaveJson(jsonLimpo, "amount");
        String type = buscarChaveJson(jsonLimpo, "type").toUpperCase();

        BigDecimal valorBruto = amountStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(amountStr);

        String categoriaFinal = description;

        // Try/Catch adicionado para blindar a chamada do runBlocking do Kotlin contra InterruptedException
        try {
            System.out.println("[IA-ENGINE] [INTEROP] Chamando o motor assíncrono do Kotlin com Coroutines...");
            dio.infrastructure.AnaliseResultado resultadoKotlin = kotlinx.coroutines.BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> budgetAnalysisService.processarAnalisePreditiva(description, valorBruto, continuation)
            );
            
            System.out.println("[IA-ENGINE] Resposta do Kotlin recebida com sucesso! Categoria: " + resultadoKotlin.getCategoria());
            if (resultadoKotlin.getCategoria() != null && !resultadoKotlin.getCategoria().isEmpty()) {
                categoriaFinal = resultadoKotlin.getCategoria();
            }
        } catch (Exception e) {
            System.err.println("[IA-ENGINE] Erro ou interrupção ao executar rotina do Kotlin: " + e.getMessage());
        }

        Transaction transaction = new Transaction();
        transaction.setDescription(categoriaFinal.isEmpty() ? "CATEGORIA_DESCONHECIDA" : categoriaFinal);
        transaction.setAmount(valorBruto);
        transaction.setType(type.contains("INCOME") ? "INCOME" : "EXPENSE");
        transaction.setCreatedAt(LocalDateTime.now());

        System.out.println("[IA-ENGINE] [TOOL CALLING ACTIVATED] Invocando Core da Aplicação...");
        Transaction cadastrada = transactionService.criarTransacao(transaction);

        return "Sucesso! Áudio processado pelo pipeline híbrido Java/Kotlin. ID: " + cadastrada.getId();
    }

    private String buscarChaveJson(String json, String chave) {
        int index = json.indexOf("\"" + chave + "\"");
        if (index == -1) return "";
        int start = json.indexOf(":", index) + 1;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return json.substring(start, end).replace("\"", "").trim();
    }
}