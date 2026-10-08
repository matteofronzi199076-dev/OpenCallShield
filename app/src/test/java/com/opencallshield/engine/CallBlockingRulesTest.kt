package com.opencallshield.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallBlockingRulesTest {
    private fun reason(
        number: String,
        contact: Boolean = false,
        spam: Boolean = false,
        foreign: Boolean = true,
        prefixes: List<String> = emptyList(),
        unknown: Boolean = false
    ) = CallBlockingRules.reason(number, contact, spam, foreign, prefixes, unknown)

    @Test fun foreignCountriesNeedNoSelection() {
        listOf("+14155551234", "+442079460123", "+33123456789", "+79123456789", "+819012345678", "+2348012345678")
            .forEach { assertTrue(it, CallBlockingRules.isForeign(it)) }
    }

    @Test fun internationalAccessPrefixAndFormattingAreRecognized() {
        listOf("00442079460123", "00 33 1 23 45 67 89", "+44 (20) 7946-0123")
            .forEach { assertTrue(it, CallBlockingRules.isForeign(it)) }
    }

    @Test fun italianMobileAndLandlineNumbersRemainAllowed() {
        listOf("+393351234567", "+390612345678", "00393351234567", "00390612345678", "+39 335 123-4567", "3351234567", "0612345678")
            .forEach { assertFalse(it, CallBlockingRules.isForeign(it)); assertNull(reason(it)) }
    }

    @Test fun withheldAndEmergencyNumbersDoNotBecomeForeign() {
        listOf("", "112", "118", "00", "+")
            .forEach { assertFalse(it, CallBlockingRules.isForeign(it)); assertNull(reason(it)) }
    }

    @Test fun savedContactsOverrideEveryBlockingRule() {
        assertNull(reason("+442079460123", contact = true, spam = true, prefixes = listOf("+44"), unknown = true))
        assertNull(reason("+393351234567", contact = true, spam = true, unknown = true))
    }

    @Test fun italianSpamStillBlocks() {
        assertEquals("Segnalato come SPAM", reason("+390281276702", spam = true))
    }

    @Test fun disablingForeignRuleAllowsForeignNumbers() {
        assertNull(reason("00442079460123", foreign = false))
        assertEquals("Numero estero (prefisso diverso da +39)", reason("00442079460123"))
    }

    @Test fun disablingForeignRulePreservesCustomPrefixesAndUnknownRule() {
        assertEquals("Prefisso sospetto (+44)", reason("+442079460123", foreign = false, prefixes = listOf("+44")))
        assertEquals("Numero non presente nei contatti", reason("3351234567", unknown = true))
        assertEquals("Numero nascosto", reason("", unknown = true))
    }

    @Test fun spamReasonHasPriorityOverForeignAndCustomRules() {
        assertEquals("Segnalato come SPAM", reason("+442079460123", spam = true, prefixes = listOf("+44"), unknown = true))
    }

    @Test fun customItalianPrefixBlockStillWorks() {
        assertEquals("Prefisso sospetto (+3902)", reason("+390281276702", prefixes = listOf("+3902")))
    }
}
