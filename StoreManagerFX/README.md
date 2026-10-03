# StoreManagerFX

Ứng dụng JavaFX quản lý một cửa hàng duy nhất và bán hàng tại quầy, dùng Java 21 và MySQL JDBC.

Tài liệu chi tiết về đồ án, hướng dẫn sử dụng và các nghiệp vụ minh họa: [MO_TA_DO_AN.md](MO_TA_DO_AN.md).

## Chức năng

- Bán hàng tại quầy (Order), kiểm tra và trừ tồn kho khi chốt đơn.
- Nhập hàng (Import), lưu phiếu nhập và cộng tồn kho.
- Quản lý sản phẩm, danh mục, nhân viên và tài khoản nội bộ.
- Xem tồn kho, chấm công và xử lý bất thường chấm công.
- Dashboard, nhật ký hoạt động, thông báo, tin nhắn và công cụ sao lưu dữ liệu.

Chỉ OWNER được tạo tài khoản nội bộ trong User Management.

### Thanh toán và hạn sử dụng

- OWNER/MANAGER/STAFF dùng Order: chọn sản phẩm, Add item; chọn dòng, sửa Qty và Update quantity. Nhập Amount received rồi Complete order. Change tự tính bằng tiền nhận trừ tổng tiền; thiếu tiền hoặc nhập sai sẽ không chốt đơn. Sau khi lưu, thông báo hiển thị mã đơn, tổng tiền, tiền nhận và tiền thừa; giỏ và ô tiền nhận được xóa.
- Import có Expiry date tùy chọn cho từng dòng (`yyyy-MM-dd`), không nhận ngày trước hôm nay. Cùng sản phẩm nhưng khác giá nhập hoặc hạn sử dụng sẽ giữ dòng riêng.
- View Inventory hiển thị ngày hết hạn gần nhất và trạng thái EXPIRED / EXPIRING SOON (trong 7 ngày kể cả hôm nay) / NORMAL / N/A. Nhấn Refresh để cập nhật. Ngày lấy từ toàn bộ lịch sử nhập, chưa theo dõi tồn theo lô hoặc tự động xuất theo hạn dùng.
- Khi khởi động, database tự thêm cột còn thiếu: `sale_orders.amount_received`, `sale_orders.change_amount` (`DECIMAL(18,2) NULL`) và `import_items.expiry_date` (`DATE NULL`). Bản ghi cũ giữ nguyên; không suy đoán tiền khách đã đưa cho đơn cũ.

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
- Mỗi dòng gồm thời gian, tài khoản/ID người thực hiện, module, hành động, đối tượng/ID, kết quả, lý do và chi tiết. Đăng nhập thất bại chưa có phiên người dùng sẽ mang actor `System`, còn tên đăng nhập đã thử nằm trong chi tiết.
- Không lấy mật khẩu hoặc thông tin kết nối database để ghi file. Nội dung ghi chú nhiều dòng được chuyển thành ký tự hiển thị `\n`/`\r` để mỗi sự kiện chỉ chiếm một dòng.
- File được ghi trước lần lưu database; lỗi lưu database không xóa dòng đã ghi. Nếu không ghi được file, chương trình báo lỗi ra console và vẫn thử lưu database. Kết quả `SUCCESS`/`FAILED` trên dòng log là kết quả thao tác nghiệp vụ, không phải trạng thái lưu database.
- Chỉ ghi các thao tác đã tích hợp audit, không tự ghi mọi lần bấm chuột và không xuất lại lịch sử cũ. File chưa tự dọn theo thời gian; có thể sao chép thư mục `logs` sang USB để lưu giữ. Log được loại khỏi Git.

Ví dụ định dạng (ID và thời gian minh họa):

```text
2026-09-26 10:00:00.000 | user=admin | user_id=1 | module=AUTHENTICATION | action=LOGIN_SUCCESS | target=USER | target_id=1 | result=SUCCESS | reason=- | details={"username":"admin"}
```

### Chi tiết audit và trải nghiệm giao diện

- Các thao tác tạo/sửa/xóa hoặc vô hiệu hóa Employee, Product, Category và User có dữ liệu `before`, `after`; thao tác thất bại có lý do và dữ liệu `attempted` khi có thể xác định. Chỉ các trường được cho phép mới được đưa vào audit, không ghi mật khẩu hay mã băm mật khẩu.
- Quản lý tài khoản ghi cả lỗi kiểm tra dữ liệu, đổi quyền và liên kết/gỡ liên kết nhân viên. Việc lưu tài khoản và liên kết nhân viên là hai bước riêng, có sự kiện riêng; nếu tài khoản đã lưu nhưng liên kết thất bại, giao diện thông báo rõ phần đã hoàn thành.
- Tác vụ qua `AsyncTaskRunner` giữ ID và tên tài khoản tại thời điểm bắt đầu để ghi audit, kể cả khi phiên đăng nhập thay đổi trong lúc chạy. Thông tin này không thay thế kiểm tra quyền nghiệp vụ.
- Lỗi truy vấn danh sách ở các màn hình đã cập nhật được báo là lỗi tải dữ liệu, thay vì hiển thị như một danh sách rỗng thành công. Đăng nhập khi mất kết nối database cũng hiển thị lỗi kết nối và ghi nhận thất bại.
- User Management tải dữ liệu ở nền, có `Refresh`, `Clear form` và khóa nút khi đang xử lý. Chọn tài khoản để sửa; để trống Password khi không muốn đổi mật khẩu. Chọn `Clear link` rồi `Save changes` để lưu việc gỡ liên kết nhân viên.
- Giao diện giữ tiếng Anh. Số tiền dùng chung định dạng `1,234.50`, chưa gắn đơn vị tiền tệ và không quy đổi giá trị trong database. Bộ lọc ngày dùng `yyyy-MM-dd`, kiểm tra ngày không hợp lệ và khoảng ngày bị đảo.

## Sao lưu database

OWNER thực hiện sao lưu trong System Tools, chọn chương trình `mysqldump` (XAMPP thường ở `C:\xampp\mysql\bin\mysqldump.exe`) và nơi lưu file `.sql`, có thể chọn USB.

Ứng dụng xuất ra file tạm trong cùng thư mục đích. Chỉ khi `mysqldump` kết thúc thành công và file có dữ liệu, ứng dụng mới chuyển file tạm thành file đã chọn. Nếu quá trình xuất bị lỗi, bản sao lưu cũ tại đường dẫn đó vẫn được giữ và thông báo hiển thị nguyên nhân từ `mysqldump`. Đây là sao lưu do người dùng thực hiện; chưa có lịch sao lưu tự động.

## Mô hình một cửa hàng

Ứng dụng không còn quản lý, lựa chọn hoặc phân công chi nhánh. Dashboard, chấm công và màn hình bất thường chấm công sử dụng phạm vi toàn cửa hàng. STAFF (bao gồm vai trò EMPLOYEE cũ) chỉ xem và chấm công cho hồ sơ nhân viên liên kết với tài khoản; OWNER/MANAGER quản lý chấm công của cửa hàng. Hồ sơ phải đang hoạt động để Check in/Check out.

Quy tắc ca làm được áp dụng chung cho cửa hàng: ưu tiên quy tắc đang hoạt động có ID mới nhất, hoặc ca mặc định 09:00–18:00 nếu chưa có quy tắc. Gói xuất dữ liệu và audit mới không có thông tin chi nhánh.

Database mới không tạo bảng/cột chi nhánh. Khi mở database cũ, ứng dụng giữ nguyên lịch sử và các bảng/cột cũ nhưng không sử dụng chúng trong nghiệp vụ; chỉ cho phép cột chi nhánh cũ của quy tắc ca nhận NULL để lưu quy tắc dùng chung. Không tự xóa dữ liệu lịch sử hay viết lại nội dung audit cũ. Có thể Check out các phiên cũ còn mở mà không chọn chi nhánh.

## Phạm vi hiện tại

Đã gỡ chức năng bán hàng online, ứng dụng mua hàng của khách và các backend/web liên quan. Ứng dụng không tạo hoặc sử dụng bảng đặt hàng online nữa. Các bảng và dữ liệu online đã tồn tại trong database được giữ nguyên để bảo toàn lịch sử.

Vai trò CUSTOMER/USER cũ vẫn được đọc tương thích; chúng không có quyền truy cập các màn hình quản lý và không còn được chọn để tạo tài khoản mới trên giao diện.

Kết quả rà soát và dọn mã ngày 26/09/2026: [REVIEW_REPORT.md](REVIEW_REPORT.md).
