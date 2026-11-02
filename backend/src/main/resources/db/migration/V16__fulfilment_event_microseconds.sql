-- Preserve the monotonic event timestamps on MySQL, whose plain TIMESTAMP defaults to seconds.
ALTER TABLE fulfilment_events MODIFY COLUMN occurred_at TIMESTAMP(6) NOT NULL;
