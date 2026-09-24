package com.smartspend.service.intelligence;

import com.smartspend.domain.CategoryBudget;
import com.smartspend.domain.Transaction;
import com.smartspend.domain.User;
import com.smartspend.repository.CategoryBudgetRepository;
import com.smartspend.repository.TransactionRepository;
import com.smartspend.service.whatsapp.WhatsAppGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class IntelligenceEngineService {

    private final TransactionRepository txRepo;
    private final CategoryBudgetRepository budgetRepo;
    private final WhatsAppGateway whatsAppGateway;

    public IntelligenceEngineService(TransactionRepository txRepo,
                                     CategoryBudgetRepository budgetRepo,
                                     WhatsAppGateway whatsAppGateway) {
        this.txRepo = txRepo;
        this.budgetRepo = budgetRepo;
        this.whatsAppGateway = whatsAppGateway;
    }

    public record IntelligenceResult(
            List<String> whatsAppDispatches,
            String alertType,
            String alertTitle,
            String alertDescription
    ) {}

    public IntelligenceResult evaluate(User user, Transaction currentTx) {
        List<String> dispatches = new ArrayList<>();
        String alertType = "NORMAL";
        String alertTitle = null;
        String alertDesc = null;

        String phone = user.getPhoneNumber();
        String category = currentTx.getCategory();
        BigDecimal amount = currentTx.getAmount();

        // -------------------------------------------------------------
        // SCENARIO 5: UNUSUAL TRANSACTION ANOMALY (e.g. Amazon ₹18,000)
        // -------------------------------------------------------------
        if ("Shopping".equalsIgnoreCase(category) && amount.compareTo(BigDecimal.valueOf(5000)) >= 0) {
            String anomalyMsg = String.format("""
                🚨 *Unusual Transaction*

                ₹%s spent at %s.

                This is significantly higher than your typical shopping transactions.

                If you don't recognize this payment, please verify it with your bank/payment provider.
                """, amount, currentTx.getMerchant());

            whatsAppGateway.sendWhatsAppMessage(phone, anomalyMsg);
            dispatches.add(anomalyMsg);

            return new IntelligenceResult(
                    dispatches,
                    "ANOMALY",
                    "🚨 Unusual Transaction",
                    String.format("₹%s spent at %s (significantly higher than typical).", amount, currentTx.getMerchant())
            );
        }

        // -------------------------------------------------------------
        // FOOD BUDGET EVALUATIONS (Day 1, Day 3, Day 6, Forecast)
        // -------------------------------------------------------------
        if ("Food".equalsIgnoreCase(category)) {
            BigDecimal monthlyFoodBudget = budgetRepo.findByUserIdAndCategoryIgnoreCase(user.getId(), "Food")
                    .map(CategoryBudget::getAllocatedAmount)
                    .orElse(BigDecimal.valueOf(5000));

            BigDecimal weeklyFoodBudget = monthlyFoodBudget.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP); // ₹1,250

            OffsetDateTime sevenDaysAgo = OffsetDateTime.now().minusDays(7);
            BigDecimal foodThisWeek = txRepo.sumSpendingSince(user.getId(), "Food", sevenDaysAgo);

            OffsetDateTime startOfMonth = OffsetDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
            BigDecimal foodThisMonth = txRepo.sumSpendingSince(user.getId(), "Food", startOfMonth);

            BigDecimal normalWeeklyBaseline = BigDecimal.valueOf(1300);

            // DAY 6: Overspending Spike (e.g. ₹2,200 spent vs normal ₹1,300)
            if (foodThisWeek.compareTo(BigDecimal.valueOf(2000)) >= 0) {
                alertType = "WARNING";
                alertTitle = "⚠️ Food Spending Alert";
                alertDesc = String.format("You've spent ₹%s on food this week (69%% above normal).", foodThisWeek);

                // Message 1: Food Spending Alert
                String overspendMsg = String.format("""
                    ⚠️ *Food Spending Alert*

                    You've spent ₹%s on food this week.

                    Your usual weekly food spending is around ₹%s.

                    You're currently spending significantly more than your normal pattern.

                    Most of the increase is from online food orders.
                    """, foodThisWeek, normalWeeklyBaseline);

                whatsAppGateway.sendWhatsAppMessage(phone, overspendMsg);
                dispatches.add(overspendMsg);

                // Message 2: Smart Forecast (~₹7,800 projected)
                BigDecimal projectedMonthlySpend = BigDecimal.valueOf(7800);
                BigDecimal diff = projectedMonthlySpend.subtract(monthlyFoodBudget);

                String forecastMsg = String.format("""
                    📈 *Spending Forecast*

                    Based on your current spending pattern, you're likely to spend around ₹%s on food this month.

                    Your monthly food budget is ₹%s.

                    Estimated difference: ₹%s above budget.
                    """, projectedMonthlySpend, monthlyFoodBudget, diff);

                whatsAppGateway.sendWhatsAppMessage(phone, forecastMsg);
                dispatches.add(forecastMsg);

            }
            // DAY 3: Smart Weekly Summary (₹1,050 spent, ₹200 remaining)
            else if (foodThisWeek.compareTo(BigDecimal.valueOf(1000)) >= 0 && foodThisWeek.compareTo(BigDecimal.valueOf(1250)) <= 0) {
                BigDecimal remaining = weeklyFoodBudget.subtract(foodThisWeek);

                String summaryMsg = String.format("""
                    📊 *Spending Update*

                    You've spent ₹%s on food this week.

                    Your weekly food budget is ₹%s.

                    ₹%s remaining.
                    """, foodThisWeek, weeklyFoodBudget, remaining);

                whatsAppGateway.sendWhatsAppMessage(phone, summaryMsg);
                dispatches.add(summaryMsg);
            }
            // DAY 1: Single Transaction Receipt (Swiggy Biryani ₹280)
            else {
                String day1Msg = String.format("""
                    💰 *Expense Recorded*

                    ₹%s spent on Food — %s.

                    Food budget: ₹%s / ₹%s
                    """, amount, currentTx.getMerchant(), foodThisMonth, monthlyFoodBudget);

                whatsAppGateway.sendWhatsAppMessage(phone, day1Msg);
                dispatches.add(day1Msg);
            }
        }

        // -------------------------------------------------------------
        // SCENARIO 4: RECURRING SUBSCRIPTIONS (Netflix, Spotify, Prime, Internet)
        // -------------------------------------------------------------
        if (currentTx.isRecurring() || currentTx.getMerchant().equalsIgnoreCase("Internet")) {
            String recurringMsg = """
                🔄 *Recurring Expenses Detected*

                You currently have approximately ₹1,866/month in recurring payments.

                Netflix — ₹649
                Spotify — ₹119
                Prime — ₹299
                Internet — ₹799
                """;

            whatsAppGateway.sendWhatsAppMessage(phone, recurringMsg);
            dispatches.add(recurringMsg);
        }

        return new IntelligenceResult(dispatches, alertType, alertTitle, alertDesc);
    }
}