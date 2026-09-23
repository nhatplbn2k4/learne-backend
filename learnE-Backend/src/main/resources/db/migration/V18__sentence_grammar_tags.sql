-- Which grammar points a practice sentence leans on, so the learner can be warned before they try
-- to translate something built on a pattern they have not studied.
--
-- A JSON array of grammar_lessons.code rather than a join table: the tags are written and read as
-- one blob per sentence and never queried across, exactly like new_words_json beside it. Codes are
-- resolved against the lessons at read time, so a tag naming a lesson that was since renamed or
-- deleted simply stops matching instead of breaking the row.
ALTER TABLE sentence_exercises ADD COLUMN grammar_codes TEXT;
