package com.smartspend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "savings_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "target_amount", nullable = false)
    private BigDecimal targetAmount;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "monthly_saving_target", nullable = false)
    private BigDecimal monthlySavingTarget;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "is_active")
    private boolean active;
}