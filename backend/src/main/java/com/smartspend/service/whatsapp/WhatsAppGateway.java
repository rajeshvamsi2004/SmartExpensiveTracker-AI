package com.smartspend.service.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WhatsAppGateway {

    @Value("${whatsapp.phone-number-id:100609346426301}")
    private String phoneNumberId;

    @Value("${whatsapp.access-token:MOCK_TOKEN}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendWhatsAppMessage(String recipientPhone, String messageText) {
        // If no Meta token configured, log to console for local simulation
        if ("MOCK_TOKEN".equals(accessToken)) {
            System.out.println("================== WHATSAPP DISPATCH ================");
            System.out.println("TO: " + recipientPhone);
            System.out.println("MESSAGE:\n" + messageText);
            System.out.println("=====================================================");
            return;
        }

        String url = "https://graph.facebook.com/v20.0/" + phoneNumberId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", recipientPhone,
                "type", "text",
                "text", Map.of("body", messageText)
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            System.err.println("WhatsApp Dispatch Error: " + e.getMessage());
        }
    }
}