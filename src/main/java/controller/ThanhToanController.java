package controller;

import config.DBConnection;
import controller.BanVeController.GheDaBanException;
import dao.KhachHangDAO;
import dao.KhachHangDAO.KhachHang;
import dao.KhuyenMaiDAO;
import dao.KhuyenMaiDAO.KhuyenMai;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tinh tien va thanh toan hoa don ban ve (ve + combo) co: khuyen mai, hang thanh vien, doi diem,
 * phuong thuc thanh toan va tien thua. Moi con so deu tinh lai o phia server.
 *
 * Thu tu tinh:  Tam tinh (ve + combo)
 *               - Khuyen mai  (1 khuyen mai / hoa don: ma khach nhap, neu khong thi khuyen mai tu dong co loi nhat)
 *               - Giam theo hang thanh vien (tren phan con lai sau khuyen mai)
 *               - Doi diem (1 diem = 1.000d, toi da 50% gia tri con lai)
 *               = KHACH PHAI TRA   ->  tich 1 diem / 20.000d khach phai tra
 */
public class ThanhToanController {

    public static final int DIEM_MOI_DONG = 20_000;    // 20.000d thanh toan = 1 diem
    public static final int GIA_TRI_DIEM = 1_000;      // 1 diem = 1.000d khi dung
    public static final int TOI_DA_DOI_DIEM_PCT = 50;  // dung diem toi da 50% gia tri hoa don

    public static final String TIEN_MAT = "Tien mat", CHUYEN_KHOAN = "Chuyen khoan", THE = "The";

    /** Du lieu giao dien gui len. */
    public static class YeuCau {
        public String maSuat;
        public List<String> dsMaGhe = new ArrayList<>();
        public Map<String, Integer> combo = new LinkedHashMap<>();
        public String sdt;            // so dien thoai thanh vien (co the trong)
        public String maNhap;         // ma khuyen mai khach nhap (co the trong)
        public int diemDung;          // so diem muon dung
        public String phuongThuc = TIEN_MAT;
        public long tienKhachDua;     // chi dung khi tra tien mat
    }

    /** Ket qua tinh tien (dung de xem truoc va de lap hoa don). */
    public static class KetQuaGia {
        public String tenPhim, doTuoi;
        public long tienVe, tienCombo, tamTinh;
        public String maKm, tenKm;
        public long giamKm;
        public String thongBaoMa;              // ly do ma khach nhap khong dung duoc (null neu hop le / khong nhap)
        public KhachHang khach;                // null neu khong co / khong tim thay thanh vien
        public boolean sdtChuaDangKy;          // da nhap SDT nhung chua la thanh vien
        public int phanTramHang;
        public long giamHang;
        public int diemToiDa, diemDung;
        public long giamDiem;
        public long tongGiam, khachPhaiTra;
        public int diemSeTich;
        // gia tung dong (de ghi chi tiet hoa don)
        public final Map<String, Long> giaGhe = new LinkedHashMap<>();
        public final Map<String, Long> giaCombo = new LinkedHashMap<>();
    }

    public static class KetQuaThanhToan {
        public String maHd;
        public KetQuaGia gia;
        public long tienThua;
        public KhachHang khachSau;   // thong tin thanh vien sau khi cong/tru diem (null neu khong co)
    }

    /** Tra tien mat nhung khach dua chua du. */
    public static class TienKhachDuaKhongDuException extends Exception {
        public TienKhachDuaKhongDuException(long can) {
            super("Tiền khách đưa chưa đủ (cần tối thiểu " + String.format("%,d", can).replace(',', '.') + "đ)");
        }
    }

    private final KhachHangDAO khachHangDao = new KhachHangDAO();

    // ===================== THANH VIEN =====================
    public KhachHang timKhach(String sdt) throws SQLException {
        return khachHangDao.timTheoSdt(sdt);
    }

    public KhachHang dangKyNhanh(String sdt, String hoTen) throws SQLException, KhachHangDAO.SdtTrungException {
        return khachHangDao.them(sdt, hoTen);
    }

    // ===================== TINH TIEN =====================
    public KetQuaGia tinhGia(YeuCau y) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return tinh(c, y, false);
        }
    }

    private static long lamTronNghin(long v) {
        return (v / 1000) * 1000;
    }

    private KetQuaGia tinh(Connection c, YeuCau y, boolean khoaKhach) throws SQLException {
        if (y.dsMaGhe == null || y.dsMaGhe.isEmpty()) throw new SQLException("Chưa chọn ghế nào");
        KetQuaGia g = new KetQuaGia();

        // ----- Suat chieu -----
        String maPhong;
        LocalDate ngayChieu;
        long giaThuong, giaVip, giaDoi;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT s.ma_phong, s.ngay_chieu, s.gia_ve_thuong, s.gia_ve_vip, s.gia_ve_doi, p.ten_phim, p.do_tuoi "
                + "FROM suat_chieu s JOIN phim p ON p.ma_phim = s.ma_phim WHERE s.ma_suat = ?")) {
            ps.setString(1, y.maSuat);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Suất chiếu không tồn tại");
                maPhong = rs.getString("ma_phong");
                ngayChieu = rs.getDate("ngay_chieu").toLocalDate();
                giaThuong = rs.getLong("gia_ve_thuong");
                giaVip = rs.getLong("gia_ve_vip");
                giaDoi = rs.getLong("gia_ve_doi");
                g.tenPhim = rs.getString("ten_phim");
                g.doTuoi = rs.getString("do_tuoi");
            }
        }

        // ----- Ve: chi nhan ghe thuoc dung phong cua suat -----
        Map<String, String> loaiGhe = new HashMap<>();
        String dauHoi = String.join(",", Collections.nCopies(y.dsMaGhe.size(), "?"));
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT ma_ghe, loai_ghe FROM ghe WHERE ma_phong = ? AND ma_ghe IN (" + dauHoi + ")")) {
            ps.setString(1, maPhong);
            for (int i = 0; i < y.dsMaGhe.size(); i++) ps.setString(i + 2, y.dsMaGhe.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) loaiGhe.put(rs.getString(1), rs.getString(2));
            }
        }
        for (String ma : y.dsMaGhe) {
            String loai = loaiGhe.get(ma);
            if (loai == null) throw new SQLException("Ghế không hợp lệ: " + ma);
            long gia = "VIP".equals(loai) ? giaVip : ("Doi".equals(loai) ? giaDoi : giaThuong);
            g.giaGhe.put(ma, gia);
            g.tienVe += gia;
        }

        // ----- Combo -----
        if (y.combo != null) {
            for (Map.Entry<String, Integer> e : y.combo.entrySet()) {
                if (e.getValue() == null || e.getValue() <= 0) continue;
                try (PreparedStatement ps = c.prepareStatement("SELECT gia FROM combo WHERE ma_combo = ?")) {
                    ps.setString(1, e.getKey());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Combo không tồn tại: " + e.getKey());
                        long gia = rs.getLong(1);
                        g.giaCombo.put(e.getKey(), gia);
                        g.tienCombo += gia * e.getValue();
                    }
                }
            }
        }
        g.tamTinh = g.tienVe + g.tienCombo;

        // ----- Khuyen mai (toi da 1 / hoa don) -----
        apDungKhuyenMai(c, y, g, ngayChieu);
        long conLai = g.tamTinh - g.giamKm;

        // ----- Thanh vien -----
        String sdt = (y.sdt == null) ? "" : y.sdt.trim();
        if (!sdt.isEmpty()) {
            String sql = "SELECT id, so_dien_thoai, ho_ten, hang, diem, tong_chi_tieu, ngay_tao FROM khach_hang "
                    + "WHERE so_dien_thoai = ?" + (khoaKhach ? " FOR UPDATE" : "");
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, sdt);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) g.khach = KhachHangDAO.doc(rs);
                    else g.sdtChuaDangKy = true;
                }
            }
        }
        if (g.khach != null) {
            g.phanTramHang = KhachHangDAO.phanTramGiam(g.khach.hang);
            g.giamHang = lamTronNghin(conLai * g.phanTramHang / 100);
            conLai -= g.giamHang;

            long toiDaTheoGiaTri = (conLai * TOI_DA_DOI_DIEM_PCT / 100) / GIA_TRI_DIEM;
            g.diemToiDa = (int) Math.max(0, Math.min(g.khach.diem, toiDaTheoGiaTri));
            g.diemDung = Math.max(0, Math.min(y.diemDung, g.diemToiDa));
            g.giamDiem = (long) g.diemDung * GIA_TRI_DIEM;
            conLai -= g.giamDiem;
        }

        g.khachPhaiTra = Math.max(0, conLai);
        g.tongGiam = g.tamTinh - g.khachPhaiTra;
        g.diemSeTich = (g.khach != null) ? (int) (g.khachPhaiTra / DIEM_MOI_DONG) : 0;
        return g;
    }

    // ----- Khuyen mai -----
    /**
     * Neu khach nhap ma hop le thi dung ma do. Ma sai / het han thi ghi ly do vao thongBaoMa
     * va van ap dung khuyen mai tu dong co loi nhat (neu co).
     */
    private void apDungKhuyenMai(Connection c, YeuCau y, KetQuaGia g, LocalDate ngayChieu) throws SQLException {
        String ma = (y.maNhap == null) ? "" : y.maNhap.trim();
        KhuyenMai chon = null;
        long giamChon = 0;

        if (!ma.isEmpty()) {
            KhuyenMai km = KhuyenMaiDAO.timTheoMaNhap(c, ma);
            if (km == null) {
                g.thongBaoMa = "Mã không tồn tại";
            } else {
                String lyDo = kiemTra(km, ngayChieu, g.tamTinh);
                if (lyDo != null) {
                    g.thongBaoMa = lyDo;
                } else {
                    chon = km;
                    giamChon = tinhGiam(km, g.tienVe, g.tamTinh);
                }
            }
        }
        if (chon == null) {   // khong nhap ma, hoac ma khong dung duoc -> khuyen mai tu dong co loi nhat
            for (KhuyenMai km : KhuyenMaiDAO.layTuDong(c)) {
                if (kiemTra(km, ngayChieu, g.tamTinh) != null) continue;
                long giam = tinhGiam(km, g.tienVe, g.tamTinh);
                if (giam > giamChon) {
                    chon = km;
                    giamChon = giam;
                }
            }
        }
        if (chon != null && giamChon > 0) {
            g.maKm = chon.ma;
            g.tenKm = chon.ten;
            g.giamKm = giamChon;
        }
    }

    /** Tra ve ly do khong ap dung duoc, hoac null neu hop le. */
    private String kiemTra(KhuyenMai km, LocalDate ngayChieu, long tamTinh) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (!km.hoatDong) return "Khuyến mãi đang tạm dừng";
        if (ngayChieu.isBefore(km.tuNgay) || ngayChieu.isAfter(km.denNgay)) {
            return "Chỉ áp dụng cho suất chiếu từ " + km.tuNgay.format(f) + " đến " + km.denNgay.format(f);
        }
        if (!km.apDungThu(ngayChieu.getDayOfWeek().getValue())) {
            return "Chỉ áp dụng cho suất chiếu vào: " + KhuyenMaiDAO.moTaThu(km.ngayTrongTuan);
        }
        if (tamTinh < km.donToiThieu) {
            return "Hóa đơn tối thiểu " + String.format("%,d", km.donToiThieu).replace(',', '.') + "đ";
        }
        if (km.gioiHan != null && km.daDung >= km.gioiHan) return "Khuyến mãi đã hết lượt sử dụng";
        return null;
    }

    private long tinhGiam(KhuyenMai km, long tienVe, long tamTinh) {
        long nen = KhuyenMaiDAO.CHO_TONG.equals(km.apDungCho) ? tamTinh : tienVe;
        long giam;
        if (KhuyenMaiDAO.PHAN_TRAM.equals(km.loaiGiam)) {
            giam = lamTronNghin(nen * km.giaTri / 100);
            if (km.giamToiDa != null) giam = Math.min(giam, km.giamToiDa);
        } else {
            giam = km.giaTri;
        }
        return Math.max(0, Math.min(giam, nen));
    }

    // ===================== THANH TOAN =====================
    /**
     * Lap hoa don trong 1 giao dich (loi thi hoan tac het): hoa don + ve + combo, tru/cong diem thanh vien.
     */
    public KetQuaThanhToan thanhToan(YeuCau y, long idNhanVien)
            throws SQLException, GheDaBanException, TienKhachDuaKhongDuException {
        String pt = (y.phuongThuc == null) ? TIEN_MAT : y.phuongThuc;
        if (!TIEN_MAT.equals(pt) && !CHUYEN_KHOAN.equals(pt) && !THE.equals(pt)) {
            throw new SQLException("Phương thức thanh toán không hợp lệ");
        }
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                KetQuaGia g = tinh(c, y, true);
                String sdt = (y.sdt == null) ? "" : y.sdt.trim();
                if (!sdt.isEmpty() && g.khach == null) {
                    throw new SQLException("Số điện thoại " + sdt + " chưa đăng ký thành viên");
                }
                if (TIEN_MAT.equals(pt) && y.tienKhachDua < g.khachPhaiTra) {
                    throw new TienKhachDuaKhongDuException(g.khachPhaiTra);
                }

                String maHd = "HD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO hoa_don (ma_hd, ma_suat, id_tai_khoan, ngay_lap, tong_tien, trang_thai, "
                        + "phuong_thuc, tien_khach_dua, giam_gia, ma_km, ma_khach_hang, diem_su_dung, diem_tich_luy) "
                        + "VALUES (?, ?, ?, NOW(), ?, 'Da thanh toan', ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, maHd);
                    ps.setString(2, y.maSuat);
                    ps.setLong(3, idNhanVien);
                    ps.setLong(4, g.khachPhaiTra);
                    ps.setString(5, pt);
                    if (TIEN_MAT.equals(pt)) ps.setLong(6, y.tienKhachDua); else ps.setNull(6, java.sql.Types.DECIMAL);
                    ps.setLong(7, g.tongGiam);
                    ps.setString(8, g.maKm);
                    if (g.khach != null) ps.setLong(9, g.khach.id); else ps.setNull(9, java.sql.Types.BIGINT);
                    ps.setInt(10, g.diemDung);
                    ps.setInt(11, g.diemSeTich);
                    ps.executeUpdate();
                }

                // Ghe: rang buoc UNIQUE (ma_suat, ma_ghe) chan ban trung ghe
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO chi_tiet_hoa_don_ghe (ma_hd, ma_ghe, ma_suat, gia_ve) VALUES (?, ?, ?, ?)")) {
                    for (Map.Entry<String, Long> e : g.giaGhe.entrySet()) {
                        ps.setString(1, maHd);
                        ps.setString(2, e.getKey());
                        ps.setString(3, y.maSuat);
                        ps.setLong(4, e.getValue());
                        ps.executeUpdate();
                    }
                }
                if (!g.giaCombo.isEmpty()) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO chi_tiet_hoa_don_combo (ma_hd, ma_combo, so_luong, don_gia) VALUES (?, ?, ?, ?)")) {
                        for (Map.Entry<String, Long> e : g.giaCombo.entrySet()) {
                            ps.setString(1, maHd);
                            ps.setString(2, e.getKey());
                            ps.setInt(3, y.combo.get(e.getKey()));
                            ps.setLong(4, e.getValue());
                            ps.executeUpdate();
                        }
                    }
                }

                KhachHang sau = null;
                if (g.khach != null) {
                    long tongMoi = g.khach.tongChiTieu + g.khachPhaiTra;
                    int diemMoi = g.khach.diem - g.diemDung + g.diemSeTich;
                    String hangMoi = KhachHangDAO.hangTheoChiTieu(tongMoi);
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE khach_hang SET diem = ?, tong_chi_tieu = ?, hang = ? WHERE id = ?")) {
                        ps.setInt(1, diemMoi);
                        ps.setLong(2, tongMoi);
                        ps.setString(3, hangMoi);
                        ps.setLong(4, g.khach.id);
                        ps.executeUpdate();
                    }
                    sau = new KhachHang();
                    sau.id = g.khach.id;
                    sau.soDienThoai = g.khach.soDienThoai;
                    sau.hoTen = g.khach.hoTen;
                    sau.diem = diemMoi;
                    sau.tongChiTieu = tongMoi;
                    sau.hang = hangMoi;
                }

                c.commit();
                KetQuaThanhToan kq = new KetQuaThanhToan();
                kq.maHd = maHd;
                kq.gia = g;
                kq.tienThua = TIEN_MAT.equals(pt) ? y.tienKhachDua - g.khachPhaiTra : 0;
                kq.khachSau = sau;
                return kq;
            } catch (SQLException e) {
                c.rollback();
                if (e.getErrorCode() == 1062) throw new GheDaBanException();
                throw e;
            } catch (TienKhachDuaKhongDuException e) {
                c.rollback();
                throw e;
            }
        }
    }
}