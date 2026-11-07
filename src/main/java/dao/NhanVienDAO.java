package dao;

import config.DBConnection;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý tài khoản nhân viên (bảng tai_khoan).
 * Mật khẩu được băm bằng Argon2i với đúng tham số của các tài khoản hiện có
 * ($argon2i$v=19$m=65536,t=2,p=1$...) nên đăng nhập nhận được ngay.
 */
public class NhanVienDAO {

    public static final String VT_QUAN_LY = "QUAN_LY";
    public static final String VT_NHAN_VIEN = "NHAN_VIEN";

    private static final int LAN_LAP = 2;
    private static final int BO_NHO_KB = 65536;
    private static final int SONG_SONG = 1;

    private final long idNguoiThucHien;   // id quản lý đang đăng nhập (để chặn tự khóa / tự hạ quyền)

    public NhanVienDAO(long idNguoiThucHien) {
        this.idNguoiThucHien = idNguoiThucHien;
    }

    public static class NhanVien {
        public long id;
        public String tenDangNhap;
        public String hoTen;
        public String vaiTro;
        public boolean hoatDong;
        public LocalDateTime ngayTao;
        public LocalDateTime lanDangNhapCuoi;
    }

    /** Tên đăng nhập đã có người dùng (cột ten_dang_nhap là UNIQUE). */
    public static class TenDangNhapTrungException extends Exception {
        public TenDangNhapTrungException(String ten) {
            super("Tên đăng nhập \"" + ten + "\" đã tồn tại");
        }
    }

    /** Vi phạm quy tắc nghiệp vụ (thông báo hiển thị thẳng cho người dùng). */
    public static class RangBuocException extends Exception {
        public RangBuocException(String thongBao) {
            super(thongBao);
        }
    }

    /** Băm mật khẩu Argon2i. Mảng ký tự truyền vào sẽ bị xóa sau khi băm. */
    public static String bamMatKhau(char[] matKhau) {
        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2i);
        try {
            return argon2.hash(LAN_LAP, BO_NHO_KB, SONG_SONG, matKhau);
        } finally {
            argon2.wipeArray(matKhau);
        }
    }

    public List<NhanVien> layTatCa() throws SQLException {
        String sql = "SELECT id, ten_dang_nhap, ho_ten, vai_tro, trang_thai, ngay_tao, lan_dang_nhap_cuoi "
                + "FROM tai_khoan ORDER BY id";
        List<NhanVien> ds = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NhanVien n = new NhanVien();
                n.id = rs.getLong("id");
                n.tenDangNhap = rs.getString("ten_dang_nhap");
                n.hoTen = rs.getString("ho_ten");
                n.vaiTro = rs.getString("vai_tro");
                n.hoatDong = rs.getInt("trang_thai") == 1;
                Timestamp t1 = rs.getTimestamp("ngay_tao");
                n.ngayTao = (t1 == null) ? null : t1.toLocalDateTime();
                Timestamp t2 = rs.getTimestamp("lan_dang_nhap_cuoi");
                n.lanDangNhapCuoi = (t2 == null) ? null : t2.toLocalDateTime();
                ds.add(n);
            }
        }
        return ds;
    }

    /** Thêm tài khoản mới. Trả về id vừa tạo. */
    public long them(String tenDangNhap, String hoTen, String vaiTro, boolean hoatDong, char[] matKhau)
            throws SQLException, TenDangNhapTrungException {
        String hash = bamMatKhau(matKhau);
        String sql = "INSERT INTO tai_khoan (ten_dang_nhap, mat_khau, ho_ten, vai_tro, trang_thai) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tenDangNhap);
            ps.setString(2, hash);
            ps.setString(3, hoTen);
            ps.setString(4, vaiTro);
            ps.setInt(5, hoatDong ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {   // Duplicate entry
                throw new TenDangNhapTrungException(tenDangNhap);
            }
            throw e;
        }
    }

    /**
     * Sửa họ tên / vai trò / trạng thái. matKhauMoi = null thì giữ nguyên mật khẩu cũ.
     * Tên đăng nhập không đổi được (hóa đơn và sự cố đang gắn với tài khoản này).
     */
    public void capNhat(long id, String hoTen, String vaiTro, boolean hoatDong, char[] matKhauMoi)
            throws SQLException, RangBuocException {
        if (id == idNguoiThucHien) {
            if (!hoatDong) {
                throw new RangBuocException("Bạn không thể khóa chính tài khoản đang đăng nhập.");
            }
            if (!VT_QUAN_LY.equals(vaiTro)) {
                throw new RangBuocException("Bạn không thể hạ quyền chính tài khoản đang đăng nhập.");
            }
        }
        String hash = (matKhauMoi == null) ? null : bamMatKhau(matKhauMoi);
        String sql = (hash == null)
                ? "UPDATE tai_khoan SET ho_ten = ?, vai_tro = ?, trang_thai = ? WHERE id = ?"
                : "UPDATE tai_khoan SET ho_ten = ?, vai_tro = ?, trang_thai = ?, mat_khau = ? WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, hoTen);
            ps.setString(i++, vaiTro);
            ps.setInt(i++, hoatDong ? 1 : 0);
            if (hash != null) ps.setString(i++, hash);
            ps.setLong(i, id);
            ps.executeUpdate();
        }
    }

    /** Khóa / mở khóa tài khoản (không xóa hẳn vì hóa đơn, sự cố vẫn tham chiếu tới tài khoản). */
    public void datTrangThai(long id, boolean hoatDong) throws SQLException, RangBuocException {
        if (id == idNguoiThucHien && !hoatDong) {
            throw new RangBuocException("Bạn không thể khóa chính tài khoản đang đăng nhập.");
        }
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE tai_khoan SET trang_thai = ? WHERE id = ?")) {
            ps.setInt(1, hoatDong ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }
}