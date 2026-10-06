-- Tach "chuoi hien tai" khoi "chuoi dai nhat".
--
-- Truoc day chi co streak_count, va no la chuoi HIEN TAI: hoc xong mot ngay thi tang, dut quang
-- thi dat lai ve 1. Dashboard lai dan nhan no la "Streak dai nhat" va lay max giua cac khoa -
-- nen con so do tut xuong moi lan nguoi hoc dut chuoi, dung thu ma "dai nhat" khong duoc lam.
--
-- Cot moi giu ky luc that su, chi tang chu khong bao gio giam. Backfill bang streak_count vi do
-- la can duoi dung nhat ta biet: chuoi dai nhat it ra phai bang chuoi dang co.
--
-- UPDATE o day chi cham cot vua them, khong dong vao du lieu cu nao.

ALTER TABLE user_course_enrollments ADD COLUMN longest_streak INTEGER NOT NULL DEFAULT 0;
UPDATE user_course_enrollments SET longest_streak = streak_count;
