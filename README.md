# 🎬 HỆ THỐNG QUẢN LÝ RẠP CHIẾU PHIM (CINEMA MANAGEMENT SYSTEM)

Dự án phần mềm quản lý rạp chiếu phim được xây dựng bằng **Java Swing** với giao diện hiện đại **FlatLaf**, áp dụng kiến trúc phân tầng chuẩn (MVC / DAO), kết nối cơ sở dữ liệu MySQL, bảo mật mật khẩu với thuật toán băm Argon2 và tích hợp mã thanh toán chuyển khoản ngân hàng VietQR.

---

## 📌 TÍNH NĂNG CHÍNH

### 1. Phân Quyền & Đăng Nhập
- Hệ thống hỗ trợ 2 vai trò chính: **Quản lý (Manager)** và **Nhân viên (Staff)**.
- Ghi nhớ đăng nhập an toàn bằng Java Preferences.
- Mã hóa mật khẩu bảo mật cao bằng thuật toán **Argon2** (`argon2-jvm`).

### 2. Nghiệp Vụ Bán Vé & Ghế Phòng Chiếu
- Chọn ngày chiếu, phim và khung giờ suất chiếu trực quan.
- Sơ đồ ghế ngồi theo từng phòng chiếu, hiển thị trạng thái ghế trống / đã đặt.
- Phân loại độ tuổi xem phim theo quy định điện ảnh.
- Hỗ trợ áp dụng mã khuyến mãi, tích điểm thành viên khách hàng.

### 3. Dịch Vụ Bắp Nước & Combo
- Bán combo bắp nước độc lập hoặc mua kèm với vé xem phim.
- Tính toán tổng tiền, chiết khấu và tự động xuất hóa đơn / phiếu bắp nước.

### 4. Thanh Toán & In Vé
- Tích hợp thanh toán tiền mặt và chuyển khoản ngân hàng qua **VietQR** (tự động tạo mã QR EMV/NAPAS kèm số tiền và mã suất chiếu/vé).
- In vé xem phim, in phiếu bắp nước với định dạng chi tiết.

### 5. Quản Trị Hệ Thống (Dành Cho Quản Lý)
- **Quản lý phim:** Thêm, cập nhật thông tin phim, thời lượng, thể loại, áp poster phim.
- **Quản lý suất chiếu & phòng chiếu:** Lên lịch chiếu theo phòng và khung giờ.
- **Quản lý nhân viên & tài khoản:** Tạo tài khoản, phân quyền, cập nhật trạng thái làm việc.
- **Quản lý khách hàng:** Danh sách hội viên, lịch sử giao dịch.
- **Quản lý khuyến mãi:** Chương trình giảm giá, voucher.
- **Xử lý sự cố:** Ghi nhận và xử lý đền bù/đổi vé khi gặp sự cố phòng chiếu.
- **Báo cáo doanh thu & Thống kê:** Báo cáo chi tiết theo ngày, phim, doanh số vé/bắp nước, hỗ trợ xuất dữ liệu ra file Excel.

---

## 🛠 CÔNG NGHỆ SỬ DỤNG

- **Ngôn ngữ:** Java (JDK 17+)
- **Giao diện (UI):** Java Swing + FlatLaf Light/Dark Theme
- **Kiến trúc:** Phân tầng MVC (Model - View - Controller - DAO)
- **Công cụ quản lý & đóng gói:** Apache Maven
- **Cơ sở dữ liệu:** MySQL (MySQL Connector/J 8.4.0)
- **Bảo mật:** Argon2-jvm 2.11
- **Mã vạch & QR Code:** ZXing 3.5.3 (Zebra Crossing)
- **Định dạng ảnh:** webp-imageio 0.1.6

---

## 📂 CẤU TRÚC THƯ MỤC

```text
QLRapChieuPhim/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/mycompany/qlrapchieuphim/  # Lớp khởi chạy ứng dụng (Main)
│   │   │   ├── config/                        # Cấu hình kết nối MySQL (DBConnection)
│   │   │   ├── controller/                    # Điều khiển nghiệp vụ & logic ứng dụng
│   │   │   ├── dao/                           # Truy xuất dữ liệu (Data Access Object)
│   │   │   ├── model/                         # Các đối tượng thực thể dữ liệu (POJO)
│   │   │   ├── util/                          # Tiện ích bổ trợ (VietQR,...)
│   │   │   └── view/                          # Giao diện người dùng Swing (Frames & Panels)
│   │   └── resources/
│   │       └── images/posters/                # Tài nguyên hình ảnh, poster phim
│   └── test/                                  # Kiểm thử ứng dụng
├── pom.xml                                    # File cấu hình thư viện Maven
└── README.md                                  # Tài liệu hướng dẫn dự án
```

---

## ⚙️ CÀI ĐẶT & KHỞI CHẠY

### 1. Yêu Cầu Môi Trường
- **Java Development Kit (JDK):** JDK 17 hoặc mới hơn.
- **Apache Maven** (hoặc tích hợp sẵn trong IDE).
- **MySQL / XAMPP Server** (cổng mặc định `3306`).
- **IDE khuyên dùng:** NetBeans IDE, IntelliJ IDEA hoặc Eclipse.

### 2. Thiết Lập Cơ Sở Dữ Liệu
1. Mở **XAMPP / MySQL Service** và khởi động dịch vụ MySQL.
2. Mở phpMyAdmin hoặc công cụ MySQL Client (Navicat, DBeaver, MySQL Workbench).
3. Tạo database mới với tên:
   ```sql
   CREATE DATABASE quan_ly_rap_chieu_phim CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
4. Kiểm tra hoặc cấu hình lại thông số đăng nhập tại `src/main/java/config/DBConnection.java`:
   ```java
   private static final String URL = "jdbc:mysql://localhost:3306/quan_ly_rap_chieu_phim?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh";
   private static final String USER = "root";       // Thay đổi nếu tài khoản MySQL khác
   private static final String PASSWORD = "";       // Mật khẩu MySQL của bạn
   ```

### 3. Chạy Ứng Dụng
- **Trong NetBeans:**
  - Chuột phải vào project `QLRapChieuPhim` -> chọn **Clean and Build**.
  - Chuột phải vào file `src/main/java/view/DangNhap.java` (hoặc `QLRapChieuPhim.java`) -> chọn **Run File** (hoặc nhấn `Shift + F6`).

- **Bằng Dòng Lệnh Maven:**
  ```bash
  mvn clean compile
  mvn exec:java -Dexec.mainClass="com.mycompany.qlrapchieuphim.QLRapChieuPhim"
  ```
