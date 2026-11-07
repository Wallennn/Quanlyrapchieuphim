package dao;

import config.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Chuong trinh khuyen mai (bang khuyen_mai). */
public class KhuyenMaiDAO {

    public static final String PHAN_TRAM = "Phan tram", SO_TIEN = "So tien";
    public static final String CHO_VE = "Ve", CHO_TONG = "Tong";

    public static class KhuyenMai {
        public String ma, ten, moTa;
        public String maNhap;            // null = tu dong ap dung
        public String loaiGiam = PHAN_TRAM;
        public long giaTri;
        public String apDungCho = CHO_VE;
        public Long giamToiDa;           // null = khong gioi han (chi dung cho giam theo %)
        public long donToiThieu;
        public String ngayTrongTuan;     // "1,2,3" (1 = thu 2 ... 7 = chu nhat); null = moi ngay
        public LocalDate tuNgay, denNgay;
        public Integer gioiHan;          // null = khong gioi han luot dung
        public boolean hoatDong = true;
        public int daDung;               // so hoa don da ap dung (khong tinh hoa don da huy)

        /** Co ap dung cho suat chieu vao thu nay khong (isoThu: 1 = thu 2 ... 7 = chu nhat). */
        public boolean apDungThu(int isoThu) {
            if (ngayTrongTuan == null || ngayTrongTuan.trim().isEmpty()) return true;
            for (String p : ngayTrongTuan.split(",")) {
                if (p.trim().equals(String.valueOf(isoThu))) return true;
            }
            return false;
        }

        public boolean tuDong() {
            return maNhap == null || maNhap.trim().isEmpty();
        }
    }

    public static String tenThu(int isoThu) {
        return isoThu == 7 ? "Chủ nhật" : "Thứ " + (isoThu + 1);
    }

    /** "1,3" -> "Thứ 2, Thứ 4"; null -> "Mọi ngày". */
    public static String moTaThu(String ngayTrongTuan) {
        if (ngayTrongTuan == null || ngayTrongTuan.trim().isEmpty()) return "Mọi ngày";
        StringBuilder sb = new StringBuilder();
        for (String p : ngayTrongTuan.split(",")) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(tenThu(Integer.parseInt(p.trim())));
        }
        return sb.toString();
    }

    static final String CHON =
            "SELECT k.ma_km, k.ten_km, k.mo_ta, k.ma_nhap, k.loai_giam, k.gia_tri, k.ap_dung_cho, k.giam_toi_da, "
            + "k.don_toi_thieu, k.ngay_trong_tuan, k.tu_ngay, k.den_ngay, k.gioi_han_su_dung, k.trang_thai, "
            + "(SELECT COUNT(*) FROM hoa_don h WHERE h.ma_km = k.ma_km AND h.trang_thai <> 'Da huy') AS da_dung "
            + "FROM khuyen_mai k ";

    public static KhuyenMai doc(ResultSet rs) throws SQLException {
        KhuyenMai k = new KhuyenMai();
        k.ma = rs.getString("ma_km");
        k.ten = rs.getString("ten_km");
        k.moTa = rs.getString("mo_ta");
        k.maNhap = rs.getString("ma_nhap");
        k.loaiGiam = rs.getString("loai_giam");
        k.giaTri = rs.getLong("gia_tri");
        k.apDungCho = rs.getString("ap_dung_cho");
        long toiDa = rs.getLong("giam_toi_da");
        k.giamToiDa = rs.wasNull() ? null : toiDa;
        k.donToiThieu = rs.getLong("don_toi_thieu");
        k.ngayTrongTuan = rs.getString("ngay_trong_tuan");
        k.tuNgay = rs.getDate("tu_ngay").toLocalDate();
        k.denNgay = rs.getDate("den_ngay").toLocalDate();
        int gh = rs.getInt("gioi_han_su_dung");
        k.gioiHan = rs.wasNull() ? null : gh;
        k.hoatDong = rs.getInt("trang_thai") == 1;
        k.daDung = rs.getInt("da_dung");
        return k;
    }

    // ----- Dung chung voi ThanhToanController (cung ket noi / giao dich) -----
    public static KhuyenMai timTheoMaNhap(Connection c, String maNhap) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(CHON + "WHERE k.ma_nhap = ?")) {
            ps.setString(1, maNhap);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? doc(rs) : null;
            }
        }
    }

    public static List<KhuyenMai> layTuDong(Connection c) throws SQLException {
        List<KhuyenMai> ds = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(CHON + "WHERE k.ma_nhap IS NULL AND k.trang_thai = 1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ds.add(doc(rs));
        }
        return ds;
    }

    // ----- Quan ly (man hinh admin) -----
    public List<KhuyenMai> layTatCa() throws SQLException {
        List<KhuyenMai> ds = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(CHON + "ORDER BY k.trang_thai DESC, k.den_ngay DESC, k.ma_km");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ds.add(doc(rs));
        }
        return ds;
    }

    /** Ma khuyen mai hoac ma nhap bi trung. */
    public static class TrungMaException extends Exception {
        public TrungMaException(String thongBao) {
            super(thongBao);
        }
    }

    public void them(KhuyenMai k) throws SQLException, TrungMaException {
        String sql = "INSERT INTO khuyen_mai (ma_km, ten_km, mo_ta, ma_nhap, loai_giam, gia_tri, ap_dung_cho, "
                + "giam_toi_da, don_toi_thieu, ngay_trong_tuan, tu_ngay, den_ngay, gioi_han_su_dung, trang_thai) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, k.ma);
            ghiTruong(ps, 2, k);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw doiLoiTrung(e, k);
        }
    }

    public void capNhat(KhuyenMai k) throws SQLException, TrungMaException {
        String sql = "UPDATE khuyen_mai SET ten_km=?, mo_ta=?, ma_nhap=?, loai_giam=?, gia_tri=?, ap_dung_cho=?, "
                + "giam_toi_da=?, don_toi_thieu=?, ngay_trong_tuan=?, tu_ngay=?, den_ngay=?, gioi_han_su_dung=?, "
                + "trang_thai=? WHERE ma_km=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ghiTruong(ps, 1, k);
            ps.setString(14, k.ma);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw doiLoiTrung(e, k);
        }
    }

    // ghi 13 truong (ten_km ... trang_thai) bat dau tu vi tri dau
    private void ghiTruong(PreparedStatement ps, int dau, KhuyenMai k) throws SQLException {
        int i = dau;
        ps.setString(i++, k.ten);
        ps.setString(i++, k.moTa);
        ps.setString(i++, k.tuDong() ? null : k.maNhap.trim());
        ps.setString(i++, k.loaiGiam);
        ps.setLong(i++, k.giaTri);
        ps.setString(i++, k.apDungCho);
        if (k.giamToiDa == null) ps.setNull(i++, java.sql.Types.DECIMAL); else ps.setLong(i++, k.giamToiDa);
        ps.setLong(i++, k.donToiThieu);
        ps.setString(i++, (k.ngayTrongTuan == null || k.ngayTrongTuan.isEmpty()) ? null : k.ngayTrongTuan);
        ps.setDate(i++, Date.valueOf(k.tuNgay));
        ps.setDate(i++, Date.valueOf(k.denNgay));
        if (k.gioiHan == null) ps.setNull(i++, java.sql.Types.INTEGER); else ps.setInt(i++, k.gioiHan);
        ps.setInt(i, k.hoatDong ? 1 : 0);
    }

    private SQLException doiLoiTrung(SQLException e, KhuyenMai k) throws TrungMaException {
        if (e.getErrorCode() == 1062) {
            String m = e.getMessage() == null ? "" : e.getMessage();
            if (m.contains("uk_khuyen_mai_ma_nhap")) throw new TrungMaException("Mã nhập \"" + k.maNhap + "\" đã được dùng cho khuyến mãi khác");
            throw new TrungMaException("Mã khuyến mãi \"" + k.ma + "\" đã tồn tại");
        }
        return e;
    }

    public void datTrangThai(String ma, boolean hoatDong) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE khuyen_mai SET trang_thai = ? WHERE ma_km = ?")) {
            ps.setInt(1, hoatDong ? 1 : 0);
            ps.setString(2, ma);
            ps.executeUpdate();
        }
    }

    /** Khuyen mai da duoc dung tren hoa don thi khong xoa duoc (nen chuyen sang tam dung). */
    public static class DaDuocDungException extends Exception {
        public DaDuocDungException() {
            super("Khuyến mãi đã được áp dụng cho hóa đơn nên không thể xóa. Hãy chuyển sang \"Tạm dừng\".");
        }
    }

    public void xoa(String ma) throws SQLException, DaDuocDungException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM khuyen_mai WHERE ma_km = ?")) {
            ps.setString(1, ma);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) throw new DaDuocDungException();
            throw e;
        }
    }
}