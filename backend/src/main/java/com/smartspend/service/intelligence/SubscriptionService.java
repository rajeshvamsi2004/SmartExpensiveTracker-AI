package com.smartspend.service.intelligence;

import com.smartspend.domain.Transaction;
import com.smartspend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private final TransactionRepository txRepo;

    public SubscriptionService(TransactionRepository txRepo) {
        this.txRepo = txRepo;
    }

    public record SubscriptionSummary(
            List<RecurringItem> subscriptions,
            BigDecimal totalMonthlyCost
    ) {}

    public record RecurringItem(String merchant, BigDecimal amount, String category) {}

    // Known common recurring subscription services in India
    private static final Set<String> KNOWN_SUBSCRIPTION_MERCHANTS = Set.of(
            "netflix", "spotify", "amazon prime", "prime", "hotstar", "youtube", "internet", "wifi", "airtel", "jio"
    );

    public SubscriptionSummary detectSubscriptions(UUID userId) {
        OffsetDateTime last60Days = OffsetDateTime.now().minusDays(60);
        List<Transaction> recentTxs = txRepo.findByCategorySince(userId, "Entertainment", last60Days);
        recentTxs.addAll(txRepo.findByCategorySince(userId, "Bills", last60Days));

        Map<String, List<Transaction>> byMerchant = recentTxs.stream()
                .collect(Collectors.groupingBy(tx -> tx.getMerchant().toLowerCase()));

        List<RecurringItem> detected = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (var entry : byMerchant.entrySet()) {
            String merchantKey = entry.getKey();
            List<Transaction> txList = entry.getValue();

            boolean isKnown = KNOWN_SUBSCRIPTION_MERCHANTS.stream().anyMatch(merchantKey::contains);

            if (isKnown || txList.size() >= 2) {
                BigDecimal avgAmount = txList.get(0).getAmount();
                String displayName = txList.get(0).getMerchant();
                String category = txList.get(0).getCategory();

                detected.add(new RecurringItem(displayName, avgAmount, category));
                total = total.add(avgAmount);
            }
        }

        return new SubscriptionSummary(detected, total);
    }
}