package com.smartspend.web.controller;

import com.smartspend.service.whatsapp.WhatsAppGateway;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppWebhookController {

    private final WhatsAppGateway whatsAppGateway;
    private final ChatClient chatClient;

    public WhatsAppWebhookController(WhatsAppGateway whatsAppGateway, ChatClient.Builder chatClientBuilder) {
        this.whatsAppGateway = whatsAppGateway;
        this.chatClient = chatClientBuilder.build();
    }

    // 1. Meta Webhook Verification Challenge
    @GetMapping("/webhook")
    public String verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && "smartspend_token".equals(token)) {
            return challenge;
        }
        return "Invalid verify token";
    }

    // 2. Inbound WhatsApp Message Handler
    @PostMapping("/webhook")
    public void receiveWhatsAppMessage(@RequestBody Map<String, Object> payload) {
        try {
            List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
            if (entries == null || entries.isEmpty()) return;

            Map<String, Object> entry = entries.get(0);
            List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
            if (changes == null || changes.isEmpty()) return;

            Map<String, Object> value = (Map<String, Object>) changes.get(0).get("value");
            List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
            if (messages == null || messages.isEmpty()) return;

            Map<String, Object> message = messages.get(0);
            String from = (String) message.get("from"); // Rahul's WhatsApp number
            Map<String, Object> textObj = (Map<String, Object>) message.get("text");
            String body = (String) textObj.get("body");

            // Process message via Spring AI
            String aiReply = chatClient.prompt()
                    .system("You are Rahul's financial wealth copilot on WhatsApp. Reply with emojis, bullet points, and figures in ₹.")
                    .user(body)
                    .call()
                    .content();

            // Reply back directly to Rahul's WhatsApp
            whatsAppGateway.sendWhatsAppMessage(from, aiReply);

        } catch (Exception e) {
            System.err.println("Webhook processing exception: " + e.getMessage());
        }
    }
}