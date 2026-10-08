package com.opencallshield.engine

/** Regole pure, condivise dal filtro delle chiamate e dai test. */
object CallBlockingRules {
    /** Senza un prefisso internazionale esplicito non deduciamo il Paese. */
    fun isForeign(rawNumber: String): Boolean {
        val number = rawNumber.filter { it.isDigit() || it == '+' }
        val internationalDigits = when {
            number.startsWith("+") -> number.drop(1)
            number.startsWith("00") -> number.drop(2)
            else -> return false
        }
        return internationalDigits.isNotEmpty() &&
            internationalDigits.all { it.isDigit() } &&
            !internationalDigits.startsWith("39")
    }

    fun reason(
        number: String,
        knownContact: Boolean,
        reportedSpam: Boolean,
        blockForeign: Boolean,
        blockedPrefixes: List<String>,
        blockUnknown: Boolean
    ): String? {
        if (knownContact) return null
        if (number.isNotEmpty() && reportedSpam) return "Segnalato come SPAM"
        if (blockForeign && isForeign(number)) {
            return "Numero estero (prefisso diverso da +39)"
        }
        if (number.isNotEmpty()) {
            val match = blockedPrefixes.firstOrNull { number.startsWith(it) }
            if (match != null) return "Prefisso sospetto ($match)"
        }
        if (blockUnknown) {
            return if (number.isEmpty()) "Numero nascosto" else "Numero non presente nei contatti"
        }
        return null
    }
}
