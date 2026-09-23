-- Multiple choice now comes in two directions, so the old column gets an explicit name.
ALTER TABLE user_word_progress RENAME COLUMN mastered_multiple_choice TO mastered_multiple_choice_vi;

-- New mode: shown the Vietnamese meaning, pick the English word.
ALTER TABLE user_word_progress
    ADD COLUMN mastered_multiple_choice_en BOOLEAN NOT NULL DEFAULT FALSE;
