# StoreManagerFX

Ứng dụng JavaFX quản lý cửa hàng và bán hàng tại quầy, dùng Java 21 và MySQL JDBC.

## Chức năng

- Bán hàng tại quầy (Order), kiểm tra và trừ tồn kho khi chốt đơn.
- Nhập hàng (Import), lưu phiếu nhập và cộng tồn kho.
- Quản lý sản phẩm, danh mục, nhân viên và tài khoản nội bộ.
- Xem tồn kho, chấm công và xử lý bất thường chấm công.
- Dashboard, nhật ký hoạt động, thông báo, tin nhắn và công cụ sao lưu dữ liệu.

Chỉ OWNER được tạo tài khoản nội bộ trong User Management.

## Chạy ứng dụng

1. Cài JDK 21 trở lên và Maven.
2. Bật MySQL/MariaDB. Trên máy dùng XAMPP, bật mục MySQL.
3. Sao chép `app-config.example.json` thành `app-config.json`, rồi điền host, port, databaseName, username và password cho máy của bạn. File cấu hình thật không được đưa lên Git.
4. Mở terminal tại thư mục chứa `pom.xml` này và chạy:

```powershell
mvn "-Dmaven.test.skip=true" clean compile javafx:run
```

Ứng dụng khởi tạo các bảng cần thiết khi kết nối database. Tài khoản mặc định được tạo nếu chưa có: `admin` / `123456` (OWNER). Mật khẩu của tài khoản đã tồn tại được giữ nguyên.

## Nhật ký hoạt động ra file text

Các sự kiện đi qua `AuditService` được tự động ghi thêm vào file UTF-8 `logs/audit-YYYY-MM-DD.txt`, đồng thời vẫn lưu vào bảng `audit_logs`. Thư mục `logs` tính từ thư mục chạy ứng dụng (thư mục chứa `pom.xml` khi chạy theo hướng dẫn trên).

- File được tạo khi phát sinh sự kiện đầu tiên trong ngày; khởi động lại ứng dụng vẫn ghi tiếp file đó. Sang ngày mới tạo file mới theo ngày giờ của máy.
- Mỗi dòng gồm thời gian, tài khoản/ID người thực hiện, module, hành động, đối tượng/ID, kết quả, chi nhánh, lý do và chi tiết. Đăng nhập thất bại chưa có phiên người dùng sẽ mang actor `System`, còn tên đăng nhập đã thử nằm trong chi tiết.
- Không lấy mật khẩu hoặc thông tin kết nối database để ghi file. Nội dung ghi chú nhiều dòng được chuyển thành ký tự hiển thị `\n`/`\r` để mỗi sự kiện chỉ chiếm một dòng.
- File được ghi trước lần lưu database; lỗi lưu database không xóa dòng đã ghi. Nếu không ghi được file, chương trình báo lỗi ra console và vẫn thử lưu database. Kết quả `SUCCESS`/`FAILED` trên dòng log là kết quả thao tác nghiệp vụ, không phải trạng thái lưu database.
- Chỉ ghi các thao tác đã tích hợp audit, không tự ghi mọi lần bấm chuột và không xuất lại lịch sử cũ. File chưa tự dọn theo thời gian; có thể sao chép thư mục `logs` sang USB để lưu giữ. Log được loại khỏi Git.

Ví dụ định dạng (ID và thời gian minh họa):

```text
2026-09-26 10:00:00.000 | user=admin | user_id=1 | module=AUTHENTICATION | action=LOGIN_SUCCESS | target=USER | target_id=1 | result=SUCCESS | branch_id=- | reason=- | details={"username":"admin"}
```

## Sao lưu database

OWNER thực hiện sao lưu trong System Tools, chọn chương trình `mysqldump` (XAMPP thường ở `C:\xampp\mysql\bin\mysqldump.exe`) và nơi lưu file `.sql`, có thể chọn USB.

Ứng dụng xuất ra file tạm trong cùng thư mục đích. Chỉ khi `mysqldump` kết thúc thành công và file có dữ liệu, ứng dụng mới chuyển file tạm thành file đã chọn. Nếu quá trình xuất bị lỗi, bản sao lưu cũ tại đường dẫn đó vẫn được giữ và thông báo hiển thị nguyên nhân từ `mysqldump`. Đây là sao lưu do người dùng thực hiện; chưa có lịch sao lưu tự động.

## Thiết kế xem trước

Mở `design-preview/index.html` bằng trình duyệt. Đây là bản thiết kế với dữ liệu minh họa, độc lập với dữ liệu MySQL.

## Phạm vi hiện tại

Đã gỡ chức năng bán hàng online, ứng dụng mua hàng của khách và các backend/web liên quan. Ứng dụng không tạo hoặc sử dụng bảng đặt hàng online nữa. Các bảng và dữ liệu online đã tồn tại trong database được giữ nguyên để bảo toàn lịch sử.

Vai trò CUSTOMER/USER cũ vẫn được đọc tương thích; chúng không có quyền truy cập các màn hình quản lý và không còn được chọn để tạo tài khoản mới trên giao diện.

Kết quả rà soát và dọn mã ngày 26/09/2026: [REVIEW_REPORT.md](REVIEW_REPORT.md).
