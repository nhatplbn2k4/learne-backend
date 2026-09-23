-- "Thi vượt": pass a 20-sentence translation test to jump straight to a later day.

-- How far this learner has unlocked by testing out, independent of day-by-day progress.
ALTER TABLE user_course_enrollments
    ADD COLUMN skip_ahead_day_number INT NOT NULL DEFAULT 0;

CREATE TABLE skip_ahead_tests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    course_id BIGINT NOT NULL REFERENCES courses(id),
    target_day_number INT NOT NULL,
    required_sentences INT NOT NULL,
    min_average NUMERIC(4,2) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT now(),
    -- Null while the test is still in progress; only one open test per user at a time.
    finished_at TIMESTAMP,
    passed BOOLEAN,
    average_score NUMERIC(4,2)
);

CREATE INDEX idx_skip_tests_user ON skip_ahead_tests (user_id, course_id, finished_at);

-- The 20 questions are frozen into the test, so editing or deleting the source practice
-- sentences later cannot change a test that is under way or already sat.
CREATE TABLE skip_ahead_test_items (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL REFERENCES skip_ahead_tests(id) ON DELETE CASCADE,
    sort_order INT NOT NULL,
    prompt_vi TEXT NOT NULL,
    answer_target TEXT NOT NULL,
    answer_phonetic TEXT,
    /* Where the question came from: an existing practice sentence, or null when AI-generated. */
    source_exercise_id BIGINT,
    /* One shot only: filled on submit and never overwritten. */
    answer_submitted TEXT,
    submitted_at TIMESTAMP,
    score INT,
    feedback_json TEXT
);

CREATE INDEX idx_skip_items_test ON skip_ahead_test_items (test_id, sort_order);
