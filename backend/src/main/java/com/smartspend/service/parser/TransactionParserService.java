package com.smartspend.service.parser;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TransactionParserService {

    public record ParsedResult(BigDecimal amount, String merchant, String category, boolean success) {}

    // Resilient amount pattern: Matches ₹280, Rs 280, INR 280, ?280, or "280 debited"
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:₹|rs\\.?|inr|\\?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)|([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:debited|spent|paid)"
    );

    // Fallback regex to find merchant after "to", "at", or "for"
    private static final Pattern FALLBACK_MERCHANT_PATTERN = Pattern.compile(
            "(?i)(?:to|at|for)\\s+([A-Za-z0-9]+)"
    );

    private static final Map<String, String> MERCHANT_CATEGORIES = Map.ofEntries(
            Map.entry("swiggy", "Food"),
            Map.entry("zomato", "Food"),
            Map.entry("blinkit", "Food"),
            Map.entry("zepto", "Food"),
            Map.entry("amazon", "Shopping"),
            Map.entry("flipkart", "Shopping"),
            Map.entry("myntra", "Shopping"),
            Map.entry("uber", "Travel"),
            Map.entry("ola", "Travel"),
            Map.entry("netflix", "Entertainment"),
            Map.entry("spotify", "Entertainment")
    );

    public ParsedResult parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new ParsedResult(BigDecimal.ZERO, "Unknown", "Other", false);
        }

        // 1. Extract Amount
        BigDecimal amount = null;
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(rawText);
        if (amountMatcher.find()) {
            String rawAmount = amountMatcher.group(1) != null ? amountMatcher.group(1) : amountMatcher.group(2);
            amount = new BigDecimal(rawAmount.replace(",", ""));
        } else {
            // General digit search as fallback
            Matcher digitMatcher = Pattern.compile("([0-9]+(?:\\.[0-9]{1,2})?)").matcher(rawText);
            if (digitMatcher.find()) {
                amount = new BigDecimal(digitMatcher.group(1));
            }
        }

        if (amount == null) {
            return new ParsedResult(BigDecimal.ZERO, "Unknown", "Other", false);
        }

        // 2. Identify Merchant & Category
        String lowerText = rawText.toLowerCase();
        String identifiedMerchant = null;
        String matchedCategory = "Other";

        for (var entry : MERCHANT_CATEGORIES.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                identifiedMerchant = Character.toUpperCase(entry.getKey().charAt(0)) + entry.getKey().substring(1);
                matchedCategory = entry.getValue();
                break;
            }
        }

        // 3. Fallback merchant name from context if unknown
        if (identifiedMerchant == null) {
            Matcher merchantMatcher = FALLBACK_MERCHANT_PATTERN.matcher(rawText);
            if (merchantMatcher.find()) {
                identifiedMerchant = merchantMatcher.group(1);
            } else {
                identifiedMerchant = "Unknown Merchant";
            }
        }

        return new ParsedResult(amount, identifiedMerchant, matchedCategory, true);
    }
}