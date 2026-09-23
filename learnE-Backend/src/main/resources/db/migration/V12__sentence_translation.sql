-- Vietnamese -> Chinese sentence translation, unlocked once every word of a day is mastered.

-- 1. Boya lessons are split across several days, so a day needs to say which lesson it came from.
ALTER TABLE lesson_days
    ADD COLUMN source_lesson_label VARCHAR(60),
    ADD COLUMN part_index INT;

-- 2. The sentences themselves, generated from the day's vocabulary plus everything learnt before.
CREATE TABLE sentence_exercises (
    id BIGSERIAL PRIMARY KEY,
    lesson_day_id BIGINT NOT NULL REFERENCES lesson_days(id),
    prompt_vi TEXT NOT NULL,
    answer_target TEXT NOT NULL,
    answer_phonetic TEXT,
    -- Words the sentence needs but the learner has not met yet, as [{term, phonetic, meaning}].
    new_words_json TEXT,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_sentence_exercises_day ON sentence_exercises (lesson_day_id, sort_order);

-- 3. One row per attempt, so earlier tries stay visible alongside the AI's feedback.
CREATE TABLE user_sentence_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    exercise_id BIGINT NOT NULL REFERENCES sentence_exercises(id),
    answer_target TEXT NOT NULL,
    score INT,
    feedback_json TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_sentence_attempts_user_exercise ON user_sentence_attempts (user_id, exercise_id);
