# LearnEnglish — hướng dẫn cho Claude Code

Trao đổi bằng **tiếng Việt**. Comment trong mã và tên biến giữ **tiếng Anh** như phần còn lại của dự án.

## Dự án

Web học từ vựng và ngữ pháp cho người Việt, hai ngôn ngữ đích: tiếng Anh và tiếng Trung (giáo trình Boya 博雅汉语).

- `learnE-Backend/` — Spring Boot 4.1.1, Java 21, JPA + Flyway
- `learnE-Frontend/` — React 19 + TypeScript + Vite + Tailwind + TanStack Query
- PostgreSQL 16 trong container Docker `learne-postgres`
- Chạy thật tại `https://learne.nhatdev.fun` qua Cloudflare Tunnel tên `learne`

## Hai repo git riêng

| Thư mục | Repo (riêng tư) |
|---|---|
| gốc dự án — backend + `deploy/` + `docker-compose.yml` + `CLAUDE.md` + `.claude/` | `nhatplbn2k4/learne-backend` |
| `learnE-Frontend/` | `nhatplbn2k4/learne-frontend` |

Repo backend đặt gốc ở **thư mục dự án**, không ở `learnE-Backend/`, vì `deploy/build.ps1` với đến `../learnE-Frontend/dist`. Hai thư mục **phải nằm cạnh nhau** trên đĩa; clone riêng một repo là không build được. `learnE-Frontend/` bị `.gitignore` của repo backend loại ra.

**Commit không gắn dòng đồng tác giả hay attribution nào.**

Tài khoản GitHub bật email privacy nên đẩy bằng email thật sẽ bị từ chối. Cả hai repo đã đặt `user.email` cục bộ thành `149066175+nhatplbn2k4@users.noreply.github.com`; cấu hình git toàn cục giữ nguyên.

Ba thứ **cố ý không lên git**, đừng "sửa" lại: `src/main/resources/slide/` (105 MB PDF giáo trình có bản quyền, không mã nào đọc), `src/main/resources/static/` (sản phẩm build, `build.ps1` xoá rồi tạo lại), `backups/` (dump DB có email và hash mật khẩu thật).

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

## Năm cái bẫy đã trả giá

**1. `mvnw test` ghi thẳng vào DB production.** Không có `src/test/resources`, và `LearnEBackendApplicationTests` là `@SpringBootTest` không ghi đè profile — nên test dùng `application.yaml`, tức `localhost:5432/learne`, đúng DB thật trong container. **Chạy test là apply migration mới vào dữ liệu thật ngay lập tức.** Trước khi thêm migration: `docker exec learne-postgres pg_dump -U learne learne > backups/...sql`, và giữ migration thuần additive (`ADD COLUMN` có DEFAULT), không bao giờ `DROP`/`UPDATE`.

**2. `mvnw compile` không chứng minh được gì.** Biên dịch tăng dần báo thành công trên mã hỏng (`\p{IsHan}` thiếu một dấu `\` đã lọt qua). Và `./mvnw -q compile | head -10; echo $?` đọc mã thoát của `head`, không phải của Maven. Chỉ `./mvnw test` hoặc `deploy\build.ps1` (có `clean`) mới là bằng chứng.

**3. Heredoc trong Bash ăn mất dấu thoát.** `\n`, `\\p` trong heredoc bị biến dạng, sinh ra chuỗi Java hỏng — đã làm sập app. Nội dung có dấu thoát thì dùng công cụ Write/Edit, không dùng heredoc.

**4. `ddl-auto: validate`.** Entity và migration Flyway phải lên cùng lúc, lệch một cái là app không khởi động. **Không sửa** V1–V18 đã chạy (Flyway kiểm checksum); `target/classes/db/migration` có bản sao cũ — build lại chứ đừng sửa tay ở đó.

**5. Có ba hình dạng "correction" song song.** Sửa một chỗ là thiếu hai chỗ:
- `sentence/dto/SentenceFeedbackDto.SentenceCorrectionDto` — chấm từng câu
- `ai/dto/GradedSentencesDto.CorrectionDto` — chấm cả bài thi vượt
- `writing/dto/CorrectionDto` — phần luyện viết

Các DTO phản hồi đều có `@JsonIgnoreProperties(ignoreUnknown = true)` nên thêm trường mới không làm hỏng `feedback_json` đã lưu.

## Quy ước mã

Ngôn ngữ nằm trên `Topic`; `Course.getLanguage()` chỉ uỷ quyền xuống. Đừng thêm cột ngôn ngữ vào `Course`.

Danh sách kiểu luyện do **backend** quyết định và đi xuống frontend qua `DaySessionDto.practiceModes`. `LanguageModes.requiredFor(Course)` là **điểm vào duy nhất**: mặc định theo ngôn ngữ (tiếng Trung 7, tiếng Anh 5), ghi đè theo khoá bằng cột `courses.practice_modes` (**rỗng = dùng mặc định**, không phải "không kiểu nào"). Frontend **không còn bản sao nào** của danh sách này — đừng tạo lại.

Lý do phải một nguồn: một từ chỉ thuộc lòng khi đúng ở **mọi kiểu bắt buộc trong cùng một buổi** (`session_correct_modes`). Hỏi ít kiểu hơn số kiểu đem đi chấm thì từ **không bao giờ** thuộc được → không bao giờ mở luyện dịch, thẻ ngày vĩnh viễn ⚠️.

Khoá học có ba nút điều chỉnh hành vi, mặc định tái lập đúng hành vi cũ: `practice_modes` (rỗng), `sentence_translation_enabled` (bật, vẫn AND với cờ ngôn ngữ), `days_always_unlocked` (tắt). Khoá "Bộ thủ Hán tự" dùng cả ba.

Khoá học có `kind`: `VOCABULARY` (chia theo ngày) hoặc `GRAMMAR` (chia theo bài). `GET /api/courses` mặc định `kind=VOCABULARY` — bỏ mặc định là khoá ngữ pháp lọt vào danh sách từ vựng.

## Frontend

`react-router-dom` chỉ dùng ở tầng ngoài (đăng nhập, `ProtectedRoute`). Toàn bộ bên trong dashboard là một union `View` trong `DashboardPage.tsx` — **URL không đổi khi chuyển màn**. Hệ quả: không có history, không deep-link được, và quay lại thì mất vị trí cuộn trừ khi tự lưu.

`<main>` trong `AppShell.tsx` dùng `lg:pl-60`, không phải container cuộn riêng — nên **cửa sổ** cuộn, dùng `window.scrollY`/`window.scrollTo`.

Khi khôi phục vị trí cuộn: cleanup của `useEffect` chạy **sau** khi React thay DOM, lúc đó trình duyệt đã kẹp `scrollY` về 0. Phải dùng `useLayoutEffect` kèm listener `scroll`. Xem `CourseDaysPage.tsx`.

## Bí mật

`GEMINI_API_KEY` và `JWT_SECRET` chỉ đọc từ biến môi trường người dùng Windows, **không bao giờ ghi vào mã**. `application-prod.yaml` cố ý **không đặt mặc định** cho `JWT_SECRET` để app từ chối khởi động thay vì chạy bằng secret dev. Đăng ký mở nhưng tài khoản mới ở trạng thái `PENDING` cho tới khi admin duyệt — trừ tài khoản **đầu tiên** của hệ thống, được `ACTIVE` ngay để có admin khởi đầu (`AuthService`).

## Gemini

Quota free tier tính **theo project, không theo người dùng** — hai người dùng cùng lúc chạy song song bình thường, thứ khan hiếm là quota chung. Quota ngày reset lúc 14:00 giờ Việt Nam.

Có **ba** thuộc tính model, đừng gộp:

| Thuộc tính | Mặc định | Dùng ở đâu |
|---|---|---|
| `app.ai.gemini.grading-model` | `gemini-3.5-flash-lite` | Chấm câu dịch, chấm bài thi vượt, chấm bài viết |
| `app.ai.gemini.bulk-model` | `gemini-3.5-flash-lite` | Sinh từ vựng, sinh câu, gắn thẻ ngữ pháp, sinh bài ngữ pháp |
| `app.ai.gemini.model` | `gemini-3.5-flash` | Sinh bài đọc, sinh đề luyện viết — và là nấc dự phòng cho hai cái trên |

Chấm bài tách riêng vì đó là lần gọi duy nhất người học phải ngồi chờ (~1,6s so với ~8s). Hai cái đầu **trùng giá trị mặc định nhưng không được gộp**: đổi cách sinh từ vựng hàng loạt không được vô tình đổi cách chấm bài.

`GeminiClient` nhớ model nào đang hỏng và tự chuyển — đừng thêm vòng thử lại bên ngoài nó.

## Skill có sẵn

`.claude/skills/` có 4 skill lấy từ claude-starter-kit v2.8.0: `security-audit`, `performance-audit`, `ops-readiness-audit`, `design-pattern-audit`. Chúng ghi báo cáo vào `documents/audits/`. Phần còn lại của bộ kit **cố ý không cài** (xoay quanh PR/CI/worktree, không hợp dự án này).
