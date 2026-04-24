package com.softart.vetclinic.service.sms;

public interface SmsService {

	/**
	 * Salje SMS na zadati broj telefona.
	 * @param toPhoneNumber broj primaoca (lokalni ili E.164)
	 * @param messageBody tekst poruke
	 * @param countryCode pozivni broj države klinike (npr. "+381")
	 * @return provider message ID
	 */
	String sendSms(String toPhoneNumber, String messageBody, String countryCode);
}
