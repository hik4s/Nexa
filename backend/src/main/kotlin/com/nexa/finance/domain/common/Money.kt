package com.nexa.finance.domain.common

/**
 * Monetary value represented in the smallest unit of its currency.
 * Examples: BRL 18.90 -> 1890, USD 18.90 -> 1890.
 */
data class Money(
    val amountInMinor: Long,
    val currency: CurrencyCode
) : Comparable<Money> {
    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math.addExact(amountInMinor, other.amountInMinor), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math.subtractExact(amountInMinor, other.amountInMinor), currency)
    }

    operator fun unaryMinus(): Money = Money(Math.negateExact(amountInMinor), currency)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amountInMinor.compareTo(other.amountInMinor)
    }

    fun isPositive(): Boolean = amountInMinor > 0

    fun isZero(): Boolean = amountInMinor == 0L

    fun isNegative(): Boolean = amountInMinor < 0

    private fun requireSameCurrency(other: Money) {
        if (currency != other.currency) {
            throw DomainValidationException(
                "Money operations require the same currency: $currency != ${other.currency}."
            )
        }
    }

    companion object {
        fun fromMinorUnits(amountInMinor: Long, currency: CurrencyCode): Money =
            Money(amountInMinor, currency)

        fun zero(currency: CurrencyCode): Money = Money(0L, currency)
    }
}
