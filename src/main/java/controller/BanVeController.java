package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ BÁN VÉ: lấy suất chiếu, sơ đồ ghế, combo và lập hóa đơn.
 */
public class BanVeController {

    // >>> GIỐNG VỚI Thongketrangchucontroller: sửa cho đúng database của bạn <<<
    private static final String URL =
            "jdbc:mysql://localhost:3306/quan_ly_rap_chieu_phim"
            + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "";

    private Connection layKetNoi() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // ===================== CÁC LỚP DỮ LIỆU =====================
    public static class SuatBan {
        public String maSuat, tenPhim, maPhong, tenPhong;
        public LocalDate ngay;
        public LocalTime gioBatDau, gioKetThuc;
        public long giaThuong, giaVip, giaDoi;
        public int tongGhe, daBan;

        public long giaTheoLoai(String loai) {
            if ("VIP".equals(loai)) return giaVip;
            if ("Doi".equals(loai)) return giaDoi;
            return giaThuong;
        }
    }

    public static class Ghe {
        public String maGhe, hang, loai;
        public int so;
        public boolean daBan;
    }

    public static class SoDoGhe {
        public int soHang, soCot;
        public List<Ghe> dsGhe = new ArrayList<>();
    }

    public static class Combo {
        public String ma, ten, moTa;
        public long gia;
    }

    /** Có ghế vừa được bán ở quầy khác trong lúc đang thanh toán. */
    public static class GheDaBanException extends Exception {
        public GheDaBanException() {
            super("Có ghế vừa được bán cho khách khác");
        }
    }

    // ===================== SUẤT CHIẾU =====================
    private static final String SQL_SUAT =
            "SELECT s.ma_suat, p.ten_phim, s.ma_phong, pc.ten_phong, "
            + "(pc.so_hang * pc.so_cot) AS tong_ghe, s.ngay_chieu, s.gio_bat_dau, s.gio_ket_thuc, "
            + "s.gia_ve_thuong, s.gia_ve_vip, s.gia_ve_doi, "
            + "(SELECT COUNT(*) FROM chi_tiet_hoa_don_ghe g WHERE g.ma_suat = s.ma_suat) AS da_ban "
            + "FROM suat_chieu s "
            + "JOIN phim p ON s.ma_phim = p.ma_phim "
            + "JOIN phong_chieu pc ON s.ma_phong = pc.ma_phong ";

    private List<SuatBan> truyVanSuat(Connection c, String dieuKien, Object thamSo) throws SQLException {
        List<SuatBan> kq = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SQL_SUAT + dieuKien)) {
            ps.setObject(1, thamSo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SuatBan s = new SuatBan();
                    s.maSuat = rs.getString("ma_suat");
                    s.tenPhim = rs.getString("ten_phim");
                    s.maPhong = rs.getString("ma_phong");
                    s.tenPhong = rs.getString("ten_phong");
                    s.tongGhe = rs.getInt("tong_ghe");
                    s.ngay = rs.getDate("ngay_chieu").toLocalDate();
                    s.gioBatDau = rs.getTime("gio_bat_dau").toLocalTime();
                    s.gioKetThuc = rs.getTime("gio_ket_thuc").toLocalTime();
                    s.giaThuong = rs.getLong("gia_ve_thuong");
                    s.giaVip = rs.getLong("gia_ve_vip");
                    s.giaDoi = rs.getLong("gia_ve_doi");
                    s.daBan = rs.getInt("da_ban");
                    kq.add(s);
                }
            }
        }
        return kq;
    }

    /** Các suất chiếu còn bán được trong ngày (bỏ phim ngưng chiếu, bỏ suất đã chiếu quá 15 phút). */
    public List<SuatBan> laySuatTheoNgay(LocalDate ngay) throws SQLException {
        List<SuatBan> ds;
        try (Connection c = layKetNoi()) {
            ds = truyVanSuat(c,
                    "WHERE s.ngay_chieu = ? AND p.trang_thai <> 'Ngung chieu' "
                    + "ORDER BY s.gio_bat_dau, pc.ten_phong",
                    java.sql.Date.valueOf(ngay));
        }
        if (ngay.equals(LocalDate.now())) {
            LocalTime moc = LocalTime.now().minusMinutes(15);
            ds.removeIf(s -> s.gioBatDau.isBefore(moc));
        }
        return ds;
    }

    public SuatBan layMotSuat(String maSuat) throws SQLException {
        try (Connection c = layKetNoi()) {
            List<SuatBan> ds = truyVanSuat(c, "WHERE s.ma_suat = ?", maSuat);
            return ds.isEmpty() ? null : ds.get(0);
        }
    }

    // ===================== SƠ ĐỒ GHẾ =====================
    public SoDoGhe laySoDoGhe(String maSuat) throws SQLException {
        SoDoGhe sd = new SoDoGhe();
        try (Connection c = layKetNoi()) {
            String maPhong;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT pc.ma_phong, pc.so_hang, pc.so_cot FROM suat_chieu s "
                    + "JOIN phong_chieu pc ON s.ma_phong = pc.ma_phong WHERE s.ma_suat = ?")) {
                ps.setString(1, maSuat);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Không tìm thấy suất chiếu " + maSuat);
                    maPhong = rs.getString("ma_phong");
                    sd.soHang = rs.getInt("so_hang");
                    sd.soCot = rs.getInt("so_cot");
                }
            }

            damBaoGheDayDu(c, maPhong, sd.soHang, sd.soCot);

            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT g.ma_ghe, g.hang_ghe, g.so_ghe, g.loai_ghe, (ct.id IS NOT NULL) AS da_ban "
                    + "FROM ghe g LEFT JOIN chi_tiet_hoa_don_ghe ct "
                    + "ON ct.ma_ghe = g.ma_ghe AND ct.ma_suat = ? "
                    + "WHERE g.ma_phong = ? ORDER BY g.hang_ghe, g.so_ghe")) {
                ps.setString(1, maSuat);
                ps.setString(2, maPhong);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Ghe g = new Ghe();
                        g.maGhe = rs.getString("ma_ghe");
                        g.hang = rs.getString("hang_ghe");
                        g.so = rs.getInt("so_ghe");
                        g.loai = rs.getString("loai_ghe");
                        g.daBan = rs.getInt("da_ban") == 1;
                        sd.dsGhe.add(g);
                    }
                }
            }
        }
        return sd;
    }

    /**
     * Bảng ghe mới chỉ có ghế của một vài phòng. Nếu phòng còn thiếu ghế so với
     * so_hang x so_cot thì tự tạo thêm (INSERT IGNORE nên không ghi đè ghế đã có).
     * Quy tắc loại ghế theo dữ liệu mẫu: hàng đầu = Thuong, hàng cuối = Doi, còn lại = VIP.
     */
    private void damBaoGheDayDu(Connection c, String maPhong, int soHang, int soCot) throws SQLException {
        int daCo;
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM ghe WHERE ma_phong = ?")) {
            ps.setString(1, maPhong);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                daCo = rs.getInt(1);
            }
        }
        if (daCo >= soHang * soCot) return;

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT IGNORE INTO ghe (ma_ghe, ma_phong, hang_ghe, so_ghe, loai_ghe) VALUES (?, ?, ?, ?, ?)")) {
            for (int h = 0; h < soHang; h++) {
                String hang = String.valueOf((char) ('A' + h));
                String loai = (h == 0) ? "Thuong" : (h == soHang - 1 ? "Doi" : "VIP");
                for (int cot = 1; cot <= soCot; cot++) {
                    ps.setString(1, maPhong + "-" + hang + cot);
                    ps.setString(2, maPhong);
                    ps.setString(3, hang);
                    ps.setInt(4, cot);
                    ps.setString(5, loai);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    // ===================== COMBO =====================
    public List<Combo> layDanhSachCombo() throws SQLException {
        List<Combo> kq = new ArrayList<>();
        try (Connection c = layKetNoi();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT ma_combo, ten_combo, mo_ta, gia FROM combo ORDER BY ma_combo");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Combo cb = new Combo();
                cb.ma = rs.getString("ma_combo");
                cb.ten = rs.getString("ten_combo");
                cb.moTa = rs.getString("mo_ta");
                cb.gia = rs.getLong("gia");
                kq.add(cb);
            }
        }
        return kq;
    }

    // ===================== THANH TOÁN =====================
    /**
     * Lập hóa đơn (1 giao dịch: lỗi thì hoàn tác toàn bộ).
     * Giá vé và giá combo được tính lại từ database, không tin giá do giao diện gửi lên.
     *
     * @return mã hóa đơn vừa tạo
     */
    public String thanhToan(String maSuat, long idTaiKhoan, List<String> dsMaGhe,
                            Map<String, Integer> combo) throws SQLException, GheDaBanException {
        if (dsMaGhe == null || dsMaGhe.isEmpty()) {
            throw new SQLException("Chưa chọn ghế nào");
        }
        try (Connection c = layKetNoi()) {
            c.setAutoCommit(false);
            try {
                List<SuatBan> ds = truyVanSuat(c, "WHERE s.ma_suat = ?", maSuat);
                if (ds.isEmpty()) throw new SQLException("Suất chiếu không tồn tại");
                SuatBan suat = ds.get(0);

                // Loại ghế (để tính giá) - chỉ nhận ghế thuộc đúng phòng của suất
                Map<String, String> loaiGhe = new HashMap<>();
                String dauHoi = String.join(",", Collections.nCopies(dsMaGhe.size(), "?"));
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT ma_ghe, loai_ghe FROM ghe WHERE ma_phong = ? AND ma_ghe IN (" + dauHoi + ")")) {
                    ps.setString(1, suat.maPhong);
                    for (int i = 0; i < dsMaGhe.size(); i++) ps.setString(i + 2, dsMaGhe.get(i));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) loaiGhe.put(rs.getString(1), rs.getString(2));
                    }
                }

                long tong = 0;
                Map<String, Long> giaVe = new HashMap<>();
                for (String ma : dsMaGhe) {
                    String loai = loaiGhe.get(ma);
                    if (loai == null) throw new SQLException("Ghế không hợp lệ: " + ma);
                    long gia = suat.giaTheoLoai(loai);
                    giaVe.put(ma, gia);
                    tong += gia;
                }

                // Giá combo
                Map<String, Long> giaCombo = new HashMap<>();
                if (combo != null) {
                    for (Map.Entry<String, Integer> e : combo.entrySet()) {
                        if (e.getValue() == null || e.getValue() <= 0) continue;
                        try (PreparedStatement ps = c.prepareStatement("SELECT gia FROM combo WHERE ma_combo = ?")) {
                            ps.setString(1, e.getKey());
                            try (ResultSet rs = ps.executeQuery()) {
                                if (!rs.next()) throw new SQLException("Combo không tồn tại: " + e.getKey());
                                long g = rs.getLong(1);
                                giaCombo.put(e.getKey(), g);
                                tong += g * e.getValue();
                            }
                        }
                    }
                }

                String maHd = "HD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO hoa_don (ma_hd, ma_suat, id_tai_khoan, ngay_lap, tong_tien, trang_thai) "
                        + "VALUES (?, ?, ?, NOW(), ?, 'Da thanh toan')")) {
                    ps.setString(1, maHd);
                    ps.setString(2, maSuat);
                    ps.setLong(3, idTaiKhoan);
                    ps.setLong(4, tong);
                    ps.executeUpdate();
                }

                // Ghế: ràng buộc UNIQUE (ma_suat, ma_ghe) sẽ chặn bán trùng ghế
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO chi_tiet_hoa_don_ghe (ma_hd, ma_ghe, ma_suat, gia_ve) VALUES (?, ?, ?, ?)")) {
                    for (String ma : dsMaGhe) {
                        ps.setString(1, maHd);
                        ps.setString(2, ma);
                        ps.setString(3, maSuat);
                        ps.setLong(4, giaVe.get(ma));
                        ps.executeUpdate();
                    }
                }

                if (!giaCombo.isEmpty()) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO chi_tiet_hoa_don_combo (ma_hd, ma_combo, so_luong, don_gia) VALUES (?, ?, ?, ?)")) {
                        for (Map.Entry<String, Long> e : giaCombo.entrySet()) {
                            ps.setString(1, maHd);
                            ps.setString(2, e.getKey());
                            ps.setInt(3, combo.get(e.getKey()));
                            ps.setLong(4, e.getValue());
                            ps.executeUpdate();
                        }
                    }
                }

                c.commit();
                return maHd;
            } catch (SQLException e) {
                c.rollback();
                if (e.getErrorCode() == 1062) { // Duplicate entry
                    throw new GheDaBanException();
                }
                throw e;
            }
        }
    }
}