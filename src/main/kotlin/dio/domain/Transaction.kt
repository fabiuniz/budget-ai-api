package dio.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class Transaction(
    var id: Long? = null,
    var description: String? = null,
    var amount: BigDecimal? = null,
    var type: String? = null,
    var createdAt: LocalDateTime? = null
)