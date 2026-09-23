CREATE TABLE reading_passages (
    id          BIGSERIAL PRIMARY KEY,
    topic_id    BIGINT REFERENCES topics(id),
    level       VARCHAR(20) NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     TEXT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reading_questions (
    id              BIGSERIAL PRIMARY KEY,
    passage_id      BIGINT NOT NULL REFERENCES reading_passages(id),
    question_text   TEXT NOT NULL,
    option_a        VARCHAR(300) NOT NULL,
    option_b        VARCHAR(300) NOT NULL,
    option_c        VARCHAR(300) NOT NULL,
    option_d        VARCHAR(300) NOT NULL,
    correct_option  VARCHAR(1) NOT NULL,
    explanation     VARCHAR(500)
);

CREATE TABLE reading_attempts (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id),
    passage_id       BIGINT NOT NULL REFERENCES reading_passages(id),
    score            INT NOT NULL,
    total_questions  INT NOT NULL,
    completed_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reading_attempt_answers (
    id               BIGSERIAL PRIMARY KEY,
    attempt_id       BIGINT NOT NULL REFERENCES reading_attempts(id) ON DELETE CASCADE,
    question_id      BIGINT NOT NULL REFERENCES reading_questions(id),
    selected_option  VARCHAR(1) NOT NULL,
    correct          BOOLEAN NOT NULL
);
