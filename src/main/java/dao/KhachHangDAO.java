package dao;

import config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Khach hang thanh vien (bang khach_hang): tra cuu theo so dien thoai, dang ky, quan ly. */
public class KhachHangDAO {

    public static final String HANG_THUONG = "Thuong", HANG_BAC = "Bac", HANG_VANG = "Vang";
    public static final long NGUONG_BAC = 2_000_000;     // tong chi tieu tu 2 trieu -> hang Bac
    public static final long NGUONG_VANG = 5_000_000;    // tu 5 trieu -> hang Vang

    public static class KhachHang {
        public long id;
        public String soDienThoai;
        public String hoTen;
        public String hang;
        public int diem;
        public long tongChiTieu;
        public LocalDateTime ngayTao;
    }

    /** So dien thoai da co thanh vien khac dung (cot so_dien_thoai la UNIQUE). */
    public static class SdtTrungException extends Exception {
        public SdtTrungException(String sdt) {
            super("Số điện thoại " + sdt + " đã được đăng ký cho thành viên khác");
        }
    }

    /** Khach da co hoa don nen khong xoa duoc. */
    public static class CoHoaDonException extends Exception {
        public CoHoaDonException() {
            super("Khách hàng đã có hóa đơn nên không thể xóa");
        }
    }

    /** So di dong Viet Nam: 10 chu so, bat dau bang 0. */
    public static boolean sdtHopLe(String sdt) {
        return sdt != null && sdt.matches("0\\d{9}");
    }

    public static String hangTheoChiTieu(long tongChiTieu) {
        if (tongChiTieu >= NGUONG_VANG) return HANG_VANG;
        if (tongChiTieu >= NGUONG_BAC) return HANG_BAC;
        return HANG_THUONG;
    }

    /** Phan tram giam tren hoa don theo hang thanh vien. */
    public static int phanTramGiam(String hang) {
        if (HANG_VANG.equals(hang)) return 5;
        if (HANG_BAC.equals(hang)) return 3;
        return 0;
    }

    public static String tenHang(String hang) {
        if (HANG_VANG.equals(hang)) return "Vàng";
        if (HANG_BAC.equals(hang)) return "Bạc";
        return "Thường";
    }

    private static final String COT = "id, so_dien_thoai, ho_ten, hang, diem, tong_chi_tieu, ngay_tao";

    /** Doc 1 dong khach_hang (theo thu tu COT). Dung chung voi ThanhToanController. */
    public static KhachHang doc(ResultSet rs) throws SQLException {
        KhachHang k = new KhachHang();
        k.id = rs.getLong("id");
        k.soDienThoai = rs.getString("so_dien_thoai");
        k.hoTen = rs.getString("ho_ten");
        k.hang = rs.getString("hang");
        k.diem = rs.getInt("diem");
        k.tongChiTieu = rs.getLong("tong_chi_tieu");
        Timestamp t = rs.getTimestamp("ngay_tao");
        k.ngayTao = (t == null) ? null : t.toLocalDateTime();
        return k;
    }

    public KhachHang timTheoSdt(String sdt) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COT + " FROM khach_hang WHERE so_dien_thoai = ?")) {
            ps.setString(1, sdt);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? doc(rs) : null;
            }
        }
    }

    public List<KhachHang> layTatCa() throws SQLException {
        List<KhachHang> ds = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + COT + " FROM khach_hang ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ds.add(doc(rs));
        }
        return ds;
    }

    /** Dang ky thanh vien moi (hang Thuong, 0 diem). Tra ve khach vua tao. */
    public KhachHang them(String sdt, String hoTen) throws SQLException, SdtTrungException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO khach_hang (so_dien_thoai, ho_ten) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sdt);
            ps.setString(2, hoTen);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) throw new SdtTrungException(sdt);
            throw e;
        }
        return timTheoSdt(sdt);
    }

    /** Quan ly sua thong tin / dieu chinh diem. Hang tu tinh lai theo tong chi tieu. */
    public void capNhat(long id, String sdt, String hoTen, int diem) throws SQLException, SdtTrungException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE khach_hang SET so_dien_thoai = ?, ho_ten = ?, diem = ? WHERE id = ?")) {
            ps.setString(1, sdt);
            ps.setString(2, hoTen);
            ps.setInt(3, Math.max(0, diem));
            ps.setLong(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) throw new SdtTrungException(sdt);
            throw e;
        }
    }

    public void xoa(long id) throws SQLException, CoHoaDonException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM khach_hang WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) throw new CoHoaDonException();   // vi pham khoa ngoai
            throw e;
        }
    }
}