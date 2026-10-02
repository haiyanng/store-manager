# MÔ TẢ ĐỒ ÁN STOREMANAGERFX

**Ứng dụng quản lý cửa hàng và bán hàng tại quầy**

Tài liệu mô tả chức năng, hướng dẫn sử dụng và nghiệp vụ tiêu biểu.

Đối chiếu với mã nguồn hiện tại ngày 02/10/2026.

## 1. Giới thiệu đồ án

StoreManagerFX là ứng dụng máy tính dành cho một cửa hàng duy nhất; mọi giao dịch và báo cáo sử dụng chung phạm vi cửa hàng. Ứng dụng tập trung vào các công việc thường ngày: quản lý sản phẩm, nhập hàng, bán hàng tại quầy, theo dõi tồn kho, quản lý nhân viên, ghi nhận chấm công và tra cứu lịch sử thao tác.

Thay vì ghi thông tin trên nhiều sổ hoặc bảng tính riêng, người sử dụng làm việc trên cùng một hệ thống. Một lần nhập hàng thành công vừa tạo phiếu nhập vừa tăng tồn kho. Một đơn bán hàng hoàn tất vừa lưu các mặt hàng đã bán vừa giảm tồn kho. Chủ cửa hàng có thể xem dữ liệu tổng hợp và tra nhật ký để biết tài khoản nào đã thực hiện một thao tác.

Đây là ứng dụng **desktop**, giao diện viết bằng JavaFX và hiển thị bằng tiếng Anh. Dữ liệu nghiệp vụ được lưu trong cơ sở dữ liệu thông qua MySQL JDBC. Môi trường chạy demo có thể sử dụng MariaDB trong XAMPP; đây là máy chủ cơ sở dữ liệu, còn JavaFX là phần giao diện ứng dụng.

Ứng dụng hiện tập trung vào bán hàng trực tiếp tại quầy. Các chức năng bán hàng online và ứng dụng mua hàng dành cho khách đã được gỡ khỏi luồng sử dụng hiện tại.

## 2. Mục tiêu và phạm vi

### 2.1. Mục tiêu

- Quản lý tập trung danh mục, sản phẩm, nhân viên và tài khoản nội bộ.
- Ghi nhận hàng nhập và hàng bán, cập nhật tồn kho theo giao dịch.
- Hạn chế sai sót như bán vượt tồn kho, nhập thiếu thông tin hoặc chốt phiếu chưa có mặt hàng.
- Theo dõi thời gian làm việc và những bản ghi chấm công cần xem xét.
- Phân chia quyền sử dụng giữa chủ cửa hàng, quản lý, nhân viên và người chỉ xem dữ liệu.
- Hỗ trợ truy vết hoạt động bằng audit log và sao lưu dữ liệu để phục hồi khi cần.

### 2.2. Các nhóm chức năng chính

| Nhóm | Màn hình | Công việc chính |
|---|---|---|
| Tổng quan | Dashboard | Xem chỉ số và thông tin phù hợp với vai trò đăng nhập |
| Bán hàng | Order | Lập giỏ hàng, hoàn tất đơn bán tại quầy, xem đơn và chi tiết đơn gần đây |
| Hàng hóa | Product, Category | Quản lý thông tin sản phẩm và nhóm sản phẩm |
| Kho | Import, View Inventory | Lập phiếu nhập, xem số lượng tồn và lịch sử biến động kho |
| Nhân sự | Employee | Quản lý hồ sơ nhân viên |
| Tài khoản | User Management | Tạo tài khoản, đổi thông tin/quyền và liên kết với hồ sơ nhân viên |
| Chấm công | Attendance | Ghi nhận giờ vào, giờ ra, lịch sử và tổng giờ trong tháng |
| Xử lý chấm công | Attendance Anomalies | Xem, báo cáo và xử lý các bản ghi bất thường |
| Theo dõi hoạt động | Audit Logs | Tra cứu người thực hiện, hành động, kết quả và chi tiết thay đổi |
| Trao đổi nội bộ | Notifications, Messages | Đọc thông báo hệ thống và trao đổi tin nhắn giữa các tài khoản |
| Dữ liệu hệ thống | System Tools | Sao lưu, phục hồi và xuất/nhập gói dữ liệu theo phạm vi hỗ trợ |

## 3. Người sử dụng và phân quyền

### 3.1. Ý nghĩa các vai trò

**OWNER — Chủ cửa hàng:** có quyền quản trị ứng dụng, bao gồm quản lý tài khoản, xem audit log, sao lưu và phục hồi database. OWNER cũng sử dụng các chức năng vận hành như nhập hàng, bán hàng và quản lý nhân viên.

**MANAGER — Quản lý:** thực hiện các công việc vận hành: quản lý nhân viên, sản phẩm, danh mục, nhập hàng, bán hàng và xem xét bất thường chấm công trong cửa hàng. MANAGER không được quản lý tài khoản hoặc truy cập các công cụ dữ liệu dành riêng cho OWNER.

**STAFF — Nhân viên:** sử dụng bán hàng tại quầy, xem hàng hóa, xem tồn kho, truy cập chấm công và các tiện ích trao đổi nội bộ. STAFF không được sửa danh mục/sản phẩm hoặc tạo tài khoản mới.

**VIEWER — Người xem:** xem Dashboard ở dạng tổng quan danh mục, Product và Category. Trong phiên bản này, VIEWER không có màn hình báo cáo tài chính riêng và không thực hiện giao dịch nhập/bán hàng.

Tên vai trò cũ `EMPLOYEE` được chuyển thành `STAFF` khi đọc từ database. `CUSTOMER` và tên cũ `USER` được giữ để tương thích dữ liệu cũ, không dùng để tạo tài khoản nội bộ mới trên giao diện.

### 3.2. Bảng quyền theo luồng giao diện

| Chức năng | OWNER | MANAGER | STAFF | VIEWER |
|---|---|---|---|---|
| Dashboard | Tổng quan điều hành | Tổng quan vận hành | Thông tin nhân viên liên kết | Tổng quan danh mục |
| Order | Có | Có | Có | Không |
| Product, Category | Xem và quản lý | Xem và quản lý | Chỉ xem | Chỉ xem |
| Employee | Có | Có | Không | Không |
| Import | Có | Có | Không | Không |
| View Inventory | Có | Có | Có | Không |
| Attendance | Có | Có | Chỉ bản thân | Không |
| Attendance Anomalies | Xem, báo cáo, xử lý | Xem và gửi báo cáo theo phạm vi | Không | Không |
| User Management | Có | Không | Không | Không |
| Audit Logs, System Tools | Có | Không | Không | Không |
| Notifications, Messages | Có | Có | Có | Không |

STAFF chỉ xem và chấm công cho hồ sơ nhân viên đang hoạt động liên kết với tài khoản của mình. OWNER và MANAGER có thể chọn nhân viên để quản lý chấm công trong cửa hàng. Nếu chưa liên kết tài khoản với hồ sơ nhân viên, người dùng cần liên hệ OWNER; không cần phân công địa điểm làm việc.

### 3.3. Phân biệt tài khoản và hồ sơ nhân viên

**Tài khoản — User** chứa tên đăng nhập, mật khẩu, vai trò và trạng thái truy cập. **Hồ sơ nhân viên — Employee** chứa họ tên, số điện thoại, địa chỉ, chức vụ, ảnh và trạng thái làm việc.

Tạo một Employee không tự tạo tài khoản đăng nhập. OWNER cần vào User Management để tạo User và chọn Employee tương ứng. Việc liên kết giúp hệ thống tìm được hồ sơ nhân viên từ tài khoản đăng nhập; một hồ sơ đã liên kết với tài khoản khác không được gán tiếp cho tài khoản mới.

## 4. Chuẩn bị và khởi động ứng dụng

### 4.1. Môi trường

- Java/JDK 21 và Maven.
- Máy chủ MySQL hoặc MariaDB đang hoạt động.
- Database và thông tin kết nối phù hợp với cấu hình của ứng dụng.
- Nếu sử dụng chức năng sao lưu/phục hồi: có chương trình `mysqldump` và `mysql` tương ứng trên máy.

Trong thư mục chứa `pom.xml`, sao chép `app-config.example.json` thành `app-config.json`, rồi điền các thông tin kết nối: `host`, `port`, `databaseName`, `username`, `password`. File ví dụ sử dụng tên database `family_business_manager_db`; các thông tin phải được điều chỉnh theo máy cài đặt.

Khởi động database trước. Với môi trường XAMPP, bật dịch vụ ở mục **MySQL**. Sau đó mở terminal tại thư mục chứa `pom.xml` và chạy:

```powershell
mvn "-Dmaven.test.skip=true" clean compile javafx:run
```

Ứng dụng khởi tạo các bảng cần thiết khi kết nối database thành công. Nếu chưa có tài khoản mặc định, hệ thống tạo `admin` với mật khẩu `123456`, vai trò OWNER. Nếu tài khoản này đã tồn tại, ứng dụng giữ mật khẩu hiện có.

### 4.2. Đăng nhập và điều hướng

1. Nhập Username và Password trên màn hình đăng nhập.
2. Đăng nhập thành công để vào Dashboard.
3. Chọn chức năng trên thanh điều hướng bên trái. Order nằm ngay dưới Dashboard để thuận tiện bán hàng.
4. Dùng các nút Notifications và Messages trên khung chính để mở tiện ích tương ứng nếu vai trò được phép.
5. Nhấn Logout khi kết thúc phiên làm việc hoặc chuyển người sử dụng.

Mỗi nhân viên nên sử dụng tài khoản được cấp riêng để audit log phản ánh đúng người thao tác. Ứng dụng không có luồng đăng ký tài khoản công khai cho người dùng tự tạo tài khoản.

### 4.3. Quy ước thao tác chung

| Thành phần | Cách hiểu và sử dụng |
|---|---|
| Create | Tạo bản ghi từ thông tin đang nhập trong biểu mẫu |
| Save changes | Lưu thay đổi cho bản ghi đang chọn |
| Clear form | Xóa nội dung biểu mẫu và bỏ chọn để chuẩn bị nhập mới; không xóa bản ghi đã lưu |
| Refresh | Tải lại dữ liệu từ database |
| Delete | Xóa bản ghi theo chức năng của màn hình, có thể bị từ chối nếu dữ liệu đang được tham chiếu |
| Deactivate | Chuyển bản ghi sang không hoạt động; không đồng nghĩa xóa dữ liệu lịch sử |
| Active / Inactive | Đang hoạt động / không hoạt động |
| Select image | Chọn ảnh từ máy cho bản ghi |
| Remove image | Bỏ ảnh khỏi biểu mẫu; lưu bản ghi để áp dụng thay đổi |
| Add item / Remove item | Thêm dòng vào hoặc bỏ dòng khỏi phiếu/giỏ đang lập |
| Clear items | Xóa các dòng của phiếu/giỏ đang lập; không xóa giao dịch đã hoàn tất |

Các màn hình đã áp dụng trạng thái tải sẽ báo Loading khi đang lấy dữ liệu, thông báo riêng khi không có bản ghi hoặc khi truy vấn bị lỗi. Khi đang xử lý, một số nút được khóa để tránh thao tác trùng.

Ngày trong các bộ lọc được nhập theo dạng `yyyy-MM-dd`, ví dụ `2026-09-29`. Số tiền hiển thị theo dạng `1,234.50`. Ứng dụng hiện chưa gắn đơn vị USD hoặc VND và không tự chuyển đổi tiền tệ. Khi nhập số tiền, dùng dạng như `15000` hoặc `15000.50`, không gõ thêm ký hiệu tiền tệ hay dấu phân tách hàng nghìn.

## 5. Hướng dẫn sử dụng các màn hình

### 5.1. Dashboard — Theo dõi tổng quan

Dashboard thay đổi theo vai trò đăng nhập. OWNER thấy các thông tin điều hành như số nhân viên, nhân viên đang làm việc, doanh thu và cảnh báo tồn kho. MANAGER thấy các chỉ số vận hành như nhân viên đang chấm công, tồn kho và số đơn bán trong tháng. STAFF thấy thông tin gắn với hồ sơ nhân viên liên kết; dữ liệu có thể thiếu nếu chưa được liên kết đầy đủ. VIEWER thấy tổng quan danh mục sản phẩm.

Phần chênh lệch thu/chi đang dựa trên doanh thu bán hàng trừ tổng chi phí nhập hàng. Không nên diễn giải con số này là lợi nhuận kế toán: hệ thống chưa tổng hợp đầy đủ lương, thuê mặt bằng, thuế, chi phí vận hành và giá vốn theo phương pháp kế toán.

### 5.2. Category — Quản lý danh mục

Danh mục giúp nhóm sản phẩm, chẳng hạn Đồ uống, Bánh kẹo hoặc Đồ gia dụng.

1. OWNER hoặc MANAGER mở Category.
2. Chọn Clear form nếu đang chọn bản ghi cũ.
3. Nhập Category name, chọn ảnh nếu cần và trạng thái Active.
4. Nhấn Create để lưu.
5. Muốn sửa, chọn dòng trong bảng, điều chỉnh thông tin rồi nhấn Save changes.
6. Dùng Deactivate khi cần ngừng sử dụng danh mục.

Việc vô hiệu hóa danh mục không nên được hiểu là đã đồng thời vô hiệu hóa tất cả sản phẩm thuộc danh mục đó. Khi ngừng bán một sản phẩm, cần kiểm tra trạng thái của chính sản phẩm.

### 5.3. Product — Quản lý sản phẩm

Thông tin sản phẩm gồm Product name, Category, SKU, Barcode, Base price, Unit, ảnh và trạng thái Active. SKU là mã nội bộ để nhận diện hàng hóa; Barcode là thông tin mã vạch; Unit là đơn vị như chai, hộp hoặc cái. Base price được dùng làm giá bán cơ sở trong luồng Order.

1. Tạo danh mục trước nếu muốn phân loại sản phẩm.
2. Mở Product và chọn Clear form.
3. Nhập tên sản phẩm, SKU, đơn vị và giá; chọn danh mục, ảnh và các thông tin bổ sung.
4. Nhấn Create.
5. Chọn một sản phẩm trong bảng để xem hoặc sửa, sau đó Save changes.

Tên, SKU và đơn vị là thông tin bắt buộc trong kiểm tra nghiệp vụ. Giá không được âm và tối đa hai chữ số thập phân. Tạo sản phẩm mới chỉ khai báo mặt hàng; muốn có số lượng để bán, cần nhập hàng qua Import. Deactivate ngừng sử dụng sản phẩm trong các giao dịch mới nhưng giữ lại dữ liệu liên quan để tra cứu lịch sử.

### 5.4. Employee — Quản lý nhân viên

OWNER hoặc MANAGER mở Employee, nhập Full name, Phone, Position, Address, ảnh và trạng thái Active. Họ tên và chức vụ là thông tin bắt buộc. Nhấn Create để tạo hồ sơ, hoặc chọn bản ghi rồi Save changes để cập nhật.

Thông tin Position mô tả công việc thực tế như Thu ngân hoặc Nhân viên kho. Nó khác với Role của tài khoản: nhập chức vụ “Quản lý” trong Employee không tự cấp quyền MANAGER cho tài khoản.

Đối với nhân viên ngừng làm việc, cần xem xét riêng trạng thái hồ sơ nhân viên và quyền truy cập của tài khoản liên kết. Không nên hiểu việc đổi trạng thái Employee là tự động khóa User, vì đây là hai đối tượng riêng biệt.

### 5.5. User Management — Cấp tài khoản nội bộ

Chỉ OWNER được sử dụng màn hình này.

1. Tạo hồ sơ Employee trước nếu tài khoản cần gắn với một nhân viên.
2. Mở User Management và chọn Clear form.
3. Nhập Username, Password và chọn Role phù hợp.
4. Chọn Linked employee nếu cần liên kết.
5. Nhấn Create và đợi thông báo kết quả.

Để sửa, chọn tài khoản, điều chỉnh Username, Role hoặc Linked employee rồi nhấn Save changes. Để trống Password nếu muốn giữ mật khẩu hiện tại. Muốn gỡ liên kết, nhấn Clear link rồi Save changes.

Tên đăng nhập không được trùng. Tài khoản OWNER được bảo vệ khỏi luồng sửa/xóa hiện tại; vì vậy không phải dòng nào trong bảng cũng có thể chỉnh sửa. Lưu tài khoản và lưu liên kết nhân viên là hai bước riêng. Nếu bước đầu thành công nhưng bước liên kết thất bại, giao diện thông báo phần đã lưu để người dùng tải lại và kiểm tra, tránh tạo thêm tài khoản trùng.

### 5.6. Import — Nhập hàng vào kho

Import trong thanh điều hướng là nghiệp vụ nhập hàng từ nhà cung cấp, không phải đọc một file Excel hoặc CSV.

1. OWNER hoặc MANAGER mở Import.
2. Nhập tên Supplier.
3. Chọn sản phẩm, nhập Qty và Unit cost.
4. Nhấn Add item; lặp lại với các mặt hàng cần nhập.
5. Kiểm tra các dòng và Total cost. Dùng Remove item hoặc Clear items nếu cần sửa giỏ nhập.
6. Nhấn Complete import để hoàn tất.
7. Kiểm tra phiếu trong Recent import receipts và số lượng mới ở View Inventory.

Số lượng phải là số nguyên dương, giá nhập không âm và tối đa hai chữ số thập phân. Tổng chi phí được tính bằng tổng của từng dòng: `số lượng × giá nhập`.

Nếu thêm lại cùng một sản phẩm với cùng giá nhập, hệ thống cộng dồn số lượng trên dòng đó. Nếu cùng sản phẩm nhưng khác giá nhập, hệ thống giữ các dòng riêng để không ghi đè giá cũ. Phiếu chỉ làm thay đổi tồn kho khi Complete import thành công.

### 5.7. Order — Bán hàng tại quầy

OWNER, MANAGER hoặc STAFF mở Order để ghi nhận đơn bán trực tiếp.

1. Chọn sản phẩm; xem ảnh, SKU, Barcode, Unit price và Stock ở phần xem trước.
2. Nhập Qty và nhấn Add item.
3. Thêm các sản phẩm khác vào Cart.
4. Kiểm tra Quantity, Unit price, Subtotal và Total.
5. Nhấn Complete order.
6. Xem đơn vừa tạo trong Recent orders; chọn đơn để xem Order details.

Giỏ không được rỗng, sản phẩm phải phù hợp với điều kiện bán và số lượng bán phải đáp ứng tồn kho. Khi hoàn tất thành công, hệ thống lưu đơn, các dòng sản phẩm, tài khoản lập đơn, tổng tiền và thời gian; đồng thời giảm tồn kho tương ứng.

Complete order là ghi nhận đơn bán trong ứng dụng. Phiên bản hiện tại không có luồng thu tiền qua ngân hàng/cổng thanh toán, tính tiền thừa hay phát hành hóa đơn điện tử. Người bán thực hiện việc nhận tiền theo quy trình cửa hàng, rồi ghi nhận giao dịch trong hệ thống. Không có thao tác hủy/hoàn trả đơn đã hoàn tất trên màn hình Order hiện tại.

### 5.8. View Inventory — Xem tồn kho

Màn hình này dùng để xem hàng tồn và lịch sử biến động, không phải nơi nhập trực tiếp số lượng để sửa kho.

Tồn kho thay đổi theo các giao dịch được hỗ trợ, trong đó hai luồng thường dùng là nhập hàng và bán hàng. Khi thấy số lượng không như dự kiến, người dùng nên tải lại dữ liệu, đối chiếu phiếu nhập/đơn bán và lịch sử kho trước khi kết luận có sai lệch.

Về nguyên tắc, `tồn cuối = tồn đầu + lượng nhập − lượng bán`, cộng thêm các biến động khác nếu đã được ghi nhận qua công cụ dữ liệu được hỗ trợ. Các dòng kho liên quan giao dịch giúp giải thích vì sao số lượng tăng hoặc giảm.

### 5.9. Attendance — Ghi nhận chấm công

Attendance lưu các phiên làm việc gồm nhân viên, giờ vào, giờ ra, số giờ và tài khoản thực hiện.

1. Mở Attendance.
2. OWNER/MANAGER chọn nhân viên cần chấm công; với STAFF, hệ thống chọn và khóa hồ sơ liên kết của chính tài khoản.
3. Khi bắt đầu làm việc, nhấn Check in.
4. Khi kết thúc, nhấn Check out cho phiên đang mở.
5. Xem Attendance history và Current month totals.

Một nhân viên không được Check in thêm nếu vẫn còn phiên chưa Check out. Check out yêu cầu có phiên đang mở của nhân viên. Hồ sơ nhân viên phải còn hoạt động; STAFF không được thao tác cho nhân viên khác.

Ứng dụng vận hành theo mô hình một cửa hàng, không yêu cầu dữ liệu phân công chi nhánh. Các phiên làm việc cũ được giữ nguyên và vẫn có thể kết thúc bằng Check out sau khi nâng cấp.

### 5.10. Attendance Anomalies — Xem xét bất thường chấm công

Attendance trả lời “nhân viên đã vào/ra lúc nào”, còn Attendance Anomalies trả lời “bản ghi nào cần được xem xét”.

Các loại bất thường hiện có gồm vào quá sớm/muộn, ra quá sớm/muộn, thời gian làm việc quá ngắn/dài, thiếu giờ ra và làm ngoài ca. Việc phát hiện dựa trên quy tắc ca làm và các ngưỡng thời gian. Quy tắc đang hoạt động có ID mới nhất được áp dụng chung cho cửa hàng. Nếu chưa có, hệ thống dùng ca mặc định 09:00–18:00; đây chưa phải công cụ lập lịch ca đầy đủ trên giao diện.

Luồng xử lý:

1. OWNER hoặc MANAGER mở Attendance Anomalies.
2. Lọc theo nhân viên, loại, mức độ, trạng thái hoặc khoảng ngày; nhấn Apply.
3. Chọn một bất thường và đọc chi tiết phiên chấm công.
4. MANAGER gửi giải trình bằng Submit report trong cửa hàng.
5. OWNER xem báo cáo, chỉnh dữ liệu giờ làm nếu cần bằng Save attendance changes, rồi chọn Resolve hoặc Dismiss theo kết quả kiểm tra.
6. OWNER có thể điều chỉnh việc thông báo cho nhân viên bằng Update employee notification.

| Trạng thái | Ý nghĩa |
|---|---|
| OPEN | Bất thường mới, chưa xử lý xong |
| REPORTED_BY_MANAGER | Đã có báo cáo của quản lý |
| RESOLVED | Đã xử lý |
| DISMISSED | Đã xem xét và bỏ qua cảnh báo |

Resolve hoặc Dismiss là thay đổi trạng thái xem xét. Muốn thay đổi giờ vào/ra phải thực hiện bước chỉnh bản ghi chấm công; không nên hiểu Resolve tự bổ sung giờ ra còn thiếu.

### 5.11. Audit Logs — Tra cứu lịch sử thao tác

OWNER mở Audit Logs để tìm các sự kiện theo hành động, loại đối tượng, module, User ID, kết quả và khoảng ngày. Nhấn Search để tìm; chọn một dòng để đọc phần chi tiết.

Một sự kiện có thể cho biết thời gian, ID/tên tài khoản thực hiện, hành động, đối tượng tác động, kết quả Success/Failed, lý do và thông tin bổ sung. Với các thao tác CRUD đã bổ sung audit, dữ liệu chi tiết dùng các trường:

- `before`: giá trị trước khi thay đổi, nếu đọc được.
- `after`: giá trị sau thao tác, nếu xác định được.
- `attempted`: dữ liệu người dùng đã yêu cầu, hữu ích khi thao tác thất bại.

Đổi mật khẩu chỉ được ghi nhận bằng thông tin có yêu cầu thay đổi; mật khẩu và mã băm mật khẩu không được đưa vào snapshot audit. Tác vụ chạy nền giữ danh tính tài khoản lúc bắt đầu để ghi nhật ký.

Các sự kiện qua AuditService được ghi vào bảng `audit_logs` và file text UTF-8 `logs/audit-YYYY-MM-DD.txt`, tính từ thư mục chạy ứng dụng. Trong cùng ngày, ứng dụng ghi tiếp file; sang ngày mới tạo file mới khi có sự kiện. Log không ghi mọi lần bấm chuột và không tự xuất lại các sự kiện cũ trong database.

File và database được ghi theo hai bước độc lập, nên có thể chỉ một nơi ghi thành công nếu xảy ra sự cố. Kết quả Success/Failed mô tả nghiệp vụ, không bảo đảm rằng cả hai nơi lưu audit đều thành công. Audit dùng để truy vết, không thay thế bản sao lưu dữ liệu.

### 5.12. Notifications và Messages

Notifications là thông báo do hệ thống tạo, chẳng hạn sau khi hoàn tất nhập hàng, bán hàng hoặc các sự kiện chấm công. Người dùng xem nội dung, tải lại và đánh dấu đã đọc bằng Mark as read.

Messages phục vụ trao đổi nội bộ giữa tài khoản. Người dùng chọn cuộc hội thoại/người nhận trong danh sách, nhập nội dung ở Compose rồi nhấn Send message. Lịch sử thể hiện người gửi, thời gian, nội dung và trạng thái đọc. Đây là tin nhắn trong ứng dụng, không phải gửi email hoặc SMS.

### 5.13. System Tools — Sao lưu và phục hồi

Chỉ OWNER được truy cập System Tools.

**Tạo bản sao lưu database:**

1. Điền đường dẫn `mysqldump executable`; với XAMPP thường là `C:\xampp\mysql\bin\mysqldump.exe`.
2. Nhấn Create Backup.
3. Chọn tên file `.sql` và nơi lưu.
4. Đợi kết quả và kiểm tra thông tin thời gian, kích thước, vị trí file trên màn hình.

Ứng dụng xuất vào file tạm trước; chỉ khi quá trình xuất thành công và có dữ liệu mới chuyển thành file đích. Đây là sao lưu do người dùng chủ động thực hiện, chưa có lịch tự chạy và chưa có cơ chế tự phát hiện USB để sao lưu.

**Phục hồi database:**

1. Chuẩn bị file SQL phù hợp với database cần phục hồi.
2. Điền `mysql executable`, ví dụ `C:\xampp\mysql\bin\mysql.exe`.
3. Nhấn Select Backup File và chọn file.
4. Đọc thông báo xác nhận rồi thực hiện nếu đúng mục đích.
5. Sau khi thành công, mở lại ứng dụng/đăng nhập lại và đối chiếu dữ liệu.

Restore thực thi nội dung SQL vào database đang cấu hình và có thể ghi đè dữ liệu hiện tại. Các giao dịch phát sinh sau thời điểm bản sao lưu được tạo không tự có trong bản sao lưu đó. Nên giữ một bản sao của dữ liệu hiện tại trước khi phục hồi.

File SQL không tự bao gồm ảnh được lưu trên ổ đĩa hoặc file audit text. Khi chuyển sang máy khác, cần xét thêm thư mục `data/images`, thư mục `logs` và cấu hình kết nối của máy mới. Sao lưu SQL và sao chép tài nguyên ngoài database là các việc khác nhau.

### 5.14. Migration & Export — Gói dữ liệu di chuyển

Trong System Tools còn có phần Migration & Export. Người dùng chọn nhóm dữ liệu cần xuất như Products, Employees, Orders, Attendance, Images hoặc Audit Logs, rồi Export Package để tạo gói dữ liệu.

Khi nhận gói, dùng Select Package, Preview và Validate để xem nội dung và các lỗi/cảnh báo trước khi nhấn Import. Luồng nhập gói hiện tại tập trung vào danh mục, sản phẩm, ảnh liên quan và dữ liệu tồn kho; không được hiểu rằng mọi nhóm có thể xuất đều đã được nhập ngược đầy đủ, đặc biệt là nhân viên, đơn bán và chấm công.

Phần Import ở đây là **nhập gói dữ liệu**, khác với màn hình Import dùng để **nhập hàng từ nhà cung cấp**. Gói di chuyển cũng khác bản sao lưu SQL và chưa phải cơ chế đồng bộ cloud.

## 6. Các nghiệp vụ tiêu biểu

Các số tiền dưới đây chỉ là giá trị minh họa, chưa gắn đơn vị tiền tệ. Có thể dùng cùng một bộ dữ liệu để trình bày luồng nhập hàng → bán hàng → kiểm tra kho → xem audit.

### 6.1. Cấp tài khoản cho nhân viên thu ngân mới

**Tình huống:** cửa hàng nhận nhân viên Nguyễn Minh An, cần cho phép bán hàng mà không cho quyền quản trị tài khoản.

**Điều kiện:** có tài khoản OWNER đăng nhập.

**Thực hiện:** tạo Employee với họ tên Nguyễn Minh An, Position là Thu ngân và trạng thái Active. Sau đó vào User Management, tạo Username `minhan`, chọn Role STAFF và Linked employee là Nguyễn Minh An.

**Kết quả:** nhân viên có tài khoản riêng để đăng nhập. STAFF có thể vào Order nhưng không có User Management, Audit Logs hoặc System Tools. Việc tạo tài khoản và liên kết nhân viên có audit tương ứng. Khi hồ sơ liên kết còn hoạt động, STAFF có thể chấm công cho chính mình mà không cần phân công chi nhánh.

**Trường hợp không hợp lệ:** Username đã tồn tại hoặc Employee đã liên kết với một tài khoản khác. Hệ thống từ chối và thông báo để sửa thông tin.

### 6.2. Nhập cùng sản phẩm với hai giá nhập

**Tình huống:** kho chưa có sản phẩm Nước suối A. Cửa hàng nhập 10 chai giá 5,000 và thêm 10 chai giá 6,000.

**Điều kiện:** đã có sản phẩm đang hoạt động với SKU `NS-A`, đơn vị chai; OWNER hoặc MANAGER đăng nhập.

**Thực hiện:** mở Import, nhập Supplier, thêm `NS-A` với Qty 10 và Unit cost 5000; sau đó thêm tiếp `NS-A` với Qty 10 và Unit cost 6000.

| Dòng | Sản phẩm | Số lượng | Giá nhập | Thành tiền |
|---|---|---:|---:|---:|
| 1 | Nước suối A | 10 | 5,000.00 | 50,000.00 |
| 2 | Nước suối A | 10 | 6,000.00 | 60,000.00 |
| Tổng | | 20 | | 110,000.00 |

**Kết quả khi Complete import thành công:** phiếu có tổng chi phí 110,000.00, kho tăng 20 chai và có lịch sử nhập tương ứng. Hệ thống không lấy giá nhập mới nhất để tính lại cả 20 chai.

**Trường hợp bổ sung:** nếu thêm tiếp 5 chai cùng giá 6,000, số lượng dòng thứ hai tăng lên 15; tổng phiếu thành 140,000.00 và tổng lượng nhập là 25 chai. Đây là ví dụ thay thế, không phải bước bắt buộc của tình huống tiếp theo.

### 6.3. Bán hàng thành công và kiểm tra tồn kho

**Tình huống:** tiếp tục từ phiếu 20 chai ở ví dụ 6.2. Giá bán cơ sở của Nước suối A là 8,000.00; khách mua 3 chai.

**Thực hiện:** nhân viên vào Order, chọn sản phẩm, nhập Qty 3, Add item và kiểm tra tổng tiền `3 × 8,000 = 24,000.00`. Sau khi xác nhận giao dịch tại quầy, nhấn Complete order.

**Kết quả:** đơn mới có 3 chai và tổng tiền 24,000.00. Tồn kho giảm từ 20 xuống 17. Người sử dụng có thể đối chiếu đơn ở Recent orders, chi tiết mặt hàng ở Order details, số lượng ở View Inventory và sự kiện ở Audit Logs bằng tài khoản OWNER.

**Điểm nghiệp vụ:** việc lưu đơn và trừ kho được thực hiện trong cùng giao dịch database. Nếu một bước trong giao dịch lỗi, hệ thống rollback thay vì cố ý giữ một đơn hoàn tất mà chưa trừ đủ kho.

### 6.4. Từ chối bán vượt tồn kho

**Tình huống:** kho còn 17 chai nhưng người bán lập đơn 18 chai.

**Kết quả mong đợi theo kiểm tra hiện có:** ứng dụng từ chối thao tác tại bước kiểm tra số lượng/tồn kho; người dùng giảm số lượng hoặc nhập thêm hàng qua đúng nghiệp vụ rồi thử lại. Một lần chốt đơn thất bại không được làm kho âm hoặc tạo đơn hoàn tất một phần.

Thông tin tồn hiển thị trên màn hình là dữ liệu đã tải tại thời điểm xem. Nếu có người khác bán cùng sản phẩm, tồn thực tế có thể thay đổi; quá trình hoàn tất vẫn phải kiểm tra/cập nhật kho tại database. Các lần chốt bị từ chối ở service có audit thất bại; lỗi bị chặn ngay trên biểu mẫu không nhất thiết tạo sự kiện audit.

### 6.5. Xử lý trường hợp quên Check out

**Tình huống:** một nhân viên đã Check in nhưng quên Check out. Khi hệ thống chạy bước quét các phiên mở và bản ghi vượt điều kiện cho phép, xuất hiện bất thường MISSING_CHECK_OUT.

**Thực hiện:** quản lý mở Attendance Anomalies, chọn bản ghi, kiểm tra lại với nhân viên và gửi báo cáo. OWNER đọc báo cáo, đối chiếu giờ làm thực tế, nhập giờ ra phù hợp rồi Save attendance changes. Sau khi kiểm tra kết quả, OWNER Resolve bất thường hoặc Dismiss nếu cảnh báo được xác định không cần xử lý.

**Kết quả:** dữ liệu chấm công và trạng thái xem xét phản ánh kết luận của người có quyền; các thao tác đã tích hợp audit lưu lại dấu vết xử lý. Không tự suy ra số giờ còn thiếu hoặc tự tính lương từ việc xuất hiện cảnh báo.

### 6.6. Truy vết việc thay đổi giá sản phẩm

**Tình huống:** OWNER nhận thấy giá Nước suối A chuyển từ 8,000.00 thành 9,000.00.

**Thực hiện:** mở Audit Logs, lọc module PRODUCT, hành động cập nhật và khoảng ngày liên quan; chọn sự kiện đúng sản phẩm.

**Kết quả có thể đọc:** tài khoản thao tác, thời điểm, ID sản phẩm, kết quả, `before.base_price` và `after.base_price`. Nếu cập nhật thất bại, đọc lý do cùng dữ liệu attempted để phân biệt giá người dùng muốn nhập với giá đã lưu. Audit hỗ trợ xác định nguồn thay đổi; không phải nút tự hoàn tác giá về bản cũ.

### 6.7. Phục hồi dữ liệu sau sự cố

**Tình huống:** cửa hàng có bản sao lưu SQL được tạo cuối ngày hôm trước và cần phục hồi sau lỗi dữ liệu.

**Thực hiện:** OWNER xác định đúng file và thời điểm sao lưu, giữ lại dữ liệu hiện tại nếu còn truy cập được, rồi thực hiện Restore trong System Tools. Sau khi phục hồi, mở lại ứng dụng và kiểm tra số sản phẩm, đơn gần nhất, phiếu nhập và tồn kho. Nếu chuyển sang máy khác, phục hồi thêm ảnh từ bản sao thư mục ảnh và cấu hình kết nối phù hợp.

**Giới hạn:** dữ liệu phát sinh sau thời điểm backup không tự khôi phục từ file SQL cũ. Audit text có thể giúp đối chiếu giao dịch đã xảy ra nhưng không phải một bản sao đầy đủ để tự dựng lại toàn bộ database. Nếu bản backup chỉ nằm trên ổ đĩa đã hỏng thì không thể trông chờ nó phục hồi máy đó; nơi giữ bản sao phải được lựa chọn theo nhu cầu vận hành.

## 7. Cách tổ chức dữ liệu và xử lý

### 7.1. Công nghệ

| Thành phần | Công nghệ/vai trò |
|---|---|
| Ngôn ngữ | Java 21 |
| Giao diện | JavaFX 21, FXML và CSS |
| Xây dựng/chạy dự án | Maven |
| Truy cập dữ liệu | JDBC với MySQL Connector/J |
| Máy chủ database | MySQL hoặc môi trường MariaDB tương thích đang dùng cho demo |
| Xử lý JSON | Gson và Jackson cho các phần dữ liệu cấu trúc |
| Tài nguyên cục bộ | Ảnh trong `data/images`, audit text trong `logs` |

Kiến trúc được tổ chức theo luồng `FXML → Controller → Presenter → Service → Repository → Database`. FXML/CSS mô tả giao diện; Controller nhận thao tác; Presenter điều phối màn hình; Service kiểm tra và xử lý nghiệp vụ; Repository chứa các thao tác lưu/đọc dữ liệu. Các giao dịch nhập/bán phối hợp nhiều thao tác dữ liệu để ghi phiếu và cập nhật kho cùng nhau.

### 7.2. Quan hệ dữ liệu chính

| Đối tượng | Quan hệ và ý nghĩa |
|---|---|
| User và Employee | Hồ sơ nhân viên có thể liên kết tới tài khoản qua User ID |
| Category và Product | Danh mục dùng để nhóm sản phẩm |
| Product và Inventory | Theo dõi số lượng hiện có của từng sản phẩm |
| Import receipt và Import item | Một phiếu nhập có nhiều dòng, mỗi dòng lưu sản phẩm, số lượng và giá nhập |
| Sale order và Sale order item | Một đơn bán có nhiều dòng, lưu số lượng và đơn giá của giao dịch |
| Inventory transaction | Ghi biến động số lượng và liên hệ với nghiệp vụ phát sinh |
| Attendance session | Liên hệ nhân viên, thời gian làm việc và người thực hiện |
| Attendance anomaly | Liên hệ phiên chấm công cần xem xét và trạng thái xử lý |
| Audit log | Ghi tài khoản thực hiện và đối tượng bị tác động trong một sự kiện |

Các dòng nhập/bán lưu đơn giá riêng của giao dịch. Vì vậy, thay đổi giá cơ sở của sản phẩm sau này không có nghĩa tự đổi đơn giá của các dòng giao dịch cũ đã lưu.

## 8. Phạm vi chưa hoàn thiện và hướng phát triển

Để đánh giá đúng đồ án, cần phân biệt chức năng đã có với phần chưa triển khai hoặc cần hoàn thiện:

- Không có bán hàng online, đăng ký tài khoản công khai hoặc quy trình giao hàng.
- Chưa có quản lý hạn sử dụng, lô hàng hoặc xuất kho theo hạn dùng.
- Chưa có luồng đổi trả/hủy đơn hoàn tất trên màn hình Order, cổng thanh toán hoặc hóa đơn điện tử.
- Chấm công chưa phải hệ thống tính lương hoặc lập lịch nhiều ca; quy tắc ca hiện áp dụng chung cho cửa hàng.
- Audit đã được tích hợp ở nhiều nghiệp vụ nhưng không bảo đảm ghi mọi thao tác hoặc lưu đồng thời thành công ở cả file và database; chưa có cơ chế tự dọn file theo thời gian.
- Backup hiện là thao tác thủ công; chưa có lịch sao lưu tự động hay cơ chế sao lưu USB tự động.
- Gói Migration & Export chưa nhập ngược đầy đủ mọi nhóm dữ liệu có thể xuất; chưa đồng bộ cloud.
- Thành phần license hiện có phần mô phỏng, không nên giới thiệu như một hệ thống cấp phép thương mại hoàn chỉnh.
- Chưa có quy ước tiền tệ cấu hình trên giao diện; cần thống nhất đơn vị sử dụng khi triển khai cho một cửa hàng cụ thể.

Các hướng phát triển này là đề xuất cho những phiên bản sau, không phải chức năng đã được bổ sung trong tài liệu này.

## 9. Kịch bản trình bày đồ án

Một buổi demo có thể đi theo trình tự sau để người xem hiểu được sự liên kết giữa các màn hình:

1. Đăng nhập OWNER và giải thích Dashboard cùng cách phân quyền.
2. Tạo danh mục Đồ uống và sản phẩm Nước suối A, SKU `NS-A`, đơn vị chai, giá bán 8,000.
3. Tạo Employee Nguyễn Minh An và cấp tài khoản STAFF `minhan` có liên kết.
4. Nhập 10 chai giá 5,000 và 10 chai giá 6,000 trong cùng phiếu; chỉ ra tổng đúng là 110,000 và kho tăng 20 chai.
5. Đăng nhập STAFF, bán 3 chai với tổng 24,000; kiểm tra kho còn 17 và mở chi tiết đơn.
6. Thử số lượng vượt tồn để minh họa kiểm tra nghiệp vụ.
7. Dùng tài khoản STAFF đã liên kết với hồ sơ nhân viên đang hoạt động để trình bày Check in/Check out. Dùng MANAGER/OWNER để xem xét bất thường của cửa hàng.
8. Đăng nhập OWNER, tra audit của các thao tác vừa thực hiện và mở file audit text tương ứng nếu cần minh họa cách lưu.
9. Tạo một bản backup SQL và giải thích cách phục hồi. Chỉ trình bày quy trình Restore khi demo thông thường; thao tác phục hồi thật cần bộ dữ liệu demo riêng vì có thể thay đổi database đang dùng.

Luồng này thể hiện ba mối liên hệ chính của ứng dụng: hàng hóa gắn với nhập/bán và tồn kho; nhân viên gắn với tài khoản và chấm công; các thao tác nghiệp vụ gắn với người thực hiện và lịch sử kiểm tra.

## 10. Nguồn đối chiếu trong project

- [README.md](README.md): cách chạy, audit text, sao lưu và phạm vi ứng dụng.
- [pom.xml](pom.xml): cấu hình Java, JavaFX, Maven và thư viện.
- [DashboardMenuRegistry.java](src/main/java/com/storemanager/domain/dashboard/model/DashboardMenuRegistry.java): menu và vai trò được truy cập.
- [PermissionGuard.java](src/main/java/com/storemanager/core/security/PermissionGuard.java): quyền thao tác chính.
- [UserManagementService.java](src/main/java/com/storemanager/domain/user/service/UserManagementService.java): tài khoản và liên kết nhân viên.
- [ImportPresenter.java](src/main/java/com/storemanager/domain/importing/presenter/ImportPresenter.java) và [ImportService.java](src/main/java/com/storemanager/domain/importing/service/ImportService.java): giỏ nhập, giá nhập, phiếu nhập và cập nhật kho.
- [SaleService.java](src/main/java/com/storemanager/domain/sale/service/SaleService.java): đơn bán và giao dịch kho.
- [AttendanceService.java](src/main/java/com/storemanager/domain/attendance/service/AttendanceService.java) và [AttendanceAnomalyService.java](src/main/java/com/storemanager/domain/attendance_anomaly/service/AttendanceAnomalyService.java): chấm công và xử lý bất thường.
- [AuditService.java](src/main/java/com/storemanager/domain/audit/service/AuditService.java): cấu trúc và việc ghi sự kiện.
- [MigrationPackageImportService.java](src/main/java/com/storemanager/domain/system_tool/migration_export/service/MigrationPackageImportService.java): phạm vi nhập gói dữ liệu hiện tại.
