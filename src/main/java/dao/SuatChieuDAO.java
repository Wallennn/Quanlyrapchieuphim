package dao;

import config.DBConnection;
import model.SuatChieu;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SuatChieuDAO {

    // Lay tat ca suat chieu cua 1 phim, trong 1 ngay cu the (vi du: hom nay)
    public List<SuatChieu> laySuatChieuTheoPhimVaNgay(String maPhim, LocalDate ngay) throws SQLException {
        List<SuatChieu> ketQua = new ArrayList<>();
        String sql = "SELECT ma_suat, ma_phim, ma_phong, ngay_chieu, gio_bat_dau, gio_ket_thuc, "
                + "gia_ve_thuong, gia_ve_vip, gia_ve_doi "
                + "FROM suat_chieu WHERE ma_phim = ? AND ngay_chieu = ? "
                + "ORDER BY gio_bat_dau";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, maPhim);
            ps.setDate(2, Date.valueOf(ngay));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SuatChieu sc = new SuatChieu();
                    sc.setMaSuat(rs.getString("ma_suat"));
                    sc.setMaPhim(rs.getString("ma_phim"));
                    sc.setMaPhong(rs.getString("ma_phong"));

                    Date ngayChieu = rs.getDate("ngay_chieu");
                    if (ngayChieu != null) {
                        sc.setNgayChieu(ngayChieu.toLocalDate());
                    }

                    Time gioBD = rs.getTime("gio_bat_dau");
                    if (gioBD != null) {
                        sc.setGioBatDau(gioBD.toLocalTime());
                    }

                    Time gioKT = rs.getTime("gio_ket_thuc");
                    if (gioKT != null) {
                        sc.setGioKetThuc(gioKT.toLocalTime());
                    }

                    sc.setGiaVeThuong(rs.getDouble("gia_ve_thuong"));
                    sc.setGiaVeVip(rs.getDouble("gia_ve_vip"));
                    sc.setGiaVeDoi(rs.getDouble("gia_ve_doi"));

                    ketQua.add(sc);
                }
            }
        }
        return ketQua;
    }
}