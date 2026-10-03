package com.nexa.finance.domain.transaction

enum class TransactionDirection {
    DEBIT,
    CREDIT
}

enum class TransactionType {
    INCOME,
    EXPENSE,
    PURCHASE,
    TRANSFER,
    PAYMENT,
    REFUND,
    CANCELLATION,
    ADJUSTMENT
}

enum class TransactionStatus {
    PENDING,
    POSTED,
    CANCELLED
}

enum class TransactionSource {
    MANUAL,
    IMPORTED,
    OPEN_FINANCE,
    SYSTEM
}
