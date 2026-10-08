package com.antigravity.expensetracker

import com.antigravity.expensetracker.data.repository.TransactionRepository
import com.antigravity.expensetracker.data.security.TrustedPackageValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityHardeningTest {

    @Test
    fun testTrustedPackageValidatorApprovesKnownInstitutions() {
        // Vietnamese Banks
        assertTrue(TrustedPackageValidator.isPackageTrusted("com.VCB"))
        assertTrue(TrustedPackageValidator.isPackageTrusted("com.mbbank"))
        assertTrue(TrustedPackageValidator.isPackageTrusted("vn.com.techcombank.bb.app"))
        assertTrue(TrustedPackageValidator.isPackageTrusted("com.vnpay.vpbankonline"))

        // Vietnamese Fintech & Wallets
        assertTrue(TrustedPackageValidator.isPackageTrusted("com.mservice.momopay"))
        assertTrue(TrustedPackageValidator.isPackageTrusted("vn.zalopay"))
        assertTrue(TrustedPackageValidator.isPackageTrusted("com.viettel.viettelpay"))

        // US / Global Banks & Fintech
        assertTrue(TrustedPackageValidator.isPackageTrusted("com.chase.sig.android"))
        assertTrue(TrustedPackageValidator.isPackageTrusted("com.wf.wellsfargomobile"))
        assertTrue(TrustedPackageValidator.isPackageTrusted("com.venmo"))
        assertTrue(TrustedPackageValidator.isPackageTrusted("com.paypal.android"))

        // Official Messaging Apps
        assertTrue(TrustedPackageValidator.isPackageTrusted("com.google.android.apps.messaging"))
        assertTrue(TrustedPackageValidator.isPackageTrusted("com.samsung.android.messaging"))
    }

    @Test
    fun testTrustedPackageValidatorRejectsUntrustedSources() {
        // Untrusted / rogue packages
        assertFalse(TrustedPackageValidator.isPackageTrusted("com.malicious.fakebank"))
        assertFalse(TrustedPackageValidator.isPackageTrusted("com.random.freegame"))
        assertFalse(TrustedPackageValidator.isPackageTrusted("com.adware.tracker"))
        assertFalse(TrustedPackageValidator.isPackageTrusted(""))
        assertFalse(TrustedPackageValidator.isPackageTrusted("   "))
    }

    @Test
    fun testTrustedPackageValidatorSupportsCustomExtensions() {
        val customApp = "com.mycreditunion.mobile"
        assertFalse(TrustedPackageValidator.isPackageTrusted(customApp))
        assertTrue(TrustedPackageValidator.isPackageTrusted(customApp, additionalPackages = setOf(customApp)))
    }

    @Test
    fun testRawTextCardNumberMasking() {
        // Test 16-digit card with spaces
        val inputWithSpaces = "Paid $80.20 with card 4111 2222 3333 4242 at Amazon"
        val maskedSpaces = TransactionRepository.sanitizeRawText(inputWithSpaces)
        assertEquals("Paid $80.20 with card **** **** **** 4242 at Amazon", maskedSpaces)

        // Test 16-digit card with hyphens
        val inputWithHyphens = "Charged USD 15.00 on 5500-1234-5678-9876 at Starbucks"
        val maskedHyphens = TransactionRepository.sanitizeRawText(inputWithHyphens)
        assertEquals("Charged USD 15.00 on **** **** **** 9876 at Starbucks", maskedHyphens)

        // Test continuous 16-digit card number
        val inputContinuous = "Alert: 4000123456789010 debited $200.00"
        val maskedContinuous = TransactionRepository.sanitizeRawText(inputContinuous)
        assertEquals("Alert: **** **** **** 9010 debited $200.00", maskedContinuous)

        // Normal 4-digit account mask should not be corrupted
        val normalText = "TK 1234 -50,000 VND tai Circle K"
        val maskedNormal = TransactionRepository.sanitizeRawText(normalText)
        assertEquals("TK 1234 -50,000 VND tai Circle K", maskedNormal)
    }
}
