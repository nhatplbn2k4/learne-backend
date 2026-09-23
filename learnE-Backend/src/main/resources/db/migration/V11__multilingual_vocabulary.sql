-- Makes the vocabulary model language-neutral so Chinese (Boya) can live beside English.

-- 1. Defensive re-run of V10's backfill before the per-mode columns disappear.
UPDATE user_word_progress SET mastered = TRUE
WHERE mastered_flashcard
  AND mastered_listen_type
  AND mastered_translate_type
  AND mastered_multiple_choice_vi
  AND mastered_multiple_choice_en;

-- 2. One column holding the set of modes aced in the current session replaces the fixed booleans,
--    so each language can require its own set of practice modes.
ALTER TABLE user_word_progress ADD COLUMN session_correct_modes VARCHAR(255);

ALTER TABLE user_word_progress
    DROP COLUMN mastered_flashcard,
    DROP COLUMN mastered_listen_type,
    DROP COLUMN mastered_translate_type,
    DROP COLUMN mastered_multiple_choice_vi,
    DROP COLUMN mastered_multiple_choice_en;

-- 3. Language-neutral word columns (catalog-only renames: no table rewrite, no data touched).
--    term     = "book" / "学生"
--    phonetic = "/bʊk/" / "xuésheng"
ALTER TABLE words RENAME COLUMN english_text TO term;
ALTER TABLE words RENAME COLUMN ipa TO phonetic;
ALTER TABLE words RENAME COLUMN example_sentence_en TO example_sentence_target;

ALTER TABLE words
    ALTER COLUMN term TYPE VARCHAR(200),
    ALTER COLUMN phonetic TYPE VARCHAR(200);

ALTER TABLE words
    ADD COLUMN han_viet VARCHAR(200),
    ADD COLUMN usage_note TEXT;

-- 4. Language lives on the topic: every word reaches one through lesson_day -> course -> topic.
ALTER TABLE topics ADD COLUMN language VARCHAR(20) NOT NULL DEFAULT 'ENGLISH';

-- 5. Boya Sơ cấp I and II are both "BEGINNER" under one topic, which the old unique index forbids.
ALTER TABLE courses DROP CONSTRAINT courses_topic_id_level_key;
ALTER TABLE courses ADD CONSTRAINT courses_topic_title_key UNIQUE (topic_id, title);

ALTER TABLE courses
    ADD COLUMN level_label VARCHAR(60),
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0;
