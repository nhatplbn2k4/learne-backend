-- Grammar courses: lessons of patterns rather than days of words.
--
-- Deliberately separate from lesson_days / sentence_exercises. Those carry live data (113 days,
-- 225 sentences) and drive the day-unlock rule and the skip-ahead question pool; making their
-- foreign keys nullable to squeeze grammar in would put every existing query at risk. Worse,
-- VocabularyService.tryCompleteLessonDay treats a day with no words as already finished, which a
-- grammar "day" would trip on its own.

CREATE TABLE grammar_lessons (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id),
    -- Stable handle the AI quotes when it spots this mistake while grading. Unique across every
    -- course: one grammar point is one lesson, even when several textbooks revisit it.
    code VARCHAR(60) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    summary TEXT,
    formula TEXT NOT NULL,
    -- [{part, meaning}] — what each piece of the formula stands for.
    components_json TEXT,
    -- [{target, phonetic, vi, note}]
    examples_json TEXT,
    notes TEXT,
    -- Which textbook lesson this came from, matching lesson_days.source_lesson_label ("Bài 16").
    source_lesson_label VARCHAR(60),
    -- 1..5, and the number of drill sentences the lesson is worth: 5, 10, 15, 20, 25.
    difficulty INT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_grammar_lessons_course ON grammar_lessons (course_id, sort_order);

CREATE TABLE grammar_exercises (
    id BIGSERIAL PRIMARY KEY,
    grammar_lesson_id BIGINT NOT NULL REFERENCES grammar_lessons(id),
    prompt_vi TEXT NOT NULL,
    answer_target TEXT NOT NULL,
    answer_phonetic TEXT,
    -- Words the sentence needs but the learner may not have met yet, as [{term, phonetic, meaning}].
    new_words_json TEXT,
    -- Which case of the pattern this one drills, e.g. "phủ định" or "câu hỏi".
    case_label VARCHAR(120),
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_grammar_exercises_lesson ON grammar_exercises (grammar_lesson_id, sort_order);

CREATE TABLE user_grammar_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    exercise_id BIGINT NOT NULL REFERENCES grammar_exercises(id),
    answer_target TEXT NOT NULL,
    score INT,
    feedback_json TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_grammar_attempts_user ON user_grammar_attempts (user_id, exercise_id);

-- One translated sentence is enough to count the lesson as studied, whatever it scored.
CREATE TABLE user_grammar_lesson_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    grammar_lesson_id BIGINT NOT NULL REFERENCES grammar_lessons(id),
    first_completed_at TIMESTAMP NOT NULL DEFAULT now(),
    last_practised_at TIMESTAMP NOT NULL DEFAULT now(),
    exercises_done INT NOT NULL DEFAULT 0,
    UNIQUE (user_id, grammar_lesson_id)
);
