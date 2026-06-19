package dio.infrastructure

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

// Em Java, isso exigiria uma classe tradicional com getters, setters, equals, hashCode, toString e construtor (ou o uso do Lombok @Data).
data class AnaliseResultado(
    val categoria: String,
    val scoreRisco: Double,
    val insightIA: String
)

@Service
class BudgetAnalysisService {

    private val log = LoggerFactory.getLogger(BudgetAnalysisService::class.java)

    /**
     * Orquestra a análise de orçamento de forma assíncrona e paralela.
     */
    // Em Java, métodos 'suspend' não existem. O retorno teria que ser encapsulado em um CompletableFuture<AnaliseResultado> para ser assíncrono.
    suspend fun processarAnalisePreditiva(descricao: String?, valorBruto: BigDecimal?): AnaliseResultado = coroutineScope {
        log.info("Iniciando análise preditiva concorrente no ecossistema Kotlin.")

        // 1. Null Safety & Early Return
        // Em Java: val valor = (valorBruto != null) ? valorBruto : BigDecimal.ZERO; (Não há operador ?:)
        val valor = valorBruto ?: BigDecimal.ZERO
        // Em Java: val descLimpa = (descricao != null) ? descricao.trim() : "CATEGORIA_DESCONHECIDA"; (Risco alto de NullPointerException se esquecer)
        val descLimpa = descricao?.trim() ?: "CATEGORIA_DESCONHECIDA"

        // 2. Concorrência Estruturada com chamadas paralelas (async/await)
        // Em Java: CompletableFuture<String> categoriaFuture = CompletableFuture.supplyAsync(() -> consultarClassificacaoIA(descLimpa), executor);
        val categoriaDeferred = async { consultarClassificacaoIA(descLimpa) }
        // Em Java: CompletableFuture<Double> riscoFuture = CompletableFuture.supplyAsync(() -> calcularScoreRiscoIA(valor), executor);
        val riscoDeferred = async { calcularScoreRiscoIA(valor) }

        // Aguarda os resultados em paralelo (tempo total = teto da maior chamada: 300ms)
        val categoriaFinal = try {
            // Em Java: categoriaFuture.join() ou .get() (lança exceções checadas que exigem try/catch obrigatório)
            categoriaDeferred.await()
        } catch (e: Exception) {
            log.error("Falha ao classificar categoria via IA. Aplicando fallback seguro.", e)
            "OUTROS"
        }

        // Em Java: riscoDeferred.get() (Bloqueia a thread atual até que o futuro seja resolvido)
        val scoreFinal = riscoDeferred.await()

        log.info("Análise finalizada com sucesso para a categoria: {}", categoriaFinal)
        
        // Em Java: new AnaliseResultado(categoriaFinal, scoreFinal, "..." + valor) (Instanciação padrão e concatenação de String manual ou String.format)
        AnaliseResultado(
            categoria = categoriaFinal,
            scoreRisco = scoreFinal,
            // Em Java não há String Interpolation direta como "$valor", exige concatenação (+) ou String template do Java recente.
            insightIA = "Orçamento processado com segurança e alta disponibilidade. Valor verificado: R$ $valor"
        )
    }

    // Em Java: private CompletableFuture<String> consultarClassificacaoIA(String descricao)
    private suspend fun consultarClassificacaoIA(descricao: String): String {
        // Em Java: Thread.sleep(300) BLOQUEIA a thread real do sistema operacional, gastando memória e CPU à toa.
        delay(300) // Simula latência de I/O de rede sem travar a Thread do SO
        val resultado = if (descricao.contains("nuvem", ignoreCase = true)) "INFRAESTRUTURA" else "OPERACIONAL"
        // Em Java: return descricao.toLowerCase().contains("nuvem") ? "INFRAESTRUTURA" : "OPERACIONAL"; (Não possui o argumento nomeado ignoreCase)
        log.info("Resultado da classificação para '$descricao': $resultado")
        return resultado 
    }

    // Em Java: private CompletableFuture<Double> calcularScoreRiscoIA(BigDecimal valor)
    private suspend fun calcularScoreRiscoIA(valor: BigDecimal): Double {
        // Em Java: Thread.sleep(150) (Bloqueante) ou agendamento via ScheduledExecutorService (extremamente complexo para um fluxo simples)
        delay(150) // Simula computação assíncrona
        val score = if (valor > BigDecimal(5000)) 0.15 else 0.85
        // Em Java: return valor.compareTo(new BigDecimal(5000)) > 0 ? 0.85 : 0.15; (Java não aceita operadores de comparação como '>' para BigDecimal)
        log.info("Score de risco determinado para R$ $valor: $score")
        return score
    }
}