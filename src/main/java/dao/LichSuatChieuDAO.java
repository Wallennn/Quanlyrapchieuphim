package dao;

import config.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Lap lich chieu (bang suat_chieu): danh sach, them, sua, xoa va KIEM TRA TRUNG PHONG.
 * Hai suat cung phong, cung ngay phai cach nhau it nhat NGHI_GIUA_SUAT_PHUT phut (don phong + quang cao).
 * Kiem tra trung gio duoc lam lai o server khi luu, nen 2 quay xep lich cung luc cung khong trung duoc.
 */
public class LichSuatChieuDAO {

    public static final int NGHI_GIUA_SUAT_PHUT = 15;
    private static final DateTimeFormatter HH = DateTimeFormatter.ofPattern("HH:mm");

    public static class Phim {
        public String ma, ten, trangThai;
        public int thoiLuong;

        @Override
        public String toString() {
            return ten + " (" + thoiLuong + " phút)";
        }
    }

    public static class Phong {
        public String ma, ten;
        public int soHang, soCot;

        @Override
        public String toString() {
            return ten;
        }
    }

    public static class Suat {
        public String maSuat, maPhim, tenPhim, maPhong, tenPhong;
        public int thoiLuong;
        public LocalDate ngay;
        public LocalTime gioBd, gioKt;
        public long giaThuong, giaVip, giaDoi;
        public int soVeDaBan, tongGhe;
    }

    /** Phong da co suat khac chieu trung khoang gio nay. */
    public static class TrungLichException extends Exception {
        public final Suat suatTrung;

        public TrungLichException(Suat suatTrung) {
            super("Phòng đã có suất " + suatTrung.gioBd.format(HH) + " – " + suatTrung.gioKt.format(HH)
                    + " (" + suatTrung.tenPhim + "). Hai suất cùng phòng phải cách nhau ít nhất "
                    + NGHI_GIUA_SUAT_PHUT + " phút.");
            this.suatTrung = suatTrung;
        }
    }

    /** Suat da co ve ban: khong doi duoc phim / phong / gio, khong xoa duoc. */
    public static class DaBanVeException extends Exception {
        public DaBanVeException(String thongBao) {
            super(thongBao);
        }
    }

    // ===================== QUY TAC TRUNG GIO =====================
    public static int phut(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    /** [bd, kt) (tinh bang phut trong ngay) co dung do voi suat khac khong, ke ca thoi gian nghi giua 2 suat. */
    public static boolean trungGio(int bd, int kt, Suat khac) {
        int bd2 = phut(khac.gioBd);
        int kt2 = phut(khac.gioKt);
        if (kt2 <= bd2) kt2 += 24 * 60;   // suat cu qua nua dem
        return bd < kt2 + NGHI_GIUA_SUAT_PHUT && bd2 < kt + NGHI_GIUA_SUAT_PHUT;
    }

    // ===================== DOC DU LIEU =====================
    public List<Phim> layPhim() throws SQLException {
        List<Phim> ds = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT ma_phim, ten_phim, thoi_luong, trang_thai FROM phim "
                     + "ORDER BY FIELD(trang_thai, 'Dang chieu', 'Sap chieu', 'Ngung chieu'), ten_phim");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Phim p = new Phim();
                p.ma = rs.getString(1);
                p.ten = rs.getString(2);
                p.thoiLuong = rs.getInt(3);
                p.trangThai = rs.getString(4);
                ds.add(p);
            }
        }
        return ds;
    }

    public List<Phong> layPhong() throws SQLException {
        List<Phong> ds = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT ma_phong, ten_phong, so_hang, so_cot FROM phong_chieu ORDER BY ten_phong");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Phong p = new Phong();
                p.ma = rs.getString(1);
                p.ten = rs.getString(2);
                p.soHang = rs.getInt(3);
                p.soCot = rs.getInt(4);
                ds.add(p);
            }
        }
        return ds;
    }

    /** Danh sach suat chieu; tuNgay = null thi lay tat ca. */
    public List<Suat> layDanhSach(LocalDate tuNgay) throws SQLException {
        String sql = "SELECT s.ma_suat, s.ma_phim, p.ten_phim, p.thoi_luong, s.ma_phong, pc.ten_phong, "
                + "s.ngay_chieu, s.gio_bat_dau, s.gio_ket_thuc, s.gia_ve_thuong, s.gia_ve_vip, s.gia_ve_doi, "
                + "pc.so_hang * pc.so_cot AS tong_ghe, "
                + "(SELECT COUNT(*) FROM chi_tiet_hoa_don_ghe ct WHERE ct.ma_suat = s.ma_suat) AS da_ban "
                + "FROM suat_chieu s JOIN phim p ON p.ma_phim = s.ma_phim JOIN phong_chieu pc ON pc.ma_phong = s.ma_phong "
                + (tuNgay != null ? "WHERE s.ngay_chieu >= ? " : "")
                + "ORDER BY s.ngay_chieu, s.gio_bat_dau, pc.ten_phong";
        List<Suat> ds = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (tuNgay != null) ps.setDate(1, Date.valueOf(tuNgay));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Suat s = new Suat();
                    s.maSuat = rs.getString("ma_suat");
                    s.maPhim = rs.getString("ma_phim");
                    s.tenPhim = rs.getString("ten_phim");
                    s.thoiLuong = rs.getInt("thoi_luong");
                    s.maPhong = rs.getString("ma_phong");
                    s.tenPhong = rs.getString("ten_phong");
                    s.ngay = rs.getDate("ngay_chieu").toLocalDate();
                    s.gioBd = rs.getTime("gio_bat_dau").toLocalTime();
                    s.gioKt = rs.getTime("gio_ket_thuc").toLocalTime();
                    s.giaThuong = rs.getLong("gia_ve_thuong");
                    s.giaVip = rs.getLong("gia_ve_vip");
                    s.giaDoi = rs.getLong("gia_ve_doi");
                    s.tongGhe = rs.getInt("tong_ghe");
                    s.soVeDaBan = rs.getInt("da_ban");
                    ds.add(s);
                }
            }
        }
        return ds;
    }

    /** Cac suat da co trong 1 phong vao 1 ngay (de to xam khung gio). boQuaMa = suat dang sua (khong tinh). */
    public List<Suat> laySuatTrongNgay(String maPhong, LocalDate ngay, String boQuaMa) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return laySuatTrongNgay(c, maPhong, ngay, boQuaMa, false);
        }
    }

    private List<Suat> laySuatTrongNgay(Connection c, String maPhong, LocalDate ngay, String boQuaMa, boolean khoa)
            throws SQLException {
        String sql = "SELECT s.ma_suat, s.gio_bat_dau, s.gio_ket_thuc, p.ten_phim "
                + "FROM suat_chieu s JOIN phim p ON p.ma_phim = s.ma_phim "
                + "WHERE s.ma_phong = ? AND s.ngay_chieu = ? AND s.ma_suat <> ? ORDER BY s.gio_bat_dau"
                + (khoa ? " FOR UPDATE" : "");
        List<Suat> ds = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ps.setDate(2, Date.valueOf(ngay));
            ps.setString(3, boQuaMa == null ? "" : boQuaMa);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Suat s = new Suat();
                    s.maSuat = rs.getString(1);
                    s.gioBd = rs.getTime(2).toLocalTime();
                    s.gioKt = rs.getTime(3).toLocalTime();
                    s.tenPhim = rs.getString(4);
                    ds.add(s);
                }
            }
        }
        return ds;
    }

    private int soVeDaBan(Connection c, String maSuat) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM chi_tiet_hoa_don_ghe WHERE ma_suat = ?")) {
            ps.setString(1, maSuat);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int thoiLuongPhim(Connection c, String maPhim) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT thoi_luong FROM phim WHERE ma_phim = ?")) {
            ps.setString(1, maPhim);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Phim không tồn tại: " + maPhim);
                return rs.getInt(1);
            }
        }
    }

    private void kiemTraHopLe(Suat s, int thoiLuong) throws SQLException {
        if (s.gioBd == null || s.ngay == null) throw new SQLException("Thiếu ngày hoặc giờ chiếu");
        if (phut(s.gioBd) + thoiLuong > 24 * 60 - 1) throw new SQLException("Suất chiếu phải kết thúc trước 24:00");
        if (s.giaThuong <= 0 || s.giaVip <= 0 || s.giaDoi <= 0) throw new SQLException("Giá vé phải lớn hơn 0");
    }

    private void kiemTraTrung(List<Suat> daCo, int bd, int kt) throws TrungLichException {
        for (Suat e : daCo) {
            if (trungGio(bd, kt, e)) throw new TrungLichException(e);
        }
    }

    // ===================== THEM / SUA / XOA =====================
    /** Them suat moi; ma suat tu sinh (SUAT###). Tra ve ma suat. */
    public String them(Suat s) throws SQLException, TrungLichException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int thoiLuong = thoiLuongPhim(c, s.maPhim);
                kiemTraHopLe(s, thoiLuong);
                int bd = phut(s.gioBd);
                // khoa cac suat cung phong cung ngay de 2 quay khong xep trung cung luc
                kiemTraTrung(laySuatTrongNgay(c, s.maPhong, s.ngay, null, true), bd, bd + thoiLuong);

                String ma;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COALESCE(MAX(CAST(SUBSTRING(ma_suat, 5) AS UNSIGNED)), 0) + 1 FROM suat_chieu "
                        + "WHERE ma_suat REGEXP '^SUAT[0-9]+$'");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    ma = String.format("SUAT%03d", rs.getLong(1));
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO suat_chieu (ma_suat, ma_phim, ma_phong, ngay_chieu, gio_bat_dau, gio_ket_thuc, "
                        + "gia_ve_thuong, gia_ve_vip, gia_ve_doi) VALUES (?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, ma);
                    ps.setString(2, s.maPhim);
                    ps.setString(3, s.maPhong);
                    ps.setDate(4, Date.valueOf(s.ngay));
                    ps.setTime(5, Time.valueOf(s.gioBd));
                    ps.setTime(6, Time.valueOf(s.gioBd.plusMinutes(thoiLuong)));
                    ps.setLong(7, s.giaThuong);
                    ps.setLong(8, s.giaVip);
                    ps.setLong(9, s.giaDoi);
                    ps.executeUpdate();
                }
                c.commit();
                return ma;
            } catch (SQLException | TrungLichException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Sua suat. Suat da ban ve chi doi duoc gia ve (phim, phong, ngay, gio phai giu nguyen).
     */
    public void capNhat(Suat s) throws SQLException, TrungLichException, DaBanVeException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // doc suat hien tai
                String maPhimCu, maPhongCu;
                LocalDate ngayCu;
                LocalTime gioCu;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT ma_phim, ma_phong, ngay_chieu, gio_bat_dau FROM suat_chieu WHERE ma_suat = ? FOR UPDATE")) {
                    ps.setString(1, s.maSuat);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Suất chiếu không còn tồn tại");
                        maPhimCu = rs.getString(1);
                        maPhongCu = rs.getString(2);
                        ngayCu = rs.getDate(3).toLocalDate();
                        gioCu = rs.getTime(4).toLocalTime();
                    }
                }
                boolean doiLich = !maPhimCu.equals(s.maPhim) || !maPhongCu.equals(s.maPhong)
                        || !ngayCu.equals(s.ngay) || !gioCu.equals(s.gioBd);
                int daBan = soVeDaBan(c, s.maSuat);
                if (s.giaThuong <= 0 || s.giaVip <= 0 || s.giaDoi <= 0) throw new SQLException("Giá vé phải lớn hơn 0");

                if (daBan > 0 && doiLich) {
                    throw new DaBanVeException("Suất đã bán " + daBan + " vé nên không đổi được phim, phòng, ngày và giờ chiếu. "
                            + "Chỉ sửa được giá vé.");
                }
                if (doiLich) {
                    int thoiLuong = thoiLuongPhim(c, s.maPhim);
                    kiemTraHopLe(s, thoiLuong);
                    int bd = phut(s.gioBd);
                    kiemTraTrung(laySuatTrongNgay(c, s.maPhong, s.ngay, s.maSuat, true), bd, bd + thoiLuong);
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE suat_chieu SET ma_phim=?, ma_phong=?, ngay_chieu=?, gio_bat_dau=?, gio_ket_thuc=?, "
                            + "gia_ve_thuong=?, gia_ve_vip=?, gia_ve_doi=? WHERE ma_suat=?")) {
                        ps.setString(1, s.maPhim);
                        ps.setString(2, s.maPhong);
                        ps.setDate(3, Date.valueOf(s.ngay));
                        ps.setTime(4, Time.valueOf(s.gioBd));
                        ps.setTime(5, Time.valueOf(s.gioBd.plusMinutes(thoiLuong)));
                        ps.setLong(6, s.giaThuong);
                        ps.setLong(7, s.giaVip);
                        ps.setLong(8, s.giaDoi);
                        ps.setString(9, s.maSuat);
                        ps.executeUpdate();
                    }
                } else {   // chi doi gia
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE suat_chieu SET gia_ve_thuong=?, gia_ve_vip=?, gia_ve_doi=? WHERE ma_suat=?")) {
                        ps.setLong(1, s.giaThuong);
                        ps.setLong(2, s.giaVip);
                        ps.setLong(3, s.giaDoi);
                        ps.setString(4, s.maSuat);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException | TrungLichException | DaBanVeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void xoa(String maSuat) throws SQLException, DaBanVeException {
        try (Connection c = DBConnection.getConnection()) {
            int daBan = soVeDaBan(c, maSuat);
            if (daBan > 0) throw new DaBanVeException("Suất đã có " + daBan + " vé nên không thể xóa.");
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM suat_chieu WHERE ma_suat = ?")) {
                ps.setString(1, maSuat);
                ps.executeUpdate();
            } catch (SQLException e) {
                if (e.getErrorCode() == 1451) throw new DaBanVeException("Suất đã có hóa đơn nên không thể xóa.");
                throw e;
            }
        }
    }
}