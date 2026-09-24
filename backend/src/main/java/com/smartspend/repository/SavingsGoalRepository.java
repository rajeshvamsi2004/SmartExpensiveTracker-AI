package com.smartspend.repository;

import com.smartspend.domain.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, UUID> {
    List<SavingsGoal> findByUserIdAndActiveTrue(UUID userId);
}