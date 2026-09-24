package com.smartspend.web.controller;

import com.smartspend.domain.CategoryBudget;
import com.smartspend.domain.Transaction;
import com.smartspend.domain.User;
import com.smartspend.repository.CategoryBudgetRepository;
import com.smartspend.repository.TransactionRepository;
import com.smartspend.repository.UserRepository;
import com.smartspend.service.intelligence.IntelligenceEngineService;
import com.smartspend.service.intelligence.IntelligenceEngineService.IntelligenceResult;
import com.smartspend.service.parser.TransactionParserService;
import com.smartspend.service.parser.TransactionParserService.ParsedResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TransactionController {

    private final UserRepository userRepo;
    private final CategoryBudgetRepository budgetRepo;
    private final TransactionRepository txRepo;
    private final TransactionParserService parserService;
    private final IntelligenceEngineService intelligenceService;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public TransactionController(UserRepository userRepo,
                                 CategoryBudgetRepository budgetRepo,
                                 TransactionRepository txRepo,
                                 TransactionParserService parserService,
                                 IntelligenceEngineService intelligenceService) {
        this.userRepo = userRepo;
        this.budgetRepo = budgetRepo;
        this.txRepo = txRepo;
        this.parserService = parserService;
        this.intelligenceService = intelligenceService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard(@RequestParam(defaultValue = "+919876543210") String phoneNumber) {
        User user = userRepo.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));

        OffsetDateTime startOfMonth = OffsetDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);

        BigDecimal foodSpend = txRepo.sumSpendingSince(user.getId(), "Food", startOfMonth);
        BigDecimal shoppingSpend = txRepo.sumSpendingSince(user.getId(), "Shopping", startOfMonth);
        BigDecimal travelSpend = txRepo.sumSpendingSince(user.getId(), "Travel", startOfMonth);
        BigDecimal totalSpent = txRepo.sumTotalSpendingSince(user.getId(), startOfMonth);

        List<Transaction> recent = txRepo.findTop20ByUserIdOrderByTimestampDesc(user.getId());

        return Map.of(
                "user", user,
                "totalSpent", totalSpent,
                "foodSpend", foodSpend,
                "shoppingSpend", shoppingSpend,
                "travelSpend", travelSpend,
                "recentTransactions", recent
        );
    }

    @PostMapping("/transactions/ingest")
    public Map<String, Object> ingestNotification(@RequestBody Map<String, String> request) {
        String rawText = request.get("rawText");
        String phoneNumber = request.getOrDefault("phoneNumber", "+919876543210");

        User user = userRepo.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> userRepo.save(User.builder()
                        .name("Rahul")
                        .phoneNumber(phoneNumber)
                        .monthlyIncome(new BigDecimal("40000.00"))
                        .overallMonthlyBudget(new BigDecimal("30000.00"))
                        .build()));

        ParsedResult parsed = parserService.parse(rawText);
        if (!parsed.success()) {
            return Map.of("status", "IGNORED", "reason", "Could not extract payment details");
        }

        boolean isSub = rawText.toLowerCase().contains("netflix") ||
                        rawText.toLowerCase().contains("spotify") ||
                        rawText.toLowerCase().contains("prime") ||
                        rawText.toLowerCase().contains("internet");

        Transaction tx = Transaction.builder()
                .userId(user.getId())
                .amount(parsed.amount())
                .merchant(parsed.merchant())
                .category(parsed.category())
                .rawPayload(rawText)
                .timestamp(OffsetDateTime.now())
                .recurring(isSub)
                .build();

        Transaction savedTx = txRepo.save(tx);

        // Run all intelligence evaluations
        IntelligenceResult intelligence = intelligenceService.evaluate(user, savedTx);

        // Broadcast to React UI via SSE
        Map<String, Object> eventData = Map.of(
                "transaction", savedTx,
                "intelligence", intelligence
        );
        broadcastEvent("TRANSACTION_SAVED", eventData);

        return Map.of(
                "status", "RECORDED",
                "transaction", savedTx,
                "dispatchedWhatsAppMessages", intelligence.whatsAppDispatches()
        );
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    private void broadcastEvent(String name, Object data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(name).data(data));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}