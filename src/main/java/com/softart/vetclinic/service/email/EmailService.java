package com.softart.vetclinic.service.email;

public interface EmailService {

    /**
     * Sends a plain-text email.
     *
     * @param toEmail   recipient email address
     * @param subject   email subject
     * @param body      email body (plain text)
     * @return message ID or status identifier
     * @throws EmailDeliveryException if sending fails
     */
    String sendEmail(String toEmail, String subject, String body) throws EmailDeliveryException;
}
