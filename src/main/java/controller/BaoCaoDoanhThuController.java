package controller;

import config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bao cao theo khoang ngay: doanh thu (tong quan, theo ngay, theo phim), hoan tien do su co
 * va ty le lap day ghe theo suat chieu. Doanh thu chi tinh hoa don "Da thanh toan".
 */
public class BaoCaoDoanhThuController {

    public static class TheoNgay {
        public LocalDate ngay;
        public long soHd, soVe, tien;
    }

    public static class TheoPhim {
        public String tenPhim;
        public long soVe, tien;
    }

    /** Mot suat chieu trong khoang ngay: so ghe da ban (hoa don da thanh toan) tren tong so ghe cua phong. */
    public static class TheoSuat {
        public String maSuat;
        public LocalDate ngay;
        public LocalTime gioBatDau;
        public String tenPhim, tenPhong;
        public long tongGhe, daBan;
        public long soCombo, tienCombo;                 // combo mua kem ve (hoa don co ghe) cua suat nay
        public String chiTietCombo = "";                // vd: "Combo 1 x2, Combo 3 x1"

        public double tyLe() {
            return tongGhe <= 0 ? 0 : (double) daBan / tongGhe;
        }
    }

    /** Bap nuoc BAN LE: hoa don chi co combo, khong co ghe. Gom theo tung loai combo. */
    public static class TheoComboLe {
        public String maCombo, tenCombo;
        public long soLuong, soHd, tien;
    }

    /** Ve ban theo khung gio chieu (gio bat dau suat). */
    public static class TheoGio {
        public int gio;
        public long soVe, tien;
    }

    public static class TheoNhanVien {
        public String hoTen;
        public long soHd, soVe, tien;
    }

    public static class TheoThanhToan {
        public String phuongThuc;   // Tien mat | Chuyen khoan | The
        public long soHd, tien;
    }

    public static class TheoKhuyenMai {
        public String ma, ten;
        public long luot, tien;     // so hoa don ap dung va doanh thu cua cac hoa don do
    }

    public static class BaoCao {
        public long tongTien, soHd, soVe, tienCombo;
        public long tienHoan, soHdHoan, soHdDoiSuat;     // boi thuong do su co trong ky (theo ngay xu ly)
        public long tongGiamGia, soHdCoGiam;              // giam gia (khuyen mai + hang thanh vien + doi diem)
        public long gheDaBan, tongGhe;                    // cong don tat ca suat trong ky
        public final List<TheoNgay> theoNgay = new ArrayList<>();
        public final List<TheoPhim> theoPhim = new ArrayList<>();
        public final List<TheoSuat> theoSuat = new ArrayList<>();
        public final List<TheoComboLe> comboLe = new ArrayList<>();      // bap nuoc ban le (khong kem ve)
        public long soLuongComboLe, soHdComboLe, tienComboLe;
        public final List<TheoGio> theoGio = new ArrayList<>();
        public final List<TheoNhanVien> theoNhanVien = new ArrayList<>();
        public final List<TheoThanhToan> theoThanhToan = new ArrayList<>();
        public final List<TheoKhuyenMai> theoKhuyenMai = new ArrayList<>();

        /** Ty le lap day chung: tong ghe da ban / tong ghe cua tat ca suat trong ky (0..1). */
        public double tyLeLapDay() {
            return tongGhe <= 0 ? 0 : (double) gheDaBan / tongGhe;
        }
    }

    public BaoCao lay(LocalDate tu, LocalDate den) throws SQLException {
        BaoCao bc = new BaoCao();
        java.sql.Date t = java.sql.Date.valueOf(tu);
        java.sql.Date d = java.sql.Date.valueOf(den);

        try (Connection c = DBConnection.getConnection()) {
            // Theo ngay: so hoa don + doanh thu
            Map<LocalDate, TheoNgay> theoNgay = new LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT DATE(ngay_lap) AS ngay, COUNT(*) AS so_hd, COALESCE(SUM(tong_tien), 0) AS tien "
                    + "FROM hoa_don WHERE trang_thai = 'Da thanh toan' AND DATE(ngay_lap) BETWEEN ? AND ? "
                    + "GROUP BY DATE(ngay_lap) ORDER BY ngay DESC")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TheoNgay n = new TheoNgay();
                        n.ngay = rs.getDate("ngay").toLocalDate();
                        n.soHd = rs.getLong("so_hd");
                        n.tien = rs.getLong("tien");
                        theoNgay.put(n.ngay, n);
                    }
                }
            }
            // Theo ngay: so ve
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT DATE(h.ngay_lap) AS ngay, COUNT(*) AS so_ve "
                    + "FROM chi_tiet_hoa_don_ghe ct JOIN hoa_don h ON ct.ma_hd = h.ma_hd "
                    + "WHERE h.trang_thai = 'Da thanh toan' AND DATE(h.ngay_lap) BETWEEN ? AND ? "
                    + "GROUP BY DATE(h.ngay_lap)")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TheoNgay n = theoNgay.get(rs.getDate("ngay").toLocalDate());
                        if (n != null) n.soVe = rs.getLong("so_ve");
                    }
                }
            }
            for (TheoNgay n : theoNgay.values()) {
                bc.theoNgay.add(n);
                bc.tongTien += n.tien;
                bc.soHd += n.soHd;
                bc.soVe += n.soVe;
            }

            // Theo phim
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT p.ten_phim, COUNT(*) AS so_ve, "
                    + "COALESCE(ROUND(SUM(ct.gia_ve * h.tong_tien / NULLIF(h.tong_tien + h.giam_gia, 0))), 0) AS tien "
                    + "FROM chi_tiet_hoa_don_ghe ct "
                    + "JOIN hoa_don h ON ct.ma_hd = h.ma_hd "
                    + "JOIN suat_chieu s ON ct.ma_suat = s.ma_suat "
                    + "JOIN phim p ON s.ma_phim = p.ma_phim "
                    + "WHERE h.trang_thai = 'Da thanh toan' AND DATE(h.ngay_lap) BETWEEN ? AND ? "
                    + "GROUP BY p.ma_phim, p.ten_phim ORDER BY tien DESC")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TheoPhim p = new TheoPhim();
                        p.tenPhim = rs.getString("ten_phim");
                        p.soVe = rs.getLong("so_ve");
                        p.tien = rs.getLong("tien");
                        bc.theoPhim.add(p);
                    }
                }
            }

            // Doanh thu combo
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(ROUND(SUM(cb.so_luong * cb.don_gia * h.tong_tien / NULLIF(h.tong_tien + h.giam_gia, 0))), 0) "
                    + "FROM chi_tiet_hoa_don_combo cb JOIN hoa_don h ON cb.ma_hd = h.ma_hd "
                    + "WHERE h.trang_thai = 'Da thanh toan' AND DATE(h.ngay_lap) BETWEEN ? AND ?")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) bc.tienCombo = rs.getLong(1);
                }
            }

            // Boi thuong do su co trong ky (tinh theo ngay xu ly su co): tien da hoan + so hoa don doi suat khac
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT "
                    + "COALESCE(SUM(CASE WHEN ct.hinh_thuc_xu_ly = 'Hoan tien' THEN 1 ELSE 0 END), 0) AS so_hoan, "
                    + "COALESCE(SUM(CASE WHEN ct.hinh_thuc_xu_ly = 'Hoan tien' THEN ct.so_tien_hoan ELSE 0 END), 0) AS tien_hoan, "
                    + "COALESCE(SUM(CASE WHEN ct.hinh_thuc_xu_ly = 'Ve moi suat khac' THEN 1 ELSE 0 END), 0) AS so_doi "
                    + "FROM chi_tiet_boi_thuong ct JOIN su_co_boi_thuong sc ON ct.ma_su_co = sc.ma_su_co "
                    + "WHERE DATE(sc.ngay_xu_ly) BETWEEN ? AND ?")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        bc.soHdHoan = rs.getLong("so_hoan");
                        bc.tienHoan = rs.getLong("tien_hoan");
                        bc.soHdDoiSuat = rs.getLong("so_doi");
                    }
                }
            }

            // Ty le lap day theo tung suat chieu trong ky (theo ngay chieu). Chi dem ghe cua hoa don da thanh toan.
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT s.ma_suat, s.ngay_chieu, s.gio_bat_dau, p.ten_phim, pc.ten_phong, "
                    + "pc.so_hang * pc.so_cot AS tong_ghe, "
                    + "(SELECT COUNT(*) FROM chi_tiet_hoa_don_ghe ct JOIN hoa_don h ON ct.ma_hd = h.ma_hd "
                    + "  WHERE ct.ma_suat = s.ma_suat AND h.trang_thai = 'Da thanh toan') AS da_ban "
                    + "FROM suat_chieu s "
                    + "JOIN phim p ON s.ma_phim = p.ma_phim "
                    + "JOIN phong_chieu pc ON s.ma_phong = pc.ma_phong "
                    + "WHERE s.ngay_chieu BETWEEN ? AND ? "
                    + "ORDER BY s.ngay_chieu DESC, s.gio_bat_dau, pc.ten_phong")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TheoSuat s = new TheoSuat();
                        s.maSuat = rs.getString("ma_suat");
                        s.ngay = rs.getDate("ngay_chieu").toLocalDate();
                        s.gioBatDau = rs.getTime("gio_bat_dau").toLocalTime();
                        s.tenPhim = rs.getString("ten_phim");
                        s.tenPhong = rs.getString("ten_phong");
                        s.tongGhe = rs.getLong("tong_ghe");
                        s.daBan = rs.getLong("da_ban");
                        bc.theoSuat.add(s);
                        bc.tongGhe += s.tongGhe;
                        bc.gheDaBan += s.daBan;
                    }
                }
            }

            // Combo MUA KEM VE: hoa don co ghe. Tinh theo suat chieu cua hoa don (gia niem yet = so luong x don gia).
            Map<String, TheoSuat> theoMaSuat = new LinkedHashMap<>();
            for (TheoSuat s : bc.theoSuat) theoMaSuat.put(s.maSuat, s);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT h.ma_suat, cb.ma_combo, cx.ten_combo, SUM(cb.so_luong) AS sl, SUM(cb.so_luong * cb.don_gia) AS tien "
                    + "FROM chi_tiet_hoa_don_combo cb "
                    + "JOIN hoa_don h ON cb.ma_hd = h.ma_hd "
                    + "JOIN combo cx ON cb.ma_combo = cx.ma_combo "
                    + "JOIN suat_chieu s ON s.ma_suat = h.ma_suat "
                    + "WHERE h.trang_thai = 'Da thanh toan' AND s.ngay_chieu BETWEEN ? AND ? "
                    + "AND EXISTS (SELECT 1 FROM chi_tiet_hoa_don_ghe g WHERE g.ma_hd = h.ma_hd) "
                    + "GROUP BY h.ma_suat, cb.ma_combo, cx.ten_combo ORDER BY h.ma_suat, cb.ma_combo")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TheoSuat s = theoMaSuat.get(rs.getString("ma_suat"));
                        if (s == null) continue;
                        long sl = rs.getLong("sl");
                        s.soCombo += sl;
                        s.tienCombo += rs.getLong("tien");
                        s.chiTietCombo += (s.chiTietCombo.isEmpty() ? "" : ", ") + rs.getString("ten_combo") + " x" + sl;
                    }
                }
            }

            // Bap nuoc BAN LE: hoa don khong co ghe nao (tinh theo ngay lap hoa don)
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT cb.ma_combo, cx.ten_combo, SUM(cb.so_luong) AS sl, COUNT(DISTINCT h.ma_hd) AS so_hd, "
                    + "SUM(cb.so_luong * cb.don_gia) AS tien "
                    + "FROM chi_tiet_hoa_don_combo cb "
                    + "JOIN hoa_don h ON cb.ma_hd = h.ma_hd "
                    + "JOIN combo cx ON cb.ma_combo = cx.ma_combo "
                    + "WHERE h.trang_thai = 'Da thanh toan' AND DATE(h.ngay_lap) BETWEEN ? AND ? "
                    + "AND NOT EXISTS (SELECT 1 FROM chi_tiet_hoa_don_ghe g WHERE g.ma_hd = h.ma_hd) "
                    + "GROUP BY cb.ma_combo, cx.ten_combo ORDER BY sl DESC, cb.ma_combo")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TheoComboLe x = new TheoComboLe();
                        x.maCombo = rs.getString("ma_combo");
                        x.tenCombo = rs.getString("ten_combo");
                        x.soLuong = rs.getLong("sl");
                        x.soHd = rs.getLong("so_hd");
                        x.tien = rs.getLong("tien");
                        bc.comboLe.add(x);
                        bc.soLuongComboLe += x.soLuong;
                        bc.tienComboLe += x.tien;
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(DISTINCT h.ma_hd) FROM chi_tiet_hoa_don_combo cb JOIN hoa_don h ON cb.ma_hd = h.ma_hd "
                    + "WHERE h.trang_thai = 'Da thanh toan' AND DATE(h.ngay_lap) BETWEEN ? AND ? "
                    + "AND NOT EXISTS (SELECT 1 FROM chi_tiet_hoa_don_ghe g WHERE g.ma_hd = h.ma_hd)")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) bc.soHdComboLe = rs.getLong(1);
                }
            }

            // Tong giam gia + so hoa don co giam
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(SUM(giam_gia), 0), COALESCE(SUM(CASE WHEN giam_gia > 0 THEN 1 ELSE 0 END), 0) "
                    + "FROM hoa_don WHERE trang_thai = 'Da thanh toan' AND DATE(ngay_lap) BETWEEN ? AND ?")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        bc.tongGiamGia = rs.getLong(1);
                        bc.soHdCoGiam = rs.getLong(2);
                    }
                }
            }

            // Khung gio ban chay (theo gio bat dau suat chieu), tinh theo tien thuc thu
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT HOUR(s.gio_bat_dau) AS gio, COUNT(*) AS so_ve, "
                    + "COALESCE(ROUND(SUM(ct.gia_ve * h.tong_tien / NULLIF(h.tong_tien + h.giam_gia, 0))), 0) AS tien "
                    + "FROM chi_tiet_hoa_don_ghe ct "
                    + "JOIN hoa_don h ON ct.ma_hd = h.ma_hd "
                    + "JOIN suat_chieu s ON ct.ma_suat = s.ma_suat "
                    + "WHERE h.trang_thai = 'Da thanh toan' AND DATE(h.ngay_lap) BETWEEN ? AND ? "
                    + "GROUP BY HOUR(s.gio_bat_dau) ORDER BY gio")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TheoGio g = new TheoGio();
                        g.gio = rs.getInt("gio");
                        g.soVe = rs.getLong("so_ve");
                        g.tien = rs.getLong("tien");
                        bc.theoGio.add(g);
                    }
                }
            }

            // Theo nhan vien (nguoi lap hoa don)
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT tk.ho_ten, COUNT(*) AS so_hd, COALESCE(SUM(h.tong_tien), 0) AS tien, "
                    + "(SELECT COUNT(*) FROM chi_tiet_hoa_don_ghe ct JOIN hoa_don h2 ON ct.ma_hd = h2.ma_hd "
                    + "  WHERE h2.id_tai_khoan = tk.id AND h2.trang_thai = 'Da thanh toan' "
                    + "  AND DATE(h2.ngay_lap) BETWEEN ? AND ?) AS so_ve "
                    + "FROM hoa_don h JOIN tai_khoan tk ON tk.id = h.id_tai_khoan "
                    + "WHERE h.trang_thai = 'Da thanh toan' AND DATE(h.ngay_lap) BETWEEN ? AND ? "
                    + "GROUP BY tk.id, tk.ho_ten ORDER BY tien DESC")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                ps.setDate(3, t);
                ps.setDate(4, d);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TheoNhanVien n = new TheoNhanVien();
                        n.hoTen = rs.getString("ho_ten");
                        n.soHd = rs.getLong("so_hd");
                        n.soVe = rs.getLong("so_ve");
                        n.tien = rs.getLong("tien");
                        bc.theoNhanVien.add(n);
                    }
                }
            }

            // Theo phuong thuc thanh toan
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT phuong_thuc, COUNT(*) AS so_hd, COALESCE(SUM(tong_tien), 0) AS tien "
                    + "FROM hoa_don WHERE trang_thai = 'Da thanh toan' AND DATE(ngay_lap) BETWEEN ? AND ? "
                    + "GROUP BY phuong_thuc ORDER BY tien DESC")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TheoThanhToan p = new TheoThanhToan();
                        p.phuongThuc = rs.getString("phuong_thuc");
                        p.soHd = rs.getLong("so_hd");
                        p.tien = rs.getLong("tien");
                        bc.theoThanhToan.add(p);
                    }
                }
            }

            // Khuyen mai da dung trong ky
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT k.ma_km, k.ten_km, COUNT(*) AS luot, COALESCE(SUM(h.tong_tien), 0) AS tien "
                    + "FROM hoa_don h JOIN khuyen_mai k ON k.ma_km = h.ma_km "
                    + "WHERE h.trang_thai = 'Da thanh toan' AND DATE(h.ngay_lap) BETWEEN ? AND ? "
                    + "GROUP BY k.ma_km, k.ten_km ORDER BY luot DESC")) {
                ps.setDate(1, t);
                ps.setDate(2, d);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TheoKhuyenMai k = new TheoKhuyenMai();
                        k.ma = rs.getString("ma_km");
                        k.ten = rs.getString("ten_km");
                        k.luot = rs.getLong("luot");
                        k.tien = rs.getLong("tien");
                        bc.theoKhuyenMai.add(k);
                    }
                }
            }

            // Suat lap day cao nhat len dau (cung ty le thi suat moi hon len truoc)
            bc.theoSuat.sort(Comparator.comparingDouble(TheoSuat::tyLe).reversed()
                    .thenComparing((TheoSuat s) -> s.ngay, Comparator.reverseOrder())
                    .thenComparing(s -> s.gioBatDau));
        }
        return bc;
    }
}