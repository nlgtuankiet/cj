package com.rainyseason.cj.common

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.floor

@Singleton
class NumberFormater @Inject constructor() {

    fun formatPercent(
        amount: Double,
        locate: Locale,
        numberOfDecimals: Int = 1,
    ): String {
        val formatter: DecimalFormat =
            NumberFormat.getCurrencyInstance(locate) as DecimalFormat
        val smartNumberOfDecimals = getSmartNumberOfDecimal(
            inputAmount = amount,
            configNumberOfDecimal = numberOfDecimals
        )
        formatter.maximumFractionDigits = smartNumberOfDecimals
        formatter.minimumFractionDigits = smartNumberOfDecimals
        formatter.decimalFormatSymbols = formatter.decimalFormatSymbols.apply {
            currencySymbol = ""
        }
        val formattedPercent = formatter.format(amount)
        val symbol = if (amount > 0) {
            "+"
        } else {
            ""
        }
        return "${symbol}$formattedPercent%"
    }

    fun formatAmount(
        amount: Double,
        currencyCode: String,
        roundToMillion: Boolean = true,
        numberOfDecimal: Int = 2,
        hideOnLargeAmount: Boolean = true,
        showCurrencySymbol: Boolean = true,
        showThousandsSeparator: Boolean = true,
    ): String {
        var tmpAmount = amount

        val roundToM = roundToMillion && tmpAmount > 1_000_000
        var roundSymbol = ""
        if (roundToM && tmpAmount >= 1_000_000_000_000) {
            roundSymbol = "T"
            tmpAmount /= 1_000_000_000_000
        } else if (roundToM && tmpAmount >= 1_000_000_000) {
            roundSymbol = "B"
            tmpAmount /= 1_000_000_000
        } else if (roundToM && tmpAmount >= 1_000_000) {
            roundSymbol = "M"
            tmpAmount /= 1_000_000
        }
        val currencyInfo = getNonNullCurrencyInfo(currencyCode)
        val locale = currencyInfo.locale
        val formatter: DecimalFormat = NumberFormat.getCurrencyInstance(locale) as DecimalFormat
        formatter.currency = Currency.getInstance(locale)
        if (!showCurrencySymbol || currencyCode.isEmpty()) {
            val symbol = formatter.decimalFormatSymbols
            symbol.currencySymbol = ""
            formatter.decimalFormatSymbols = symbol
        }
        val numberOfDecimals = getSmartNumberOfDecimal(
            inputAmount = tmpAmount,
            configNumberOfDecimal = numberOfDecimal,
            hideOnLargeAmount = hideOnLargeAmount
        )
        formatter.maximumFractionDigits = numberOfDecimals
        formatter.minimumFractionDigits = numberOfDecimals
        formatter.isGroupingUsed = showThousandsSeparator
        var formattedPrice = formatter.format(tmpAmount)
        if (roundToM) {
            formattedPrice += roundSymbol
        }
        return formattedPrice
    }

    /**
     * Ex: 2 decimal
     * ....**
     * 0.001234
     * -> 4
     */
    private fun getSmartNumberOfDecimal(
        inputAmount: Double,
        configNumberOfDecimal: Int?,
        hideOnLargeAmount: Boolean = false,
    ): Int {
        val amount = abs(inputAmount)

        if (configNumberOfDecimal == null) {
            return Int.MAX_VALUE
        }
        if (configNumberOfDecimal == 0) {
            return 0
        }
        if (amount == 0.0) {
            return 0
        }
        if (amount >= 100 && hideOnLargeAmount) {
            return 0
        }
        if (amount >= 0.1) {
            return configNumberOfDecimal
        }
        var result = 0
        var tempAmount = amount
        while (floor(tempAmount * 10).toInt() == 0) {
            result++
            tempAmount *= 10
        }
        result += configNumberOfDecimal
        return result
    }
}
