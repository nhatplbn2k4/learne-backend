-- Ba hanh vi truoc day suy ra tu ngon ngu, nay ghi de duoc theo tung khoa hoc.
--
-- Khoa "Bo thu Han tu" la ly do: bo thu khong go duoc bang IME pinyin va khong doc thanh tieng
-- duoc, nen no chi dung 4 trong 7 kieu luyen cua tieng Trung; no cung khong co bai luyen dich
-- va moi ngay deu mo san. Ba dieu do truoc day khong the dien dat duoc.
--
-- Mac dinh cua ca ba cot tai lap dung hanh vi cu, nen moi khoa dang chay khong doi gi:
--   practice_modes NULL/rong     -> danh sach kieu luyen mac dinh cua ngon ngu (LanguageModes)
--   sentence_translation_enabled -> bat, va van phai AND voi co cua ngon ngu o frontend
--   days_always_unlocked         -> tat, ngay van mo tuan tu nhu cu
--
-- Thuan additive: chi ADD COLUMN co DEFAULT, khong rewrite bang tren PG 16.

ALTER TABLE courses ADD COLUMN practice_modes VARCHAR(255);
ALTER TABLE courses ADD COLUMN sentence_translation_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE courses ADD COLUMN days_always_unlocked BOOLEAN NOT NULL DEFAULT FALSE;
