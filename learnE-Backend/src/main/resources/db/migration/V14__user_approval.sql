-- Registration is open again, but a new account cannot be used until an admin approves it.

ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Everyone who already had an account keeps working; only accounts created from now on wait.
UPDATE users SET status = 'ACTIVE';
