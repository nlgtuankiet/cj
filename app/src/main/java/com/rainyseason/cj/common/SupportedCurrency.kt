package com.rainyseason.cj.common

import java.util.Locale

@Suppress("unused", "SpellCheckingInspection")
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
    val cmcId: String,
) {

    fun displayName(): String {
        return if (code.isEmpty()) {
            name
        } else {
            "${code.uppercase()} • $name"
        }
    }
    companion object {
        val NONE = CurrencyInfo(
            code = "",
            name = "None",
            locale = Locale.US,
            cmcId = ""
        )
        val USD = CurrencyInfo(
            code = "usd",
            name = "US Dollar",
            locale = Locale.US,
            cmcId = "2781",
        )
    }
}

fun currencyInfoOf(id: String): CurrencyInfo {
    return getNonNullCurrencyInfo(id)
}

fun getNonNullCurrencyInfo(code: String): CurrencyInfo {
    return SUPPORTED_CURRENCY[code] ?: CurrencyInfo.NONE
}

val SUPPORTED_CURRENCY = mapOf(
    "usd" to CurrencyInfo.USD,
    "idr" to CurrencyInfo(
        code = "idr",
        name = "Indonesian Rupiah",
        locale = Locale("id", "ID"),
        cmcId = "2794",
    ),
    "twd" to CurrencyInfo(
        code = "twd",
        name = "New Taiwan Dollar",
        locale = Locale.TAIWAN,
        cmcId = "2811",
    ),
    "eur" to CurrencyInfo(
        code = "eur",
        name = "Euro",
        locale = Locale.FRANCE,
        cmcId = "2790",
    ),
    "krw" to CurrencyInfo(
        code = "krw",
        name = "South Korean Won",
        locale = Locale.KOREA,
        cmcId = "2798",
    ),
    "jpy" to CurrencyInfo(
        code = "jpy",
        name = "Japanese Yen",
        locale = Locale.JAPAN,
        cmcId = "2797",
    ),
    "rub" to CurrencyInfo(
        code = "rub",
        name = "Russian Ruble",
        locale = Locale("ru", "RU"),
        cmcId = "2806",
    ),
    "cny" to CurrencyInfo(
        code = "cny",
        name = "Chinese Yuan",
        locale = Locale.CHINA,
        cmcId = "2787",
    ),
    "vnd" to CurrencyInfo(
        code = "vnd",
        name = "Vietnamese đồng",
        locale = Locale("vi", "VN"),
        cmcId = "2823",
    ),
    "gbp" to CurrencyInfo(
        code = "gbp",
        name = "British Pound Sterling",
        locale = Locale.UK,
        cmcId = "2791",
    ),

    // added since 1.5
    "cad" to CurrencyInfo(
        code = "cad",
        name = "Canadian Dollar",
        locale = Locale.CANADA,
        cmcId = "2784",
    ),
    "nzd" to CurrencyInfo(
        code = "nzd",
        name = "New Zealand Dollar",
        locale = Locale("en", "NZ"),
        cmcId = "2802",
    ),
    "inr" to CurrencyInfo(
        code = "inr",
        name = "Indian Rupee",
        locale = Locale("hi", "IN"),
        cmcId = "2796",
    ),
    "aud" to CurrencyInfo(
        code = "aud",
        name = "Australian Dollar",
        locale = Locale("en", "AU"),
        cmcId = "2782",
    ),
    "php" to CurrencyInfo(
        code = "php",
        name = "Philippine Pesor",
        locale = Locale("en", "PH"),
        cmcId = "2803",
    ),
    "pln" to CurrencyInfo(
        code = "pln",
        name = "Polish Zloty",
        locale = Locale("pl", "PL"),
        cmcId = "2805",
    ),
    "sgd" to CurrencyInfo(
        code = "sgd",
        name = "Singapore Dollar",
        locale = Locale("en", "SG"),
        cmcId = "2808",
    ),
    "sek" to CurrencyInfo(
        code = "sek",
        name = "Swedish Krona",
        locale = Locale("sv", "SE"),
        cmcId = "2807",
    ),
    "chf" to CurrencyInfo(
        code = "chf",
        name = "Swiss Franc",
        locale = Locale("fr", "CH"),
        cmcId = "2785",
    ),
    "myr" to CurrencyInfo(
        code = "myr",
        name = "Malaysian Ringgit",
        locale = Locale("ms", "MY"),
        cmcId = "2800",
    ),
    "thb" to CurrencyInfo(
        code = "thb",
        name = "Thai Baht",
        locale = Locale("th", "TH"),
        cmcId = "2809",
    ),
    "rub" to CurrencyInfo(
        code = "rub",
        name = "Russian Ruble",
        locale = Locale("ru", "RU"),
        cmcId = "2806",
    ),
    "czk" to CurrencyInfo(
        code = "czk",
        name = "Czech Koruna",
        locale = Locale("cs", "CZ"),
        cmcId = "2788",
    ),
    "brl" to CurrencyInfo(
        code = "brl",
        name = "Brazil Real",
        locale = Locale("pt", "BR"),
        cmcId = "2783",
    ),
    "dkk" to CurrencyInfo(
        code = "dkk",
        name = "Danish Krone",
        locale = Locale("da", "DK"),
        cmcId = "2789",
    ),
    "ils" to CurrencyInfo(
        code = "ils",
        name = "Israeli New Shekel",
        locale = Locale("iw", "IL"),
        cmcId = "2795",
    ),
    "huf" to CurrencyInfo(
        code = "huf",
        name = "Hungarian Forint",
        locale = Locale("hu", "HU"),
        cmcId = "2793",
    ),
)

val SUPPORTED_CURRENCY_VALUES = SUPPORTED_CURRENCY.values.sortedBy { it.code }
