package controller;

import dao.PhimDAO;
import dao.SuatChieuDAO;
import model.Phim;
import model.SuatChieu;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhimController {

    private final PhimDAO phimDAO = new PhimDAO();
    private final SuatChieuDAO suatChieuDAO = new SuatChieuDAO();

    // Phim co suat chieu HOM NAY (chi giu phim that su co suat trong ngay)
    public Map<Phim, List<SuatChieu>> layDanhSachPhimVaSuatChieuHomNay() throws SQLException {
        return layPhimVaSuatChieuTheoNgay(LocalDate.now());
    }

    // Phim co suat chieu NGAY MAI (dung de hien muc "De xuat")
    public Map<Phim, List<SuatChieu>> layDanhSachPhimVaSuatChieuNgayMai() throws SQLException {
        return layPhimVaSuatChieuTheoNgay(LocalDate.now().plusDays(1));
    }

    // Ham dung chung: lay phim + suat chieu cua 1 ngay bat ky.
    // Phim nao khong co suat chieu trong ngay do thi bo qua (khong dua vao ket qua).
    private Map<Phim, List<SuatChieu>> layPhimVaSuatChieuTheoNgay(LocalDate ngay) throws SQLException {
        Map<Phim, List<SuatChieu>> ketQua = new LinkedHashMap<>();

        List<Phim> danhSachPhim = phimDAO.layDanhSachDangChieu();

        for (Phim phim : danhSachPhim) {
            List<SuatChieu> cacSuatChieu = suatChieuDAO.laySuatChieuTheoPhimVaNgay(phim.getMaPhim(), ngay);
            if (!cacSuatChieu.isEmpty()) {
                ketQua.put(phim, cacSuatChieu);
            }
        }

        return ketQua;
    }
}