ALTER TABLE order_access_challenges ADD COLUMN login_email VARCHAR(254);
ALTER TABLE fulfilment_events ADD COLUMN notification_eligible BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE fulfilment_events ADD COLUMN notification_sent_at TIMESTAMP(6);
ALTER TABLE fulfilment_events ADD COLUMN notification_next_attempt_at TIMESTAMP(6);
ALTER TABLE fulfilment_events ADD COLUMN notification_attempts INT NOT NULL DEFAULT 0;
CREATE INDEX fulfilment_mail_queue ON fulfilment_events(notification_eligible, notification_sent_at, notification_next_attempt_at);
CREATE INDEX orders_email_history ON pending_orders(email,created_at);
