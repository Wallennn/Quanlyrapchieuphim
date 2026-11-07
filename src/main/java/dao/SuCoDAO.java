package dao;

import config.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Xu ly su co & boi thuong: ghi phieu su co, hoan tien hoac doi ve sang suat khac cho khach.
// Moi lan xu ly chay trong 1 giao dich: hoac thanh cong het, hoac khong thay doi gi.
public class SuCoDAO {

    public static final String HOAN_TIEN = "Hoan tien";               // gia tri trong cot hinh_thuc_xu_ly
    public static final String VE_SUAT_KHAC = "Ve moi suat khac";

    // ===== Cac lop du lieu (class thuong, ham doc cung ten voi field) =====

    // Mot suat chieu (dung de chon suat bi su co va suat de doi sang)
    public static final class SuatSuCo {
        private final String maSuat;
        private final String tenPhim;
        private final String tenPhong;
        private final LocalDate ngayChieu;
        private final LocalTime gioBatDau;
        private final int conTrong;      // so ghe con trong

        public SuatSuCo(String maSuat, String tenPhim, String tenPhong, LocalDate ngayChieu,
                        LocalTime gioBatDau, int conTrong) {
            this.maSuat = maSuat;
            this.tenPhim = tenPhim;
            this.tenPhong = tenPhong;
            this.ngayChieu = ngayChieu;
            this.gioBatDau = gioBatDau;
            this.conTrong = conTrong;
        }

        public String maSuat() { return maSuat; }
        public String tenPhim() { return tenPhim; }
        public String tenPhong() { return tenPhong; }
        public LocalDate ngayChieu() { return ngayChieu; }
        public LocalTime gioBatDau() { return gioBatDau; }
        public int conTrong() { return conTrong; }

        @Override
        public String toString() {
            String gio = gioBatDau.format(DateTimeFormatter.ofPattern("HH:mm"));
            String ngay = ngayChieu.equals(LocalDate.now()) ? "" : ngayChieu.format(DateTimeFormatter.ofPattern("dd/MM")) + " ";
            String phong = tenPhong == null ? "" : tenPhong.replace("Phòng chiếu ", "Phòng ");
            return ngay + gio + "  •  " + tenPhim + "  •  " + phong + "  •  còn " + conTrong + " ghế";
        }
    }

    // Hoa don co ve cua 1 suat (ung vien de hoan tien / doi suat)
    public static final class HoaDonAnhHuong {
        private final String maHd;
        private final int soVe;
        private final double tongTien;
        private final LocalDateTime ngayLap;

        public HoaDonAnhHuong(String maHd, int soVe, double tongTien, LocalDateTime ngayLap) {
            this.maHd = maHd;
            this.soVe = soVe;
            this.tongTien = tongTien;
            this.ngayLap = ngayLap;
        }

        public String maHd() { return maHd; }
        public int soVe() { return soVe; }
        public double tongTien() { return tongTien; }
        public LocalDateTime ngayLap() { return ngayLap; }
    }

    // Loai su co: ma = gia tri trong database, ten = chu hien thi
    public static final class LoaiSuCo {
        public static final LoaiSuCo MAT_DIEN = new LoaiSuCo("Mat dien", "Mất điện");
        public static final LoaiSuCo HONG_MAY_CHIEU = new LoaiSuCo("Hong may chieu", "Hỏng máy chiếu");
        public static final LoaiSuCo HONG_PHIM = new LoaiSuCo("Hong phim", "Hỏng phim");
        public static final LoaiSuCo KHAC = new LoaiSuCo("Khac", "Khác");

        private final String ma;
        private final String ten;

        public LoaiSuCo(String ma, String ten) {
            this.ma = ma;
            this.ten = ten;
        }

        public String ma() { return ma; }
        public String ten() { return ten; }

        public static LoaiSuCo[] tatCa() {
            return new LoaiSuCo[]{MAT_DIEN, HONG_MAY_CHIEU, HONG_PHIM, KHAC};
        }

        public static String tenTuMa(String ma) {
            for (LoaiSuCo l : tatCa()) {
                if (l.ma.equals(ma)) return l.ten;
            }
            return ma == null ? "" : ma;
        }

        @Override
        public String toString() {
            return ten;
        }
    }

    // 1 hoa don can xu ly: hoan tien, hoac doi sang suat moi
    public static final class DongXuLy {
        private final String maHd;
        private final String hinhThuc;      // HOAN_TIEN hoac VE_SUAT_KHAC
        private final String maSuatMoi;     // chi dung khi doi suat, con lai null

        public DongXuLy(String maHd, String hinhThuc, String maSuatMoi) {
            this.maHd = maHd;
            this.hinhThuc = hinhThuc;
            this.maSuatMoi = maSuatMoi;
        }

        public String maHd() { return maHd; }
        public String hinhThuc() { return hinhThuc; }
        public String maSuatMoi() { return maSuatMoi; }
    }

    // Ket qua sau khi xu ly
    public static final class KetQuaXuLy {
        private final String maSuCo;
        private final int soHoanTien;
        private final double tongHoan;
        private final int soDoiSuat;

        public KetQuaXuLy(String maSuCo, int soHoanTien, double tongHoan, int soDoiSuat) {
            this.maSuCo = maSuCo;
            this.soHoanTien = soHoanTien;
            this.tongHoan = tongHoan;
            this.soDoiSuat = soDoiSuat;
        }

        public String maSuCo() { return maSuCo; }
        public int soHoanTien() { return soHoanTien; }
        public double tongHoan() { return tongHoan; }
        public int soDoiSuat() { return soDoiSuat; }
    }

    // 1 dong lich su su co
    public static final class LichSuSuCo {
        private final String maSuCo;
        private final String tenPhim;
        private final LocalDate ngayChieu;
        private final LocalTime gioBatDau;
        private final String loaiSuCo;
        private final LocalDateTime ngayXuLy;
        private final int soHoaDon;
        private final double tongHoan;

        public LichSuSuCo(String maSuCo, String tenPhim, LocalDate ngayChieu, LocalTime gioBatDau,
                          String loaiSuCo, LocalDateTime ngayXuLy, int soHoaDon, double tongHoan) {
            this.maSuCo = maSuCo;
            this.tenPhim = tenPhim;
            this.ngayChieu = ngayChieu;
            this.gioBatDau = gioBatDau;
            this.loaiSuCo = loaiSuCo;
            this.ngayXuLy = ngayXuLy;
            this.soHoaDon = soHoaDon;
            this.tongHoan = tongHoan;
        }

        public String maSuCo() { return maSuCo; }
        public String tenPhim() { return tenPhim; }
        public LocalDate ngayChieu() { return ngayChieu; }
        public LocalTime gioBatDau() { return gioBatDau; }
        public String loaiSuCo() { return loaiSuCo; }
        public LocalDateTime ngayXuLy() { return ngayXuLy; }
        public int soHoaDon() { return soHoaDon; }
        public double tongHoan() { return tongHoan; }
    }

    // ===== Truy van =====

    private static final String SELECT_SUAT =
            "SELECT sc.ma_suat, p.ten_phim, pc.ten_phong, sc.ngay_chieu, sc.gio_bat_dau, "
            + "(SELECT COUNT(*) FROM ghe g WHERE g.ma_phong = sc.ma_phong) "
            + "- (SELECT COUNT(*) FROM chi_tiet_hoa_don_ghe ct WHERE ct.ma_suat = sc.ma_suat) AS con_trong "
            + "FROM suat_chieu sc "
            + "JOIN phim p ON sc.ma_phim = p.ma_phim "
            + "JOIN phong_chieu pc ON sc.ma_phong = pc.ma_phong ";

    // Suat chieu hom nay (de chon suat bi su co)
    public List<SuatSuCo> laySuatHomNay() throws SQLException {
        String sql = SELECT_SUAT + "WHERE sc.ngay_chieu = CURDATE() ORDER BY sc.gio_bat_dau, sc.ma_phong";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return docDanhSachSuat(rs);
        }
    }

    // Suat co the doi sang: con chua chieu hom nay + tat ca suat ngay mai (tru suat dang bi su co)
    public List<SuatSuCo> laySuatCoTheDoiSang(String maSuatLoaiTru) throws SQLException {
        String sql = SELECT_SUAT
                + "WHERE sc.ma_suat <> ? AND ((sc.ngay_chieu = CURDATE() AND sc.gio_bat_dau > CURTIME()) "
                + "OR sc.ngay_chieu = DATE_ADD(CURDATE(), INTERVAL 1 DAY)) "
                + "ORDER BY sc.ngay_chieu, sc.gio_bat_dau, sc.ma_phong";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSuatLoaiTru);
            try (ResultSet rs = ps.executeQuery()) {
                return docDanhSachSuat(rs);
            }
        }
    }

    private List<SuatSuCo> docDanhSachSuat(ResultSet rs) throws SQLException {
        List<SuatSuCo> ketQua = new ArrayList<>();
        while (rs.next()) {
            ketQua.add(new SuatSuCo(
                    rs.getString("ma_suat"),
                    rs.getString("ten_phim"),
                    rs.getString("ten_phong"),
                    rs.getDate("ngay_chieu").toLocalDate(),
                    rs.getTime("gio_bat_dau").toLocalTime(),
                    Math.max(0, rs.getInt("con_trong"))));
        }
        return ketQua;
    }

    // Cac hoa don da thanh toan co ve cua 1 suat (hoa don chi co combo khong tinh)
    public List<HoaDonAnhHuong> layHoaDonCoVe(String maSuat) throws SQLException {
        String sql = "SELECT hd.ma_hd, COUNT(ct.id) AS so_ve, hd.tong_tien, hd.ngay_lap "
                + "FROM hoa_don hd JOIN chi_tiet_hoa_don_ghe ct ON ct.ma_hd = hd.ma_hd "
                + "WHERE hd.ma_suat = ? AND hd.trang_thai = 'Da thanh toan' "
                + "GROUP BY hd.ma_hd, hd.tong_tien, hd.ngay_lap ORDER BY hd.ngay_lap";
        List<HoaDonAnhHuong> ketQua = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSuat);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp lap = rs.getTimestamp("ngay_lap");
                    ketQua.add(new HoaDonAnhHuong(rs.getString("ma_hd"), rs.getInt("so_ve"),
                            rs.getDouble("tong_tien"), lap != null ? lap.toLocalDateTime() : null));
                }
            }
        }
        return ketQua;
    }

    public List<LichSuSuCo> layLichSuGanDay() throws SQLException {
        String sql = "SELECT sc.ma_su_co, p.ten_phim, s.ngay_chieu, s.gio_bat_dau, sc.loai_su_co, sc.ngay_xu_ly, "
                + "(SELECT COUNT(*) FROM chi_tiet_boi_thuong b WHERE b.ma_su_co = sc.ma_su_co) AS so_hd, "
                + "(SELECT COALESCE(SUM(b.so_tien_hoan), 0) FROM chi_tiet_boi_thuong b WHERE b.ma_su_co = sc.ma_su_co) AS tong_hoan "
                + "FROM su_co_boi_thuong sc "
                + "JOIN suat_chieu s ON sc.ma_suat = s.ma_suat "
                + "JOIN phim p ON s.ma_phim = p.ma_phim "
                + "ORDER BY sc.ngay_xu_ly DESC LIMIT 50";
        List<LichSuSuCo> ketQua = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp xl = rs.getTimestamp("ngay_xu_ly");
                ketQua.add(new LichSuSuCo(
                        rs.getString("ma_su_co"),
                        rs.getString("ten_phim"),
                        rs.getDate("ngay_chieu").toLocalDate(),
                        rs.getTime("gio_bat_dau").toLocalTime(),
                        rs.getString("loai_su_co"),
                        xl != null ? xl.toLocalDateTime() : null,
                        rs.getInt("so_hd"),
                        rs.getDouble("tong_hoan")));
            }
        }
        return ketQua;
    }

    // ===== Ghi phieu su co + xu ly tung hoa don =====
    // Loi nghiep vu (hoa don da xu ly roi, ghe suat moi da co nguoi...) duoc nem ra duoi dang SQLException
    // voi noi dung tieng Viet de hien thi cho nhan vien; khi do KHONG co gi bi thay doi trong database.
    public KetQuaXuLy xuLySuCo(String maSuat, String loaiSuCo, String moTa, long idTaiKhoan,
                               List<DongXuLy> cacDong) throws SQLException {
        SQLException loiCuoi = null;

        for (int lan = 0; lan < 5; lan++) {           // thu lai neu lo trung ma su co
            String maSuCo = sinhMaSuCo();

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO su_co_boi_thuong (ma_su_co, ma_suat, loai_su_co, mo_ta_su_co, id_tai_khoan) "
                            + "VALUES (?, ?, ?, ?, ?)")) {
                        ps.setString(1, maSuCo);
                        ps.setString(2, maSuat);
                        ps.setString(3, loaiSuCo);
                        ps.setString(4, moTa == null || moTa.isBlank() ? null : moTa.trim());
                        ps.setLong(5, idTaiKhoan);
                        ps.executeUpdate();
                    }

                    int soHoan = 0, soDoi = 0;
                    double tongHoan = 0;

                    for (DongXuLy d : cacDong) {
                        double tongTien = khoaVaKiemTraHoaDon(conn, d.maHd(), maSuat);

                        if (HOAN_TIEN.equals(d.hinhThuc())) {
                            try (PreparedStatement ps = conn.prepareStatement(
                                    "UPDATE hoa_don SET trang_thai = 'Da hoan tien' WHERE ma_hd = ?")) {
                                ps.setString(1, d.maHd());
                                ps.executeUpdate();
                            }
                            ghiChiTiet(conn, maSuCo, d.maHd(), HOAN_TIEN, null, tongTien);
                            soHoan++;
                            tongHoan += tongTien;
                        } else {
                            if (d.maSuatMoi() == null || d.maSuatMoi().equals(maSuat)) {
                                throw new SQLException("Hóa đơn " + d.maHd() + ": chưa chọn suất mới hợp lệ để đổi.");
                            }
                            doiSuat(conn, d.maHd(), d.maSuatMoi());
                            ghiChiTiet(conn, maSuCo, d.maHd(), VE_SUAT_KHAC, d.maSuatMoi(), null);
                            soDoi++;
                        }
                    }

                    conn.commit();
                    return new KetQuaXuLy(maSuCo, soHoan, tongHoan, soDoi);

                } catch (SQLException e) {
                    conn.rollback();
                    boolean trungMaSuCo = e.getErrorCode() == 1062 && e.getMessage() != null
                            && e.getMessage().contains(maSuCo);
                    if (trungMaSuCo) {
                        loiCuoi = e;
                    } else {
                        throw e;
                    }
                }
            }
        }
        throw loiCuoi != null ? loiCuoi : new SQLException("Không thể tạo mã sự cố");
    }

    // Khoa dong hoa don (tranh 2 may cung xu ly 1 hoa don) va kiem tra: dung suat, dang o trang thai da thanh toan
    private double khoaVaKiemTraHoaDon(Connection conn, String maHd, String maSuat) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT tong_tien, trang_thai, ma_suat FROM hoa_don WHERE ma_hd = ? FOR UPDATE")) {
            ps.setString(1, maHd);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Không tìm thấy hóa đơn " + maHd);
                }
                if (!"Da thanh toan".equals(rs.getString("trang_thai"))) {
                    throw new SQLException("Hóa đơn " + maHd + " không còn ở trạng thái \"đã thanh toán\" "
                            + "(có thể đã được xử lý rồi). Hãy tải lại danh sách.");
                }
                if (!maSuat.equals(rs.getString("ma_suat"))) {
                    throw new SQLException("Hóa đơn " + maHd + " không thuộc suất bị sự cố. Hãy tải lại danh sách.");
                }
                return rs.getDouble("tong_tien");
            }
        }
    }

    // Chuyen cac ve cua hoa don sang suat moi: moi ghe cu duoc doi sang ghe CUNG HANG, CUNG SO trong phong cua suat moi
    private void doiSuat(Connection conn, String maHd, String maSuatMoi) throws SQLException {
        List<Long> idVe = new ArrayList<>();
        List<String> hang = new ArrayList<>();
        List<Integer> so = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ct.id, g.hang_ghe, g.so_ghe FROM chi_tiet_hoa_don_ghe ct "
                + "JOIN ghe g ON g.ma_ghe = ct.ma_ghe WHERE ct.ma_hd = ?")) {
            ps.setString(1, maHd);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    idVe.add(rs.getLong("id"));
                    hang.add(rs.getString("hang_ghe"));
                    so.add(rs.getInt("so_ghe"));
                }
            }
        }

        for (int i = 0; i < idVe.size(); i++) {
            String nhanGhe = hang.get(i) + so.get(i);

            // Ghe tuong ung trong phong cua suat moi
            String gheMoi;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT g2.ma_ghe FROM suat_chieu s JOIN ghe g2 ON g2.ma_phong = s.ma_phong "
                    + "WHERE s.ma_suat = ? AND g2.hang_ghe = ? AND g2.so_ghe = ?")) {
                ps.setString(1, maSuatMoi);
                ps.setString(2, hang.get(i));
                ps.setInt(3, so.get(i));
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Hóa đơn " + maHd + ": phòng của suất mới không có ghế " + nhanGhe
                                + ". Hãy chọn suất khác hoặc hoàn tiền.");
                    }
                    gheMoi = rs.getString(1);
                }
            }

            // Ghe do o suat moi da co ai dat chua (ke ca hoa don da hoan, vi database cam trung ma_suat + ma_ghe)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM chi_tiet_hoa_don_ghe WHERE ma_suat = ? AND ma_ghe = ?")) {
                ps.setString(1, maSuatMoi);
                ps.setString(2, gheMoi);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new SQLException("Hóa đơn " + maHd + ": ghế " + nhanGhe + " ở suất mới đã có người đặt. "
                                + "Hãy chọn suất khác hoặc hoàn tiền.");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE chi_tiet_hoa_don_ghe SET ma_ghe = ?, ma_suat = ? WHERE id = ?")) {
                ps.setString(1, gheMoi);
                ps.setString(2, maSuatMoi);
                ps.setLong(3, idVe.get(i));
                ps.executeUpdate();
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("UPDATE hoa_don SET ma_suat = ? WHERE ma_hd = ?")) {
            ps.setString(1, maSuatMoi);
            ps.setString(2, maHd);
            ps.executeUpdate();
        }
    }

    private void ghiChiTiet(Connection conn, String maSuCo, String maHd, String hinhThuc,
                            String maSuatMoi, Double soTienHoan) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO chi_tiet_boi_thuong (ma_su_co, ma_hd, hinh_thuc_xu_ly, ma_suat_moi, so_tien_hoan) "
                + "VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, maSuCo);
            ps.setString(2, maHd);
            ps.setString(3, hinhThuc);
            ps.setString(4, maSuatMoi);
            if (soTienHoan == null) {
                ps.setNull(5, java.sql.Types.DECIMAL);
            } else {
                ps.setDouble(5, soTienHoan);
            }
            ps.executeUpdate();
        }
    }

    // Ma su co: SC + yyMMddHHmmss + 2 so ngau nhien (16 ky tu, cot ma_su_co cho toi da 20)
    private String sinhMaSuCo() {
        return "SC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + String.format("%02d", ThreadLocalRandom.current().nextInt(100));
    }
}