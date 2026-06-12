package dio.domain;

import java.math.BigDecimal;

public record DashboardReport(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance
) {}