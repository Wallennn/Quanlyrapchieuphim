package dao;

import config.DBConnection;
import controller.ThanhToanController;
import dao.KhachHangDAO.KhachHang;
import model.ComboBan;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BanComboDAO {

    // Suat chieu hom nay (de chon "combo nay ban cho suat nao")
    public static final class SuatHomNay {
        private final String maSuat;
        private final String tenPhim;
        private final String tenPhong;
        private final LocalTime gioBatDau;
        private final LocalTime gioKetThuc;

        public SuatHomNay(String maSuat, String tenPhim, String tenPhong, LocalTime gioBatDau, LocalTime gioKetThuc) {
            this.maSuat = maSuat;
            this.tenPhim = tenPhim;
            this.tenPhong = tenPhong;
            this.gioBatDau = gioBatDau;
            this.gioKetThuc = gioKetThuc;
        }

        public String maSuat() {
            return maSuat;
        }

        public String tenPhim() {
            return tenPhim;
        }

        public String tenPhong() {
            return tenPhong;
        }

        public LocalTime gioBatDau() {
            return gioBatDau;
        }

        public LocalTime gioKetThuc() {
            return gioKetThuc;
        }

        @Override
        public String toString() {
            String phong = tenPhong == null ? "" : tenPhong.replace("Phòng chiếu ", "Phòng ");
            return gioBatDau.format(DateTimeFormatter.ofPattern("HH:mm")) + "  •  " + tenPhim + "  •  " + phong;
        }
    }

    // 1 dong trong don hang: combo + so luong
    public static final class DongCombo {
        private final ComboBan combo;
        private final int soLuong;

        public DongCombo(ComboBan combo, int soLuong) {
            this.combo = combo;
            this.soLuong = soLuong;
        }

        public ComboBan combo() {
            return combo;
        }

        public int soLuong() {
            return soLuong;
        }
    }

    // Ket qua sau khi lap hoa don (kem thong tin tich diem neu khach la thanh vien)
    public static final class HoaDonCombo {
        private final String maHd;
        private final double tongTien;
        private final KhachHang khach;       // thanh vien sau khi cong diem (null neu khach vang lai)
        private final int diemCong;

        public HoaDonCombo(String maHd, double tongTien) {
            this(maHd, tongTien, null, 0);
        }

        public HoaDonCombo(String maHd, double tongTien, KhachHang khach, int diemCong) {
            this.maHd = maHd;
            this.tongTien = tongTien;
            this.khach = khach;
            this.diemCong = diemCong;
        }

        public String maHd() {
            return maHd;
        }

        public double tongTien() {
            return tongTien;
        }

        /** Thanh vien sau khi cong diem, null neu khach khong nhap SDT. */
        public KhachHang khach() {
            return khach;
        }

        /** So diem vua duoc cong cho hoa don nay. */
        public int diemCong() {
            return diemCong;
        }
    }

    /** SDT da nhap nhung chua dang ky thanh vien. */
    public static final class ChuaDangKyException extends SQLException {
        public ChuaDangKyException(String sdt) {
            super("Số điện thoại " + sdt + " chưa đăng ký thành viên");
        }
    }

    /** So diem se tich cho hoa don combo: 1 diem / 20.000d (cung quy tac voi ban ve). */
    public static int diemSeTich(double tong) {
        return (int) ((long) tong / ThanhToanController.DIEM_MOI_DONG);
    }

    public List<ComboBan> layDanhSachCombo() throws SQLException {
        String sql = "SELECT ma_combo, ten_combo, mo_ta, gia, hinh_anh FROM combo ORDER BY ma_combo";
        List<ComboBan> ketQua = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ketQua.add(new ComboBan(
                        rs.getString("ma_combo"),
                        rs.getString("ten_combo"),
                        rs.getString("mo_ta"),
                        rs.getDouble("gia"),
                        rs.getString("hinh_anh")));
            }
        }
        return ketQua;
    }

    public List<SuatHomNay> laySuatChieuHomNay() throws SQLException {
        String sql = "SELECT sc.ma_suat, p.ten_phim, pc.ten_phong, sc.gio_bat_dau, sc.gio_ket_thuc "
                + "FROM suat_chieu sc "
                + "JOIN phim p ON sc.ma_phim = p.ma_phim "
                + "JOIN phong_chieu pc ON sc.ma_phong = pc.ma_phong "
                + "WHERE sc.ngay_chieu = CURDATE() "
                + "ORDER BY sc.gio_bat_dau, sc.ma_phong";
        List<SuatHomNay> ketQua = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ketQua.add(new SuatHomNay(
                        rs.getString("ma_suat"),
                        rs.getString("ten_phim"),
                        rs.getString("ten_phong"),
                        rs.getTime("gio_bat_dau").toLocalTime(),
                        rs.getTime("gio_ket_thuc").toLocalTime()));
            }
        }
        return ketQua;
    }

    // Lap hoa don combo BAN LE (khong kem ve, khong gan suat chieu) trong 1 giao dich.
    // Neu co SDT thanh vien: tich diem (1 diem / 20.000d) va cong don tong chi tieu (hang thanh vien tu cap nhat).
    // Gia combo duoc doc lai tu database ngay luc thanh toan (khong tin gia dang hien tren man hinh).
    public HoaDonCombo taoHoaDonCombo(long idTaiKhoan, List<DongCombo> cacDong, String sdt) throws SQLException {
        SQLException loiCuoi = null;
        String soDt = (sdt == null) ? "" : sdt.trim();

        for (int lan = 0; lan < 5; lan++) {          // thu lai neu lo trung ma hoa don
            String maHd = sinhMaHoaDon();

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    double tong = 0;
                    double[] donGia = new double[cacDong.size()];

                    try (PreparedStatement ps = conn.prepareStatement("SELECT gia FROM combo WHERE ma_combo = ?")) {
                        for (int i = 0; i < cacDong.size(); i++) {
                            ps.setString(1, cacDong.get(i).combo().maCombo());
                            try (ResultSet rs = ps.executeQuery()) {
                                if (!rs.next()) {
                                    throw new SQLException("Combo không còn tồn tại: " + cacDong.get(i).combo().maCombo());
                                }
                                donGia[i] = rs.getDouble(1);
                            }
                            tong += donGia[i] * cacDong.get(i).soLuong();
                        }
                    }

                    // Thanh vien (khoa dong de cong diem an toan khi 2 may cung ban)
                    KhachHang khach = null;
                    if (!soDt.isEmpty()) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT id, so_dien_thoai, ho_ten, hang, diem, tong_chi_tieu, ngay_tao "
                                        + "FROM khach_hang WHERE so_dien_thoai = ? FOR UPDATE")) {
                            ps.setString(1, soDt);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) khach = KhachHangDAO.doc(rs);
                            }
                        }
                        if (khach == null) throw new ChuaDangKyException(soDt);
                    }
                    int diemCong = (khach != null) ? diemSeTich(tong) : 0;

                    // ma_suat de NULL: ban le khong thuoc suat chieu nao
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO hoa_don (ma_hd, ma_suat, id_tai_khoan, tong_tien, trang_thai, ma_khach_hang, diem_tich_luy) "
                                    + "VALUES (?, NULL, ?, ?, 'Da thanh toan', ?, ?)")) {
                        ps.setString(1, maHd);
                        ps.setLong(2, idTaiKhoan);
                        ps.setDouble(3, tong);
                        if (khach != null) ps.setLong(4, khach.id); else ps.setNull(4, java.sql.Types.BIGINT);
                        ps.setInt(5, diemCong);
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        if (e.getErrorCode() == 1048) {   // cot ma_suat con dang NOT NULL
                            throw new SQLException("Cơ sở dữ liệu chưa cho phép hóa đơn không có suất chiếu. "
                                    + "Hãy chạy lệnh SQL: ALTER TABLE hoa_don MODIFY ma_suat varchar(20) NULL;", e);
                        }
                        throw e;
                    }

                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO chi_tiet_hoa_don_combo (ma_hd, ma_combo, so_luong, don_gia) VALUES (?, ?, ?, ?)")) {
                        for (int i = 0; i < cacDong.size(); i++) {
                            ps.setString(1, maHd);
                            ps.setString(2, cacDong.get(i).combo().maCombo());
                            ps.setInt(3, cacDong.get(i).soLuong());
                            ps.setDouble(4, donGia[i]);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    KhachHang sau = null;
                    if (khach != null) {
                        long tongMoi = khach.tongChiTieu + (long) tong;
                        int diemMoi = khach.diem + diemCong;
                        String hangMoi = KhachHangDAO.hangTheoChiTieu(tongMoi);
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE khach_hang SET diem = ?, tong_chi_tieu = ?, hang = ? WHERE id = ?")) {
                            ps.setInt(1, diemMoi);
                            ps.setLong(2, tongMoi);
                            ps.setString(3, hangMoi);
                            ps.setLong(4, khach.id);
                            ps.executeUpdate();
                        }
                        sau = new KhachHang();
                        sau.id = khach.id;
                        sau.soDienThoai = khach.soDienThoai;
                        sau.hoTen = khach.hoTen;
                        sau.diem = diemMoi;
                        sau.tongChiTieu = tongMoi;
                        sau.hang = hangMoi;
                    }

                    conn.commit();
                    return new HoaDonCombo(maHd, tong, sau, diemCong);

                } catch (SQLException e) {
                    conn.rollback();
                    if (e.getErrorCode() == 1062) {   // 1062 = trung khoa chinh (ma_hd) -> sinh ma khac roi thu lai
                        loiCuoi = e;
                    } else {
                        throw e;
                    }
                }
            }
        }
        throw loiCuoi != null ? loiCuoi : new SQLException("Không thể tạo mã hóa đơn");
    }

    // Ma hoa don combo: HDCB + yyMMddHHmmss + 2 so ngau nhien (18 ky tu, cot ma_hd cho toi da 20)
    private String sinhMaHoaDon() {
        return "HDCB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + String.format("%02d", ThreadLocalRandom.current().nextInt(100));
    }
}