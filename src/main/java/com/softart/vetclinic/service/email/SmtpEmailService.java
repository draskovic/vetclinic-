package com.softart.vetclinic.service.email;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("!prod")
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@vetclinic.com}")
    private String fromEmail;

    @Value("${app.email.enabled:false}")
    private boolean enabled;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void init() {
        log.info("Email service initialized. Enabled: {}, From: {}", enabled, fromEmail);
    }

    @Override
    public String sendEmail(String toEmail, String subject, String body) throws EmailDeliveryException {
        if (!enabled) {
            log.info("[EMAIL DISABLED] Simulating send to: {}, subject: {}", toEmail, subject);
            return "DISABLED-" + System.currentTimeMillis();
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            String messageId = "EMAIL-" + System.currentTimeMillis();
            log.info("Email sent successfully to: {}, subject: {}, id: {}", toEmail, subject, messageId);
            return messageId;
        } catch (Exception e) {
            log.error("Failed to send email to: {}, subject: {}", toEmail, subject, e);
            throw new EmailDeliveryException("Failed to send email to " + toEmail + ": " + e.getMessage(), e);
        }
    }
}
