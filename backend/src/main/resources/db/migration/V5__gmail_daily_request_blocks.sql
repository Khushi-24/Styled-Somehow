CREATE TABLE order_access_email_blocks (email_hash VARCHAR(64) PRIMARY KEY, blocked_until TIMESTAMP(6));
-- Old challenge hashes used the literal email, not the canonical Gmail inbox.
-- Invalidate existing short-lived codes so alias limits start consistently.
DELETE FROM order_access_challenges;
