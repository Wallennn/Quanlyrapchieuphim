package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lấy số liệu cho màn hình Trang chủ nhân viên.
 * Chỉ mở 1 kết nối duy nhất cho toàn bộ số liệu để tải nhanh.
 */
public class Thongketrangchucontroller {

    // >>> SỬA 3 DÒNG NÀY CHO ĐÚNG DATABASE CỦA BẠN <<<
    // (useSSL=false giúp kết nối MySQL local nhanh hơn)
    private static final String URL =
            "jdbc:mysql://localhost:3306/quan_ly_rap_chieu_phim"
            + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "";

    private Connection layKetNoi() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // Một dòng trong bảng "Suất chiếu hôm nay"
    public static class DongSuatChieu {
        public String maSuat;
        public String tenPhim;
        public String poster;
        public String tenPhong;
        public LocalTime gioBatDau;
        public LocalTime gioKetThuc;
    }

    // Toàn bộ dữ liệu của trang chủ
    public static class DuLieuTrangChu {
        public int soSuat;
        public int soVe;
        public long doanhThu;
        public int soPhong;
        public List<DongSuatChieu> danhSach = new ArrayList<>();
    }

    private long layMotSo(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    public DuLieuTrangChu layDuLieuTrangChu() throws SQLException {
        DuLieuTrangChu kq = new DuLieuTrangChu();
        try (Connection c = layKetNoi()) {
            kq.soSuat = (int) layMotSo(c,
                    "SELECT COUNT(*) FROM suat_chieu WHERE ngay_chieu = CURDATE()");

            kq.soVe = (int) layMotSo(c,
                    "SELECT COUNT(*) FROM chi_tiet_hoa_don_ghe g "
                    + "JOIN hoa_don h ON g.ma_hd = h.ma_hd "
                    + "WHERE DATE(h.ngay_lap) = CURDATE() AND h.trang_thai = 'Da thanh toan'");

            kq.doanhThu = layMotSo(c,
                    "SELECT COALESCE(SUM(tong_tien), 0) FROM hoa_don "
                    + "WHERE DATE(ngay_lap) = CURDATE() AND trang_thai = 'Da thanh toan'");

            kq.soPhong = (int) layMotSo(c,
                    "SELECT COUNT(DISTINCT ma_phong) FROM suat_chieu "
                    + "WHERE ngay_chieu = CURDATE() AND CURTIME() BETWEEN gio_bat_dau AND gio_ket_thuc");

            String sql = "SELECT s.ma_suat, p.ten_phim, p.poster, pc.ten_phong, s.gio_bat_dau, s.gio_ket_thuc "
                    + "FROM suat_chieu s "
                    + "JOIN phim p ON s.ma_phim = p.ma_phim "
                    + "JOIN phong_chieu pc ON s.ma_phong = pc.ma_phong "
                    + "WHERE s.ngay_chieu = CURDATE() "
                    + "ORDER BY s.gio_bat_dau";
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DongSuatChieu d = new DongSuatChieu();
                    d.maSuat = rs.getString("ma_suat");
                    d.tenPhim = rs.getString("ten_phim");
                    d.poster = rs.getString("poster");
                    d.tenPhong = rs.getString("ten_phong");
                    d.gioBatDau = rs.getTime("gio_bat_dau").toLocalTime();
                    d.gioKetThuc = rs.getTime("gio_ket_thuc").toLocalTime();
                    kq.danhSach.add(d);
                }
            }
        }
        return kq;
    }
}