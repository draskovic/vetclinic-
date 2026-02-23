package com.softart.vetclinic.service.sms;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
        if (enabled) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS servis inicijalizovan (from: {})", fromNumber);
        } else {
            log.info("Twilio SMS servis je ONEMOGUCEN (enabled=false)");
        }
    }

    @Override
    public String sendSms(String toPhoneNumber, String messageBody) {
        if (!enabled) {
            log.info("[SMS DISABLED] Simulacija slanja na {}: {}", toPhoneNumber, messageBody);
            return "DISABLED-" + System.currentTimeMillis();
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromNumber),
                    messageBody
            ).create();

            log.info("SMS uspesno poslat na {} (SID: {})", toPhoneNumber, message.getSid());
            return message.getSid();
        } catch (ApiException e) {
            log.error("Twilio API greska pri slanju SMS na {}: {} (code: {})",
                    toPhoneNumber, e.getMessage(), e.getCode());
            throw new SmsDeliveryException(
                    "Twilio error [code=" + e.getCode() + "]: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Neocekivana greska pri slanju SMS na {}: {}", toPhoneNumber, e.getMessage());
            throw new SmsDeliveryException("Greska pri slanju SMS: " + e.getMessage(), e);
        }
    }
}
