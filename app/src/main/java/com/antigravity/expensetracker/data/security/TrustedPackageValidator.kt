package com.antigravity.expensetracker.data.security

/**
 * Validates whether an incoming notification originates from an authorized,
 * legitimate financial institution, mobile wallet, or system SMS package.
 *
 * Prevents notification spoofing attacks where arbitrary untrusted applications
 * or malicious games emit fake transaction notifications to tamper with account
 * balances and ledger records (OWASP MASVS-PLATFORM / M1).
 */
object TrustedPackageValidator {

    private val DEFAULT_TRUSTED_PACKAGES = setOf(
        // Vietnamese Banks
        "com.VCB",                              // Vietcombank Digibank
        "com.mbbank",                           // MB Bank
        "vn.com.techcombank.bb.app",            // Techcombank Mobile
        "com.techcombank.mobile",               // Techcombank Legacy
        "com.vnpay.vpbankonline",               // VPBank NEO
        "com.acb.bih.retail",                   // ACB ONE
        "com.bidv.cplus",                       // BIDV SmartOTP / SmartBanking
        "com.tpb.mb.gprsandroid",               // TPBank Mobile
        "com.vietinbank.ipay",                  // VietinBank iPay
        "com.sacombank.ewallet",                // Sacombank mBanking
        "com.hdbank.mb",                        // HDBank

        // Vietnamese Fintech & e-Wallets
        "com.mservice.momopay",                 // MoMo
        "vn.zalopay",                           // ZaloPay
        "com.viettel.viettelpay",               // Viettel Money
        "com.vnpay.wallet",                     // VNPAY Wallet
        "com.shopeepay.vn",                     // ShopeePay

        // Global / US Banks & Fintech
        "com.chase.sig.android",                // Chase Mobile
        "com.wf.wellsfargomobile",              // Wells Fargo Mobile
        "com.infonow.bofa",                     // Bank of America
        "com.citi.citimobile",                  // Citi Mobile
        "com.capitalone.mobile",                // Capital One
        "com.venmo",                            // Venmo
        "com.paypal.android",                   // PayPal
        "com.squareup.cash",                    // Cash App

        // Default Android System SMS / Messaging Apps (for bank SMS notifications)
        "com.google.android.apps.messaging",    // Google Messages
        "com.samsung.android.messaging",        // Samsung Messages
        "com.android.mms"                       // AOSP Messaging
    )

    /**
     * Checks if the package is in the trusted set.
     */
    fun isPackageTrusted(packageName: String, additionalPackages: Set<String> = emptySet()): Boolean {
        if (packageName.isBlank()) return false
        val normalized = packageName.trim()
        return DEFAULT_TRUSTED_PACKAGES.contains(normalized) || additionalPackages.contains(normalized)
    }

    /**
     * Returns the list of standard trusted package identifiers.
     */
    fun getTrustedPackages(): Set<String> = DEFAULT_TRUSTED_PACKAGES
}
