package com.smartspend.config;

import com.smartspend.domain.CategoryBudget;
import com.smartspend.domain.User;
import com.smartspend.repository.CategoryBudgetRepository;
import com.smartspend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Configuration
public class DataSeeder {

    public static final String RAHUL_PHONE = "+919876543210";

    @Bean
    CommandLineRunner seedInitialData(UserRepository userRepo, CategoryBudgetRepository budgetRepo) {
        return args -> {
            if (userRepo.findByPhoneNumber(RAHUL_PHONE).isEmpty()) {
                // 1. Create Rahul without hardcoded ID (Hibernate generates it cleanly)
                User rahul = User.builder()
                        .name("Rahul")
                        .phoneNumber(RAHUL_PHONE)
                        .monthlyIncome(new BigDecimal("40000.00"))
                        .overallMonthlyBudget(new BigDecimal("30000.00"))
                        .build();

                User savedRahul = userRepo.save(rahul);
                UUID rahulId = savedRahul.getId();

                // 2. Setup Rahul's Budgets
                budgetRepo.saveAll(List.of(
                        CategoryBudget.builder().userId(rahulId).category("Food").allocatedAmount(new BigDecimal("5000.00")).build(),
                        CategoryBudget.builder().userId(rahulId).category("Shopping").allocatedAmount(new BigDecimal("4000.00")).build(),
                        CategoryBudget.builder().userId(rahulId).category("Travel").allocatedAmount(new BigDecimal("3000.00")).build()
                ));

                System.out.println("=================================================");
                System.out.println(">>> SEED SUCCESS: Rahul created with ID: " + rahulId);
                System.out.println(">>> Budgets: Food ₹5,000 | Shopping ₹4,000 | Travel ₹3,000");
                System.out.println("=================================================");
            }
        };
    }
}