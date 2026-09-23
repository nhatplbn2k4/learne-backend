INSERT INTO reading_passages (topic_id, level, title, content)
SELECT t.id, 'BEGINNER', 'A Day in My Life',
'My name is Lan. I live with my family in a small house in Hanoi. Every morning, I wake up at 6 AM and take a shower. Then I have breakfast with my parents before going to school.

My father works in a bank and my mother is a teacher. I have one brother and one sister. In the evening, we usually have dinner together at 7 PM. After dinner, I do my homework and then go to bed at 10 PM.

On weekends, my family likes to visit my grandparents. We eat lunch together and talk about our week. I love spending time with my family.'
FROM topics t WHERE t.slug = 'daily-life';

INSERT INTO reading_questions (passage_id, question_text, option_a, option_b, option_c, option_d, correct_option, explanation)
SELECT p.id, q.question_text, q.option_a, q.option_b, q.option_c, q.option_d, q.correct_option, q.explanation
FROM reading_passages p
JOIN (VALUES
    ('What time does Lan wake up?', '5 AM', '6 AM', '7 AM', '10 AM', 'B', 'Bài đọc ghi: "I wake up at 6 AM".'),
    ('Where does Lan live?', 'Ho Chi Minh City', 'Da Nang', 'Hanoi', 'Hue', 'C', 'Bài đọc ghi: "I live with my family in a small house in Hanoi."'),
    ('What is Lan''s father''s job?', 'Teacher', 'Doctor', 'Bank worker', 'Driver', 'C', 'Bài đọc ghi: "My father works in a bank".'),
    ('What time does the family have dinner?', '6 PM', '7 PM', '8 PM', '9 PM', 'B', 'Bài đọc ghi: "we usually have dinner together at 7 PM".'),
    ('What does the family do on weekends?', 'Go to school', 'Visit grandparents', 'Go to work', 'Stay home alone', 'B', 'Bài đọc ghi: "my family likes to visit my grandparents".')
) AS q(question_text, option_a, option_b, option_c, option_d, correct_option, explanation) ON true
WHERE p.title = 'A Day in My Life';
