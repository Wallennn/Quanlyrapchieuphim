package controller;

import config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/** Do tuoi phim (P, K, T13, T16, T18) - tra cuu theo ten phim de hien thi tren cac man hinh. */
public class DoTuoiController {

    public Map<String, String> layTheoTenPhim() throws SQLException {
        Map<String, String> kq = new HashMap<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT ten_phim, do_tuoi FROM phim");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) kq.put(rs.getString(1), rs.getString(2));
        }
        return kq;
    }

    /** Mo ta theo quy dinh phan loai phim Viet Nam. */
    public static String moTa(String doTuoi) {
        if (doTuoi == null) return "";
        switch (doTuoi) {
            case "P":   return "P - Phim phổ biến, mọi độ tuổi";
            case "K":   return "K - Dưới 13 tuổi phải có người bảo hộ đi cùng";
            case "T13": return "T13 - Cấm khán giả dưới 13 tuổi";
            case "T16": return "T16 - Cấm khán giả dưới 16 tuổi";
            case "T18": return "T18 - Cấm khán giả dưới 18 tuổi";
            default:    return doTuoi;
        }
    }

    /** Phim T16/T18 can nhan vien kiem tra giay to khi ban ve. */
    public static boolean canKiemTraTuoi(String doTuoi) {
        return "T16".equals(doTuoi) || "T18".equals(doTuoi);
    }
}