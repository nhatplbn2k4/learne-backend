-- When the AI spots a grammar mistake that no lesson covers, the gap is recorded for the admin
-- instead of being lost in one learner's feedback.
--
-- One row per grammar point rather than per occurrence: the same gap hit twenty times is one thing
-- to write, not twenty notifications. normalized_name is what makes that dedup work, since the AI
-- names the same point slightly differently each time.

CREATE TABLE grammar_gap_requests (
    id BIGSERIAL PRIMARY KEY,
    language VARCHAR(20) NOT NULL,
    grammar_name VARCHAR(200) NOT NULL,
    -- grammar_name lower-cased, stripped of Vietnamese diacritics and punctuation.
    normalized_name VARCHAR(200) NOT NULL,
    -- One sample occurrence, so the admin can see what the learner was actually trying to say.
    example_prompt TEXT,
    example_answer TEXT,
    explanation TEXT,
    first_reported_at TIMESTAMP NOT NULL DEFAULT now(),
    last_reported_at TIMESTAMP NOT NULL DEFAULT now(),
    report_count INT NOT NULL DEFAULT 1,
    -- OPEN | RESOLVED | IGNORED
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolved_lesson_id BIGINT REFERENCES grammar_lessons(id),
    CONSTRAINT grammar_gap_requests_name_key UNIQUE (language, normalized_name)
);

CREATE INDEX idx_grammar_gaps_status ON grammar_gap_requests (status, last_reported_at DESC);
