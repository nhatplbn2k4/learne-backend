# LearnEnglish — hướng dẫn cho Claude Code

Trao đổi bằng **tiếng Việt**. Comment trong mã và tên biến giữ **tiếng Anh** như phần còn lại của dự án.

## Dự án

Web học từ vựng và ngữ pháp cho người Việt, hai ngôn ngữ đích: tiếng Anh và tiếng Trung (giáo trình Boya 博雅汉语).

- `learnE-Backend/` — Spring Boot 4.1.1, Java 21, JPA + Flyway
- `learnE-Frontend/` — React 19 + TypeScript + Vite + Tailwind + TanStack Query
- PostgreSQL 16 trong container Docker `learne-postgres`
- Chạy thật tại `https://learne.nhatdev.fun` qua Cloudflare Tunnel tên `learne`

Đây **không phải git repo**. Đừng đề xuất branch, commit, PR hay hook pre-commit.

## Xây dựng và chạy

| Lệnh | Việc |
|---|---|
| `deploy\build.ps1` | build frontend → nhúng vào jar → `mvnw clean package` |
| `deploy\app.ps1` | chỉ chạy app (cổng 8080), tunnel không bị ảnh hưởng |
| `deploy\tunnel.ps1` | chỉ chạy tunnel |
| `deploy\start.ps1` | chạy cả hai trong một cửa sổ |

**`build.ps1` dừng app đang chạy** để gỡ khoá file jar (Windows khoá jar khi java đang mở nó). Build xong **phải chạy lại `app.ps1`** — quên bước này là site trả 530 trong khi log tunnel vẫn báo khoẻ mạnh. Đã xảy ra.

`JAVA_HOME` trên máy này trỏ vào một JDK không tồn tại. `_common.ps1` tự đổi sang `C:\Program Files\Java\jdk-21`; khi gọi Maven thủ công thì phải tự đặt lại.

Mọi file `.ps1` phải lưu **UTF-8 có BOM**, nếu không tiếng Việt trong script hỏng. Kiểm tra: `head -c 3 file.ps1 | xxd -p` phải ra `efbbbf`.

## Bốn cái bẫy đã trả giá

**1. `mvnw compile` không chứng minh được gì.** Biên dịch tăng dần báo thành công trên mã hỏng (`\p{IsHan}` thiếu một dấu `\` đã lọt qua). Và `./mvnw -q compile | head -10; echo $?` đọc mã thoát của `head`, không phải của Maven. Chỉ `./mvnw test` hoặc `deploy\build.ps1` (có `clean`) mới là bằng chứng.

**2. Heredoc trong Bash ăn mất dấu thoát.** `\n`, `\\p` trong heredoc bị biến dạng, sinh ra chuỗi Java hỏng — đã làm sập app. Nội dung có dấu thoát thì dùng công cụ Write/Edit, không dùng heredoc.

**3. `ddl-auto: validate`.** Entity và migration Flyway phải lên cùng lúc, lệch một cái là app không khởi động. **Không sửa** V1–V18 đã chạy (Flyway kiểm checksum); `target/classes/db/migration` có bản sao cũ — build lại chứ đừng sửa tay ở đó.

**4. Có ba hình dạng "correction" song song.** Sửa một chỗ là thiếu hai chỗ:
- `sentence/dto/SentenceFeedbackDto.SentenceCorrectionDto` — chấm từng câu
- `ai/dto/GradedSentencesDto.CorrectionDto` — chấm cả bài thi vượt
- `writing/dto/CorrectionDto` — phần luyện viết

Các DTO phản hồi đều có `@JsonIgnoreProperties(ignoreUnknown = true)` nên thêm trường mới không làm hỏng `feedback_json` đã lưu.

## Quy ước mã

Ngôn ngữ nằm trên `Topic`; `Course.getLanguage()` chỉ uỷ quyền xuống. Đừng thêm cột ngôn ngữ vào `Course`.

Tiếng Trung có 7 kiểu luyện, tiếng Anh 5 — xem `LanguageModes.java`, và `MODES_BY_LANGUAGE` ở frontend **phải khớp**. Một từ được tính thuộc lòng khi trả lời đúng ở mọi kiểu trong **cùng một buổi** (`session_correct_modes`).

Khoá học có `kind`: `VOCABULARY` (chia theo ngày) hoặc `GRAMMAR` (chia theo bài). `GET /api/courses` mặc định `kind=VOCABULARY` — bỏ mặc định là khoá ngữ pháp lọt vào danh sách từ vựng.

## Frontend

`react-router-dom` chỉ dùng ở tầng ngoài (đăng nhập, `ProtectedRoute`). Toàn bộ bên trong dashboard là một union `View` trong `DashboardPage.tsx` — **URL không đổi khi chuyển màn**. Hệ quả: không có history, không deep-link được, và quay lại thì mất vị trí cuộn trừ khi tự lưu.

`<main>` trong `AppShell.tsx` dùng `lg:pl-60`, không phải container cuộn riêng — nên **cửa sổ** cuộn, dùng `window.scrollY`/`window.scrollTo`.

Khi khôi phục vị trí cuộn: cleanup của `useEffect` chạy **sau** khi React thay DOM, lúc đó trình duyệt đã kẹp `scrollY` về 0. Phải dùng `useLayoutEffect` kèm listener `scroll`. Xem `CourseDaysPage.tsx`.

## Bí mật

`GEMINI_API_KEY` và `JWT_SECRET` chỉ đọc từ biến môi trường người dùng Windows, **không bao giờ ghi vào mã**. `application-prod.yaml` cố ý **không đặt mặc định** cho `JWT_SECRET` để app từ chối khởi động thay vì chạy bằng secret dev. Đăng ký mở nhưng tài khoản mới ở trạng thái `PENDING` cho tới khi admin duyệt — trừ tài khoản **đầu tiên** của hệ thống, được `ACTIVE` ngay để có admin khởi đầu (`AuthService`).

## Gemini

Quota free tier tính **theo project, không theo người dùng** — hai người dùng cùng lúc chạy song song bình thường, thứ khan hiếm là quota chung. Quota ngày reset lúc 14:00 giờ Việt Nam.

`gemini-3.5-flash` là model chấm bài (chậm hơn, phản hồi sâu hơn), `gemini-3.5-flash-lite` cho việc hàng loạt. `GeminiClient` nhớ model nào đang hỏng và tự chuyển — đừng thêm vòng thử lại bên ngoài nó.

## Skill có sẵn

`.claude/skills/` có 4 skill lấy từ claude-starter-kit v2.8.0: `security-audit`, `performance-audit`, `ops-readiness-audit`, `design-pattern-audit`. Chúng ghi báo cáo vào `documents/audits/`. Phần còn lại của bộ kit **cố ý không cài** (xoay quanh PR/CI/worktree, không hợp dự án này).
