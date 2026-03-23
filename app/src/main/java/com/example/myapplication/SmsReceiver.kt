package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val body = message.displayMessageBody
                val expense = parseExpense(body)
                if (expense != null) {
                    ExpenseRepository.addExpense(expense)
                }
            }
        }
    }

    private fun parseExpense(message: String): Expense? {
        // Simple regex to find amounts
        val amountPattern = Pattern.compile("(?i)(?:rs|inr|usd|\\$)\\.?\\s*([\\d,]+\\.?\\d*)")
        val matcher = amountPattern.matcher(message)

        if (matcher.find()) {
            val amountStr = matcher.group(1) ?: return null
            // Convert the matched amount string to Double, removing any commas
            val amount = amountStr.replace(",", "").toDoubleOrNull() ?: return null
            
            // Extract currency
            val fullMatch = matcher.group(0) ?: ""
            val currency = when {
                fullMatch.contains("$", ignoreCase = true) -> "USD"
                fullMatch.contains("rs", ignoreCase = true) -> "Rs"
                fullMatch.contains("inr", ignoreCase = true) -> "INR"
                else -> "Rs"
            }
            
            val category = ExpenseRepository.categorize(message)
            val analysis = ExpenseRepository.analyzeSpend(message, category)
            
            return Expense(
                amount = amount,
                currency = currency,
                description = message,
                timestamp = System.currentTimeMillis(),
                category = category,
                isGoodSpend = analysis.first,
                suggestion = analysis.second
            )
        }
        return null
    }
}
