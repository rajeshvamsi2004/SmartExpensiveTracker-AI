package com.smartspend.service.ai;

import com.smartspend.domain.SavingsGoal;
import com.smartspend.domain.User;
import com.smartspend.repository.CategoryBudgetRepository;
import com.smartspend.repository.SavingsGoalRepository;
import com.smartspend.repository.TransactionRepository;
import com.smartspend.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class FinancialTools {

    public record SpendQueryRequest(String category, int daysBack) {}
    public record SpendQueryResponse(String category, BigDecimal amountSpent, int daysBack) {}

    public record CreateGoalRequest(double targetAmount, int durationMonths) {}
    public record CreateGoalResponse(String status, BigDecimal target, BigDecimal monthlyTarget, int durationMonths) {}

    @Bean
    @Description("Get how much money Rahul has spent in a specific category (e.g., Food, Shopping, Travel) over the past N days.")
    public Function<SpendQueryRequest, SpendQueryResponse> getCategorySpending(
            UserRepository userRepo, TransactionRepository txRepo) {
        return request -> {
            User rahul = userRepo.findByPhoneNumber("+919876543210").orElseThrow();
            OffsetDateTime since = OffsetDateTime.now().minusDays(request.daysBack() <= 0 ? 30 : request.daysBack());
            BigDecimal spent = txRepo.sumSpendingSince(rahul.getId(), request.category(), since);
            return new SpendQueryResponse(request.category(), spent, request.daysBack());
        };
    }

    @Bean
    @Description("Create a new savings goal for Rahul with a target amount and duration in months.")
    public Function<CreateGoalRequest, CreateGoalResponse> createSavingsGoal(
            UserRepository userRepo, SavingsGoalRepository goalRepo) {
        return request -> {
            User rahul = userRepo.findByPhoneNumber("+919876543210").orElseThrow();
            BigDecimal target = BigDecimal.valueOf(request.targetAmount());
            BigDecimal monthlyTarget = target.divide(BigDecimal.valueOf(request.durationMonths()), 2, RoundingMode.HALF_UP);

            SavingsGoal goal = SavingsGoal.builder()
                    .userId(rahul.getId())
                    .targetAmount(target)
                    .durationMonths(request.durationMonths())
                    .monthlySavingTarget(monthlyTarget)
                    .startDate(LocalDate.now())
                    .active(true)
                    .build();

            goalRepo.save(goal);

            return new CreateGoalResponse("SUCCESS", target, monthlyTarget, request.durationMonths());
        };
    }
}