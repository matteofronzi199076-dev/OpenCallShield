package com.opencallshield.engine

import android.content.Context
import com.opencallshield.data.SpamRepository

/** Contatti, lista SPAM, blocco estero, prefissi personalizzati, sconosciuti. */
class SpamDetector(
    private val context: Context,
    private val repo: SpamRepository
) {
    sealed class Decision {
        data object Allow : Decision()
        data class Block(val reason: String, val silence: Boolean) : Decision()
    }

    suspend fun evaluate(rawNumber: String?): Decision {
        val settings = repo.settings
        val number = SpamRepository.normalize(rawNumber.orEmpty())
        val knownContact = number.isNotEmpty() && ContactsChecker.isKnownContact(context, number)
        val reason = CallBlockingRules.reason(
            number = number,
            knownContact = knownContact,
            reportedSpam = !knownContact && number.isNotEmpty() && repo.isSpam(number),
            blockForeign = settings.blockForeign,
            blockedPrefixes = if (settings.blockPrefixes) settings.prefixes() else emptyList(),
            blockUnknown = settings.blockUnknown
        )
        return if (reason == null) Decision.Allow else Decision.Block(reason, settings.silenceInsteadOfReject)
    }
}
