CREATE TABLE topics (
    id          BIGSERIAL PRIMARY KEY,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(500)
);

CREATE TABLE courses (
    id          BIGSERIAL PRIMARY KEY,
    topic_id    BIGINT NOT NULL REFERENCES topics(id),
    level       VARCHAR(20) NOT NULL,
    title       VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    UNIQUE (topic_id, level)
);

CREATE TABLE lesson_days (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES courses(id),
    day_number  INT NOT NULL,
    title       VARCHAR(150),
    UNIQUE (course_id, day_number)
);

CREATE TABLE words (
    id                    BIGSERIAL PRIMARY KEY,
    lesson_day_id         BIGINT NOT NULL REFERENCES lesson_days(id),
    english_text          VARCHAR(150) NOT NULL,
    ipa                   VARCHAR(150),
    part_of_speech        VARCHAR(50),
    vietnamese_meaning    VARCHAR(300) NOT NULL,
    example_sentence_en   VARCHAR(500),
    example_sentence_vi   VARCHAR(500)
);

CREATE TABLE user_course_enrollments (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    course_id           BIGINT NOT NULL REFERENCES courses(id),
    current_day_number  INT NOT NULL DEFAULT 1,
    streak_count        INT NOT NULL DEFAULT 0,
    last_study_date     DATE,
    started_at          TIMESTAMP NOT NULL DEFAULT now(),
    completed_at        TIMESTAMP,
    UNIQUE (user_id, course_id)
);

CREATE TABLE user_lesson_day_progress (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users(id),
    lesson_day_id  BIGINT NOT NULL REFERENCES lesson_days(id),
    completed_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, lesson_day_id)
);

CREATE TABLE user_word_progress (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(id),
    word_id           BIGINT NOT NULL REFERENCES words(id),
    ease_factor       DOUBLE PRECISION NOT NULL DEFAULT 2.5,
    interval_days     INT NOT NULL DEFAULT 0,
    repetitions       INT NOT NULL DEFAULT 0,
    next_review_date  DATE NOT NULL,
    last_reviewed_at  TIMESTAMP,
    UNIQUE (user_id, word_id)
);
