package dao;

import config.DBConnection;
import model.Phim;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PhimDAO {

    // Lay danh sach phim dang co trang thai "Dang chieu"
    public List<Phim> layDanhSachDangChieu() throws SQLException {
        List<Phim> ketQua = new ArrayList<>();
        String sql = "SELECT ma_phim, ten_phim, the_loai, thoi_luong, ngay_khoi_chieu, "
                + "dao_dien, mo_ta, poster, trang_thai "
                + "FROM phim WHERE trang_thai = 'Dang chieu' ORDER BY ten_phim";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ketQua.add(mapRowToPhim(rs));
            }
        }
        return ketQua;
    }

    // Ham tien ich: chuyen 1 dong ket qua truy van (ResultSet) thanh 1 doi tuong Phim
    // Tach rieng ra ham nay de tai su dung, khong phai copy-paste code cho moi truy van
    private Phim mapRowToPhim(ResultSet rs) throws SQLException {
        Phim p = new Phim();
        p.setMaPhim(rs.getString("ma_phim"));
        p.setTenPhim(rs.getString("ten_phim"));
        p.setTheLoai(rs.getString("the_loai"));
        p.setThoiLuong(rs.getInt("thoi_luong"));

        Date ngayKC = rs.getDate("ngay_khoi_chieu");
        if (ngayKC != null) {
            p.setNgayKhoiChieu(ngayKC.toLocalDate());
        }

        p.setDaoDien(rs.getString("dao_dien"));
        p.setMoTa(rs.getString("mo_ta"));
        p.setPoster(rs.getString("poster"));
        p.setTrangThai(rs.getString("trang_thai"));
        return p;
    }
}