INSERT INTO writing_prompts (topic_id, level, title, instructions, min_words)
SELECT t.id, 'BEGINNER', 'Viết về gia đình bạn',
'Write a short paragraph about your family. Mention how many people are in your family, who they are, and what they do. Write at least 50 words.',
50
FROM topics t WHERE t.slug = 'daily-life';

INSERT INTO writing_prompts (topic_id, level, title, instructions, min_words)
SELECT t.id, 'BEGINNER', 'Một ngày của bạn',
'Describe your daily routine in English, from the morning until you go to bed. Write at least 50 words.',
50
FROM topics t WHERE t.slug = 'daily-life';
