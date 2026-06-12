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

@Component
public class BudgetAiEngine {

    private final TransactionService transactionService;
    private final RestTemplate restTemplate;

    @Value("${google.ai.studio.api.key}")
    private String apiKey;

    // URL oficial correta sem expor a chave como parâmetro de query
    // Altere para o modelo correto sem o "1.5"
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    public BudgetAiEngine(TransactionService transactionService) {
        this.transactionService = transactionService;
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
            return "Falha ao ler arquivo de áudio físico.";
        } catch (Exception e) {
            System.err.println("[IA-ENGINE] Erro na integração com Google AI Studio: " + e.getMessage());
            return "Falha ao processar IA no Google AI Studio: " + e.getMessage();
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

        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(new BigDecimal(amountStr));
        transaction.setType(type.contains("INCOME") ? "INCOME" : "EXPENSE");
        transaction.setCreatedAt(LocalDateTime.now());

        System.out.println("[IA-ENGINE] [TOOL CALLING ACTIVATED] Invocando Core da Aplicação...");
        Transaction cadastrada = transactionService.criarTransacao(transaction);

        return "Sucesso! Áudio interpretado pelo Google AI Studio e registrado. ID: " + cadastrada.getId() + " | Tipo: " + cadastrada.getType();
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