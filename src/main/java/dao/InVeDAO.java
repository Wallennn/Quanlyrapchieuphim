package dao;

import config.DBConnection;
import model.ComboIn;
import model.HoaDonBapNuoc;
import model.HoaDonTomTat;
import model.VeIn;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class InVeDAO {

    // Tim hoa don de in ve.
    // tuKhoa: go ma hoa don hoac ten phim (de trong = lay het)
    // chiHomNay: true = chi lay hoa don lap trong ngay hom nay
    public List<HoaDonTomTat> timHoaDon(String tuKhoa, boolean chiHomNay) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT hd.ma_hd, p.ten_phim, sc.ngay_chieu, sc.gio_bat_dau, pc.ten_phong, "
                + "(SELECT COUNT(*) FROM chi_tiet_hoa_don_ghe ct WHERE ct.ma_hd = hd.ma_hd) AS so_ve, "
                + "hd.tong_tien, hd.trang_thai, hd.ngay_lap, "
                + "(SELECT COALESCE(SUM(cb.so_luong), 0) FROM chi_tiet_hoa_don_combo cb WHERE cb.ma_hd = hd.ma_hd) AS so_combo "
                + "FROM hoa_don hd "
                + "JOIN suat_chieu sc ON hd.ma_suat = sc.ma_suat "
                + "JOIN phim p ON sc.ma_phim = p.ma_phim "
                + "JOIN phong_chieu pc ON sc.ma_phong = pc.ma_phong "
                + "WHERE EXISTS (SELECT 1 FROM chi_tiet_hoa_don_ghe x WHERE x.ma_hd = hd.ma_hd) ");   // chi hoa don co ve

        boolean coTuKhoa = tuKhoa != null && !tuKhoa.isBlank();
        if (chiHomNay) {
            sql.append("AND DATE(hd.ngay_lap) = CURDATE() ");
        }
        if (coTuKhoa) {
            sql.append("AND (hd.ma_hd LIKE ? OR p.ten_phim LIKE ?) ");
        }
        sql.append("ORDER BY hd.ngay_lap DESC LIMIT 200");

        List<HoaDonTomTat> ketQua = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (coTuKhoa) {
                String mau = "%" + tuKhoa.trim() + "%";
                ps.setString(1, mau);
                ps.setString(2, mau);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ngayLap = rs.getTimestamp("ngay_lap");
                    ketQua.add(new HoaDonTomTat(
                            rs.getString("ma_hd"),
                            rs.getString("ten_phim"),
                            rs.getDate("ngay_chieu").toLocalDate(),
                            rs.getTime("gio_bat_dau").toLocalTime(),
                            rs.getString("ten_phong"),
                            rs.getInt("so_ve"),
                            rs.getDouble("tong_tien"),
                            rs.getString("trang_thai"),
                            ngayLap != null ? ngayLap.toLocalDateTime() : null,
                            rs.getInt("so_combo")));
                }
            }
        }
        return ketQua;
    }

    // Lay cac dong combo (bap nuoc) cua 1 hoa don - dung cho ca hoa don ve kem combo va hoa don bap nuoc rieng
    public List<ComboIn> layComboTheoHoaDon(String maHd) throws SQLException {
        String sql = "SELECT c.ten_combo, ct.so_luong, ct.don_gia "
                + "FROM chi_tiet_hoa_don_combo ct JOIN combo c ON ct.ma_combo = c.ma_combo "
                + "WHERE ct.ma_hd = ? ORDER BY ct.ma_combo";
        List<ComboIn> ketQua = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maHd);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ketQua.add(new ComboIn(rs.getString("ten_combo"), rs.getInt("so_luong"), rs.getDouble("don_gia")));
                }
            }
        }
        return ketQua;
    }

    // Tim hoa don BAP NUOC RIENG (co combo, khong co ve nao). tuKhoa: ma hoa don / ten khach / so dien thoai
    public List<HoaDonBapNuoc> timHoaDonBapNuocRieng(String tuKhoa, boolean chiHomNay) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT hd.ma_hd, hd.ngay_lap, hd.tong_tien, hd.trang_thai, hd.diem_tich_luy, "
                + "(SELECT COALESCE(SUM(cb.so_luong), 0) FROM chi_tiet_hoa_don_combo cb WHERE cb.ma_hd = hd.ma_hd) AS so_combo, "
                + "kh.ho_ten AS ten_khach, kh.so_dien_thoai, kh.hang, kh.diem AS diem_hien_co, tk.ho_ten AS ten_nv "
                + "FROM hoa_don hd "
                + "LEFT JOIN khach_hang kh ON hd.ma_khach_hang = kh.id "
                + "LEFT JOIN tai_khoan tk ON hd.id_tai_khoan = tk.id "
                + "WHERE EXISTS (SELECT 1 FROM chi_tiet_hoa_don_combo y WHERE y.ma_hd = hd.ma_hd) "
                + "AND NOT EXISTS (SELECT 1 FROM chi_tiet_hoa_don_ghe x WHERE x.ma_hd = hd.ma_hd) ");

        boolean coTuKhoa = tuKhoa != null && !tuKhoa.isBlank();
        if (chiHomNay) {
            sql.append("AND DATE(hd.ngay_lap) = CURDATE() ");
        }
        if (coTuKhoa) {
            sql.append("AND (hd.ma_hd LIKE ? OR kh.ho_ten LIKE ? OR kh.so_dien_thoai LIKE ?) ");
        }
        sql.append("ORDER BY hd.ngay_lap DESC LIMIT 200");

        List<HoaDonBapNuoc> ketQua = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (coTuKhoa) {
                String mau = "%" + tuKhoa.trim() + "%";
                ps.setString(1, mau);
                ps.setString(2, mau);
                ps.setString(3, mau);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ngayLap = rs.getTimestamp("ngay_lap");
                    ketQua.add(new HoaDonBapNuoc(
                            rs.getString("ma_hd"),
                            ngayLap != null ? ngayLap.toLocalDateTime() : null,
                            rs.getInt("so_combo"),
                            rs.getDouble("tong_tien"),
                            rs.getString("trang_thai"),
                            rs.getString("ten_khach"),
                            rs.getString("so_dien_thoai"),
                            rs.getString("hang"),
                            rs.getInt("diem_hien_co"),
                            rs.getInt("diem_tich_luy"),
                            rs.getString("ten_nv")));
                }
            }
        }
        return ketQua;
    }

    // Lay tat ca ve (moi ghe 1 ve) cua 1 hoa don
    public List<VeIn> layVeTheoHoaDon(String maHd) throws SQLException {
        String sql = "SELECT hd.ma_hd, p.ten_phim, pc.ten_phong, sc.ngay_chieu, sc.gio_bat_dau, sc.gio_ket_thuc, "
                + "g.hang_ghe, g.so_ghe, g.loai_ghe, ct.gia_ve, hd.ngay_lap, tk.ho_ten, hd.trang_thai "
                + "FROM chi_tiet_hoa_don_ghe ct "
                + "JOIN hoa_don hd ON ct.ma_hd = hd.ma_hd "
                + "JOIN suat_chieu sc ON ct.ma_suat = sc.ma_suat "
                + "JOIN phim p ON sc.ma_phim = p.ma_phim "
                + "JOIN phong_chieu pc ON sc.ma_phong = pc.ma_phong "
                + "JOIN ghe g ON ct.ma_ghe = g.ma_ghe "
                + "LEFT JOIN tai_khoan tk ON hd.id_tai_khoan = tk.id "
                + "WHERE ct.ma_hd = ? "
                + "ORDER BY g.hang_ghe, g.so_ghe";

        List<VeIn> ketQua = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, maHd);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ngayLap = rs.getTimestamp("ngay_lap");
                    ketQua.add(new VeIn(
                            rs.getString("ma_hd"),
                            rs.getString("ten_phim"),
                            rs.getString("ten_phong"),
                            rs.getDate("ngay_chieu").toLocalDate(),
                            rs.getTime("gio_bat_dau").toLocalTime(),
                            rs.getTime("gio_ket_thuc").toLocalTime(),
                            rs.getString("hang_ghe"),
                            rs.getInt("so_ghe"),
                            rs.getString("loai_ghe"),
                            rs.getDouble("gia_ve"),
                            ngayLap != null ? ngayLap.toLocalDateTime() : null,
                            rs.getString("ho_ten"),
                            rs.getString("trang_thai")));
                }
            }
        }
        return ketQua;
    }
}