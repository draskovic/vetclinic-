package com.softart.vetclinic.service.email;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@Profile("prod")
public class ResendEmailService implements EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${app.email.resend.api-key:disabled}")
    private String apiKey;

    @Value("${app.email.from:onboarding@resend.dev}")
    private String fromEmail;

    @Value("${app.email.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        log.info("Resend email service initialized. Enabled: {}, From: {}", enabled, fromEmail);
    }

    @Override
    public String sendEmail(String toEmail, String subject, String body) throws EmailDeliveryException {
        if (!enabled) {
            log.info("[EMAIL DISABLED] Simulating send to: {}, subject: {}", toEmail, subject);
            return "DISABLED-" + System.currentTimeMillis();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                    "from", fromEmail,
                    "to", new String[]{toEmail},
                    "subject", subject,
                    "text", body
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    RESEND_API_URL, HttpMethod.POST, request, Map.class
            );

            String messageId = response.getBody() != null
                    ? String.valueOf(response.getBody().get("id"))
                    : "RESEND-" + System.currentTimeMillis();

            log.info("Email sent successfully to: {}, subject: {}, id: {}", toEmail, subject, messageId);
            return messageId;

        } catch (Exception e) {
            log.error("Failed to send email to: {}, subject: {}", toEmail, subject, e);
            throw new EmailDeliveryException("Failed to send email to " + toEmail + ": " + e.getMessage(), e);
        }
    }
}
