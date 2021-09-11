package com.rainyseason.cj.common

import java.util.Locale


val CURRENCY_TO_NAME = mapOf(
    "usd" to "US Dollar",
    "idr" to "Indonesian Rupiah",
    "twd" to "New Taiwan Dollar",
    "eur" to "Euro",
    "krw" to "South Korean Won",
    "jpy" to "Japanese Yen",
    "rub" to "Russian Ruble",
    "cny" to "Chinese Yuan",
    "vnd" to "Vietnamese đồng",
    "gbp" to "British Pound Sterling",
    "aed" to "United Arab Emirates Dirham",
    "ars" to "Argentine Peso",
    "aud" to "Australian Dollar",
    "bdt" to "Bangladeshi Taka",
    "bhd" to "Bahraini Dinar",
    "bmd" to "Bermudian Dollar",
    "brl" to "Brazil Real",
    "cad" to "Canadian Dollar",
    "chf" to "Swiss Franc",
    "clp" to "Chilean Peso",
    "czk" to "Czech Koruna",
    "dkk" to "Danish Krone",

    "hkd" to "Hong Kong Dollar",
    "huf" to "Hungarian Forint",
    "ils" to "Israeli New Shekel",
    "inr" to "Indian Rupee",
    "kwd" to "Kuwaiti Dinar",
    "lkr" to "Sri Lankan Rupee",
    "mmk" to "Burmese Kyat",
    "mxn" to "Mexican Peso",
    "myr" to "Malaysian Ringgit",
    "ngn" to "Nigerian Naira",
    "nok" to "Norwegian Krone",
    "nzd" to "New Zealand Dollar",
    "php" to "Philippine Peso",
    "pkr" to "Pakistani Rupee",
    "pln" to "Polish Zloty",
    "sar" to "Saudi Riyal",
    "sek" to "Swedish Krona",
    "sgd" to "Singapore Dollar",
    "thb" to "Thai Baht",
    "try" to "Turkish Lira",
    "uah" to "Ukrainian hryvnia",
    "vef" to "Venezuelan bolívar fuerte",
    "zar" to "South African Rand",
    "xdr" to "IMF Special Drawing Rights",
)

data class CurrencyInfo(
    val code: String,
    val name: String,
    val locale: Locale,
)

val SUPPORTED_CURRENCY = mapOf(
    "usd" to CurrencyInfo(
        code = "usd",
        name = "US Dollar",
        locale = Locale.US,
    ),
    "idr" to CurrencyInfo(
        code = "idr",
        name = "Indonesian Rupiah",
        locale = Locale("id", "ID"),
    ),
    "twd" to CurrencyInfo(
        code = "twd",
        name = "New Taiwan Dollar",
        locale = Locale.TAIWAN,
    ),
    "eur" to CurrencyInfo(
        code = "eur",
        name = "Euro",
        locale = Locale.FRANCE,
    ),
    "krw" to CurrencyInfo(
        code = "krw",
        name = "South Korean Won",
        locale = Locale.KOREA,
    ),
    "jpy" to CurrencyInfo(
        code = "jpy",
        name = "Japanese Yen",
        locale = Locale.JAPAN,
    ),
    "rub" to CurrencyInfo(
        code = "rub",
        name = "Russian Ruble",
        locale = Locale("ru", "RU"),
    ),
    "cny" to CurrencyInfo(
        code = "cny",
        name = "Chinese Yuan",
        locale = Locale.CHINA
    ),
    "vnd" to CurrencyInfo(
        code = "vnd",
        name = "Vietnamese đồng",
        locale = Locale("vi", "VN")
    ),
    "gbp" to CurrencyInfo(
        code = "gbp",
        name = "British Pound Sterling",
        locale = Locale.UK
    ),
)
