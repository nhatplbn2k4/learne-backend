INSERT INTO topics (slug, name, description)
VALUES ('daily-life', 'Đời sống hằng ngày', 'Từ vựng cơ bản dùng trong sinh hoạt hằng ngày');

INSERT INTO courses (topic_id, level, title, description)
SELECT id, 'BEGINNER', 'Từ vựng đời sống hằng ngày - Cơ bản', '3 ngày học, mỗi ngày khoảng 30 phút'
FROM topics WHERE slug = 'daily-life';

INSERT INTO lesson_days (course_id, day_number, title)
SELECT c.id, d.day_number, d.title
FROM courses c
JOIN (VALUES
    (1, 'Thành viên gia đình'),
    (2, 'Đồ ăn hằng ngày'),
    (3, 'Sinh hoạt hằng ngày')
) AS d(day_number, title) ON true
WHERE c.title = 'Từ vựng đời sống hằng ngày - Cơ bản';

-- Day 1: Thành viên gia đình
INSERT INTO words (lesson_day_id, english_text, ipa, part_of_speech, vietnamese_meaning, example_sentence_en, example_sentence_vi)
SELECT ld.id, w.english_text, w.ipa, w.part_of_speech, w.vietnamese_meaning, w.example_sentence_en, w.example_sentence_vi
FROM lesson_days ld
JOIN courses c ON c.id = ld.course_id AND ld.day_number = 1
JOIN (VALUES
    ('family', '/ˈfæməli/', 'noun', 'gia đình', 'My family has four people.', 'Gia đình tôi có bốn người.'),
    ('mother', '/ˈmʌðər/', 'noun', 'mẹ', 'My mother is a teacher.', 'Mẹ tôi là giáo viên.'),
    ('father', '/ˈfɑːðər/', 'noun', 'bố', 'My father works in a bank.', 'Bố tôi làm việc ở ngân hàng.'),
    ('brother', '/ˈbrʌðər/', 'noun', 'anh/em trai', 'I have one older brother.', 'Tôi có một anh trai.'),
    ('sister', '/ˈsɪstər/', 'noun', 'chị/em gái', 'My sister lives in Hanoi.', 'Chị tôi sống ở Hà Nội.'),
    ('parents', '/ˈperənts/', 'noun', 'cha mẹ', 'My parents love me very much.', 'Cha mẹ tôi rất yêu thương tôi.')
) AS w(english_text, ipa, part_of_speech, vietnamese_meaning, example_sentence_en, example_sentence_vi) ON true
WHERE c.title = 'Từ vựng đời sống hằng ngày - Cơ bản';

-- Day 2: Đồ ăn hằng ngày
INSERT INTO words (lesson_day_id, english_text, ipa, part_of_speech, vietnamese_meaning, example_sentence_en, example_sentence_vi)
SELECT ld.id, w.english_text, w.ipa, w.part_of_speech, w.vietnamese_meaning, w.example_sentence_en, w.example_sentence_vi
FROM lesson_days ld
JOIN courses c ON c.id = ld.course_id AND ld.day_number = 2
JOIN (VALUES
    ('breakfast', '/ˈbrekfəst/', 'noun', 'bữa sáng', 'I eat breakfast at 7 AM.', 'Tôi ăn sáng lúc 7 giờ.'),
    ('lunch', '/lʌntʃ/', 'noun', 'bữa trưa', 'We have lunch together.', 'Chúng tôi ăn trưa cùng nhau.'),
    ('dinner', '/ˈdɪnər/', 'noun', 'bữa tối', 'Dinner is ready.', 'Bữa tối đã sẵn sàng.'),
    ('rice', '/raɪs/', 'noun', 'cơm/gạo', 'Rice is the main food in Vietnam.', 'Cơm là món ăn chính ở Việt Nam.'),
    ('vegetable', '/ˈvedʒtəbl/', 'noun', 'rau củ', 'Vegetables are good for health.', 'Rau củ tốt cho sức khoẻ.'),
    ('fruit', '/fruːt/', 'noun', 'trái cây', 'I eat fruit every day.', 'Tôi ăn trái cây mỗi ngày.')
) AS w(english_text, ipa, part_of_speech, vietnamese_meaning, example_sentence_en, example_sentence_vi) ON true
WHERE c.title = 'Từ vựng đời sống hằng ngày - Cơ bản';

-- Day 3: Sinh hoạt hằng ngày
INSERT INTO words (lesson_day_id, english_text, ipa, part_of_speech, vietnamese_meaning, example_sentence_en, example_sentence_vi)
SELECT ld.id, w.english_text, w.ipa, w.part_of_speech, w.vietnamese_meaning, w.example_sentence_en, w.example_sentence_vi
FROM lesson_days ld
JOIN courses c ON c.id = ld.course_id AND ld.day_number = 3
JOIN (VALUES
    ('wake up', '/weɪk ʌp/', 'verb', 'thức dậy', 'I wake up at 6 AM.', 'Tôi thức dậy lúc 6 giờ.'),
    ('go to work', '/ɡəʊ tə wɜːk/', 'verb phrase', 'đi làm', 'He goes to work by bus.', 'Anh ấy đi làm bằng xe buýt.'),
    ('take a shower', '/teɪk ə ˈʃaʊər/', 'verb phrase', 'tắm', 'I take a shower every morning.', 'Tôi tắm mỗi buổi sáng.'),
    ('have a rest', '/hæv ə rest/', 'verb phrase', 'nghỉ ngơi', 'Let''s have a rest for a while.', 'Hãy nghỉ ngơi một lát.'),
    ('go to bed', '/ɡəʊ tə bed/', 'verb phrase', 'đi ngủ', 'I go to bed at 10 PM.', 'Tôi đi ngủ lúc 10 giờ tối.'),
    ('get dressed', '/ɡet drest/', 'verb phrase', 'mặc quần áo', 'She gets dressed quickly.', 'Cô ấy mặc quần áo nhanh chóng.')
) AS w(english_text, ipa, part_of_speech, vietnamese_meaning, example_sentence_en, example_sentence_vi) ON true
WHERE c.title = 'Từ vựng đời sống hằng ngày - Cơ bản';
