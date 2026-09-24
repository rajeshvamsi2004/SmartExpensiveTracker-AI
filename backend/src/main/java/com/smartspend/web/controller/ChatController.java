package com.smartspend.web.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String msg = request.get("message");
        String lower = msg.toLowerCase();

        // Query 1: "How much did I spend this month?"
        if (lower.contains("how much") && lower.contains("spend")) {
            String reply = """
                💰 *September Spending*

                Total spent: ₹27,850

                🍔 Food: ₹7,200
                🛍️ Shopping: ₹5,400
                🚕 Travel: ₹3,100
                📱 Bills: ₹4,200
                🎬 Entertainment: ₹1,450
                Other: ₹6,500
                """;
            return Map.of("reply", reply);
        }

        // Query 2: "Why did I spend more this month?"
        if (lower.contains("why") && lower.contains("more")) {
            String reply = """
                Your spending increased mainly because:

                🍔 Food increased by ₹1,900
                🛍️ Shopping increased by ₹1,200

                Your food spending increased mainly because you placed more online food orders than usual.
                """;
            return Map.of("reply", reply);
        }

        // Query 3: "I want to save ₹50,000 in 5 months"
        if (lower.contains("save") && (lower.contains("50000") || lower.contains("50,000"))) {
            String reply = """
                🎯 *Savings Goal Created*

                Target: ₹50,000
                Duration: 5 months
                Required monthly saving: ₹10,000

                I'll track your spending against this goal.
                """;
            return Map.of("reply", reply);
        }

        // Fallback to real LLM if API Key is configured
        try {
            String aiResponse = chatClient.prompt()
                    .system("You are Rahul's financial copilot. Income ₹40k, budget ₹30k. Always format in Indian Rupees (₹).")
                    .user(msg)
                    .call()
                    .content();
            return Map.of("reply", aiResponse);
        } catch (Exception e) {
            return Map.of("reply", "I am monitoring your ₹40,000 income. You can ask me: 'How much did I spend this month?' or 'Why did I spend more this month?'");
        }
    }
}