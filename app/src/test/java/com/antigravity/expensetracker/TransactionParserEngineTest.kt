package com.antigravity.expensetracker

import com.antigravity.expensetracker.data.model.TransactionType
import com.antigravity.expensetracker.data.parser.TransactionParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionParserEngineTest {

    private lateinit var parserEngine: TransactionParserEngine

    @Before
    fun setUp() {
        parserEngine = TransactionParserEngine()
    }

    @Test
    fun testEnglishDebitStarbucks() {
        val text = "Paid $25.50 to Starbucks"
        val output = parserEngine.parse(text, 1000L)
        assertNotNull(output)
        assertEquals(25.50, output!!.amount, 0.001)
        assertEquals("USD", output.currency)
        assertEquals(TransactionType.DEBIT, output.type)
        assertEquals("Starbucks", output.counterparty)
    }

    @Test
    fun testEnglishDebitUber() {
        val text = "Debited USD 12.00 at Uber"
        val output = parserEngine.parse(text, 2000L)
        assertNotNull(output)
        assertEquals(12.00, output!!.amount, 0.001)
        assertEquals("USD", output.currency)
        assertEquals(TransactionType.DEBIT, output.type)
        assertEquals("Uber", output.counterparty)
    }

    @Test
    fun testEnglishDebitWithAccountMask() {
        val text = "Alert: Account ending 4242 was charged $80.20 at Amazon"
        val output = parserEngine.parse(text, 3000L)
        assertNotNull(output)
        assertEquals(80.20, output!!.amount, 0.001)
        assertEquals("4242", output.accountIdentifier)
        assertEquals(TransactionType.DEBIT, output.type)
        assertEquals("Amazon", output.counterparty)
    }

    @Test
    fun testEnglishCreditDeposit() {
        val text = "Received $500.00 from John"
        val output = parserEngine.parse(text, 4000L)
        assertNotNull(output)
        assertEquals(500.00, output!!.amount, 0.001)
        assertEquals("USD", output.currency)
        assertEquals(TransactionType.CREDIT, output.type)
        assertEquals("John", output.counterparty)
    }

    @Test
    fun testEnglishCreditRefund() {
        val text = "Refund of $35.00 from Nike"
        val output = parserEngine.parse(text, 5000L)
        assertNotNull(output)
        assertEquals(35.00, output!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, output.type)
        assertEquals("Nike", output.counterparty)
    }

    @Test
    fun testP2PCreditSarah() {
        val text = "Sarah sent you $25.00"
        val output = parserEngine.parse(text, 6000L)
        assertNotNull(output)
        assertEquals(25.00, output!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, output.type)
        assertEquals("Sarah", output.counterparty)
    }

    @Test
    fun testVietnameseDebitVietcombank() {
        val text = "TK 1234 -50,000 VND tai Circle K"
        val output = parserEngine.parse(text, 7000L)
        assertNotNull(output)
        assertEquals(50000.0, output!!.amount, 0.001)
        assertEquals("VND", output.currency)
        assertEquals(TransactionType.DEBIT, output.type)
        assertEquals("Circle K", output.counterparty)
        assertEquals("1234", output.accountIdentifier)
    }

    @Test
    fun testVietnameseDebitMoMo() {
        val text = "Thanh toan 65.000d tai HighLands Coffee"
        val output = parserEngine.parse(text, 8000L)
        assertNotNull(output)
        assertEquals(65000.0, output!!.amount, 0.001)
        assertEquals("VND", output.currency)
        assertEquals(TransactionType.DEBIT, output.type)
        assertEquals("HighLands Coffee", output.counterparty)
    }

    @Test
    fun testVietnameseCreditMBBank() {
        val text = "TK 9876 +2.000.000 VND tu CONG TY ABC"
        val output = parserEngine.parse(text, 9000L)
        assertNotNull(output)
        assertEquals(2000000.0, output!!.amount, 0.001)
        assertEquals("VND", output.currency)
        assertEquals(TransactionType.CREDIT, output.type)
        assertEquals("CONG TY ABC", output.counterparty)
        assertEquals("9876", output.accountIdentifier)
    }

    @Test
    fun testNoiseRejectionOtp() {
        val text = "Your bank verification code is 839201. Never share this OTP."
        assertTrue(parserEngine.isNoise(text))
        val output = parserEngine.parse(text, 10000L)
        assertNull(output)
    }

    @Test
    fun testNoiseRejectionPromo() {
        val text = "Grab: Nhap ma PROMO50 de duoc discount 50% cho chuyen di tiep theo!"
        assertTrue(parserEngine.isNoise(text))
        val output = parserEngine.parse(text, 11000L)
        assertNull(output)
    }

    @Test
    fun testNoiseRejectionLoginAlert() {
        val text = "Security alert: You logged in from an unrecognized device."
        assertTrue(parserEngine.isNoise(text))
        val output = parserEngine.parse(text, 12000L)
        assertNull(output)
    }
}
