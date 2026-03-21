package com.wolfcola.equatecontinued

object SISuffixHelper {
    private val siSuffixMap = mapOf(
        "E24" to "yotta",
        "E21" to "zetta",
        "E18" to "exa",
        "E15" to "peta",
        "E12" to "tera",
        "E9" to "giga",
        "E6" to "mega",
        "E3" to "kilo",
        "E2" to "hecto",
        "E1" to "deca",
        "E-1" to "deci",
        "E-2" to "centi",
        "E-3" to "milli",
        "E-6" to "micro",
        "E-9" to "nano",
        "E-12" to "pico",
        "E-15" to "femto",
        "E-18" to "atto",
        "E-21" to "zepto"
    )

    @JvmStatic
    fun getSuffixName(number: String): String {
        if (number.matches(Regex(".*E.*E.*")) || !number.contains("E")) return ""
        return siSuffixMap[number.substring(number.indexOf("E"))] ?: ""
    }
}
