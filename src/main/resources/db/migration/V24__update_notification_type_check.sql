ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check;

ALTER TABLE notification ADD CONSTRAINT notification_type_check
    CHECK (type IN (
        'APPOINTMENT_REMINDER',
        'VACCINATION_DUE',
        'INVOICE_DUE',
        'FOLLOW_UP',
        'BOOKING_CREATED',
        'BOOKING_CONFIRMED',
        'BOOKING_CANCELLED'
    ));