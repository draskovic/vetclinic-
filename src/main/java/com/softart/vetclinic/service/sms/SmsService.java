package com.softart.vetclinic.service.sms;

public interface SmsService {

    /**
     * Salje SMS na zadati broj telefona.
     * @param toPhoneNumber broj primaoca (E.164 format, npr. "+381641234567")
     * @param messageBody tekst poruke
     * @return provider message ID (npr. Twilio SID)
     * @throws SmsDeliveryException ako slanje ne uspe
     */
    String sendSms(String toPhoneNumber, String messageBody);
}
