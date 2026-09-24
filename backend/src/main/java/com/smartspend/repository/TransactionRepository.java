package com.smartspend.repository;

import com.smartspend.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.userId = :userId
          AND LOWER(t.category) = LOWER(:category)
          AND t.timestamp >= :since
    """)
    BigDecimal sumSpendingSince(
        @Param("userId") UUID userId,
        @Param("category") String category,
        @Param("since") OffsetDateTime since
    );

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.userId = :userId
          AND t.timestamp >= :since
    """)
    BigDecimal sumTotalSpendingSince(
        @Param("userId") UUID userId,
        @Param("since") OffsetDateTime since
    );

    @Query(value = """
        SELECT AVG(amount) as mean, COALESCE(STDDEV(amount), 0) as stddev
        FROM transactions
        WHERE user_id = :userId AND LOWER(category) = LOWER(:category)
    """, nativeQuery = true)
    CategoryStats getCategoryStatistics(@Param("userId") UUID userId, @Param("category") String category);

    // Fetch transactions by category since a given timestamp
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.userId = :userId
          AND LOWER(t.category) = LOWER(:category)
          AND t.timestamp >= :since
        ORDER BY t.timestamp DESC
    """)
    List<Transaction> findByCategorySince(
        @Param("userId") UUID userId,
        @Param("category") String category,
        @Param("since") OffsetDateTime since
    );

    List<Transaction> findTop20ByUserIdOrderByTimestampDesc(UUID userId);

    List<Transaction> findByUserIdAndMerchantIgnoreCaseOrderByTimestampDesc(UUID userId, String merchant);

    interface CategoryStats {
        BigDecimal getMean();
        BigDecimal getStddev();
    }
}