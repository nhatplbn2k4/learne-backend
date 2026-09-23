-- Mastery is now earned within a single practice session instead of accumulating across sessions.
-- The five per-mode columns keep their meaning but are scoped to the session in `mastery_session_id`.
ALTER TABLE user_word_progress
    ADD COLUMN mastered BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN mastery_session_id VARCHAR(64);

-- Anyone who already had every mode ticked keeps the mastery they earned under the old rule.
UPDATE user_word_progress
SET mastered = TRUE
WHERE mastered_flashcard
  AND mastered_listen_type
  AND mastered_translate_type
  AND mastered_multiple_choice_vi
  AND mastered_multiple_choice_en;
