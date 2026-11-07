package view;

import dao.KhachHangDAO;
import model.ComboIn;
import model.HoaDonBapNuoc;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Ve (xem truoc + in) 1 phieu / hoa don BAP NUOC: tieu de, thong tin hoa don, (thong tin khach hang), cac dong combo, tong tien.
// Dung chung cho tab "In ve + bap nuoc" (phieu di kem ve) va tab "Bap nuoc rieng" (hoa don co thong tin khach hang).
final class PhieuBapNuoc {

    private static final int RONG = InVeKemBapNuocPanel.VE_RONG;
    private static final int CAO_TIEU_DE = 32;
    private static final int DONG_TT = 18;     // chieu cao 1 dong thong tin
    private static final int DONG_CB = 20;     // chieu cao 1 dong combo

    private PhieuBapNuoc() {
    }

    private static int soDongThongTin(boolean hienKhach, boolean thanhVien) {
        return 2 + (hienKhach ? 1 : 0) + (hienKhach && thanhVien ? 1 : 0);
    }

    static int chieuCao(int soCombo, boolean hienKhach, boolean thanhVien) {
        int top = CAO_TIEU_DE + 12 + soDongThongTin(hienKhach, thanhVien) * DONG_TT + 8;
        int cuoiDong = top + 20 + soCombo * DONG_CB;
        return cuoiDong + 8 + 32 + 10;
    }

    // nhanPhai: chu goc phai thanh tieu de; hienKhach: co hien dong thong tin khach hang khong; kh: thong tin khach (co the null = khach le)
    static void ve(Graphics2D g0, int x, int y, String nhanPhai, String maHd, LocalDateTime ngayLap, String tenNv,
                   boolean hienKhach, HoaDonBapNuoc kh, List<ComboIn> combo) {
        boolean thanhVien = hienKhach && kh != null && kh.laThanhVien();
        int h = chieuCao(combo.size(), hienKhach, thanhVien);

        Graphics2D g = (Graphics2D) g0.create();
        g.translate(x, y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Shape khung = new RoundRectangle2D.Float(0.5f, 0.5f, RONG - 1, h - 1, 16, 16);
        g.setColor(Color.WHITE);
        g.fill(khung);

        Shape clipCu = g.getClip();
        g.clip(khung);
        g.setColor(InVeKemBapNuocPanel.MAU_NEN_HEADER);
        g.fillRect(0, 0, RONG, CAO_TIEU_DE);
        g.setClip(clipCu);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g.drawString("RẠP CHIẾU PHIM", 14, 21);
        g.setColor(InVeKemBapNuocPanel.MAU_NAU_NHAT);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g.drawString(nhanPhai, RONG - 14 - g.getFontMetrics().stringWidth(nhanPhai), 21);

        g.setColor(InVeKemBapNuocPanel.MAU_NAU_NHAT);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(khung);

        // --- thong tin hoa don / khach hang ---
        int by = CAO_TIEU_DE + 12 + 13;
        dong(g, "Mã HĐ", maHd, by, true);
        by += DONG_TT;
        String lap = (ngayLap != null ? ngayLap.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "")
                + (tenNv != null ? "  •  NV: " + tenNv : "");
        dong(g, "Lập lúc", InVeKemBapNuocPanel.catChu(lap, g.getFontMetrics(new Font("Segoe UI", Font.PLAIN, 12)), RONG - 14 - 98), by, false);
        by += DONG_TT;
        if (hienKhach) {
            if (thanhVien) {
                dong(g, "Khách hàng", InVeKemBapNuocPanel.catChu(kh.tenKhach() + "  •  " + kh.soDienThoai(),
                        g.getFontMetrics(new Font("Segoe UI", Font.BOLD, 12)), RONG - 14 - 98), by, true);
                by += DONG_TT;
                dong(g, "Tích điểm", "Hạng " + KhachHangDAO.tenHang(kh.hang()) + "  •  +" + kh.diemTichHd()
                        + " điểm  •  hiện có " + kh.diemHienCo(), by, false);
            } else {
                dong(g, "Khách hàng", "Khách lẻ (không tích điểm)", by, false);
            }
        }

        // --- bang combo ---
        int top = CAO_TIEU_DE + 12 + soDongThongTin(hienKhach, thanhVien) * DONG_TT + 8;
        int xTien = RONG - 14, xSl = RONG - 118;
        g.setColor(InVeKemBapNuocPanel.MAU_NAU_NHAT);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
        g.drawLine(14, top, RONG - 14, top);

        g.setColor(InVeKemBapNuocPanel.MAU_CHU_XAM);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g.drawString("Combo", 14, top + 15);
        phai(g, "SL", xSl, top + 15);
        phai(g, "Thành tiền", xTien, top + 15);

        int cy = top + 20;
        double tong = 0;
        Font fThuong = new Font("Segoe UI", Font.PLAIN, 12);
        for (ComboIn c : combo) {
            g.setColor(InVeKemBapNuocPanel.MAU_CHU_DAM);
            g.setFont(fThuong);
            g.drawString(InVeKemBapNuocPanel.catChu(c.tenCombo(), g.getFontMetrics(), xSl - 40 - 14), 14, cy + 15);
            phai(g, "x" + c.soLuong(), xSl, cy + 15);
            phai(g, InVeKemBapNuocPanel.dinhDangTien(c.thanhTien()) + "đ", xTien, cy + 15);
            tong += c.thanhTien();
            cy += DONG_CB;
        }

        g.setColor(InVeKemBapNuocPanel.MAU_NAU_NHAT);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
        g.drawLine(14, cy + 4, RONG - 14, cy + 4);

        g.setColor(InVeKemBapNuocPanel.MAU_CHU_XAM);
        g.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g.drawString("Tổng cộng", 14, cy + 8 + 22);
        g.setColor(InVeKemBapNuocPanel.MAU_NAU_TOI);
        g.setFont(new Font("Segoe UI", Font.BOLD, 18));
        phai(g, InVeKemBapNuocPanel.dinhDangTien(tong) + "đ", xTien, cy + 8 + 22);

        g.dispose();
    }

    private static void dong(Graphics2D g, String nhan, String giaTri, int y, boolean dam) {
        g.setColor(InVeKemBapNuocPanel.MAU_CHU_XAM);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g.drawString(nhan + ":", 14, y);
        g.setColor(InVeKemBapNuocPanel.MAU_CHU_DAM);
        g.setFont(new Font("Segoe UI", dam ? Font.BOLD : Font.PLAIN, 12));
        g.drawString(giaTri, 14 + 84, y);
    }

    private static void phai(Graphics2D g, String chu, int xPhai, int y) {
        g.drawString(chu, xPhai - g.getFontMetrics().stringWidth(chu), y);
    }
}