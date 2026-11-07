package controller;

import config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * So lieu cho man hinh Tong quan cua Quan tri vien.
 * Moi so lieu truy van rieng; so nao loi thi tra ve -1 (man hinh se hien dau "—" thay vi sap ca trang).
 */
public class ThongKeQuanLyController {

    public static class ThongKe {
        public long soNhanVien = -1;
        public long soPhimDangChieu = -1;
        public long soPhong = -1;
        public long soSuat = -1;      // so suat chieu trong ngay duoc chon
        public long doanhThu = -1;    // doanh thu trong ngay duoc chon
    }

    public ThongKe lay(LocalDate ngay) throws SQLException {
        ThongKe tk = new ThongKe();
        try (Connection c = DBConnection.getConnection()) {
            // Nhan vien = tai khoan khong phai quan ly (neu cot vai_tro khac ten thi dem tat ca tai khoan)
            tk.soNhanVien = truyVan(c, "SELECT COUNT(*) FROM tai_khoan WHERE vai_tro <> 'QUAN_LY'");
            if (tk.soNhanVien < 0) tk.soNhanVien = truyVan(c, "SELECT COUNT(*) FROM tai_khoan");

            // Phim dang chieu: uu tien trang_thai = 'Dang chieu'; neu khong co thi dem phim chua ngung chieu
            tk.soPhimDangChieu = truyVan(c, "SELECT COUNT(*) FROM phim WHERE trang_thai = 'Dang chieu'");
            if (tk.soPhimDangChieu <= 0) {
                tk.soPhimDangChieu = truyVan(c, "SELECT COUNT(*) FROM phim WHERE trang_thai <> 'Ngung chieu'");
            }

            tk.soPhong = truyVan(c, "SELECT COUNT(*) FROM phong_chieu");
            tk.soSuat = truyVan(c, "SELECT COUNT(*) FROM suat_chieu WHERE ngay_chieu = ?",
                    java.sql.Date.valueOf(ngay));
            tk.doanhThu = truyVan(c,
                    "SELECT COALESCE(SUM(tong_tien), 0) FROM hoa_don "
                    + "WHERE DATE(ngay_lap) = ? AND trang_thai = 'Da thanh toan'",
                    java.sql.Date.valueOf(ngay));
        }
        return tk;
    }

    private long truyVan(Connection c, String sql, Object... thamSo) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < thamSo.length; i++) ps.setObject(i + 1, thamSo[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("[THONG KE] Loi truy van: " + sql + " -> " + e.getMessage());
            return -1;
        }
    }
}