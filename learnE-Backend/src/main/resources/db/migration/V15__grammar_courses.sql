-- Courses come in two kinds now: vocabulary (days of words) and grammar (lessons of patterns).
-- Everything that exists today is vocabulary, and the default keeps it that way.

ALTER TABLE courses ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'VOCABULARY';
