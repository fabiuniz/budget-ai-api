package dio.infrastructure

import java.math.BigDecimal

sealed class AudioProcessingState {
    object Processing : AudioProcessingState()
    data class Success(val description: String, val amount: BigDecimal, val type: String) : AudioProcessingState()
    data class Error(val message: String, val throwable: Throwable? = null) : AudioProcessingState()
}

// Extension Function para sanitizar strings vindas da IA
fun String.sanitizeAiTokens(): String {
    return this.replace("`", "").replace("markdown", "").trim()
}