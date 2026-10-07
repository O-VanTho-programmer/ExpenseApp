package com.antigravity.expensetracker.data.parser

import com.antigravity.expensetracker.data.model.TransactionType
import java.util.regex.Pattern

class TransactionParserEngine {

    // Noise filter keywords
    private val noiseKeywords = listOf(
        "otp",
        "verification code",
        "security code",
        "ma xac thuc",
        "xac thuc",
        "promo",
        "khuyen mai",
        "uu dai",
        "voucher",
        "discount",
        "coupon",
        "logged in",
        "dang nhap",
        "security alert",
        "password",
        "mat khau",
        "login attempt"
    )

    // English Debit Patterns
    private val englishDebitPattern1 = Pattern.compile(
        "(?i)(?:paid|spent|debited|charged|purchase(?: of)?)\\s+([A-Z$€£₫]{1,3})?\\s?([0-9]+(?:[.,][0-9]{2,3})*(?:[.,][0-9]{1,2})?)\\s*(?:[A-Z$€£₫]{1,3}|đ)?\\s+(?:to|at|for)\\s+([^.,;\\n]+)"
    )

    private val englishDebitPattern2 = Pattern.compile(
        "(?i)(?:debit|spent|paid|withdrew)\\s*(?:of)?\\s+([A-Z$€£₫]{1,3})?\\s?([0-9]+(?:[.,][0-9]{2,3})*(?:[.,][0-9]{1,2})?)\\s*(?:[A-Z$€£₫]{1,3}|đ)?(?:.*?(?:at|to)\\s+([^.,;\\n]+))?"
    )

    // English Credit Pattern with Counterparty
    private val englishCreditPatternWithCounterparty = Pattern.compile(
        "(?i)(?:received|credited|deposit(?:ed| of)?|refund(?:ed| of)?)\\s+([A-Z$€£₫]{1,3})?\\s?([0-9]+(?:[.,][0-9]{2,3})*(?:[.,][0-9]{1,2})?)\\s*(?:[A-Z$€£₫]{1,3}|đ)?\\s+(?:from|by|into|tai)\\s+([^.,;\\n]+)"
    )

    // English Credit Pattern standalone
    private val englishCreditPatternStandalone = Pattern.compile(
        "(?i)(?:received|credited|deposit(?:ed| of)?|refund(?:ed| of)?)\\s+([A-Z$€£₫]{1,3})?\\s?([0-9]+(?:[.,][0-9]{2,3})*(?:[.,][0-9]{1,2})?)\\s*(?:[A-Z$€£₫]{1,3}|đ)?"
    )

    private val p2pCreditPattern = Pattern.compile(
        "(?i)([^.,;\\n]+?)\\s+sent you\\s+([A-Z$€£₫]{1,3})?\\s?([0-9]+(?:[.,][0-9]{2,3})*(?:[.,][0-9]{1,2})?)"
    )

    // Vietnamese Bank / e-Wallet Patterns
    // Example: "TK 1234 -50,000 VND tai Circle K" or "GD thanh toan -100.000d tai Shopee"
    private val vnDebitPattern = Pattern.compile(
        "(?i)(?:TK\\s*([0-9]{3,4})?\\s*)?-(?:[A-Z$€£₫]{1,3}\\s*)?([0-9]+(?:[.,][0-9]{3})*)\\s*(?:VND|đ|USD)?\\s*(?:tai|den|cho)\\s+([^.,;\\n]+)"
    )

    // Example: "Thanh toan 50.000d tai HighLands Coffee"
    private val vnThanhToanPattern = Pattern.compile(
        "(?i)thanh toan\\s+([0-9]+(?:[.,][0-9]{3})*)\\s*(?:VND|đ|d)?\\s+tai\\s+([^.,;\\n]+)"
    )

    // Example: "TK 1234 +500,000 VND tu NGUYEN VAN A"
    private val vnCreditPattern = Pattern.compile(
        "(?i)(?:TK\\s*([0-9]{3,4})?\\s*)?\\+(?:[A-Z$€£₫]{1,3}\\s*)?([0-9]+(?:[.,][0-9]{3})*)\\s*(?:VND|đ|USD)?\\s*(?:tu\\s+([^.,;\\n]+))?"
    )

    // Account Mask Pattern: "Account ending 4242", "ending in 4242", "TK 1234", "account ...9876", "card *1234"
    private val accountMaskPattern = Pattern.compile(
        "(?i)(?:account(?:\\s+ending(?:\\s+in)?)?|ending(?:\\s+in)?|tail|acc|TK|the|card|so tai khoan)\\s*[:.*#]*\\s*([0-9]{3,4})"
    )

    fun isNoise(text: String): Boolean {
        val lower = text.lowercase()
        return noiseKeywords.any { lower.contains(it) }
    }

    fun parse(rawText: String, timestamp: Long): ParsedOutput? {
        if (isNoise(rawText)) {
            return null
        }

        val accountIdentifier = extractAccountMask(rawText)

        // 1. Try English Debit Pattern 1
        val deb1 = englishDebitPattern1.matcher(rawText)
        if (deb1.find()) {
            val curr = normalizeCurrency(deb1.group(1), rawText)
            val amt = parseAmount(deb1.group(2))
            val counterparty = deb1.group(3)?.trim().orEmpty()
            if (amt > 0) {
                return ParsedOutput(
                    amount = amt,
                    currency = curr,
                    type = TransactionType.DEBIT,
                    counterparty = counterparty.ifEmpty { "Unknown Merchant" },
                    accountIdentifier = accountIdentifier,
                    timestamp = timestamp
                )
            }
        }

        // 2. Try English Credit Pattern with Counterparty
        val credWithCp = englishCreditPatternWithCounterparty.matcher(rawText)
        if (credWithCp.find()) {
            val curr = normalizeCurrency(credWithCp.group(1), rawText)
            val amt = parseAmount(credWithCp.group(2))
            val counterparty = credWithCp.group(3)?.trim().orEmpty()
            if (amt > 0) {
                return ParsedOutput(
                    amount = amt,
                    currency = curr,
                    type = TransactionType.CREDIT,
                    counterparty = counterparty.ifEmpty { "Deposit" },
                    accountIdentifier = accountIdentifier,
                    timestamp = timestamp
                )
            }
        }

        // Try English Credit Pattern Standalone
        val credSolo = englishCreditPatternStandalone.matcher(rawText)
        if (credSolo.find()) {
            val curr = normalizeCurrency(credSolo.group(1), rawText)
            val amt = parseAmount(credSolo.group(2))
            if (amt > 0) {
                return ParsedOutput(
                    amount = amt,
                    currency = curr,
                    type = TransactionType.CREDIT,
                    counterparty = "Deposit",
                    accountIdentifier = accountIdentifier,
                    timestamp = timestamp
                )
            }
        }

        // 3. Try P2P Credit (e.g. "Sarah sent you $25.00")
        val p2p = p2pCreditPattern.matcher(rawText)
        if (p2p.find()) {
            val counterparty = p2p.group(1)?.trim().orEmpty()
            val curr = normalizeCurrency(p2p.group(2), rawText)
            val amt = parseAmount(p2p.group(3))
            if (amt > 0) {
                return ParsedOutput(
                    amount = amt,
                    currency = curr,
                    type = TransactionType.CREDIT,
                    counterparty = counterparty.ifEmpty { "P2P Transfer" },
                    accountIdentifier = accountIdentifier,
                    timestamp = timestamp
                )
            }
        }

        // 4. Try VN Debit Pattern
        val vnDeb = vnDebitPattern.matcher(rawText)
        if (vnDeb.find()) {
            val acc = vnDeb.group(1) ?: accountIdentifier
            val amt = parseAmount(vnDeb.group(2))
            val counterparty = vnDeb.group(3)?.trim().orEmpty()
            val curr = if (rawText.contains("USD", ignoreCase = true)) "USD" else "VND"
            if (amt > 0) {
                return ParsedOutput(
                    amount = amt,
                    currency = curr,
                    type = TransactionType.DEBIT,
                    counterparty = counterparty.ifEmpty { "Thanh toan" },
                    accountIdentifier = acc,
                    timestamp = timestamp
                )
            }
        }

        // 5. Try VN Thanh Toan Pattern
        val vnTt = vnThanhToanPattern.matcher(rawText)
        if (vnTt.find()) {
            val amt = parseAmount(vnTt.group(1))
            val counterparty = vnTt.group(2)?.trim().orEmpty()
            if (amt > 0) {
                return ParsedOutput(
                    amount = amt,
                    currency = "VND",
                    type = TransactionType.DEBIT,
                    counterparty = counterparty.ifEmpty { "Thanh toan" },
                    accountIdentifier = accountIdentifier,
                    timestamp = timestamp
                )
            }
        }

        // 6. Try VN Credit Pattern
        val vnCred = vnCreditPattern.matcher(rawText)
        if (vnCred.find()) {
            val acc = vnCred.group(1) ?: accountIdentifier
            val amt = parseAmount(vnCred.group(2))
            val counterparty = vnCred.group(3)?.trim().orEmpty()
            val curr = if (rawText.contains("USD", ignoreCase = true)) "USD" else "VND"
            if (amt > 0) {
                return ParsedOutput(
                    amount = amt,
                    currency = curr,
                    type = TransactionType.CREDIT,
                    counterparty = counterparty.ifEmpty { "Nhan tien" },
                    accountIdentifier = acc,
                    timestamp = timestamp
                )
            }
        }

        // 7. General Fallback
        val deb2 = englishDebitPattern2.matcher(rawText)
        if (deb2.find()) {
            val curr = normalizeCurrency(deb2.group(1), rawText)
            val amt = parseAmount(deb2.group(2))
            val counterparty = deb2.group(3)?.trim().orEmpty()
            if (amt > 0) {
                return ParsedOutput(
                    amount = amt,
                    currency = curr,
                    type = TransactionType.DEBIT,
                    counterparty = counterparty.ifEmpty { "Expense" },
                    accountIdentifier = accountIdentifier,
                    timestamp = timestamp
                )
            }
        }

        return null
    }

    private fun extractAccountMask(text: String): String? {
        val matcher = accountMaskPattern.matcher(text)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun normalizeCurrency(symbol: String?, fullText: String): String {
        val sym = symbol?.trim().orEmpty()
        return when {
            sym == "$" || fullText.contains("USD", ignoreCase = true) -> "USD"
            sym == "€" || fullText.contains("EUR", ignoreCase = true) -> "EUR"
            sym == "£" || fullText.contains("GBP", ignoreCase = true) -> "GBP"
            sym == "₫" || fullText.contains("VND", ignoreCase = true) || fullText.contains("đ") -> "VND"
            sym.isNotEmpty() -> sym
            else -> "USD"
        }
    }

    private fun parseAmount(raw: String?): Double {
        if (raw == null) return 0.0
        val clean = raw.trim()
        return try {
            // Check if Vietnamese or European format (e.g., 50.000 or 1.250,50)
            if (clean.contains(".") && !clean.contains(",")) {
                // If there is only dot, check if it's thousands separator (e.g. 50.000 or 1.000.000)
                val dotParts = clean.split(".")
                if (dotParts.size > 2 || (dotParts.size == 2 && dotParts[1].length == 3)) {
                    // Thousand separator
                    clean.replace(".", "").toDouble()
                } else {
                    clean.toDouble()
                }
            } else if (clean.contains(",") && !clean.contains(".")) {
                // E.g., 1,250 or 1,250,000 or decimal comma 12,50
                val commaParts = clean.split(",")
                if (commaParts.size == 2 && commaParts[1].length <= 2) {
                    // Decimal comma
                    clean.replace(",", ".").toDouble()
                } else {
                    // Thousand separator
                    clean.replace(",", "").toDouble()
                }
            } else if (clean.contains(",") && clean.contains(".")) {
                val lastComma = clean.lastIndexOf(',')
                val lastDot = clean.lastIndexOf('.')
                if (lastDot > lastComma) {
                    // Standard US 1,250.50
                    clean.replace(",", "").toDouble()
                } else {
                    // European 1.250,50
                    clean.replace(".", "").replace(",", ".").toDouble()
                }
            } else {
                clean.toDouble()
            }
        } catch (e: Exception) {
            0.0
        }
    }
}
