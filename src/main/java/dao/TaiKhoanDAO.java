package dao;

import config.DBConnection;
import model.TaiKhoan;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TaiKhoanDAO {

    // Tim tai khoan theo ten dang nhap, tra ve null neu khong tim thay
    public TaiKhoan timTheoTenDangNhap(String tenDangNhap) throws SQLException {
        String sql = "SELECT id, ten_dang_nhap, mat_khau, ho_ten, vai_tro, trang_thai "
                + "FROM tai_khoan WHERE ten_dang_nhap = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tenDangNhap);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TaiKhoan tk = new TaiKhoan();
                    tk.setId(rs.getLong("id"));
                    tk.setTenDangNhap(rs.getString("ten_dang_nhap"));
                    tk.setMatKhau(rs.getString("mat_khau"));
                    tk.setHoTen(rs.getString("ho_ten"));
                    tk.setVaiTro(rs.getString("vai_tro"));
                    tk.setTrangThai(rs.getBoolean("trang_thai"));
                    return tk;
                }
                return null; // khong tim thay tai khoan nao
            }
        }
    }

    // Cap nhat thoi gian dang nhap cuoi cung (tuy chon, khong bat buoc)
    public void capNhatLanDangNhapCuoi(long id) throws SQLException {
        String sql = "UPDATE tai_khoan SET lan_dang_nhap_cuoi = NOW() WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}