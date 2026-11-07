package view;

import controller.Thongketrangchucontroller;
import controller.Thongketrangchucontroller.DongSuatChieu;
import controller.Thongketrangchucontroller.DuLieuTrangChu;
import model.TaiKhoan;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * Giao diện tab "Trang chủ" của nhân viên:
 * banner chào + 4 thẻ thống kê + bảng suất chiếu hôm nay.
 * Dữ liệu được tải ở luồng nền nên giao diện hiện ngay, không bị đứng.
 */
public class Trangchupanel extends JPanel {

    private static final Color NAU_DAM    = new Color(0x8A, 0x6D, 0x4A);
    private static final Color NAU_NHAT   = new Color(0xB2, 0x90, 0x6B);
    private static final Color NEN_KEM    = new Color(0xFB, 0xF6, 0xEF);
    private static final Color NEN_THE    = new Color(0xFF, 0xFD, 0xF9);
    private static final Color NEN_HEADER = new Color(0x3A, 0x30, 0x28);
    private static final Color CHU_XAM    = new Color(0x8A, 0x87, 0x82);
    private static final Color CHU_DEN    = new Color(0x2E, 0x2A, 0x26);
    private static final Color VIEN       = new Color(0xE5, 0xDD, 0xD0);

    private static final String FONT = "Segoe UI";

    // Chiều rộng cố định các cột: STT | Tên phim (co giãn) | Phòng | Giờ chiếu | Trạng thái | Thao tác
    private static final int[] RONG_COT = {60, 100, 130, 100, 150, 170};
    private static final int CAO_HANG = 52;
    private static final int POSTER_RONG = 32, POSTER_CAO = 44;

    private final TaiKhoan taiKhoan;
    @SuppressWarnings("unused")
    private final IntConsumer chuyenMenu;               // để dành, hiện chưa dùng
    private final Function<String, Image> docPoster;
    private final Thongketrangchucontroller controller = new Thongketrangchucontroller();
    private final NumberFormat dinhDangSo = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    // Các nhãn sẽ được điền số liệu sau khi tải xong
    private final JLabel lblSuat = new JLabel("...");
    private final JLabel lblVe = new JLabel("...");
    private final JLabel lblDoanhThu = new JLabel("...");
    private final JLabel lblPhong = new JLabel("...");
    private JPanel danhSach;                             // vùng chứa các dòng suất chiếu
    private final Map<String, Image> cachePoster = new HashMap<>();

    private JLabel lblGio;
    private Timer timerDongHo;

    public Trangchupanel(TaiKhoan taiKhoan, IntConsumer chuyenMenu, Function<String, Image> docPoster) {
        this.taiKhoan = taiKhoan;
        this.chuyenMenu = chuyenMenu;
        this.docPoster = docPoster;

        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel tren = new JPanel(new BorderLayout(0, 15));
        tren.setOpaque(false);
        tren.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        tren.add(taoBanner(), BorderLayout.NORTH);
        tren.add(taoHangThongKe(), BorderLayout.CENTER);
        add(tren, BorderLayout.NORTH);

        add(taoBangSuatChieu(), BorderLayout.CENTER);

        taiDuLieuNen();
    }

    // ===================== TẢI DỮ LIỆU Ở LUỒNG NỀN =====================
    private void taiDuLieuNen() {
        new SwingWorker<DuLieuTrangChu, Void>() {
            @Override
            protected DuLieuTrangChu doInBackground() throws Exception {
                DuLieuTrangChu d = controller.layDuLieuTrangChu();
                // Đọc + thu nhỏ poster sẵn ở luồng nền
                for (DongSuatChieu s : d.danhSach) {
                    if (s.poster != null && !cachePoster.containsKey(s.poster) && docPoster != null) {
                        Image goc = docPoster.apply(s.poster);
                        if (goc != null) {
                            cachePoster.put(s.poster, thuNho(goc, POSTER_RONG, POSTER_CAO));
                        }
                    }
                }
                return d;
            }

            @Override
            protected void done() {
                danhSach.removeAll();
                try {
                    DuLieuTrangChu d = get();
                    lblSuat.setText(String.valueOf(d.soSuat));
                    lblVe.setText(String.valueOf(d.soVe));
                    lblDoanhThu.setText(dinhDangSo.format(d.doanhThu));
                    lblPhong.setText(String.valueOf(d.soPhong));

                    if (d.danhSach.isEmpty()) {
                        danhSach.add(nhanThongBao("Hôm nay không có suất chiếu nào.", CHU_XAM));
                    }
                    int stt = 1;
                    for (DongSuatChieu s : d.danhSach) {
                        danhSach.add(taoHang(stt++, s));
                    }
                } catch (Exception e) {
                    Throwable goc = (e.getCause() != null) ? e.getCause() : e;
                    lblSuat.setText("--");
                    lblVe.setText("--");
                    lblDoanhThu.setText("--");
                    lblPhong.setText("--");
                    danhSach.add(nhanThongBao("Lỗi tải dữ liệu: " + goc.getMessage(), Color.RED));
                    goc.printStackTrace();
                }
                danhSach.revalidate();
                danhSach.repaint();
            }
        }.execute();
    }

    private static BufferedImage thuNho(Image goc, int rong, int cao) {
        BufferedImage kq = new BufferedImage(rong, cao, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = kq.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(goc, 0, 0, rong, cao, null);
        g2.dispose();
        return kq;
    }

    private JLabel nhanThongBao(String text, Color mau) {
        JLabel l = nhan(text, Font.PLAIN, 14, mau);
        l.setBorder(BorderFactory.createEmptyBorder(20, 15, 0, 0));
        return l;
    }

    // Đồng hồ chỉ chạy khi panel đang hiển thị
    @Override
    public void addNotify() {
        super.addNotify();
        if (timerDongHo != null) timerDongHo.start();
    }

    @Override
    public void removeNotify() {
        if (timerDongHo != null) timerDongHo.stop();
        super.removeNotify();
    }

    // ===================== BANNER =====================
    private JPanel taoBanner() {
        JPanel p = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x3A, 0x2A, 0x1E),
                        getWidth(), 0, new Color(0x6B, 0x4A, 0x2E)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 125));
        p.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 0, 18);
        p.add(taoIconTron(VeIcon.Loai.NGUOI, new Color(0x5A, 0x43, 0x30), 64), gbc);

        JPanel chu = new JPanel();
        chu.setOpaque(false);
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));

        String ten = (taiKhoan != null && taiKhoan.getHoTen() != null) ? taiKhoan.getHoTen() : "nhân viên";
        JLabel lblChao = new JLabel("Xin chào, " + ten + "!");
        lblChao.setFont(new Font(FONT, Font.BOLD, 26));
        lblChao.setForeground(Color.WHITE);

        JLabel lblPhu = new JLabel("Chúc bạn một ngày làm việc hiệu quả!");
        lblPhu.setFont(new Font(FONT, Font.PLAIN, 15));
        lblPhu.setForeground(new Color(0xF0, 0xE6, 0xD8));

        JLabel lblNgay = new JLabel(tenThu(LocalDate.now()) + ", "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblNgay.setIcon(new VeIcon(VeIcon.Loai.LICH, 16, new Color(0xE0, 0xD2, 0xBE)));
        lblNgay.setIconTextGap(8);
        lblNgay.setFont(new Font(FONT, Font.PLAIN, 13));
        lblNgay.setForeground(new Color(0xE0, 0xD2, 0xBE));

        lblGio = new JLabel();
        lblGio.setFont(new Font(FONT, Font.PLAIN, 13));
        lblGio.setForeground(new Color(0xE0, 0xD2, 0xBE));
        lblGio.setIcon(new VeIcon(VeIcon.Loai.DONG_HO, 16, new Color(0xE0, 0xD2, 0xBE)));
        lblGio.setIconTextGap(8);
        capNhatGio();
        timerDongHo = new Timer(30_000, e -> capNhatGio());

        JPanel dongNgayGio = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dongNgayGio.setOpaque(false);
        dongNgayGio.setAlignmentX(Component.LEFT_ALIGNMENT);
        dongNgayGio.add(lblNgay);
        dongNgayGio.add(Box.createHorizontalStrut(30));
        dongNgayGio.add(lblGio);

        lblChao.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblPhu.setAlignmentX(Component.LEFT_ALIGNMENT);
        chu.add(lblChao);
        chu.add(Box.createVerticalStrut(4));
        chu.add(lblPhu);
        chu.add(Box.createVerticalStrut(10));
        chu.add(dongNgayGio);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        p.add(chu, gbc);

        JLabel trangTri = new JLabel(new VeIcon(VeIcon.Loai.PHIM, 90, new Color(0xF5, 0xC9, 0x6B, 150)));
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        p.add(trangTri, gbc);

        return p;
    }

    private void capNhatGio() {
        lblGio.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    private String tenThu(LocalDate d) {
        int thu = d.getDayOfWeek().getValue(); // 1 = thứ 2 ... 7 = chủ nhật
        return thu == 7 ? "Chủ nhật" : "Thứ " + (thu + 1);
    }

    // ===================== 4 THẺ THỐNG KÊ =====================
    private JPanel taoHangThongKe() {
        JPanel hang = new JPanel(new GridLayout(1, 4, 15, 0));
        hang.setOpaque(false);
        hang.setPreferredSize(new Dimension(0, 100));
        hang.add(taoTheThongKe(VeIcon.Loai.PHIM, NAU_NHAT, "Suất chiếu hôm nay", lblSuat, "suất chiếu"));
        hang.add(taoTheThongKe(VeIcon.Loai.VE, NAU_DAM, "Vé đã bán", lblVe, "vé"));
        hang.add(taoTheThongKe(VeIcon.Loai.TIEN, NAU_NHAT, "Doanh thu hôm nay", lblDoanhThu, "VNĐ"));
        hang.add(taoTheThongKe(VeIcon.Loai.MAN_HINH, NAU_DAM, "Phòng đang chiếu", lblPhong, "phòng"));
        return hang;
    }

    private JPanel taoTheThongKe(VeIcon.Loai bieuTuong, Color mauIcon, String tieuDe, JLabel lblGiaTri, String donVi) {
        JPanel the = new TheBoGoc(new GridBagLayout(), NEN_THE, 18);
        the.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 0, 14);
        the.add(taoIconTron(bieuTuong, mauIcon, 56), gbc);

        JPanel chu = new JPanel();
        chu.setOpaque(false);
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
        JLabel l1 = new JLabel(tieuDe);
        l1.setFont(new Font(FONT, Font.BOLD, 14));
        l1.setForeground(CHU_DEN);
        lblGiaTri.setFont(new Font(FONT, Font.BOLD, 28));
        lblGiaTri.setForeground(CHU_DEN);
        JLabel l3 = new JLabel(donVi);
        l3.setFont(new Font(FONT, Font.PLAIN, 12));
        l3.setForeground(CHU_XAM);
        chu.add(l1);
        chu.add(lblGiaTri);
        chu.add(l3);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        the.add(chu, gbc);

        JLabel mui = new JLabel("\u203A");
        mui.setFont(new Font(FONT, Font.PLAIN, 26));
        mui.setForeground(CHU_XAM);
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        the.add(mui, gbc);

        return the;
    }

    // ===================== BẢNG SUẤT CHIẾU HÔM NAY =====================
    private JPanel taoBangSuatChieu() {
        JPanel the = new TheBoGoc(new BorderLayout(0, 12), NEN_THE, 18);
        the.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel dauThe = new JPanel(new BorderLayout());
        dauThe.setOpaque(false);
        JLabel lblTieuDe = new JLabel("Suất chiếu hôm nay");
        lblTieuDe.setIcon(new VeIcon(VeIcon.Loai.PHIM, 24, NAU_DAM));
        lblTieuDe.setIconTextGap(10);
        lblTieuDe.setFont(new Font(FONT, Font.BOLD, 18));
        lblTieuDe.setForeground(CHU_DEN);
        dauThe.add(lblTieuDe, BorderLayout.WEST);

        JLabel lblNgay = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblNgay.setIcon(new VeIcon(VeIcon.Loai.LICH, 16, CHU_DEN));
        lblNgay.setIconTextGap(8);
        lblNgay.setFont(new Font(FONT, Font.PLAIN, 13));
        lblNgay.setForeground(CHU_DEN);
        lblNgay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VIEN, 1, true),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        dauThe.add(lblNgay, BorderLayout.EAST);
        the.add(dauThe, BorderLayout.NORTH);

        String[] ten = {"STT", "Tên phim", "Phòng", "Giờ chiếu", "Trạng thái", "Thao tác"};
        Component[] cotDau = new Component[6];
        for (int i = 0; i < 6; i++) {
            cotDau[i] = nhan(ten[i], Font.BOLD, 13, Color.WHITE);
        }
        JPanel header = ghepHang(cotDau, NEN_HEADER);
        header.setPreferredSize(new Dimension(0, 40));

        danhSach = new JPanel();
        danhSach.setLayout(new BoxLayout(danhSach, BoxLayout.Y_AXIS));
        danhSach.setBackground(Color.WHITE);
        danhSach.add(nhanThongBao("Đang tải dữ liệu...", CHU_XAM));

        JScrollPane scroll = new JScrollPane(danhSach);
        scroll.setBorder(BorderFactory.createLineBorder(VIEN, 1));
        scroll.setColumnHeaderView(header);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        the.add(scroll, BorderLayout.CENTER);

        return the;
    }

    private JPanel taoHang(int stt, DongSuatChieu d) {
        JPanel cotPhim = new JPanel(new BorderLayout(10, 0));
        cotPhim.setOpaque(false);
        cotPhim.add(taoPosterNho(d), BorderLayout.WEST);
        cotPhim.add(nhan(d.tenPhim, Font.PLAIN, 14, CHU_DEN), BorderLayout.CENTER);

        LocalTime bayGio = LocalTime.now();
        String trangThai;
        Color nen, chu;
        if (bayGio.isBefore(d.gioBatDau)) {
            trangThai = "Sắp chiếu";
            nen = new Color(0xFF, 0xE6, 0xB3);
            chu = new Color(0xB2, 0x6A, 0x00);
        } else if (bayGio.isAfter(d.gioKetThuc)) {
            trangThai = "Đã kết thúc";
            nen = new Color(0xE3, 0xE6, 0xEA);
            chu = new Color(0x5A, 0x65, 0x70);
        } else {
            trangThai = "Đang chiếu";
            nen = new Color(0xD5, 0xF0, 0xD9);
            chu = new Color(0x2E, 0x7D, 0x32);
        }

        JButton btnChiTiet = new JButton("Xem chi tiết");
        btnChiTiet.setIcon(new VeIcon(VeIcon.Loai.MAT, 16, Color.WHITE));
        btnChiTiet.setIconTextGap(8);
        btnChiTiet.setFont(new Font(FONT, Font.PLAIN, 12));
        btnChiTiet.setForeground(Color.WHITE);
        btnChiTiet.setBackground(NEN_HEADER);
        btnChiTiet.setFocusPainted(false);
        btnChiTiet.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnChiTiet.putClientProperty("JButton.buttonType", "roundRect");
        btnChiTiet.addActionListener(e -> hienChiTiet(d, trangThai));

        Component[] cot = {
            nhan(String.valueOf(stt), Font.PLAIN, 14, CHU_DEN),
            cotPhim,
            nhan(d.tenPhong, Font.PLAIN, 13, CHU_DEN),
            nhan(d.gioBatDau.format(DateTimeFormatter.ofPattern("HH:mm")), Font.BOLD, 13, CHU_DEN),
            new NhanTrangThai(trangThai, nen, chu),
            btnChiTiet
        };
        JPanel hang = ghepHang(cot, Color.WHITE);
        hang.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xEF, 0xE7, 0xDA)));
        hang.setPreferredSize(new Dimension(0, CAO_HANG));
        hang.setMaximumSize(new Dimension(Integer.MAX_VALUE, CAO_HANG));
        return hang;
    }

    // Ghép các ô thành 1 dòng; cột thứ 2 (tên phim) co giãn, các cột còn lại rộng cố định
    private JPanel ghepHang(Component[] cot, Color nen) {
        JPanel hang = new JPanel(new GridBagLayout());
        hang.setBackground(nen);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        for (int i = 0; i < cot.length; i++) {
            JPanel o = new JPanel(new GridBagLayout());
            o.setOpaque(false);
            GridBagConstraints g = new GridBagConstraints();
            g.weightx = 1;
            g.anchor = (i == 1) ? GridBagConstraints.WEST : GridBagConstraints.CENTER;
            g.fill = (i == 1) ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
            o.add(cot[i], g);
            o.setPreferredSize(new Dimension(RONG_COT[i], 10));

            gbc.gridx = i;
            gbc.weightx = (i == 1) ? 1 : 0;
            gbc.insets = (i == 1) ? new Insets(0, 8, 0, 8) : new Insets(0, 0, 0, 0);
            hang.add(o, gbc);
        }
        return hang;
    }

    private JComponent taoPosterNho(DongSuatChieu d) {
        Image anh = (d.poster != null) ? cachePoster.get(d.poster) : null;
        if (anh != null) {
            JLabel l = new JLabel(new ImageIcon(anh));
            l.setPreferredSize(new Dimension(POSTER_RONG, POSTER_CAO));
            return l;
        }
        JLabel l = new JLabel(d.tenPhim.isEmpty() ? "?" : d.tenPhim.substring(0, 1).toUpperCase(), SwingConstants.CENTER);
        l.setFont(new Font(FONT, Font.BOLD, 16));
        l.setForeground(NAU_NHAT);
        l.setOpaque(true);
        l.setBackground(NEN_KEM);
        l.setBorder(BorderFactory.createLineBorder(VIEN, 1));
        l.setPreferredSize(new Dimension(POSTER_RONG, POSTER_CAO));
        return l;
    }

    private void hienChiTiet(DongSuatChieu d, String trangThai) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("HH:mm");
        JOptionPane.showMessageDialog(this,
                "Phim: " + d.tenPhim
                + "\nPhòng: " + d.tenPhong
                + "\nGiờ chiếu: " + d.gioBatDau.format(f) + " - " + d.gioKetThuc.format(f)
                + "\nTrạng thái: " + trangThai,
                "Chi tiết suất chiếu " + d.maSuat, JOptionPane.INFORMATION_MESSAGE);
    }

    // ===================== CÁC THÀNH PHẦN DÙNG CHUNG =====================
    private JLabel nhan(String text, int kieu, int co, Color mau) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, kieu, co));
        l.setForeground(mau);
        return l;
    }

    private JLabel taoIconTron(VeIcon.Loai loai, Color nen, int kichThuoc) {
        JLabel l = new JLabel(new VeIcon(loai, kichThuoc / 2, Color.WHITE), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(nen);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setPreferredSize(new Dimension(kichThuoc, kichThuoc));
        return l;
    }

    private static class TheBoGoc extends JPanel {
        private final Color nen;
        private final int banKinh;

        TheBoGoc(LayoutManager lm, Color nen, int banKinh) {
            super(lm);
            this.nen = nen;
            this.banKinh = banKinh;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(nen);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, banKinh, banKinh);
            g2.setColor(VIEN);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, banKinh, banKinh);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class NhanTrangThai extends JLabel {
        private final Color nen;

        NhanTrangThai(String text, Color nen, Color chu) {
            super(text, SwingConstants.CENTER);
            this.nen = nen;
            setForeground(chu);
            setFont(new Font(FONT, Font.BOLD, 12));
            setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(nen);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ===== Icon vẽ bằng code (không phụ thuộc font emoji) =====
    private static class VeIcon implements Icon {
        enum Loai { NGUOI, PHIM, VE, TIEN, MAN_HINH, LICH, DONG_HO, MAT }

        private final Loai loai;
        private final int s;
        private final Color mau;

        VeIcon(Loai loai, int kichThuoc, Color mau) {
            this.loai = loai;
            this.s = kichThuoc;
            this.mau = mau;
        }

        @Override public int getIconWidth() { return s; }
        @Override public int getIconHeight() { return s; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.setColor(mau);
            float w = Math.max(1.5f, Math.min(s / 12f, 4f));
            g2.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double p = w;
            double d = s - 2 * p;

            switch (loai) {
                case NGUOI -> {
                    g2.draw(new Ellipse2D.Double(s * 0.32, p, s * 0.36, s * 0.36));
                    g2.draw(new Arc2D.Double(s * 0.14, s * 0.55, s * 0.72, s * 0.8, 0, 180, Arc2D.OPEN));
                }
                case PHIM -> {
                    g2.draw(new RoundRectangle2D.Double(p, s * 0.2, d, s * 0.6, 4, 4));
                    g2.draw(new java.awt.geom.Line2D.Double(s * 0.3, s * 0.2, s * 0.3, s * 0.8));
                    g2.draw(new java.awt.geom.Line2D.Double(s * 0.7, s * 0.2, s * 0.7, s * 0.8));
                    for (int i = 0; i < 3; i++) {
                        double yy = s * (0.32 + 0.18 * i);
                        g2.draw(new java.awt.geom.Line2D.Double(p, yy, s * 0.3, yy));
                        g2.draw(new java.awt.geom.Line2D.Double(s * 0.7, yy, s - p, yy));
                    }
                }
                case VE -> {
                    g2.draw(new RoundRectangle2D.Double(p, s * 0.25, d, s * 0.5, s * 0.15, s * 0.15));
                    g2.setStroke(new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                            1f, new float[]{w * 1.2f, w * 1.2f}, 0));
                    g2.draw(new java.awt.geom.Line2D.Double(s * 0.68, s * 0.27, s * 0.68, s * 0.73));
                }
                case TIEN -> {
                    g2.draw(new Ellipse2D.Double(p, p, d, d));
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, (int) (s * 0.55))));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (s - fm.stringWidth("$")) / 2;
                    int ty = (s - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString("$", tx, ty);
                }
                case MAN_HINH -> {
                    g2.draw(new RoundRectangle2D.Double(p, s * 0.15, d, s * 0.5, 4, 4));
                    g2.draw(new java.awt.geom.Line2D.Double(s * 0.5, s * 0.65, s * 0.5, s * 0.85));
                    g2.draw(new java.awt.geom.Line2D.Double(s * 0.3, s * 0.85, s * 0.7, s * 0.85));
                }
                case LICH -> {
                    g2.draw(new RoundRectangle2D.Double(p, s * 0.18, d, s * 0.7, 4, 4));
                    g2.draw(new java.awt.geom.Line2D.Double(p, s * 0.4, s - p, s * 0.4));
                    g2.draw(new java.awt.geom.Line2D.Double(s * 0.3, p, s * 0.3, s * 0.28));
                    g2.draw(new java.awt.geom.Line2D.Double(s * 0.7, p, s * 0.7, s * 0.28));
                }
                case DONG_HO -> {
                    g2.draw(new Ellipse2D.Double(p, p, d, d));
                    g2.draw(new java.awt.geom.Line2D.Double(s / 2.0, s / 2.0, s / 2.0, s * 0.25));
                    g2.draw(new java.awt.geom.Line2D.Double(s / 2.0, s / 2.0, s * 0.7, s * 0.6));
                }
                case MAT -> {
                    g2.draw(new Ellipse2D.Double(p, s * 0.28, d, s * 0.44));
                    g2.fill(new Ellipse2D.Double(s * 0.38, s * 0.38, s * 0.24, s * 0.24));
                }
            }
            g2.dispose();
        }
    }
}