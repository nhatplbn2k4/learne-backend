CREATE TABLE writing_prompts (
    id            BIGSERIAL PRIMARY KEY,
    topic_id      BIGINT REFERENCES topics(id),
    level         VARCHAR(20) NOT NULL,
    title         VARCHAR(200) NOT NULL,
    instructions  TEXT NOT NULL,
    min_words     INT NOT NULL DEFAULT 50,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE writing_submissions (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users(id),
    prompt_id      BIGINT NOT NULL REFERENCES writing_prompts(id),
    content        TEXT NOT NULL,
    word_count     INT NOT NULL,
    score          INT,
    feedback_json  TEXT,
    submitted_at   TIMESTAMP NOT NULL DEFAULT now()
);
