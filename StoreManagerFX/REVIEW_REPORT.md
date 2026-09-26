# Rà soát StoreManagerFX — 26/09/2026

## Phạm vi và giới hạn

Rà soát mã nguồn Java, quyền truy cập, luồng nhập/bán hàng và tồn kho, audit, công cụ backup, tham chiếu FXML, cấu hình Maven và bản HTML preview. Bản nguồn trước đợt rà soát được lưu tại `../../.local/review-baseline-20260926` để phân biệt với các thay đổi đã có từ trước.

Không chạy test tự động theo yêu cầu của người dùng. Kết quả biên dịch và kiểm tra tĩnh không chứng minh mọi tình huống nghiệp vụ đều không còn lỗi. Chưa thực hiện chu trình backup/restore dữ liệu hoặc kiểm tra giao diện theo từng vai trò trong đợt này.

## Các vấn đề đã sửa

| Vấn đề | Thay đổi |
| --- | --- |
| Dashboard của VIEWER rơi vào luồng OWNER và gọi dữ liệu ngoài quyền của vai trò | `DashboardAnalyticsService` kiểm tra quyền và tạo thống kê sản phẩm/danh mục riêng; `DashboardHomeController` hiển thị các thống kê này. Chỉ khởi tạo phần chấm công nhanh cho EMPLOYEE. |
| EmployeeService chưa kiểm tra quyền khi thêm, sửa, xóa nhân viên | Yêu cầu quyền OWNER/MANAGER tại service; từ chối và ghi audit khi không có quyền. Kiểm tra đối tượng null trước khi cập nhật Employee và Category. |
| Cộng số lượng lớn có thể tràn kiểu int | Dùng `Math.addExact` khi cộng dòng Import, Order và cập nhật tồn kho. Lỗi được đưa về luồng xử lý lỗi hiện có, không lưu số lượng đã tràn. |
| Giá có hơn hai chữ số thập phân có thể bị database làm tròn khác với tổng tính trên ứng dụng | Kiểm tra độ chính xác giá nhập ở presenter/service và giá sản phẩm ở service, phù hợp cột DECIMAL có hai chữ số thập phân. Không tự đổi giá người dùng nhập. |
| Backup ghi trực tiếp vào file đích, có thể làm hỏng bản cũ khi mysqldump thất bại | Xuất vào file tạm cùng thư mục; kiểm tra mã thoát và file không rỗng trước khi chuyển sang đích. Đọc thông báo lỗi có giới hạn, ghi audit thất bại và dọn file tạm. Lỗi ghi lịch sử sau khi file SQL đã lưu được báo vào console. |
| Bản HTML còn Payroll và nút điều chỉnh kho đã gỡ khỏi ứng dụng | Xóa menu/dữ liệu Payroll, hành động điều chỉnh kho và cập nhật tài liệu preview. |

Lỗi Import được hỏi lại đã được sửa từ trước đợt rà soát: `ImportPresenter.findImportItem` chỉ gộp khi cùng sản phẩm **và cùng giá nhập**. Khác giá giữ thành hai dòng. Ví dụ `10 × 5.000 + 10 × 6.000 = 110.000đ`; không ghi đè giá toàn bộ số lượng bằng giá mới nhất.

## Mã thừa đã xóa

- 10 file Java không còn sử dụng: `AppConfig`, `BasePresenter`, `BaseRepository`, `BaseService`, `DatabaseConfig`, `WindowManager`, `SessionUser`, `LoginRequest`, `auth/repositoty/AuthRepository`, `DashboardController` cũ.
- 5 FXML không có luồng truy cập: `dashboard/dashboard.fxml`, bốn màn hình `employee-placeholder`, `inventory-placeholder`, `order-placeholder`, `product-placeholder`.
- Các hàm private không còn được gọi trong Dashboard, Attendance Anomaly; các overload xử lý tồn kho cũ của Import/Order; các handler điều hướng cũ không còn gắn trong FXML.
- 27 import Java không dùng; dependency Lombok không có mã sử dụng. Maven dùng `release=21` để giới hạn API theo Java 21.

Giữ các handler ảnh được FXML gọi, các service/repository chi nhánh còn phục vụ chấm công và dữ liệu hiện hữu, các enum tương thích lịch sử, cùng các lớp mô hình dùng qua binding/serialization. Không coi việc thiếu lời gọi Java trực tiếp là đủ để xóa chúng.

## Kiểm tra đã thực hiện

- Maven `clean compile javafx:run` với `-Dmaven.test.skip=true`: biên dịch thành công 181 file Java theo Java 21.
- Kiểm tra tĩnh 23 FXML: XML hợp lệ, controller, field được inject, handler và đường dẫn tài nguyên đều khớp.
- `node --check design-preview/app.js`: đạt.
- `git diff --check`: đạt; Git có thông báo chuyển đổi LF/CRLF theo cấu hình Windows.
- Lần mở đầu gặp kết nối database bị từ chối vì MySQL chưa chạy. Sau khi bật lại MySQL của XAMPP, truy vấn đọc xác nhận MariaDB 10.4.32 và database `family_business_manager_db`; mở lại JavaFX nhận `Database initialized successfully`.
- Maven còn cảnh báo effective model của dependency JavaFX 21. Cảnh báo này không chặn biên dịch hoặc khởi động; chưa thay phiên bản thư viện trong đợt dọn mã này.
