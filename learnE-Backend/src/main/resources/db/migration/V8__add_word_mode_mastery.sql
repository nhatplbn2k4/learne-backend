-- A word counts as fully mastered only after it has been answered correctly in every practice mode.
ALTER TABLE user_word_progress
    ADD COLUMN mastered_flashcard       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN mastered_listen_type     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN mastered_translate_type  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN mastered_multiple_choice BOOLEAN NOT NULL DEFAULT FALSE;
