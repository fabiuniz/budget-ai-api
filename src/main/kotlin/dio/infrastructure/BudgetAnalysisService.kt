package dio.infrastructure

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

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
     * Demonstra o uso prático de Coroutines (async/await) para evitar o bloqueio de Threads da JVM.
     * Trata dados com técnicas estritas de Null Safety (O que revisamos para a TQI).
     */
    suspend fun processarAnalisePreditiva(descricao: String?, valorBruto: BigDecimal?): AnaliseResultado = coroutineScope {
        log.info("Iniciando análise preditiva concorrente no ecossistema Kotlin.")

        // 1. Null Safety & Early Return
        val valor = valorBruto ?: BigDecimal.ZERO
        val descLimpa = descricao?.trim() ?: "CATEGORIA_DESCONHECIDA"

        // 2. Concorrência Estruturada com chamadas paralelas (async/await)
        val categoriaDeferred = async { consultarClassificacaoIA(descLimpa) }
        val riscoDeferred = async { calcularScoreRiscoIA(valor) }

        // Aguarda os resultados em paralelo (tempo total = teto da maior chamada: 300ms)
        val categoriaFinal = try {
            categoriaDeferred.await()
        } catch (e: Exception) {
            log.error("Falha ao classificar categoria via IA. Aplicando fallback seguro.", e)
            "OUTROS"
        }

        val scoreFinal = riscoDeferred.await()

        log.info("Análise finalizada com sucesso para a categoria: {}", categoriaFinal)
        
        AnaliseResultado(
            categoria = categoriaFinal,
            scoreRisco = scoreFinal,
            insightIA = "Orçamento processado com segurança e alta disponibilidade. Valor verificado: R$ \$valor"
        )
    }

    private suspend fun consultarClassificacaoIA(descricao: String): String {
        delay(300) // Simula latência de I/O de rede sem travar a Thread do SO
        return if (descricao.contains("nuvem", ignoreCase = true)) "INFRAESTRUTURA" else "OPERACIONAL"
    }

    private suspend fun calcularScoreRiscoIA(valor: BigDecimal): Double {
        delay(150) // Simula computação assíncrona
        return if (valor > BigDecimal(5000)) 0.85 else 0.15
    }
}
