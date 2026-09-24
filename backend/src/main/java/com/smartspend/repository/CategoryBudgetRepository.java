package com.smartspend.repository;

import com.smartspend.domain.CategoryBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryBudgetRepository extends JpaRepository<CategoryBudget, UUID> {
    List<CategoryBudget> findByUserId(UUID userId);
    Optional<CategoryBudget> findByUserIdAndCategoryIgnoreCase(UUID userId, String category);
}