package dio.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import dio.application.input.TransactionService;
import dio.domain.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
public class BudgetAiEngine {

    private final TransactionService transactionService;
    private final RestTemplate restTemplate;
    private final BudgetAnalysisService budgetAnalysisService;
    private final ObjectMapper objectMapper;

    @Value("${google.ai.studio.api.key}")
    private String apiKey;

    //private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    // Record interno para tipagem forte do JSON retornado pela Inteligência Artificial
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeminiFinanceResponse(String description, BigDecimal amount, String type) {}

    public BudgetAiEngine(TransactionService transactionService, BudgetAnalysisService budgetAnalysisService, ObjectMapper objectMapper) {
        this.transactionService = transactionService;
        this.budgetAnalysisService = budgetAnalysisService;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @CircuitBreaker(name = "geminiApi", fallbackMethod = "fallbackGemini")
    public String processarAudioEIntencaoReal(File arquivoAudio) throws Exception {
    
        byte[] fileContent = Files.readAllBytes(arquivoAudio.toPath());
        String audioBase64 = Base64.getEncoder().encodeToString(fileContent);

        String promptComando = "Analise o áudio anexado contendo uma movimentação financeira em português. " +
                "Extraia as informações e retorne EXCLUSIVAMENTE um objeto JSON válido contendo os campos: " +
                "'description' (texto), 'amount' (número decimal) e 'type' ('INCOME' ou 'EXPENSE'). " +
                "Não use marcações markdown como ```json.";

        Map<String, Object> requestBody = mapiarRequestGemini(audioBase64, promptComando);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-goog-api-key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // Se a linha abaixo der erro 503, o Resilience4j captura o erro imediatamente!
        ResponseEntity<Map> response = restTemplate.postForEntity(GEMINI_API_URL, entity, Map.class);

        String textoJsonDaIa = extrairTextoDaResposta(response.getBody());
        
        return executarToolCalling(textoJsonDaIa);
    }
    public String fallbackGemini(File arquivoAudio, Throwable t) {
        System.err.println("[CIRCUIT-BREAKER] Chamada para o Gemini bloqueada ou falhou. Motivo: " + t.getMessage());
        return "ERRO_RESILIENCIA: O serviço de IA está temporariamente indisponível (Circuit Breaker Aberto). Sua requisição será reprocessada assim que o sistema estabilizar.";
    }

    private Map<String, Object> mapiarRequestGemini(String audioBase64, String prompt) {
        Map<String, Object> inlineData = Map.of("mimeType", "audio/mpeg", "data", audioBase64);
        Map<String, Object> partAudio = Map.of("inlineData", inlineData);
        Map<String, Object> partText = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(partAudio, partText));
        
        return Map.of("contents", Collections.singletonList(content));
    }

    @SuppressWarnings("rawtypes")
    private String extrairTextoDaResposta(Map responseBody) {
        try {
            List candidates = (List) responseBody.get("candidates");
            Map firstCandidate = (Map) candidates.get(0);
            Map content = (Map) firstCandidate.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);
            return (String) firstPart.get("text");
        } catch (Exception e) {
            throw new RuntimeException("Falha ao navegar na árvore estrutural de resposta do Gemini.", e);
        }
    }

    private String executarToolCalling(String jsonIa) {
        // Limpeza segura usando Expressões Regulares
        String jsonLimpo = jsonIa.replaceAll("```json|```", "").trim();
        String categoriaFinal;
        GeminiFinanceResponse dadosIa;

        try {
            // Sênior approach: Usando Jackson para realizar o Parse estruturado e tipado
            dadosIa = objectMapper.readValue(jsonLimpo, GeminiFinanceResponse.class);
            categoriaFinal = dadosIa.description();
        } catch (Exception e) {
            System.err.println("[PARSER-FAIL] Falha ao converter JSON do Gemini para Record. Payload: " + jsonLimpo);
            return "ERRO_PARSER: Payload gerado pela IA quebrou o contrato esperado.";
        }

        try {
            // Executando interoperabilidade com as suspensões e escopos assíncronos do Kotlin Coroutines
            dio.infrastructure.AnaliseResultado resultadoKotlin = kotlinx.coroutines.BuildersKt.runBlocking(
                    kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> budgetAnalysisService.processarAnalisePreditiva(dadosIa.description(), dadosIa.amount(), continuation)
            );
            
            if (resultadoKotlin.getCategoria() != null && !resultadoKotlin.getCategoria().isEmpty()) {
                categoriaFinal = resultadoKotlin.getCategoria();
            }
        } catch (Exception e) {
            System.err.println("[INTEROP-FAIL] Fallback ativado para o Core Kotlin: " + e.getMessage());
        }

        Transaction transaction = new Transaction();
        String descricaoOriginal = (dadosIa.description() != null) ? dadosIa.description() : "Sem descrição";
        transaction.setDescription(descricaoOriginal);
        transaction.setAmount(dadosIa.amount() != null ? dadosIa.amount() : BigDecimal.ZERO);
        transaction.setType("INCOME".equalsIgnoreCase(dadosIa.type()) ? "INCOME" : "EXPENSE");
        transaction.setCreatedAt(LocalDateTime.now());

        Transaction cadastrada = transactionService.criarTransacao(transaction);
        return "Sucesso! Transação persistida de forma estruturada. ID: " + cadastrada.getId();
    }
}