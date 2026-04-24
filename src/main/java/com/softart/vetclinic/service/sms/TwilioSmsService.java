package com.softart.vetclinic.service.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TwilioSmsService implements SmsService {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final boolean enabled;

    public TwilioSmsService(
            @Value("${app.sms.twilio.account-sid}") String accountSid,
            @Value("${app.sms.twilio.auth-token}") String authToken,
            @Value("${app.sms.twilio.from-number}") String fromNumber,
            @Value("${app.sms.twilio.enabled:false}") boolean enabled) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.enabled = enabled;
    }

    @PostConstruct
    public void init() {
        
        log.info("Twilio config: accountSid={}, enabled={}", 
                accountSid != null ? accountSid.substring(0, Math.min(5, accountSid.length())) + "..." : "null", 
                enabled);
        if (enabled) {
            try {
                Twilio.init(accountSid, authToken);
                log.info("Twilio SMS servis inicijalizovan (from: {})", fromNumber);
            } catch (Exception e) {
                log.error("Greska pri inicijalizaciji Twilio: {}", e.getMessage(), e);
            }
        } else {
            log.info("Twilio SMS servis je ONEMOGUCEN (enabled=false)");
        }
    }




    @Override
    public String sendSms(String toPhoneNumber, String messageBody, String countryCode) {
        if (!enabled) {
            log.info("[SMS DISABLED] Simulacija slanja na {}: {}", toPhoneNumber, messageBody);
            return "DISABLED-" + System.currentTimeMillis();
        }

        String normalized = normalizeToE164(toPhoneNumber, countryCode);
        log.info("Normalizovan broj: {} -> {}", toPhoneNumber, normalized);

        try {
            Message message = Message.creator(
                    new PhoneNumber(normalized),
                    new PhoneNumber(fromNumber),
                    messageBody
            ).create();

            log.info("SMS uspesno poslat na {} (SID: {})", normalized, message.getSid());
            return message.getSid();
        } catch (ApiException e) {
            log.error("Twilio API greska pri slanju SMS na {}: {} (code: {})",
                    normalized, e.getMessage(), e.getCode());
            throw new SmsDeliveryException(
                    "Twilio error [code=" + e.getCode() + "]: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Neocekivana greska pri slanju SMS na {}: {}", normalized, e.getMessage());
            throw new SmsDeliveryException("Greska pri slanju SMS: " + e.getMessage(), e);
        }
    }
    private String normalizeToE164(String phone, String countryCode) {
        if (phone == null) return null;
        String raw = phone.replaceAll("[\\s\\-()]+", "");
        if (raw.startsWith("+")) return raw;
        if (raw.startsWith("00")) return "+" + raw.substring(2);
        String digitsOnly = countryCode.startsWith("+") ? countryCode.substring(1) : countryCode;
        if (raw.startsWith("0")) return countryCode + raw.substring(1);
        if (raw.startsWith(digitsOnly)) return "+" + raw;
        return countryCode + raw;
    }
}
