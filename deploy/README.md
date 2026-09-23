# Chạy app qua Cloudflare Tunnel

Mọi thứ vẫn chạy trên máy bạn (kể cả Postgres trong Docker). `cloudflared` chủ động kết nối ra
Cloudflare, nên **không cần mở port, không cần IP tĩnh, không cần thẻ tín dụng**. Backend phục vụ
luôn frontend đã build nên chỉ có **một URL duy nhất** và không dính CORS.

## Chuẩn bị một lần

Tạo `JWT_SECRET` và lưu vĩnh viễn (profile `prod` **từ chối khởi động** nếu thiếu — cố ý, để app
mở ra internet không bao giờ chạy bằng secret mặc định trong repo):

```powershell
[Environment]::SetEnvironmentVariable(
  "JWT_SECRET",
  [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Max 256 })),
  "User")
```

Mở PowerShell mới cho biến môi trường có hiệu lực.

## Chỉ dùng, không sửa code

Một cửa sổ là đủ:

```powershell
cd C:\Users\nhat6\Documents\LearnEnglish
.\deploy\build.ps1     # chỉ khi có sửa code
.\deploy\start.ps1
```

`start.ps1` bật Docker Desktop nếu cần → khởi động Postgres → chạy app với profile `prod` → bật
tunnel và in URL `https://<ngẫu-nhiên>.trycloudflare.com`. `Ctrl+C` dừng cả hai.

## Vừa dùng vừa sửa code — hai cửa sổ

Tunnel chỉ chuyển tiếp tới cổng 8080, **không gắn với tiến trình nào**. Nên app phía sau có thể
build lại và khởi động lại thoải mái mà **URL không đổi** — miễn là đừng tắt cửa sổ tunnel.

| Cửa sổ 1 (mở một lần, để yên) | Cửa sổ 2 (chạy lại tuỳ ý) |
|---|---|
| `.\deploy\tunnel.ps1` | `.\deploy\build.ps1` rồi `.\deploy\app.ps1` |

`Ctrl+C` ở cửa sổ 2 chỉ dừng app, tunnel vẫn sống. Trong lúc app tắt, URL trả 502 rồi tự hoạt động
lại khi app lên.

`build.ps1` tự dừng app đang chạy trước khi đóng gói — bắt buộc, vì Windows khoá file jar khi app
đang mở, khiến Maven bỏ qua bước đóng gói và để lại jar báo *"no main manifest attribute"*.

### Sửa mỗi frontend: không cần build lại jar

Mặc định frontend nằm bên trong jar, nên sửa nó phải đóng gói lại (~23s) và khởi động lại backend
(~10s). Thêm `-LiveFrontend` để backend đọc thẳng `learnE-Frontend/dist`:

```powershell
.\deploy\app.ps1 -LiveFrontend
```

Từ đó, sửa giao diện chỉ cần build lại frontend rồi **F5 trình duyệt** — backend không khởi động
lại, tunnel không đổi URL. Chỉ khi sửa **backend** mới cần `build.ps1` + chạy lại `app.ps1`.

Mở **cửa sổ thứ ba** và chạy chế độ watch, khỏi phải gõ lại sau mỗi lần sửa:

```powershell
cd learnE-Frontend
npm run build -- --watch
```

Lưu file là nó tự build (~0.3 giây), chỉ việc F5. Lưu ý: `tsc` chỉ chạy một lần lúc khởi động, nên
lỗi TypeScript phát sinh sau đó watcher **không** báo — thỉnh thoảng chạy `npx tsc -b` để kiểm tra.

| | Mặc định | `-LiveFrontend` |
|---|---|---|
| Frontend lấy từ | trong jar | `learnE-Frontend/dist` |
| Sửa FE mất | build jar + restart BE (~33s) | `npm run build` (~0.3s) + F5 |
| Jar tự chứa đủ | có | không (cần thư mục dist) |

## Đang phát triển thì đừng dùng tunnel

Sửa giao diện liên tục thì chạy chế độ dev, có hot reload, không phải build lại gì:

```powershell
# cửa sổ 1
cd learnE-Backend; .\mvnw.cmd spring-boot:run
# cửa sổ 2
cd learnE-Frontend; npm run dev     # http://localhost:5173
```

Dev và prod đều dùng cổng 8080 nên **không chạy song song được**.

## Tên miền riêng — URL cố định

Quick tunnel đổi URL mỗi lần chạy. Có tên miền riêng trên Cloudflare thì dùng *named tunnel*, địa
chỉ cố định vĩnh viễn và miễn phí.

Sau khi tên miền đã nằm trong tài khoản Cloudflare, chạy **một lần**:

```powershell
.\deploy\setup-tunnel.ps1 -Domain vidu.com
```

Script sẽ: mở trình duyệt cho bạn chọn zone → tạo tunnel tên `learne` → ghi
`%USERPROFILE%\.cloudflared\config.yml` trỏ `learne.vidu.com` về `localhost:8080` → tạo bản ghi DNS.

Từ đó chạy hằng ngày:

```powershell
.\deploy\start.ps1 -TunnelName learne
# hoac 2 cua so:
.\deploy\tunnel.ps1 -TunnelName learne
.\deploy\app.ps1 -LiveFrontend
```

Đổi `-Subdomain` nếu muốn tên khác (mặc định `learne`), `-TunnelName` nếu muốn đặt tên tunnel khác.

## Những điều cần biết

- **URL đổi mỗi lần chạy.** Quick tunnel không cần tài khoản Cloudflare nên đánh đổi bằng URL ngẫu
  nhiên. Muốn URL cố định thì phải có domain trỏ về Cloudflare, tạo *named tunnel*, rồi chạy
  `.\deploy\start.ps1 -TunnelName ten-tunnel`.
- **PC phải bật** thì URL mới sống. Tắt máy là app không truy cập được.
- **Đăng ký đã bị khoá** trong profile `prod` (`app.auth.allow-registration: false`), vì ai có URL
  cũng gọi được API. Cần thêm tài khoản thì tạo lúc chạy dev (`mvnw spring-boot:run`, không có
  profile `prod`) rồi mới bật tunnel.
- **Thư mục `learnE-Backend/src/main/resources/static` là sản phẩm build**, do `build.ps1` sinh ra từ
  `learnE-Frontend/dist`. Đừng sửa tay trong đó.

## Gặp lỗi?

**`Port 8080 was already in use`** — còn một backend dev (`mvnw spring-boot:run`) đang chạy. Script
giờ tự nhận ra và dừng nó, nhưng nếu cổng bị một chương trình khác chiếm thì script sẽ báo rõ PID để
bạn tự xử lý. Dev và prod không chạy song song được vì cùng dùng cổng 8080.

**Script không chạy / báo lỗi cú pháp lạ** — file `.ps1` phải là UTF-8 **có BOM**. Nếu bạn sửa bằng
editor lưu thành UTF-8 không BOM thì Windows PowerShell 5.1 sẽ đọc sai ký tự tiếng Việt và vỡ cú pháp.

## Khác biệt giữa dev và prod

| | Dev (`mvnw spring-boot:run`) | Prod (`start.ps1`) |
|---|---|---|
| Frontend | Vite ở cổng 5173, proxy `/api` sang 8080 | Backend phục vụ tại cổng 8080 |
| CORS | Cho phép `http://localhost:5173` | Không cần, cùng origin |
| Đăng ký | Mở | Khoá |
| `JWT_SECRET` | Có mặc định để tiện chạy máy | Bắt buộc, không có mặc định |
