package view;

import com.formdev.flatlaf.FlatLightLaf;
import controller.ThongKeQuanLyController;
import controller.ThongKeQuanLyController.ThongKe;
import model.TaiKhoan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Trang chu cua QUAN TRI VIEN: thanh menu + man hinh "Tong quan"
 * (banner chao, 4 the thong ke, bang tong quan he thong).
 * Cac tab chua co man hinh rieng se hien "dang duoc xay dung"; tab Su co & boi thuong dung SuCoBoiThuongPanel.
 */
public class TrangChuQuanLy extends JFrame {

    private static final String FONT = "Segoe UI";
    private static final Color NAU_DAM    = new Color(0x8A, 0x6D, 0x4A);
    private static final Color NAU_NHAT   = new Color(0xB2, 0x90, 0x6B);
    private static final Color NEN_KEM    = new Color(0xFB, 0xF6, 0xEF);
    private static final Color NEN_HEADER = new Color(0x3A, 0x30, 0x28);
    private static final Color CHU_XAM    = new Color(0x8A, 0x87, 0x82);
    private static final Color CHU_DAM    = new Color(0x2E, 0x2A, 0x26);
    private static final Color VIEN       = new Color(0xEA, 0xDF, 0xCF);
    private static final Color XANH_LA    = new Color(0x3F, 0xA3, 0x4D);
    private static final Color CAM        = new Color(0xF0, 0xA9, 0x3B);
    private static final Color XANH_DUONG = new Color(0x2F, 0x6F, 0xDE);

    private static final String[] MUC_MENU = {
        "Tổng quan", "Quản lý nhân viên", "Quản lý phim", "Phòng chiếu",
        "Lịch suất chiếu", "Sự cố & bồi thường", "Báo cáo doanh thu",
        "Khách hàng thành viên", "Khuyến mãi"
    };

    private final TaiKhoan taiKhoan;
    private final ThongKeQuanLyController controller = new ThongKeQuanLyController();
    private final NumberFormat dinhDangSo = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    private JPanel panelNoiDung;
    private JScrollPane trangTongQuan;

    private final JLabel lblNhanVien = new JLabel("—");
    private final JLabel lblPhim = new JLabel("—");
    private final JLabel lblSuat = new JLabel("—");
    private final JLabel lblDoanhThu = new JLabel("—");
    private final JLabel lblTieuDeSuat = new JLabel("Suất chiếu hôm nay");
    private final JLabel lblTieuDeDoanhThu = new JLabel("Doanh thu hôm nay");
    private final JLabel lblBangNhanVien = new JLabel("—");
    private final JLabel lblBangPhim = new JLabel("—");
    private final JLabel lblBangPhong = new JLabel("—");
    private final JLabel lblBangSuat = new JLabel("—");
    private final JLabel lblNgayHienTai = new JLabel();
    private final JLabel lblGioHienTai = new JLabel();
    private JComboBox<LocalDate> cboNgay;

    public TrangChuQuanLy(TaiKhoan taiKhoan) {
        this.taiKhoan = taiKhoan;
        FlatLightLaf.setup();

        setTitle("Trang chủ Quản trị - Quản lý rạp chiếu phim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 860);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel goc = new JPanel(new BorderLayout());
        goc.setBackground(NEN_KEM);
        setContentPane(goc);
        goc.add(taoHeader(), BorderLayout.NORTH);

        panelNoiDung = new JPanel(new BorderLayout());
        panelNoiDung.setBackground(NEN_KEM);
        goc.add(panelNoiDung, BorderLayout.CENTER);

        trangTongQuan = taoTrangTongQuan();
        chonMuc(0);
        capNhatDongHo();
        new javax.swing.Timer(30_000, e -> capNhatDongHo()).start();
    }

    // ===================== HEADER =====================
    private JPanel taoHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(NEN_HEADER);
        header.setPreferredSize(new Dimension(0, 78));
        header.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));

        JPanel logo = new JPanel();
        logo.setOpaque(false);
        logo.setLayout(new BoxLayout(logo, BoxLayout.Y_AXIS));
        JLabel ten = new JLabel("RẠP CHIẾU PHIM");
        ten.setFont(new Font(FONT, Font.BOLD, 18));
        ten.setForeground(Color.WHITE);
        JLabel phuDe = new JLabel("HỆ THỐNG QUẢN LÝ");
        phuDe.setFont(new Font(FONT, Font.PLAIN, 11));
        phuDe.setForeground(new Color(0xD0, 0xC2, 0xB2));
        logo.add(Box.createVerticalGlue());
        logo.add(ten);
        logo.add(phuDe);
        logo.add(Box.createVerticalGlue());

        logo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { chonMuc(0); }
        });
        header.add(logo, BorderLayout.WEST);

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        phai.setOpaque(false);
        phai.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
        JLabel nguoiDung = new JLabel(taiKhoan != null ? taiKhoan.getHoTen() : "Quản trị viên",
                new BieuTuong(BieuTuong.NGUOI, 20, Color.WHITE), SwingConstants.LEFT);
        nguoiDung.setIconTextGap(8);
        nguoiDung.setFont(new Font(FONT, Font.PLAIN, 14));
        nguoiDung.setForeground(Color.WHITE);
        JPanel dung = new JPanel();
        dung.setBackground(new Color(255, 255, 255, 70));
        dung.setPreferredSize(new Dimension(1, 26));
        JButton btnDangXuat = new JButton("Đăng xuất", new BieuTuong(BieuTuong.DANG_XUAT, 20, Color.WHITE));
        btnDangXuat.setIconTextGap(8);
        btnDangXuat.setFont(new Font(FONT, Font.PLAIN, 14));
        btnDangXuat.setForeground(Color.WHITE);
        btnDangXuat.setContentAreaFilled(false);
        btnDangXuat.setBorderPainted(false);
        btnDangXuat.setFocusPainted(false);
        btnDangXuat.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDangXuat.addActionListener(e -> dangXuat());
        phai.add(nguoiDung);
        phai.add(dung);
        phai.add(btnDangXuat);
        header.add(phai, BorderLayout.EAST);
        return header;
    }

    private void dangXuat() {
        int xn = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn đăng xuất?",
                "Xác nhận đăng xuất", JOptionPane.YES_NO_OPTION);
        if (xn == JOptionPane.YES_OPTION) {
            dispose();
            new DangNhap().setVisible(true);
        }
    }

    // ===================== CHUYEN TAB =====================
    private void chonMuc(int chiSo) {
        panelNoiDung.removeAll();
        if (chiSo == 0) {
            panelNoiDung.add(trangTongQuan, BorderLayout.CENTER);
            taiThongKe();   // quay lai Tong quan thi cap nhat lai so lieu
        } else {
            JPanel khung = new JPanel(new BorderLayout());
            khung.setBackground(NEN_KEM);
            khung.add(taoThanhQuayLai(MUC_MENU[chiSo]), BorderLayout.NORTH);
            khung.add(taoNoiDungMuc(chiSo), BorderLayout.CENTER);
            panelNoiDung.add(khung, BorderLayout.CENTER);
        }
        panelNoiDung.revalidate();
        panelNoiDung.repaint();
    }

    /** Thanh phia tren cac man hinh con: nut "Quay lai tong quan" + ten man hinh. */
    private JPanel taoThanhQuayLai(String tenMuc) {
        JPanel thanh = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        thanh.setOpaque(false);
        thanh.setBorder(BorderFactory.createEmptyBorder(18, 30, 0, 30));

        JButton btn = new JButton("‹  Quay lại tổng quan");
        btn.setFont(new Font(FONT, Font.PLAIN, 14));
        btn.setForeground(CHU_DAM);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.addActionListener(e -> chonMuc(0));
        thanh.add(btn);

        JLabel lbl = new JLabel(tenMuc);
        lbl.setFont(new Font(FONT, Font.BOLD, 18));
        lbl.setForeground(CHU_XAM);
        thanh.add(lbl);
        return thanh;
    }

    private JComponent taoNoiDungMuc(int chiSo) {
        try {
            switch (chiSo) {
                case 1: {
                    long idQuanLy = (taiKhoan != null) ? taiKhoan.getId() : 1L;
                    return boc(new NhanVienPanel(idQuanLy));
                }
                case 2:
                    return boc(new BangDuLieuPanel("phim", "Quản lý phim", "ma_phim", true, true, true, null));
                case 3:
                    return boc(new BangDuLieuPanel("phong_chieu", "Phòng chiếu", "ma_phong", true, true, true, null));
                case 4:
                    return boc(new BangDuLieuPanel("suat_chieu", "Lịch suất chiếu", "ngay_chieu DESC, gio_bat_dau", true, true, true, null));
                case 5: {
                    long id = (taiKhoan != null) ? taiKhoan.getId() : 1L;
                    return boc(new SuCoBoiThuongPanel(id));
                }
                case 6:
                    return boc(new BaoCaoDoanhThuPanel());
                case 7:
                    return boc(new KhachHangPanel());
                case 8:
                    return boc(new KhuyenMaiPanel());
                default:
                    return nhanTam("Chức năng \"" + MUC_MENU[chiSo] + "\" đang được xây dựng...");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return nhanTam("Lỗi tải màn hình: " + e);
        }
    }

    /** Boc man hinh con trong khung co le cho dong deu voi trang Tong quan. */
    private JComponent boc(JComponent noiDung) {
        JPanel khung = new JPanel(new BorderLayout());
        khung.setBackground(NEN_KEM);
        khung.setBorder(BorderFactory.createEmptyBorder(14, 30, 20, 30));
        khung.add(noiDung, BorderLayout.CENTER);
        return khung;
    }

    private JLabel nhanTam(String chu) {
        JLabel l = new JLabel(chu, SwingConstants.CENTER);
        l.setFont(new Font(FONT, Font.PLAIN, 16));
        l.setForeground(CHU_XAM);
        return l;
    }

    // ===================== TRANG TONG QUAN =====================
    private JScrollPane taoTrangTongQuan() {
        JPanel trang = new JPanel();
        trang.setLayout(new BoxLayout(trang, BoxLayout.Y_AXIS));
        trang.setBackground(NEN_KEM);
        trang.setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));

        Banner banner = new Banner();
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 156));
        trang.add(banner);
        trang.add(Box.createVerticalStrut(22));

        JPanel hangThe = new JPanel(new GridLayout(1, 4, 18, 0));
        hangThe.setOpaque(false);
        hangThe.setAlignmentX(Component.LEFT_ALIGNMENT);
        hangThe.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        hangThe.add(taoThe(BieuTuong.NHIEU_NGUOI, new JLabel("Nhân viên"), lblNhanVien, "nhân viên", 1));
        hangThe.add(taoThe(BieuTuong.PHIM, new JLabel("Phim đang chiếu"), lblPhim, "phim", 2));
        hangThe.add(taoThe(BieuTuong.LICH_DONG_HO, lblTieuDeSuat, lblSuat, "suất chiếu", 4));
        hangThe.add(taoThe(BieuTuong.TIEN, lblTieuDeDoanhThu, lblDoanhThu, "doanh thu", 6));
        trang.add(hangThe);
        trang.add(Box.createVerticalStrut(22));

        BoTron khung = new BoTron(new BorderLayout(0, 14), Color.WHITE, VIEN, 18);
        khung.setBorder(BorderFactory.createEmptyBorder(18, 24, 22, 24));
        khung.setAlignmentX(Component.LEFT_ALIGNMENT);
        khung.add(taoDauBang(), BorderLayout.NORTH);
        khung.add(taoBang(), BorderLayout.CENTER);
        trang.add(khung);

        JScrollPane cuon = new JScrollPane(trang);
        cuon.setBorder(null);
        cuon.getViewport().setBackground(NEN_KEM);
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        return cuon;
    }

    private BoTron taoThe(int loaiIcon, JLabel tieuDe, JLabel giaTri, String donVi, int tabDich) {
        BoTron the = new BoTron(new BorderLayout(16, 0), Color.WHITE, VIEN, 18);
        the.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        the.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        the.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { chonMuc(tabDich); }
        });

        the.add(new HuyHieu(loaiIcon, 76), BorderLayout.WEST);

        tieuDe.setFont(new Font(FONT, Font.BOLD, 15));
        tieuDe.setForeground(CHU_DAM);
        giaTri.setFont(new Font(FONT, Font.BOLD, giaTri == lblDoanhThu ? 22 : 32));
        giaTri.setForeground(CHU_DAM);
        JLabel nhanDonVi = new JLabel(donVi);
        nhanDonVi.setFont(new Font(FONT, Font.PLAIN, 13));
        nhanDonVi.setForeground(CHU_XAM);
        JPanel chu = new JPanel();
        chu.setOpaque(false);
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
        chu.add(Box.createVerticalGlue());
        chu.add(tieuDe);
        chu.add(giaTri);
        chu.add(nhanDonVi);
        chu.add(Box.createVerticalGlue());
        the.add(chu, BorderLayout.CENTER);

        JLabel mui = new JLabel("›");
        mui.setFont(new Font(FONT, Font.PLAIN, 30));
        mui.setForeground(new Color(0xB5, 0xAE, 0xA4));
        the.add(mui, BorderLayout.EAST);
        return the;
    }

    private JPanel taoDauBang() {
        JPanel dau = new JPanel(new BorderLayout());
        dau.setOpaque(false);

        JLabel tieuDe = new JLabel("Tổng quan hệ thống", new BieuTuong(BieuTuong.LUOI, 28, NAU_DAM), SwingConstants.LEFT);
        tieuDe.setIconTextGap(14);
        tieuDe.setFont(new Font(FONT, Font.BOLD, 21));
        tieuDe.setForeground(CHU_DAM);
        dau.add(tieuDe, BorderLayout.WEST);

        cboNgay = new JComboBox<>();
        LocalDate homNay = LocalDate.now();
        for (int i = 0; i < 7; i++) cboNgay.addItem(homNay.minusDays(i));
        cboNgay.setFont(new Font(FONT, Font.PLAIN, 14));
        cboNgay.setPreferredSize(new Dimension(170, 38));
        cboNgay.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof LocalDate) {
                    setText(((LocalDate) v).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
                return this;
            }
        });
        cboNgay.addActionListener(e -> taiThongKe());
        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        phai.setOpaque(false);
        phai.add(cboNgay);
        dau.add(phai, BorderLayout.EAST);
        return dau;
    }

    private JPanel taoBang() {
        JPanel bang = new JPanel();
        bang.setLayout(new BoxLayout(bang, BoxLayout.Y_AXIS));
        bang.setOpaque(false);

        bang.add(dongBang(52, true, new Color(0x4A, 0x3B, 0x2E),
                tieuDeCot("STT"), tieuDeCot("Chức năng quản lý"), tieuDeCot("Số lượng"),
                tieuDeCot("Trạng thái"), tieuDeCot("Thao tác")));

        bang.add(dongChucNang(1, BieuTuong.NGUOI, "Tài khoản nhân viên", lblBangNhanVien,
                new Pill("Đang hoạt động", XANH_LA), new NutBo("Quản lý", BieuTuong.BANH_RANG, 150, () -> chonMuc(1)), false));
        bang.add(dongChucNang(2, BieuTuong.PHIM, "Danh sách phim", lblBangPhim,
                new Pill("Đang cập nhật", CAM), new NutBo("Quản lý", BieuTuong.BANH_RANG, 150, () -> chonMuc(2)), false));
        bang.add(dongChucNang(3, BieuTuong.MAN_HINH, "Phòng chiếu", lblBangPhong,
                new Pill("Hoạt động", XANH_LA), new NutBo("Quản lý", BieuTuong.BANH_RANG, 150, () -> chonMuc(3)), false));
        bang.add(dongChucNang(4, BieuTuong.LICH, "Lịch suất chiếu", lblBangSuat,
                new Pill("Đã lập lịch", XANH_DUONG), new NutBo("Xem lịch", BieuTuong.LICH, 150, () -> chonMuc(4)), false));
        bang.add(dongChucNang(5, BieuTuong.NHIEU_NGUOI, "Khách hàng thành viên", new JLabel("Tích điểm"),
                new Pill("Sẵn sàng", XANH_LA), new NutBo("Quản lý", BieuTuong.BANH_RANG, 150, () -> chonMuc(7)), false));
        bang.add(dongChucNang(6, BieuTuong.TIEN, "Khuyến mãi", new JLabel("Ưu đãi"),
                new Pill("Sẵn sàng", XANH_LA), new NutBo("Quản lý", BieuTuong.BANH_RANG, 150, () -> chonMuc(8)), false));
        JLabel homNay = new JLabel("Hôm nay");
        bang.add(dongChucNang(7, BieuTuong.BIEU_DO, "Báo cáo doanh thu", homNay,
                new Pill("Sẵn sàng", XANH_LA), new NutBo("Xem báo cáo", BieuTuong.BIEU_DO, 176, () -> chonMuc(6)), false));
        bang.add(dongChucNang(8, BieuTuong.CANH_BAO, "Sự cố & bồi thường", new JLabel("Lập phiếu"),
                new Pill("Sẵn sàng", XANH_LA), new NutBo("Xử lý sự cố", BieuTuong.CANH_BAO, 150, () -> chonMuc(5)), true));
        return bang;
    }

    private JLabel tieuDeCot(String chu) {
        JLabel l = new JLabel(chu);
        l.setFont(new Font(FONT, Font.BOLD, 14));
        l.setForeground(Color.WHITE);
        return l;
    }

    private JPanel dongChucNang(int stt, int loaiIcon, String ten, JLabel soLuong, Pill pill, NutBo nut, boolean cuoi) {
        JLabel lblStt = new JLabel(String.valueOf(stt));
        lblStt.setFont(new Font(FONT, Font.PLAIN, 14));
        lblStt.setForeground(CHU_DAM);
        JLabel lblTen = new JLabel(ten, new BieuTuong(loaiIcon, 30, CHU_DAM), SwingConstants.LEFT);
        lblTen.setIconTextGap(18);
        lblTen.setFont(new Font(FONT, Font.PLAIN, 14));
        lblTen.setForeground(CHU_DAM);
        soLuong.setFont(new Font(FONT, Font.PLAIN, 14));
        soLuong.setForeground(CHU_DAM);
        JPanel dong = dongBang(62, false, null, lblStt, lblTen, soLuong, pill, nut);
        dong.setBorder(cuoi ? BorderFactory.createEmptyBorder()
                : BorderFactory.createMatteBorder(0, 0, 1, 0, VIEN));
        return dong;
    }

    /** Mot dong cua bang: 5 cot chia theo ti le co dinh, nen header va cac dong luon thang hang. */
    private JPanel dongBang(int cao, boolean laTieuDe, Color nen, Component... o) {
        JPanel dong = new JPanel(new BoCucCot(0.07, 0.38, 0.17, 0.21, 0.17));
        if (nen != null) dong.setBackground(nen); else dong.setOpaque(false);
        dong.setAlignmentX(Component.LEFT_ALIGNMENT);
        dong.setPreferredSize(new Dimension(0, cao));
        dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, cao));
        for (Component c : o) dong.add(c);
        return dong;
    }

    // ===================== DU LIEU =====================
    private void capNhatDongHo() {
        LocalDateTime bay = LocalDateTime.now();
        String thu = (bay.getDayOfWeek() == DayOfWeek.SUNDAY) ? "Chủ nhật" : "Thứ " + (bay.getDayOfWeek().getValue() + 1);
        lblNgayHienTai.setText(thu + ", " + bay.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblGioHienTai.setText(bay.format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    private String so(long v) {
        return v < 0 ? "—" : dinhDangSo.format(v);
    }

    private void taiThongKe() {
        final LocalDate ngay = (cboNgay != null && cboNgay.getSelectedItem() != null)
                ? (LocalDate) cboNgay.getSelectedItem() : LocalDate.now();
        new SwingWorker<ThongKe, Void>() {
            @Override
            protected ThongKe doInBackground() throws Exception {
                return controller.lay(ngay);
            }

            @Override
            protected void done() {
                try {
                    ThongKe t = get();
                    boolean homNay = ngay.equals(LocalDate.now());
                    String nhanNgay = homNay ? "hôm nay" : "ngày " + ngay.format(DateTimeFormatter.ofPattern("dd/MM"));
                    lblTieuDeSuat.setText("Suất chiếu " + nhanNgay);
                    lblTieuDeDoanhThu.setText("Doanh thu " + nhanNgay);
                    lblNhanVien.setText(so(t.soNhanVien));
                    lblPhim.setText(so(t.soPhimDangChieu));
                    lblSuat.setText(so(t.soSuat));
                    lblDoanhThu.setText(t.doanhThu < 0 ? "—" : dinhDangSo.format(t.doanhThu) + " VNĐ");
                    lblBangNhanVien.setText(so(t.soNhanVien));
                    lblBangPhim.setText(so(t.soPhimDangChieu));
                    lblBangPhong.setText(so(t.soPhong));
                    lblBangSuat.setText(so(t.soSuat));
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(TrangChuQuanLy.this,
                            "Lỗi kết nối cơ sở dữ liệu:\n" + e.getCause().getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TrangChuQuanLy(null).setVisible(true));
    }

    // =====================================================================
    //                      CAC THANH PHAN GIAO DIEN TU VE
    // =====================================================================

    private static void khuRangCua(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /**
     * Bo cuc theo cot: moi cot chiem mot ti le chieu rong co dinh cua dong.
     * Cot dau can giua, cac cot sau can trai; moi o can giua theo chieu doc.
     */
    private static class BoCucCot implements LayoutManager {
        private final double[] tyLe;

        BoCucCot(double... tyLe) {
            this.tyLe = tyLe;
        }

        @Override public void addLayoutComponent(String ten, Component c) { }
        @Override public void removeLayoutComponent(Component c) { }
        @Override public Dimension preferredLayoutSize(Container p) { return new Dimension(100, 40); }
        @Override public Dimension minimumLayoutSize(Container p) { return new Dimension(100, 40); }

        @Override
        public void layoutContainer(Container p) {
            Insets in = p.getInsets();
            int rong = p.getWidth() - in.left - in.right;
            int cao = p.getHeight() - in.top - in.bottom;
            double daDung = 0;
            for (int i = 0; i < p.getComponentCount() && i < tyLe.length; i++) {
                int x0 = in.left + (int) Math.round(rong * daDung);
                daDung += tyLe[i];
                int x1 = in.left + (int) Math.round(rong * daDung);
                int rongCot = x1 - x0;
                Component c = p.getComponent(i);
                Dimension d = c.getPreferredSize();
                int rongO = Math.min(d.width, rongCot);
                int x = (i == 0) ? x0 + (rongCot - rongO) / 2 : x0 + (i == 1 ? 10 : 0);
                c.setBounds(x, in.top + (cao - d.height) / 2, rongO, d.height);
            }
        }
    }

    /** Panel bo goc, co the co vien. */
    private static class BoTron extends JPanel {
        private final Color nen, vien;
        private final int banKinh;

        BoTron(LayoutManager lm, Color nen, Color vien, int banKinh) {
            super(lm);
            this.nen = nen;
            this.vien = vien;
            this.banKinh = banKinh;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            khuRangCua(g2);
            RoundRectangle2D hinh = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, banKinh, banKinh);
            g2.setColor(nen);
            g2.fill(hinh);
            if (vien != null) {
                g2.setColor(vien);
                g2.draw(hinh);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Vien tron mau nau co bieu tuong o giua (o the thong ke, avatar). */
    private static class HuyHieu extends JComponent {
        private final int loai, kichThuoc;

        HuyHieu(int loai, int kichThuoc) {
            this.loai = loai;
            this.kichThuoc = kichThuoc;
            setPreferredSize(new Dimension(kichThuoc, kichThuoc));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            khuRangCua(g2);
            int y = (getHeight() - kichThuoc) / 2;
            g2.setColor(NAU_NHAT);
            g2.fill(new Ellipse2D.Double(0, y, kichThuoc, kichThuoc));
            int icon = (int) (kichThuoc * 0.5);
            new BieuTuong(loai, icon, Color.WHITE).paintIcon(this, g2, (kichThuoc - icon) / 2, y + (kichThuoc - icon) / 2);
            g2.dispose();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }

    /** Nhan trang thai dang vien thuoc: cham mau + chu. */
    private static class Pill extends JComponent {
        private final String chu;
        private final Color mauCham;

        Pill(String chu, Color mauCham) {
            this.chu = chu;
            this.mauCham = mauCham;
            setFont(new Font(FONT, Font.PLAIN, 13));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(chu) + 52, 32);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            khuRangCua(g2);
            g2.setColor(new Color(0xF1, 0xEE, 0xE8));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g2.setColor(mauCham);
            g2.fill(new Ellipse2D.Double(14, getHeight() / 2.0 - 4.5, 9, 9));
            g2.setColor(new Color(0x6A, 0x66, 0x60));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(chu, 32, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Nut bo tron mau nau (Quan ly, Xem lich, Xem bao cao). */
    private static class NutBo extends JButton {
        private boolean dangTro = false;

        NutBo(String chu, int loaiIcon, int rong, Runnable khiBam) {
            super(chu, new BieuTuong(loaiIcon, 18, Color.WHITE));
            setFont(new Font(FONT, Font.PLAIN, 14));
            setForeground(Color.WHITE);
            setIconTextGap(8);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setPreferredSize(new Dimension(rong, 38));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> khiBam.run());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { dangTro = true; repaint(); }

                @Override
                public void mouseExited(MouseEvent e) { dangTro = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            khuRangCua(g2);
            g2.setColor(dangTro ? NAU_DAM : NAU_NHAT);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Banner chao mung: nen gradient nau, cuon phim va ngoi sao trang tri. */
    private class Banner extends JPanel {
        Banner() {
            super(new BorderLayout(26, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 36, 0, 36));
            setPreferredSize(new Dimension(0, 156));

            add(new HuyHieu(BieuTuong.NGUOI, 94), BorderLayout.WEST);

            String ten = (taiKhoan != null && taiKhoan.getHoTen() != null) ? taiKhoan.getHoTen() : "Quản trị viên";
            JLabel chao = new JLabel("Xin chào, " + ten + "!");
            chao.setFont(new Font(FONT, Font.BOLD, 32));
            chao.setForeground(Color.WHITE);
            JLabel loiChuc = new JLabel("Chúc bạn một ngày quản lý hiệu quả!");
            loiChuc.setFont(new Font(FONT, Font.PLAIN, 17));
            loiChuc.setForeground(new Color(0xF3, 0xE9, 0xDC));

            lblNgayHienTai.setFont(new Font(FONT, Font.PLAIN, 14));
            lblNgayHienTai.setForeground(new Color(0xF3, 0xE9, 0xDC));
            lblNgayHienTai.setIcon(new BieuTuong(BieuTuong.LICH, 22, new Color(0xF3, 0xE9, 0xDC)));
            lblNgayHienTai.setIconTextGap(10);
            lblGioHienTai.setFont(new Font(FONT, Font.PLAIN, 14));
            lblGioHienTai.setForeground(new Color(0xF3, 0xE9, 0xDC));
            lblGioHienTai.setIcon(new BieuTuong(BieuTuong.DONG_HO, 22, new Color(0xF3, 0xE9, 0xDC)));
            lblGioHienTai.setIconTextGap(10);
            JPanel dongGio = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            dongGio.setOpaque(false);
            dongGio.add(lblNgayHienTai);
            dongGio.add(Box.createHorizontalStrut(36));
            dongGio.add(lblGioHienTai);

            JPanel chu = new JPanel();
            chu.setOpaque(false);
            chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
            chu.add(Box.createVerticalGlue());
            chao.setAlignmentX(Component.LEFT_ALIGNMENT);
            loiChuc.setAlignmentX(Component.LEFT_ALIGNMENT);
            dongGio.setAlignmentX(Component.LEFT_ALIGNMENT);
            chu.add(chao);
            chu.add(Box.createVerticalStrut(2));
            chu.add(loiChuc);
            chu.add(Box.createVerticalStrut(10));
            chu.add(dongGio);
            chu.add(Box.createVerticalGlue());
            add(chu, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            khuRangCua(g2);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D khung = new RoundRectangle2D.Double(0, 0, w, h, 20, 20);
            g2.setClip(khung);
            g2.setPaint(new GradientPaint(0, 0, new Color(0x4A, 0x3B, 0x2E), w, h, new Color(0x8A, 0x6D, 0x4A)));
            g2.fill(khung);

            Path2D song = new Path2D.Double();
            song.moveTo(w * 0.25, h);
            song.curveTo(w * 0.45, h * 0.2, w * 0.65, h * 0.95, w, h * 0.3);
            song.lineTo(w, h);
            song.closePath();
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fill(song);

            AffineTransform cu = g2.getTransform();
            g2.rotate(Math.toRadians(-28), w * 0.88, h * 0.75);
            int bx = (int) (w * 0.58), by = (int) (h * 0.62);
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillRoundRect(bx, by, (int) (w * 0.5), 56, 10, 10);
            g2.setColor(new Color(0x4A, 0x3B, 0x2E, 170));
            for (int x = bx + 8; x < w * 1.08; x += 22) {
                g2.fillRoundRect(x, by + 7, 12, 8, 3, 3);
                g2.fillRoundRect(x, by + 41, 12, 8, 3, 3);
            }
            g2.setTransform(cu);

            int rx = w - 92, ry = 76;
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fill(new Ellipse2D.Double(rx - 52, ry - 52, 104, 104));
            g2.setColor(new Color(0x5E, 0x4A, 0x39));
            g2.fill(new Ellipse2D.Double(rx - 12, ry - 12, 24, 24));
            for (int k = 0; k < 5; k++) {
                double a = Math.PI * 2 * k / 5 - Math.PI / 2;
                g2.fill(new Ellipse2D.Double(rx + 30 * Math.cos(a) - 9, ry + 30 * Math.sin(a) - 9, 18, 18));
            }
            sao(g2, w * 0.86, h * 0.25, 13);
            sao(g2, w * 0.79, h * 0.78, 9);
            g2.dispose();
            super.paintComponent(g);
        }

        private void sao(Graphics2D g2, double cx, double cy, double r) {
            double t = r * 0.22;
            Path2D p = new Path2D.Double();
            p.moveTo(cx, cy - r);
            p.lineTo(cx + t, cy - t);
            p.lineTo(cx + r, cy);
            p.lineTo(cx + t, cy + t);
            p.lineTo(cx, cy + r);
            p.lineTo(cx - t, cy + t);
            p.lineTo(cx - r, cy);
            p.lineTo(cx - t, cy - t);
            p.closePath();
            g2.setColor(new Color(255, 255, 255, 110));
            g2.fill(p);
        }
    }

    /** Bieu tuong net mong ve bang Java2D (khong can file anh). */
    private static class BieuTuong implements Icon {
        static final int NGUOI = 0, NHIEU_NGUOI = 1, PHIM = 2, LICH = 3, TIEN = 4, MAN_HINH = 5,
                BIEU_DO = 6, LUOI = 7, BANH_RANG = 8, DONG_HO = 9, DANG_XUAT = 10, LICH_DONG_HO = 11, CANH_BAO = 12;

        private final int loai, s;
        private final Color mau;

        BieuTuong(int loai, int kichThuoc, Color mau) {
            this.loai = loai;
            this.s = kichThuoc;
            this.mau = mau;
        }

        @Override public int getIconWidth() { return s; }
        @Override public int getIconHeight() { return s; }

        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            khuRangCua(g);
            g.translate(x, y);
            g.setColor(mau);
            g.setStroke(new BasicStroke(Math.max(1.4f, s / 13f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (loai) {
                case NGUOI -> nguoi(g, s * 0.5, s * 0.06, 1.0);
                case NHIEU_NGUOI -> {
                    nguoi(g, s * 0.5, s * 0.12, 0.85);
                    nguoi(g, s * 0.18, s * 0.3, 0.6);
                    nguoi(g, s * 0.82, s * 0.3, 0.6);
                }
                case PHIM -> {
                    g.draw(new RoundRectangle2D.Double(s * .12, s * .38, s * .76, s * .5, 4, 4));
                    g.draw(new RoundRectangle2D.Double(s * .12, s * .16, s * .76, s * .18, 3, 3));
                    for (int i = 0; i < 3; i++) g.drawLine((int) (s * (.3 + i * .22)), (int) (s * .16), (int) (s * (.22 + i * .22)), (int) (s * .34));
                }
                case LICH -> lich(g);
                case LICH_DONG_HO -> {
                    lich(g);
                    g.setColor(Color.WHITE.equals(mau) ? NAU_NHAT : Color.WHITE);
                    g.fill(new Ellipse2D.Double(s * .52, s * .5, s * .46, s * .46));
                    g.setColor(mau);
                    g.draw(new Ellipse2D.Double(s * .52, s * .5, s * .46, s * .46));
                    g.drawLine((int) (s * .75), (int) (s * .6), (int) (s * .75), (int) (s * .73));
                    g.drawLine((int) (s * .75), (int) (s * .73), (int) (s * .84), (int) (s * .73));
                }
                case TIEN -> {
                    g.draw(new Ellipse2D.Double(s * .1, s * .1, s * .8, s * .8));
                    g.setFont(new Font(FONT, Font.BOLD, (int) (s * .55)));
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString("$", (s - fm.stringWidth("$")) / 2f, s * .5f + (fm.getAscent() - fm.getDescent()) / 2f);
                }
                case MAN_HINH -> {
                    g.draw(new RoundRectangle2D.Double(s * .1, s * .12, s * .8, s * .76, 5, 5));
                    g.draw(new RoundRectangle2D.Double(s * .24, s * .26, s * .52, s * .48, 3, 3));
                }
                case BIEU_DO -> {
                    g.drawLine((int) (s * .12), (int) (s * .88), (int) (s * .88), (int) (s * .88));
                    g.fillRect((int) (s * .2), (int) (s * .5), (int) (s * .12), (int) (s * .38));
                    g.fillRect((int) (s * .44), (int) (s * .3), (int) (s * .12), (int) (s * .58));
                    g.fillRect((int) (s * .68), (int) (s * .14), (int) (s * .12), (int) (s * .74));
                }
                case LUOI -> {
                    g.fill(new RoundRectangle2D.Double(s * .08, s * .08, s * .36, s * .36, 5, 5));
                    g.fill(new RoundRectangle2D.Double(s * .56, s * .08, s * .36, s * .36, 5, 5));
                    g.fill(new RoundRectangle2D.Double(s * .08, s * .56, s * .36, s * .36, 5, 5));
                    g.fill(new RoundRectangle2D.Double(s * .56, s * .56, s * .36, s * .36, 5, 5));
                }
                case BANH_RANG -> {
                    g.draw(new Ellipse2D.Double(s * .3, s * .3, s * .4, s * .4));
                    for (int k = 0; k < 8; k++) {
                        double a = Math.PI * k / 4;
                        g.drawLine((int) (s * .5 + s * .26 * Math.cos(a)), (int) (s * .5 + s * .26 * Math.sin(a)),
                                (int) (s * .5 + s * .4 * Math.cos(a)), (int) (s * .5 + s * .4 * Math.sin(a)));
                    }
                }
                case DONG_HO -> {
                    g.draw(new Ellipse2D.Double(s * .08, s * .08, s * .84, s * .84));
                    g.drawLine((int) (s * .5), (int) (s * .26), (int) (s * .5), (int) (s * .52));
                    g.drawLine((int) (s * .5), (int) (s * .52), (int) (s * .7), (int) (s * .62));
                }
                case DANG_XUAT -> {
                    Path2D p = new Path2D.Double();
                    p.moveTo(s * .46, s * .14);
                    p.lineTo(s * .14, s * .14);
                    p.lineTo(s * .14, s * .86);
                    p.lineTo(s * .46, s * .86);
                    g.draw(p);
                    g.drawLine((int) (s * .36), (int) (s * .5), (int) (s * .88), (int) (s * .5));
                    g.drawLine((int) (s * .88), (int) (s * .5), (int) (s * .72), (int) (s * .34));
                    g.drawLine((int) (s * .88), (int) (s * .5), (int) (s * .72), (int) (s * .66));
                }
                case CANH_BAO -> {
                    Path2D t = new Path2D.Double();
                    t.moveTo(s * .5, s * .12);
                    t.lineTo(s * .92, s * .86);
                    t.lineTo(s * .08, s * .86);
                    t.closePath();
                    g.draw(t);
                    g.drawLine((int) (s * .5), (int) (s * .38), (int) (s * .5), (int) (s * .6));
                    g.fill(new Ellipse2D.Double(s * .46, s * .68, s * .08, s * .08));
                }
                default -> { }
            }
            g.dispose();
        }

        private void nguoi(Graphics2D g, double cx, double top, double ti) {
            double d = s * 0.34 * ti;
            g.draw(new Ellipse2D.Double(cx - d / 2, top, d, d));
            double rong = s * 0.62 * ti;
            g.draw(new Arc2D.Double(cx - rong / 2, top + d + s * 0.06 * ti, rong, rong, 0, 180, Arc2D.OPEN));
        }

        private void lich(Graphics2D g) {
            g.draw(new RoundRectangle2D.Double(s * .12, s * .2, s * .76, s * .68, 6, 6));
            g.drawLine((int) (s * .12), (int) (s * .42), (int) (s * .88), (int) (s * .42));
            g.drawLine((int) (s * .32), (int) (s * .1), (int) (s * .32), (int) (s * .28));
            g.drawLine((int) (s * .68), (int) (s * .1), (int) (s * .68), (int) (s * .28));
            g.fill(new Ellipse2D.Double(s * .26, s * .56, s * .09, s * .09));
            g.fill(new Ellipse2D.Double(s * .46, s * .56, s * .09, s * .09));
        }
    }
}