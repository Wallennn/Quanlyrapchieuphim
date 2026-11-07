package view;

import dao.BanComboDAO;
import dao.BanComboDAO.DongCombo;
import dao.BanComboDAO.ChuaDangKyException;
import dao.BanComboDAO.HoaDonCombo;
import dao.KhachHangDAO;
import dao.KhachHangDAO.KhachHang;
import dao.KhachHangDAO.SdtTrungException;
import model.ComboBan;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.net.URL;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Man hinh "Combo bap nuoc" (ban le, khong kem ve): chon combo + so luong -> (tuy chon) nhap SDT de tich diem -> thanh toan -> in hoa don
public class BanComboPanel extends JPanel {

    private static final Color MAU_NAU_DAM    = new Color(0x8A, 0x6D, 0x4A);
    private static final Color MAU_NAU_NHAT   = new Color(0xB2, 0x90, 0x6B);
    private static final Color MAU_NAU_TOI    = new Color(0x6B, 0x52, 0x3B);
    private static final Color MAU_NEN_KEM    = new Color(0xFB, 0xF6, 0xEF);
    private static final Color MAU_CHU_XAM    = new Color(0x8A, 0x87, 0x82);
    private static final Color MAU_CHU_DAM    = new Color(0x2E, 0x2A, 0x26);
    private static final Color MAU_VIEN       = new Color(0xEA, 0xDF, 0xCF);
    private static final Color MAU_VANG       = new Color(0xE0, 0x9B, 0x2D);

    private static final int SO_LUONG_TOI_DA = 20;   // moi combo toi da 20 phan trong 1 don

    private final BanComboDAO dao;
    private final long idNhanVien;

    private List<ComboBan> cacCombo = new ArrayList<>();
    private final Map<String, Integer> soLuong = new LinkedHashMap<>();   // ma combo -> so luong dang chon
    private final Map<String, TheCombo> cacThe = new HashMap<>();

    private final JPanel panelDanhSachCombo = new JPanel();
    private final JTextField txtSdt = new JTextField();
    private final JButton btnTim = new JButton("Kiểm tra");
    private final JButton btnDangKy = new JButton("Đăng ký thành viên");
    private final JLabel lblKhach = new JLabel(" ");
    private final JLabel lblDiemDuKien = new JLabel(" ");
    private KhachHang khachDangDung;          // thanh vien da kiem tra (null neu khach vang lai / chua kiem tra)
    private String sdtDaKiemTra;              // SDT da bam "Kiem tra" (de biet khi nao nguoi dung sua lai so)
    private final JPanel panelDon = new JPanel();
    private final JLabel lblTong = new JLabel("0đ");
    private final JButton btnThanhToan = new JButton("Thanh toán");
    private final JButton btnXoaDon = new JButton("Xóa đơn");

    public BanComboPanel(long idNhanVien) {
        this(idNhanVien, new BanComboDAO());
    }

    BanComboPanel(long idNhanVien, BanComboDAO dao) {
        this.idNhanVien = idNhanVien;
        this.dao = dao;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        JLabel lblTieuDe = new JLabel("Combo bắp nước");
        lblTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTieuDe.setForeground(MAU_CHU_DAM);
        add(lblTieuDe, BorderLayout.NORTH);

        JPanel giua = new JPanel(new GridBagLayout());
        giua.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.gridy = 0;
        g.weighty = 1.0;
        g.gridx = 0;
        g.weightx = 0.62;
        g.insets = new Insets(0, 0, 0, 20);
        giua.add(taoPanelDanhSachCombo(), g);
        g.gridx = 1;
        g.weightx = 0.38;
        g.insets = new Insets(0, 0, 0, 0);
        giua.add(taoPanelDonHang(), g);
        add(giua, BorderLayout.CENTER);

        taiDuLieu();
        capNhatDon();
    }

    // ===== Ben trai: danh sach combo =====
    private JPanel taoPanelDanhSachCombo() {
        JPanel the = new JPanel(new BorderLayout(0, 10));
        the.setBackground(Color.WHITE);
        the.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        the.setPreferredSize(new Dimension(620, 100));

        JLabel lblTieuDe = new JLabel("Chọn combo và số lượng");
        lblTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTieuDe.setForeground(MAU_CHU_DAM);
        the.add(lblTieuDe, BorderLayout.NORTH);

        panelDanhSachCombo.setLayout(new BoxLayout(panelDanhSachCombo, BoxLayout.Y_AXIS));
        panelDanhSachCombo.setBackground(Color.WHITE);

        // Boc trong panel BorderLayout.NORTH de cac the khong bi gian ra khi it combo
        JPanel vungCuon = new JPanel(new BorderLayout());
        vungCuon.setBackground(Color.WHITE);
        vungCuon.add(panelDanhSachCombo, BorderLayout.NORTH);

        JScrollPane cuon = new JScrollPane(vungCuon);
        cuon.setBorder(null);
        cuon.getViewport().setBackground(Color.WHITE);
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        the.add(cuon, BorderLayout.CENTER);
        return the;
    }

    // ===== Ben phai: don hang + thanh toan =====
    private JPanel taoPanelDonHang() {
        JPanel the = new JPanel(new BorderLayout(0, 12));
        the.setBackground(Color.WHITE);
        the.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        the.setPreferredSize(new Dimension(380, 100));

        // --- Dau: tieu de + chon suat ---
        JPanel dau = new JPanel();
        dau.setOpaque(false);
        dau.setLayout(new BoxLayout(dau, BoxLayout.Y_AXIS));

        JLabel lblTieuDe = new JLabel("Đơn hàng");
        lblTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTieuDe.setForeground(MAU_CHU_DAM);
        lblTieuDe.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSdt = new JLabel("Số điện thoại thành viên (nếu có) để tích điểm:");
        lblSdt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSdt.setForeground(MAU_CHU_XAM);
        lblSdt.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtSdt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSdt.putClientProperty("JTextField.placeholderText", "Ví dụ: 0901234567");
        btnTim.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnTim.setFocusPainted(false);
        btnTim.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTim.addActionListener(e -> timThanhVien());
        txtSdt.addActionListener(e -> timThanhVien());
        txtSdt.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { sdtDaDoi(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { sdtDaDoi(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { sdtDaDoi(); }
        });
        JPanel dongSdt = new JPanel(new BorderLayout(8, 0));
        dongSdt.setOpaque(false);
        dongSdt.setAlignmentX(Component.LEFT_ALIGNMENT);
        dongSdt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        dongSdt.add(txtSdt, BorderLayout.CENTER);
        dongSdt.add(btnTim, BorderLayout.EAST);

        lblKhach.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblKhach.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnDangKy.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDangKy.setFocusPainted(false);
        btnDangKy.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDangKy.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDangKy.setVisible(false);
        btnDangKy.addActionListener(e -> dangKyNhanh());

        dau.add(lblTieuDe);
        dau.add(Box.createVerticalStrut(10));
        dau.add(lblSdt);
        dau.add(Box.createVerticalStrut(4));
        dau.add(dongSdt);
        dau.add(Box.createVerticalStrut(4));
        dau.add(lblKhach);
        dau.add(Box.createVerticalStrut(2));
        dau.add(btnDangKy);
        the.add(dau, BorderLayout.NORTH);

        // --- Giua: cac dong da chon ---
        panelDon.setLayout(new BoxLayout(panelDon, BoxLayout.Y_AXIS));
        panelDon.setBackground(MAU_NEN_KEM);
        panelDon.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JPanel vungDon = new JPanel(new BorderLayout());
        vungDon.setBackground(MAU_NEN_KEM);
        vungDon.add(panelDon, BorderLayout.NORTH);
        JScrollPane cuon = new JScrollPane(vungDon);
        cuon.setBorder(BorderFactory.createLineBorder(MAU_VIEN, 1));
        cuon.getViewport().setBackground(MAU_NEN_KEM);
        the.add(cuon, BorderLayout.CENTER);

        // --- Duoi: tong tien + nut ---
        JPanel duoi = new JPanel();
        duoi.setOpaque(false);
        duoi.setLayout(new BoxLayout(duoi, BoxLayout.Y_AXIS));

        JLabel lblNhanTong = new JLabel("Tổng cộng");
        lblNhanTong.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblNhanTong.setForeground(MAU_CHU_XAM);
        lblTong.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTong.setForeground(MAU_NAU_TOI);

        JPanel dongTong = new JPanel(new BorderLayout());
        dongTong.setOpaque(false);
        dongTong.add(lblNhanTong, BorderLayout.WEST);
        dongTong.add(lblTong, BorderLayout.EAST);
        dongTong.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnXoaDon.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnXoaDon.setFocusPainted(false);
        btnXoaDon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnXoaDon.addActionListener(e -> xoaDon());

        btnThanhToan.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnThanhToan.setForeground(Color.WHITE);
        btnThanhToan.setBackground(MAU_NAU_TOI);
        btnThanhToan.setFocusPainted(false);
        btnThanhToan.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnThanhToan.addActionListener(this::thanhToan);

        JPanel dongNut = new JPanel(new GridBagLayout());
        dongNut.setOpaque(false);
        dongNut.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.gridy = 0;
        g.ipady = 10;
        g.gridx = 0;
        g.weightx = 0.35;
        g.insets = new Insets(0, 0, 0, 10);
        dongNut.add(btnXoaDon, g);
        g.gridx = 1;
        g.weightx = 0.65;
        g.insets = new Insets(0, 0, 0, 0);
        dongNut.add(btnThanhToan, g);

        lblDiemDuKien.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDiemDuKien.setForeground(new Color(0x2E, 0x7D, 0x32));
        lblDiemDuKien.setAlignmentX(Component.LEFT_ALIGNMENT);

        duoi.add(lblDiemDuKien);
        duoi.add(Box.createVerticalStrut(4));
        duoi.add(dongTong);
        duoi.add(Box.createVerticalStrut(12));
        duoi.add(dongNut);
        the.add(duoi, BorderLayout.SOUTH);
        return the;
    }

    // ===== Nap du lieu tu database =====
    private void taiDuLieu() {
        try {
            cacCombo = dao.layDanhSachCombo();
        } catch (SQLException e) {
            cacCombo = new ArrayList<>();
            JLabel lblLoi = new JLabel("Lỗi khi tải danh sách combo: " + e.getMessage());
            lblLoi.setForeground(Color.RED);
            panelDanhSachCombo.add(lblLoi);
        }
        for (ComboBan c : cacCombo) {
            soLuong.put(c.maCombo(), 0);
            TheCombo the = new TheCombo(c);
            cacThe.put(c.maCombo(), the);
            panelDanhSachCombo.add(the);
            panelDanhSachCombo.add(Box.createVerticalStrut(12));
        }
        if (cacCombo.isEmpty() && panelDanhSachCombo.getComponentCount() == 0) {
            JLabel lblRong = new JLabel("Chưa có combo nào trong cơ sở dữ liệu.");
            lblRong.setForeground(MAU_CHU_XAM);
            panelDanhSachCombo.add(lblRong);
        }
    }

    // ===== Thay doi so luong =====
    private void doiSoLuong(String maCombo, int chenhLech) {
        int moi = Math.max(0, Math.min(SO_LUONG_TOI_DA, soLuong.get(maCombo) + chenhLech));
        soLuong.put(maCombo, moi);
        cacThe.get(maCombo).datSoLuong(moi);
        capNhatDon();
    }

    private void xoaDon() {
        for (String ma : soLuong.keySet()) {
            soLuong.put(ma, 0);
            cacThe.get(ma).datSoLuong(0);
        }
        capNhatDon();
    }

    // Ve lai phan "Don hang" ben phai + tong tien + trang thai nut
    private void capNhatDon() {
        panelDon.removeAll();
        double tong = 0;
        int soDong = 0;

        for (ComboBan c : cacCombo) {
            int sl = soLuong.getOrDefault(c.maCombo(), 0);
            if (sl <= 0) continue;
            soDong++;
            tong += c.gia() * sl;
            panelDon.add(taoDongDon(c, sl));
            panelDon.add(Box.createVerticalStrut(8));
        }
        if (soDong == 0) {
            JLabel lbl = new JLabel("Chưa chọn combo nào.");
            lbl.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            lbl.setForeground(MAU_CHU_XAM);
            panelDon.add(lbl);
        }

        lblTong.setText(dinhDangTien(tong) + "đ");
        boolean coHang = soDong > 0;
        btnXoaDon.setEnabled(coHang);
        boolean sdtOk = txtSdt.getText().trim().isEmpty() || khachDangDung != null;   // co nhap SDT thi phai kiem tra xong
        btnThanhToan.setEnabled(coHang && sdtOk);
        if (khachDangDung != null && coHang) {
            lblDiemDuKien.setText("Tích điểm hóa đơn này: +" + BanComboDAO.diemSeTich(tong) + " điểm");
        } else {
            lblDiemDuKien.setText(" ");
        }

        panelDon.revalidate();
        panelDon.repaint();
    }

    private JPanel taoDongDon(ComboBan c, int sl) {
        JPanel dong = new JPanel(new BorderLayout(8, 0));
        dong.setOpaque(false);
        dong.setAlignmentX(Component.LEFT_ALIGNMENT);
        dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel lblTen = new JLabel("<html><b>" + thoatHtml(c.tenCombo()) + "</b><br><font color='#8A8782'>"
                + sl + " × " + dinhDangTien(c.gia()) + "đ</font></html>");
        lblTen.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblTien = new JLabel(dinhDangTien(c.gia() * sl) + "đ");
        lblTien.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTien.setForeground(MAU_CHU_DAM);

        dong.add(lblTen, BorderLayout.CENTER);
        dong.add(lblTien, BorderLayout.EAST);
        return dong;
    }

    // ===== Thanh vien (tich diem) =====
    /** Nguoi dung go/sua SDT -> bat buoc kiem tra lai. */
    private void sdtDaDoi() {
        String s = txtSdt.getText().trim();
        if (khachDangDung != null || sdtDaKiemTra != null) {
            if (!s.equals(sdtDaKiemTra)) {
                khachDangDung = null;
                sdtDaKiemTra = null;
                btnDangKy.setVisible(false);
                lblKhach.setText(" ");
            }
        }
        if (!s.isEmpty() && khachDangDung == null) {
            lblKhach.setForeground(MAU_CHU_XAM);
            lblKhach.setText("Bấm \"Kiểm tra\" để áp dụng thành viên, hoặc xóa số nếu là khách vãng lai.");
        }
        capNhatDon();
    }

    private void timThanhVien() {
        String sdt = txtSdt.getText().trim();
        khachDangDung = null;
        sdtDaKiemTra = null;
        btnDangKy.setVisible(false);
        if (sdt.isEmpty()) {
            lblKhach.setText(" ");
            capNhatDon();
            return;
        }
        if (!KhachHangDAO.sdtHopLe(sdt)) {
            lblKhach.setForeground(new Color(0xC6, 0x28, 0x28));
            lblKhach.setText("Số điện thoại gồm 10 chữ số, bắt đầu bằng 0");
            capNhatDon();
            return;
        }
        try {
            KhachHang k = new KhachHangDAO().timTheoSdt(sdt);
            sdtDaKiemTra = sdt;
            if (k != null) {
                khachDangDung = k;
                hienThongTinKhach();
            } else {
                lblKhach.setForeground(MAU_VANG);
                lblKhach.setText("Số này chưa là thành viên.");
                btnDangKy.setVisible(true);
            }
        } catch (SQLException ex) {
            lblKhach.setForeground(new Color(0xC6, 0x28, 0x28));
            lblKhach.setText("Lỗi tra cứu: " + ex.getMessage());
        }
        capNhatDon();
    }

    private void hienThongTinKhach() {
        KhachHang k = khachDangDung;
        lblKhach.setForeground(new Color(0x2E, 0x7D, 0x32));
        lblKhach.setText("<html><b>" + thoatHtml(k.hoTen) + "</b> • Hạng " + KhachHangDAO.tenHang(k.hang)
                + " • " + k.diem + " điểm</html>");
    }

    private void dangKyNhanh() {
        String sdt = sdtDaKiemTra;
        if (sdt == null) return;
        String ten = JOptionPane.showInputDialog(this, "Họ tên khách hàng (SĐT " + sdt + "):",
                "Đăng ký thành viên", JOptionPane.QUESTION_MESSAGE);
        if (ten == null) return;
        String hoTen = ten.trim();
        if (hoTen.isEmpty() || hoTen.length() > 100) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập họ tên (tối đa 100 ký tự).",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            khachDangDung = new KhachHangDAO().them(sdt, hoTen);
            btnDangKy.setVisible(false);
            hienThongTinKhach();
        } catch (SdtTrungException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Trùng số điện thoại", JOptionPane.WARNING_MESSAGE);
            timThanhVien();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi đăng ký: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        capNhatDon();
    }

    // ===== Thanh toan =====
    private void thanhToan(ActionEvent e) {
        List<DongCombo> cacDong = new ArrayList<>();
        double tong = 0;
        int tongPhan = 0;
        for (ComboBan c : cacCombo) {
            int sl = soLuong.getOrDefault(c.maCombo(), 0);
            if (sl > 0) {
                cacDong.add(new DongCombo(c, sl));
                tong += c.gia() * sl;
                tongPhan += sl;
            }
        }
        if (cacDong.isEmpty()) {
            return;
        }
        if (!txtSdt.getText().trim().isEmpty() && khachDangDung == null) {
            JOptionPane.showMessageDialog(this, "Bạn đã nhập số điện thoại nhưng chưa kiểm tra thành viên.\n"
                    + "Bấm \"Kiểm tra\", hoặc xóa số điện thoại nếu khách vãng lai.", "Chưa áp dụng thành viên",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String thanhVien = "";
        if (khachDangDung != null) {
            thanhVien = "\nThành viên: " + khachDangDung.hoTen + " (" + khachDangDung.soDienThoai + ")"
                    + "\nTích điểm: +" + BanComboDAO.diemSeTich(tong) + " điểm";
        }
        int xacNhan = JOptionPane.showConfirmDialog(this,
                "Thanh toán " + tongPhan + " combo, tổng " + dinhDangTien(tong) + "đ" + thanhVien
                        + "\n\nXác nhận lập hóa đơn?",
                "Xác nhận thanh toán", JOptionPane.YES_NO_OPTION);
        if (xacNhan != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            HoaDonCombo hd = dao.taoHoaDonCombo(idNhanVien, cacDong,
                    khachDangDung != null ? khachDangDung.soDienThoai : null);

            String ketQua = "Đã lập hóa đơn " + hd.maHd() + "\nTổng tiền: " + dinhDangTien(hd.tongTien()) + "đ";
            String tenKhach = null;
            if (hd.khach() != null) {
                tenKhach = hd.khach().hoTen + " - " + hd.khach().soDienThoai;
                ketQua += "\n\nThành viên " + hd.khach().hoTen + ": +" + hd.diemCong() + " điểm, hiện có "
                        + hd.khach().diem + " điểm (hạng " + KhachHangDAO.tenHang(hd.khach().hang) + ")";
            }
            int in = JOptionPane.showConfirmDialog(this, ketQua + "\n\nIn hóa đơn cho khách?",
                    "Thanh toán thành công", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (in == JOptionPane.YES_OPTION) {
                inHoaDon(new ThongTinHoaDon(hd.maHd(), LocalDateTime.now(), tenKhach, hd.diemCong(), cacDong, hd.tongTien()));
            }
            xoaDon();
            txtSdt.setText("");       // sang khach moi
        } catch (ChuaDangKyException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() + ". Hãy đăng ký thành viên trước.",
                    "Chưa là thành viên", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi lập hóa đơn: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== In hoa don =====
    private void inHoaDon(ThongTinHoaDon tt) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Hoa don " + tt.maHd());
        job.setPrintable(new HoaDonPrintable(tt));
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi in: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static final class ThongTinHoaDon {
        private final String maHd;
        private final LocalDateTime thoiGian;
        private final String khach;          // "Ten - SDT" neu la thanh vien, null neu khach vang lai
        private final int diemCong;
        private final List<DongCombo> cacDong;
        private final double tong;

        public ThongTinHoaDon(String maHd, LocalDateTime thoiGian, String khach, int diemCong,
                              List<DongCombo> cacDong, double tong) {
            this.maHd = maHd;
            this.thoiGian = thoiGian;
            this.khach = khach;
            this.diemCong = diemCong;
            this.cacDong = cacDong;
            this.tong = tong;
        }

        public String maHd() {
            return maHd;
        }

        public LocalDateTime thoiGian() {
            return thoiGian;
        }

        public String khach() {
            return khach;
        }

        public int diemCong() {
            return diemCong;
        }

        public List<DongCombo> cacDong() {
            return cacDong;
        }

        public double tong() {
            return tong;
        }
    }

    static final int HD_RONG = 300;

    static class HoaDonPrintable implements Printable {
        private final ThongTinHoaDon tt;

        HoaDonPrintable(ThongTinHoaDon tt) {
            this.tt = tt;
        }

        @Override
        public int print(Graphics g, PageFormat pf, int trang) {
            if (trang > 0) {
                return NO_SUCH_PAGE;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(pf.getImageableX(), pf.getImageableY());
            double tyLe = Math.min(1.0, pf.getImageableWidth() / HD_RONG);
            g2.scale(tyLe, tyLe);
            veHoaDon(g2, tt);
            g2.dispose();
            return PAGE_EXISTS;
        }
    }

    // Ve 1 hoa don dang bien lai (rong HD_RONG diem), dung cho in. y luon la duong co chu (baseline).
    static void veHoaDon(Graphics2D g, ThongTinHoaDon tt) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);

        int y = 28;
        g.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chuGiua(g, "RẠP CHIẾU PHIM", y);
        y += 18;
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chuGiua(g, "HÓA ĐƠN COMBO BẮP NƯỚC", y);
        y += 14;
        duongKe(g, y);
        y += 20;

        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g.drawString("Mã HĐ: " + tt.maHd(), 10, y);
        y += 16;
        g.drawString("Ngày: " + tt.thoiGian().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), 10, y);
        y += 16;
        if (tt.khach() != null) {
            g.drawString("Thành viên: " + catChu(tt.khach(), g.getFontMetrics(), HD_RONG - 86), 10, y);
            y += 16;
        }
        y -= 4;
        duongKe(g, y);
        y += 22;

        for (DongCombo d : tt.cacDong()) {
            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g.drawString(catChu(d.combo().tenCombo(), g.getFontMetrics(), HD_RONG - 20), 10, y);
            y += 17;
            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g.drawString(d.soLuong() + " x " + dinhDangTien(d.combo().gia()) + "đ", 10, y);
            String tien = dinhDangTien(d.combo().gia() * d.soLuong()) + "đ";
            g.drawString(tien, HD_RONG - 10 - g.getFontMetrics().stringWidth(tien), y);
            y += 24;
        }
        y -= 10;
        duongKe(g, y);
        y += 26;

        g.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g.drawString("TỔNG CỘNG", 10, y);
        String tong = dinhDangTien(tt.tong()) + "đ";
        g.drawString(tong, HD_RONG - 10 - g.getFontMetrics().stringWidth(tong), y);
        y += 14;
        duongKe(g, y);
        y += 26;

        g.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        if (tt.khach() != null && tt.diemCong() > 0) {
            chuGiua(g, "Điểm tích lũy hóa đơn này: +" + tt.diemCong(), y);
            y += 20;
        }
        chuGiua(g, "Cảm ơn quý khách!", y);
    }

    private static void chuGiua(Graphics2D g, String chu, int y) {
        g.drawString(chu, (HD_RONG - g.getFontMetrics().stringWidth(chu)) / 2, y);
    }

    private static void duongKe(Graphics2D g, int y) {
        Stroke cu = g.getStroke();
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
        g.drawLine(8, y, HD_RONG - 8, y);
        g.setStroke(cu);
    }

    // ===== The 1 combo: anh + ten + mo ta + gia + nut -/+ =====
    private class TheCombo extends JPanel {
        private final ComboBan combo;
        private final JLabel lblSoLuong = new JLabel("0", SwingConstants.CENTER);
        private boolean daChon = false;

        TheCombo(ComboBan combo) {
            this.combo = combo;
            setOpaque(false);
            setLayout(new BorderLayout(16, 0));
            setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            setPreferredSize(new Dimension(300, 104));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            // Anh
            JPanel anhWrap = new JPanel(new GridBagLayout());
            anhWrap.setOpaque(false);
            anhWrap.add(taoAnh(combo));
            add(anhWrap, BorderLayout.WEST);

            // Ten + mo ta + gia
            JLabel lblTen = new JLabel(combo.tenCombo());
            lblTen.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lblTen.setForeground(MAU_CHU_DAM);
            lblTen.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lblMoTa = new JLabel("<html><body style='width:230px'>"
                    + thoatHtml(combo.moTa() == null ? "" : combo.moTa()) + "</body></html>");
            lblMoTa.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblMoTa.setForeground(MAU_CHU_XAM);
            lblMoTa.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lblGia = new JLabel(dinhDangTien(combo.gia()) + "đ");
            lblGia.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblGia.setForeground(MAU_NAU_DAM);
            lblGia.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel chu = new JPanel();
            chu.setOpaque(false);
            chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
            chu.add(Box.createVerticalGlue());
            chu.add(lblTen);
            chu.add(Box.createVerticalStrut(3));
            chu.add(lblMoTa);
            chu.add(Box.createVerticalStrut(4));
            chu.add(lblGia);
            chu.add(Box.createVerticalGlue());
            add(chu, BorderLayout.CENTER);

            // Nut - so luong +
            JButton btnTru = taoNutBuoc("−");
            JButton btnCong = taoNutBuoc("+");
            btnTru.addActionListener(e -> doiSoLuong(combo.maCombo(), -1));
            btnCong.addActionListener(e -> doiSoLuong(combo.maCombo(), +1));

            lblSoLuong.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lblSoLuong.setPreferredSize(new Dimension(34, 32));

            JPanel buoc = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            buoc.setOpaque(false);
            buoc.add(btnTru);
            buoc.add(lblSoLuong);
            buoc.add(btnCong);
            JPanel buocWrap = new JPanel(new GridBagLayout());
            buocWrap.setOpaque(false);
            buocWrap.add(buoc);
            add(buocWrap, BorderLayout.EAST);
        }

        void datSoLuong(int n) {
            lblSoLuong.setText(String.valueOf(n));
            lblSoLuong.setForeground(n > 0 ? MAU_NAU_TOI : MAU_CHU_DAM);
            daChon = n > 0;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(daChon ? new Color(0xFF, 0xF8, 0xEC) : MAU_NEN_KEM);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
            g2.setColor(daChon ? MAU_VANG : MAU_VIEN);
            g2.setStroke(new BasicStroke(daChon ? 2f : 1f));
            g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 16, 16));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private JButton taoNutBuoc(String chu) {
        JButton nut = new JButton(chu);
        nut.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nut.setFocusPainted(false);
        nut.setMargin(new Insets(0, 0, 0, 0));
        nut.setPreferredSize(new Dimension(32, 32));
        nut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return nut;
    }

    // Anh combo: neu co file trong src/main/resources/images/combos/ thi dung, khong thi ve hinh thung bap
    private JComponent taoAnh(ComboBan combo) {
        Image anh = docAnhCombo(combo.hinhAnh());
        if (anh != null) {
            return new JLabel(new ImageIcon(anh.getScaledInstance(72, 72, Image.SCALE_SMOOTH)));
        }
        return new BieuTuongBap();
    }

    private Image docAnhCombo(String tenFile) {
        if (tenFile == null || tenFile.trim().isEmpty()) {
            return null;
        }
        URL url = getClass().getResource("/images/combos/" + tenFile.trim());
        if (url == null) {
            System.err.println("[COMBO] Khong tim thay anh: " + tenFile + " (thu muc src/main/resources/images/combos/)");
            return null;
        }
        try {
            BufferedImage anh = ImageIO.read(url);
            if (anh == null) {
                System.err.println("[COMBO] File ton tai nhung khong doc duoc (sai dinh dang that): " + tenFile);
            }
            return anh;
        } catch (Exception e) {
            System.err.println("[COMBO] Loi doc anh " + tenFile + ": " + e.getMessage());
            return null;
        }
    }

    // Hinh thung bap rang ve bang Java2D (dung khi combo chua co anh)
    private static class BieuTuongBap extends JComponent {
        BieuTuongBap() {
            Dimension d = new Dimension(72, 72);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(0xF6, 0xEC, 0xDD));
            g2.fillRoundRect(0, 0, 71, 71, 16, 16);

            // Bap rang
            int[][] bap = {{18, 16, 17}, {31, 8, 19}, {46, 15, 17}, {26, 22, 17}, {40, 22, 17}};
            for (int i = 0; i < bap.length; i++) {
                g2.setColor(i % 2 == 0 ? new Color(0xFF, 0xF3, 0xC4) : new Color(0xFF, 0xE6, 0x8C));
                g2.fillOval(bap[i][0], bap[i][1], bap[i][2], bap[i][2]);
                g2.setColor(new Color(0xE8, 0xC5, 0x6B));
                g2.drawOval(bap[i][0], bap[i][1], bap[i][2], bap[i][2]);
            }

            // Thung soc do trang
            Polygon thung = new Polygon(new int[]{14, 58, 53, 19}, new int[]{32, 32, 66, 66}, 4);
            g2.setColor(Color.WHITE);
            g2.fillPolygon(thung);
            Shape clipCu = g2.getClip();
            g2.clip(thung);
            g2.setColor(new Color(0xD9, 0x4F, 0x4F));
            for (int x = 14; x < 58; x += 11) {
                g2.fillRect(x, 32, 6, 36);
            }
            g2.setClip(clipCu);
            g2.setColor(new Color(0xA8, 0x3A, 0x3A));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawPolygon(thung);
            g2.dispose();
        }
    }

    // ===== Ham phu tro =====
    private static String thoatHtml(String chu) {
        return chu.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String catChu(String chu, FontMetrics fm, int rongToiDa) {
        if (chu == null) return "";
        if (fm.stringWidth(chu) <= rongToiDa) return chu;
        int n = chu.length();
        while (n > 1 && fm.stringWidth(chu.substring(0, n) + "...") > rongToiDa) {
            n--;
        }
        return chu.substring(0, n) + "...";
    }

    // 59000 -> "59.000"
    private static String dinhDangTien(double so) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(so);
    }
}