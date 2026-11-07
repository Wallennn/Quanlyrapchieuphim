package view;

import controller.BanVeController;
import controller.DoTuoiController;
import controller.ThanhToanController;
import controller.BanVeController.Combo;
import controller.BanVeController.Ghe;
import controller.BanVeController.GheDaBanException;
import controller.BanVeController.SoDoGhe;
import controller.BanVeController.SuatBan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.QuadCurve2D;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Màn hình "Bán vé":
 *  [Cột trái] chọn ngày + suất chiếu  |  [Giữa] sơ đồ ghế  |  [Phải] hóa đơn + combo + thanh toán.
 * Mọi truy vấn database chạy ở luồng nền nên giao diện không bị đứng.
 */
public class BanVePanel extends JPanel {

    private static final Color NAU_DAM    = new Color(0x8A, 0x6D, 0x4A);
    private static final Color NAU_NHAT   = new Color(0xB2, 0x90, 0x6B);
    private static final Color NEN_KEM    = new Color(0xFB, 0xF6, 0xEF);
    private static final Color NEN_THE    = new Color(0xFF, 0xFD, 0xF9);
    private static final Color NEN_HEADER = new Color(0x3A, 0x30, 0x28);
    private static final Color CHU_XAM    = new Color(0x8A, 0x87, 0x82);
    private static final Color CHU_DEN    = new Color(0x2E, 0x2A, 0x26);
    private static final Color VIEN       = new Color(0xE5, 0xDD, 0xD0);

    private static final String FONT = "Segoe UI";
    private static final String TAT_CA = "Tất cả phim";
    private static final int TOI_DA_GHE = 8;      // số vé tối đa trong 1 hóa đơn
    private static final int TOI_DA_COMBO = 10;   // số lượng tối đa mỗi loại combo
    private static final int GHE_RONG = 34, GHE_CAO = 30;

    private static final DateTimeFormatter DD = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HH = DateTimeFormatter.ofPattern("HH:mm");

    private final BanVeController controller = new BanVeController();
    private final ThanhToanController thanhToanController = new ThanhToanController();
    private Map<String, String> doTuoiTheoPhim = new HashMap<>();   // ten phim -> P / K / T13 / T16 / T18
    private final NumberFormat dinhDangSo = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    private final long idNhanVien;

    // ----- Trạng thái -----
    private LocalDate ngay = LocalDate.now();
    private List<SuatBan> dsSuat = new ArrayList<>();
    private SuatBan suatDangChon;
    private SoDoGhe soDoHienTai;
    private final Map<String, Ghe> gheDangChon = new LinkedHashMap<>();   // maGhe -> ghế
    private List<Combo> dsCombo = new ArrayList<>();
    private final Map<String, Integer> soLuongCombo = new LinkedHashMap<>();
    private final Map<String, JLabel> nhanSoLuongCombo = new HashMap<>();
    private final Map<String, TheSuat> cacTheSuat = new LinkedHashMap<>();
    private int phienSuat = 0;   // để bỏ qua kết quả tải cũ khi người dùng đã đổi ý
    private int phienGhe = 0;
    private boolean dangNapLoc = false;

    // ----- Thành phần giao diện -----
    private JLabel lblNgay;
    private JButton btnTruoc;
    private JComboBox<String> cboPhim;
    private JPanel listSuat;
    private JPanel khuGhe;
    private JLabel lblPhim, lblThongTin, lblTong;
    private JPanel listHoaDon;
    private JPanel khuCombo;
    private JButton btnThanhToan, btnBoChon;

    /**
     * @param idNhanVien     id tài khoản đang đăng nhập (ghi vào hóa đơn)
     * @param maSuatChonSan  mã suất cần mở sẵn (vd: bấm giờ chiếu ở tab Danh sách phim); null nếu không có
     */
    public BanVePanel(long idNhanVien, String maSuatChonSan) {
        this.idNhanVien = idNhanVien;

        setLayout(new BorderLayout(0, 15));
        setOpaque(false);
        add(taoThanhTren(), BorderLayout.NORTH);

        JPanel noiDung = new JPanel(new BorderLayout(15, 0));
        noiDung.setOpaque(false);
        noiDung.add(taoCotSuatChieu(), BorderLayout.WEST);
        noiDung.add(taoCotSoDoGhe(), BorderLayout.CENTER);
        noiDung.add(taoCotHoaDon(), BorderLayout.EAST);
        add(noiDung, BorderLayout.CENTER);

        hienThiChuaChonSuat();
        capNhatNhanNgay();
        capNhatHoaDon();

        chayNen(controller::layDanhSachCombo, ds -> {
            dsCombo = ds;
            veKhuCombo();
            capNhatHoaDon();
        });
        taiSuat(ngay, maSuatChonSan, maSuatChonSan != null);
    }

    // ===================== THANH TRÊN: TIÊU ĐỀ + CHỌN NGÀY =====================
    private JPanel taoThanhTren() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel tieuDe = new JLabel("Bán vé");
        tieuDe.setFont(new Font(FONT, Font.BOLD, 22));
        tieuDe.setForeground(CHU_DEN);
        p.add(tieuDe, BorderLayout.WEST);

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        phai.setOpaque(false);

        btnTruoc = nutNho("\u2039");
        btnTruoc.addActionListener(e -> {
            if (ngay.isAfter(LocalDate.now())) taiSuat(ngay.minusDays(1), null, false);
        });

        lblNgay = new JLabel("", SwingConstants.CENTER);
        lblNgay.setFont(new Font(FONT, Font.BOLD, 14));
        lblNgay.setForeground(CHU_DEN);
        lblNgay.setPreferredSize(new Dimension(170, 32));
        lblNgay.setBorder(BorderFactory.createLineBorder(VIEN, 1, true));
        lblNgay.setOpaque(true);
        lblNgay.setBackground(Color.WHITE);

        JButton btnSau = nutNho("\u203A");
        btnSau.addActionListener(e -> taiSuat(ngay.plusDays(1), null, false));

        JButton btnHomNay = nutNho("Hôm nay");
        btnHomNay.setFont(new Font(FONT, Font.BOLD, 13));
        btnHomNay.setPreferredSize(new Dimension(90, 32));
        btnHomNay.addActionListener(e -> taiSuat(LocalDate.now(), null, false));

        phai.add(btnTruoc);
        phai.add(lblNgay);
        phai.add(btnSau);
        phai.add(btnHomNay);
        p.add(phai, BorderLayout.EAST);
        return p;
    }

    private JButton nutNho(String chu) {
        JButton b = new JButton(chu);
        b.setFont(new Font(FONT, Font.BOLD, 16));
        b.setForeground(CHU_DEN);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(0, 4, 0, 4));
        b.setPreferredSize(new Dimension(36, 32));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private void capNhatNhanNgay() {
        lblNgay.setText(tenThu(ngay) + ", " + ngay.format(DD));
        btnTruoc.setEnabled(ngay.isAfter(LocalDate.now()));
    }

    private String tenThu(LocalDate d) {
        int thu = d.getDayOfWeek().getValue();
        return thu == 7 ? "Chủ nhật" : "Thứ " + (thu + 1);
    }

    // ===================== CỘT TRÁI: SUẤT CHIẾU =====================
    private JPanel taoCotSuatChieu() {
        JPanel the = new TheBoGoc(new BorderLayout(0, 10), NEN_THE, 18);
        the.setPreferredSize(new Dimension(320, 0));
        the.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel dau = new JPanel(new BorderLayout(0, 10));
        dau.setOpaque(false);
        dau.add(tieuDeThe("Chọn suất chiếu"), BorderLayout.NORTH);

        cboPhim = new JComboBox<>();
        cboPhim.setFont(new Font(FONT, Font.PLAIN, 13));
        cboPhim.addItem(TAT_CA);
        cboPhim.addActionListener(e -> {
            if (!dangNapLoc) veDanhSachSuat();
        });
        dau.add(cboPhim, BorderLayout.SOUTH);
        the.add(dau, BorderLayout.NORTH);

        listSuat = new JPanel();
        listSuat.setLayout(new BoxLayout(listSuat, BoxLayout.Y_AXIS));
        listSuat.setOpaque(false);
        JScrollPane sp = new JScrollPane(listSuat);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        the.add(sp, BorderLayout.CENTER);
        return the;
    }

    private void taiSuat(LocalDate canTai, String maSuatCanChon, boolean layNgayTuSuat) {
        final int phien = ++phienSuat;
        phienGhe++; // hủy việc tải sơ đồ ghế đang chạy dở (nếu có)

        listSuat.removeAll();
        listSuat.add(nhanThongBao("Đang tải suất chiếu..."));
        listSuat.revalidate();
        listSuat.repaint();

        chayNen(() -> {
            LocalDate n = canTai;
            if (layNgayTuSuat && maSuatCanChon != null) {
                SuatBan truoc = controller.layMotSuat(maSuatCanChon);
                if (truoc != null) n = truoc.ngay;
            }
            KetQuaSuat kq = new KetQuaSuat();
            kq.ngay = n;
            kq.ds = controller.laySuatTheoNgay(n);
            try {
                kq.doTuoi = new DoTuoiController().layTheoTenPhim();
            } catch (Exception e) {
                // chua chay file nang cap CSDL (cot do_tuoi chua co): bo qua, khong hien nhan do tuoi
            }
            return kq;
        }, kq -> {
            if (phien != phienSuat) return;
            ngay = kq.ngay;
            dsSuat = kq.ds;
            doTuoiTheoPhim = kq.doTuoi;
            capNhatNhanNgay();
            napLocPhim();
            veDanhSachSuat();

            SuatBan chon = null;
            if (maSuatCanChon != null) {
                for (SuatBan s : dsSuat) {
                    if (s.maSuat.equals(maSuatCanChon)) {
                        chon = s;
                        break;
                    }
                }
            }
            if (chon != null) chonSuat(chon);
            else resetChonSuat();
        });
    }

    private static class KetQuaSuat {
        LocalDate ngay;
        List<SuatBan> ds;
        Map<String, String> doTuoi = new HashMap<>();
    }

    private void napLocPhim() {
        Object dangChon = cboPhim.getSelectedItem();
        Set<String> tenPhim = new LinkedHashSet<>();
        for (SuatBan s : dsSuat) tenPhim.add(s.tenPhim);

        dangNapLoc = true;
        cboPhim.removeAllItems();
        cboPhim.addItem(TAT_CA);
        for (String t : tenPhim) cboPhim.addItem(t);
        cboPhim.setSelectedItem(dangChon != null && tenPhim.contains(dangChon.toString()) ? dangChon : TAT_CA);
        dangNapLoc = false;
    }

    private void veDanhSachSuat() {
        listSuat.removeAll();
        cacTheSuat.clear();

        Object loc = cboPhim.getSelectedItem();
        boolean locTheoPhim = loc != null && !TAT_CA.equals(loc.toString());
        int dem = 0;
        for (SuatBan s : dsSuat) {
            if (locTheoPhim && !s.tenPhim.equals(loc.toString())) continue;
            TheSuat the = new TheSuat(s);
            the.setChon(suatDangChon != null && suatDangChon.maSuat.equals(s.maSuat));
            cacTheSuat.put(s.maSuat, the);
            listSuat.add(the);
            listSuat.add(Box.createVerticalStrut(8));
            dem++;
        }
        if (dem == 0) {
            listSuat.add(nhanThongBao(ngay.equals(LocalDate.now())
                    ? "Hôm nay không còn suất chiếu nào."
                    : "Ngày này không có suất chiếu."));
        }
        listSuat.revalidate();
        listSuat.repaint();
    }

    // Thẻ 1 suất chiếu trong danh sách bên trái
    private class TheSuat extends JPanel {
        private final SuatBan suat;
        private boolean chon;

        TheSuat(SuatBan s) {
            super(new BorderLayout(0, 3));
            this.suat = s;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(100, 82));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel lblGio = new JLabel(s.gioBatDau.format(HH) + " \u2013 " + s.gioKetThuc.format(HH));
            lblGio.setFont(new Font(FONT, Font.BOLD, 15));
            lblGio.setForeground(NAU_DAM);
            JLabel lblPhong = new JLabel(s.tenPhong);
            lblPhong.setFont(new Font(FONT, Font.PLAIN, 12));
            lblPhong.setForeground(CHU_XAM);
            JPanel dong1 = new JPanel(new BorderLayout());
            dong1.setOpaque(false);
            dong1.add(lblGio, BorderLayout.WEST);
            dong1.add(lblPhong, BorderLayout.EAST);

            JLabel lblTenPhim = new JLabel(s.tenPhim);
            lblTenPhim.setFont(new Font(FONT, Font.PLAIN, 14));
            lblTenPhim.setForeground(CHU_DEN);

            long giaThapNhat = Math.min(s.giaThuong, Math.min(s.giaVip, s.giaDoi));
            JLabel lblInfo = new JLabel("Còn " + Math.max(0, s.tongGhe - s.daBan) + "/" + s.tongGhe
                    + " ghế  \u2022  từ " + tien(giaThapNhat));
            lblInfo.setFont(new Font(FONT, Font.PLAIN, 12));
            lblInfo.setForeground(CHU_XAM);

            add(dong1, BorderLayout.NORTH);
            JPanel dongPhim = new JPanel(new BorderLayout(6, 0));
            dongPhim.setOpaque(false);
            dongPhim.add(lblTenPhim, BorderLayout.CENTER);
            String dt = doTuoiTheoPhim.get(s.tenPhim);
            if (dt != null) {
                JPanel bocChip = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                bocChip.setOpaque(false);
                bocChip.add(new DoTuoiChip(dt));
                dongPhim.add(bocChip, BorderLayout.EAST);
            }
            add(dongPhim, BorderLayout.CENTER);
            add(lblInfo, BorderLayout.SOUTH);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    chonSuat(suat);
                }
            });
        }

        void setChon(boolean chon) {
            this.chon = chon;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(chon ? NEN_KEM : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(chon ? NAU_DAM : VIEN);
            g2.setStroke(new BasicStroke(chon ? 2f : 1f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ===================== CỘT GIỮA: SƠ ĐỒ GHẾ =====================
    private JPanel taoCotSoDoGhe() {
        JPanel the = new TheBoGoc(new BorderLayout(0, 10), NEN_THE, 18);
        the.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        the.add(tieuDeThe("Chọn ghế"), BorderLayout.NORTH);

        khuGhe = new JPanel(new BorderLayout());
        khuGhe.setOpaque(false);
        the.add(khuGhe, BorderLayout.CENTER);

        the.add(taoChuThich(), BorderLayout.SOUTH);
        return the;
    }

    private void hienThiChuaChonSuat() {
        hienThiThongBaoGiua("Chọn một suất chiếu ở bên trái để xem sơ đồ ghế");
    }

    private void hienThiThongBaoGiua(String noiDung) {
        khuGhe.removeAll();
        JLabel l = new JLabel(noiDung, SwingConstants.CENTER);
        l.setFont(new Font(FONT, Font.PLAIN, 15));
        l.setForeground(CHU_XAM);
        khuGhe.add(l, BorderLayout.CENTER);
        khuGhe.revalidate();
        khuGhe.repaint();
    }

    private void chonSuat(SuatBan s) {
        suatDangChon = s;
        gheDangChon.clear();
        soDoHienTai = null;
        for (TheSuat t : cacTheSuat.values()) {
            boolean laSuatNay = t.suat.maSuat.equals(s.maSuat);
            t.setChon(laSuatNay);
            if (laSuatNay) {
                // cuộn danh sách tới suất đang chọn (khi mở sẵn từ tab Danh sách phim)
                SwingUtilities.invokeLater(() -> t.scrollRectToVisible(t.getBounds()));
            }
        }
        capNhatHoaDon();

        final int phien = ++phienGhe;
        hienThiThongBaoGiua("Đang tải sơ đồ ghế...");
        chayNen(() -> controller.laySoDoGhe(s.maSuat), sd -> {
            if (phien != phienGhe) return;
            soDoHienTai = sd;
            veSoDoGhe(sd);
        });
    }

    private void resetChonSuat() {
        suatDangChon = null;
        soDoHienTai = null;
        gheDangChon.clear();
        phienGhe++;
        for (TheSuat t : cacTheSuat.values()) t.setChon(false);
        hienThiChuaChonSuat();
        capNhatHoaDon();
    }

    private void veSoDoGhe(SoDoGhe sd) {
        Map<String, Ghe> theoViTri = new HashMap<>();
        for (Ghe g : sd.dsGhe) theoViTri.put(g.hang + g.so, g);

        JPanel luoi = new JPanel(new GridBagLayout());
        luoi.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);

        for (int r = 0; r < sd.soHang; r++) {
            String hang = String.valueOf((char) ('A' + r));
            gbc.gridy = r;

            gbc.gridx = 0;
            luoi.add(nhanHang(hang), gbc);
            for (int c = 1; c <= sd.soCot; c++) {
                gbc.gridx = c;
                Ghe g = theoViTri.get(hang + c);
                if (g == null) {
                    luoi.add(Box.createRigidArea(new Dimension(GHE_RONG, GHE_CAO)), gbc);
                } else {
                    luoi.add(taoNutGhe(g), gbc);
                }
            }
            gbc.gridx = sd.soCot + 1;
            luoi.add(nhanHang(hang), gbc);
        }

        JPanel boc = new JPanel(new GridBagLayout()); // canh giữa sơ đồ
        boc.setOpaque(false);
        GridBagConstraints gbBoc = new GridBagConstraints();
        gbBoc.anchor = GridBagConstraints.NORTH;
        gbBoc.weightx = 1;
        gbBoc.weighty = 1;
        boc.add(luoi, gbBoc);

        JPanel giua = new JPanel(new BorderLayout(0, 14));
        giua.setOpaque(false);
        giua.add(new ManHinh(), BorderLayout.NORTH);
        giua.add(boc, BorderLayout.CENTER);

        JScrollPane sp = new JScrollPane(giua);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        khuGhe.removeAll();
        khuGhe.add(sp, BorderLayout.CENTER);
        khuGhe.revalidate();
        khuGhe.repaint();
    }

    private JLabel nhanHang(String hang) {
        JLabel l = new JLabel(hang, SwingConstants.CENTER);
        l.setFont(new Font(FONT, Font.BOLD, 13));
        l.setForeground(CHU_XAM);
        l.setPreferredSize(new Dimension(22, GHE_CAO));
        return l;
    }

    private NutGhe taoNutGhe(Ghe g) {
        NutGhe nut = new NutGhe(g);
        if (g.daBan) {
            nut.setToolTipText(g.hang + g.so + " - đã bán");
            return nut;
        }
        nut.setToolTipText(g.hang + g.so + " - " + tenLoai(g.loai)
                + (suatDangChon != null ? " - " + tien(suatDangChon.giaTheoLoai(g.loai)) : ""));
        nut.addActionListener(e -> {
            if (gheDangChon.containsKey(g.maGhe)) {
                gheDangChon.remove(g.maGhe);
                nut.setChon(false);
            } else {
                if (gheDangChon.size() >= TOI_DA_GHE) {
                    JOptionPane.showMessageDialog(this,
                            "Mỗi hóa đơn chỉ bán tối đa " + TOI_DA_GHE + " vé.",
                            "Giới hạn số vé", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                gheDangChon.put(g.maGhe, g);
                nut.setChon(true);
            }
            capNhatHoaDon();
        });
        return nut;
    }

    private static Color mauNenGhe(String loai) {
        if ("VIP".equals(loai)) return new Color(0xE3, 0xC9, 0xA3);
        if ("Doi".equals(loai)) return new Color(0xF0, 0xCF, 0xC6);
        return new Color(0xF6, 0xEE, 0xE0);
    }

    private static Color mauVienGhe(String loai) {
        if ("VIP".equals(loai)) return new Color(0xA9, 0x84, 0x52);
        if ("Doi".equals(loai)) return new Color(0xC0, 0x7A, 0x66);
        return new Color(0xC9, 0xB6, 0x98);
    }

    private static final Color NEN_GHE_DA_BAN = new Color(0xDD, 0xDA, 0xD5);
    private static final Color VIEN_GHE_DA_BAN = new Color(0xCF, 0xCB, 0xC4);

    // Nút ghế tự vẽ: màu theo loại ghế / đã bán / đang chọn
    private static class NutGhe extends JButton {
        private final Ghe ghe;
        private boolean chon = false;

        NutGhe(Ghe g) {
            super(String.valueOf(g.so));
            this.ghe = g;
            setPreferredSize(new Dimension(GHE_RONG, GHE_CAO));
            setFont(new Font(FONT, Font.BOLD, 11));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setMargin(new Insets(0, 0, 0, 0));
            if (g.daBan) {
                setEnabled(false);
            } else {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }

        void setChon(boolean chon) {
            this.chon = chon;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color nen, vien, chu;
            if (ghe.daBan) {
                nen = NEN_GHE_DA_BAN;
                vien = VIEN_GHE_DA_BAN;
                chu = new Color(0xA5, 0xA1, 0x9A);
            } else if (chon) {
                nen = NEN_HEADER;
                vien = NEN_HEADER;
                chu = Color.WHITE;
            } else {
                nen = mauNenGhe(ghe.loai);
                vien = mauVienGhe(ghe.loai);
                chu = CHU_DEN;
            }
            g2.setColor(nen);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(vien);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String text = String.valueOf(ghe.so);
            g2.setColor(chu);
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // Thanh "MÀN HÌNH" cong phía trên sơ đồ ghế
    private static class ManHinh extends JPanel {
        ManHinh() {
            setOpaque(false);
            setPreferredSize(new Dimension(100, 46));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(NAU_NHAT);
            g2.draw(new QuadCurve2D.Double(w * 0.12, 20, w * 0.5, 2, w * 0.88, 20));

            g2.setFont(new Font(FONT, Font.BOLD, 12));
            g2.setColor(CHU_XAM);
            FontMetrics fm = g2.getFontMetrics();
            String t = "MÀN HÌNH";
            g2.drawString(t, (w - fm.stringWidth(t)) / 2, 40);
            g2.dispose();
        }
    }

    // Chú thích màu ghế
    private JPanel taoChuThich() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 4));
        p.setOpaque(false);
        p.add(muc(mauNenGhe("Thuong"), mauVienGhe("Thuong"), "Thường"));
        p.add(muc(mauNenGhe("VIP"), mauVienGhe("VIP"), "VIP"));
        p.add(muc(mauNenGhe("Doi"), mauVienGhe("Doi"), "Đôi"));
        p.add(muc(NEN_HEADER, NEN_HEADER, "Đang chọn"));
        p.add(muc(NEN_GHE_DA_BAN, VIEN_GHE_DA_BAN, "Đã bán"));
        return p;
    }

    private JPanel muc(Color nen, Color vien, String ten) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        JComponent o = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(nen);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.setColor(vien);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
            }
        };
        o.setPreferredSize(new Dimension(18, 16));
        JLabel l = new JLabel(ten);
        l.setFont(new Font(FONT, Font.PLAIN, 12));
        l.setForeground(CHU_XAM);
        p.add(o);
        p.add(l);
        return p;
    }

    // ===================== CỘT PHẢI: HÓA ĐƠN =====================
    private JPanel taoCotHoaDon() {
        JPanel the = new TheBoGoc(new BorderLayout(0, 10), NEN_THE, 18);
        the.setPreferredSize(new Dimension(310, 0));
        the.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        the.add(tieuDeThe("Thông tin vé"), BorderLayout.NORTH);

        // --- Giữa: thông tin suất + danh sách ghế/combo đã chọn ---
        JPanel giua = new JPanel(new BorderLayout(0, 8));
        giua.setOpaque(false);

        JPanel thongTin = new JPanel();
        thongTin.setOpaque(false);
        thongTin.setLayout(new BoxLayout(thongTin, BoxLayout.Y_AXIS));
        lblPhim = new JLabel(" ");
        lblPhim.setFont(new Font(FONT, Font.BOLD, 16));
        lblPhim.setForeground(CHU_DEN);
        lblPhim.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblThongTin = new JLabel(" ");
        lblThongTin.setFont(new Font(FONT, Font.PLAIN, 12));
        lblThongTin.setForeground(CHU_XAM);
        lblThongTin.setAlignmentX(Component.LEFT_ALIGNMENT);
        thongTin.add(lblPhim);
        thongTin.add(Box.createVerticalStrut(3));
        thongTin.add(lblThongTin);
        giua.add(thongTin, BorderLayout.NORTH);

        listHoaDon = new JPanel();
        listHoaDon.setLayout(new BoxLayout(listHoaDon, BoxLayout.Y_AXIS));
        listHoaDon.setOpaque(false);
        JScrollPane sp = new JScrollPane(listHoaDon);
        sp.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, VIEN));
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        giua.add(sp, BorderLayout.CENTER);
        the.add(giua, BorderLayout.CENTER);

        // --- Dưới: combo + tổng tiền + nút ---
        JPanel duoi = new JPanel();
        duoi.setOpaque(false);
        duoi.setLayout(new BoxLayout(duoi, BoxLayout.Y_AXIS));

        khuCombo = new JPanel();
        khuCombo.setOpaque(false);
        khuCombo.setLayout(new BoxLayout(khuCombo, BoxLayout.Y_AXIS));
        khuCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        duoi.add(khuCombo);

        JPanel dongTong = new JPanel(new BorderLayout());
        dongTong.setOpaque(false);
        dongTong.setAlignmentX(Component.LEFT_ALIGNMENT);
        dongTong.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        JLabel lblTongNhan = new JLabel("Tạm tính");
        lblTongNhan.setFont(new Font(FONT, Font.PLAIN, 14));
        lblTongNhan.setForeground(CHU_XAM);
        lblTong = new JLabel("0đ");
        lblTong.setFont(new Font(FONT, Font.BOLD, 22));
        lblTong.setForeground(NAU_DAM);
        dongTong.add(lblTongNhan, BorderLayout.WEST);
        dongTong.add(lblTong, BorderLayout.EAST);
        dongTong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        duoi.add(dongTong);

        btnThanhToan = new JButton("Thanh toán");
        btnThanhToan.setFont(new Font(FONT, Font.BOLD, 15));
        btnThanhToan.setForeground(Color.WHITE);
        btnThanhToan.setBackground(NEN_HEADER);
        btnThanhToan.setFocusPainted(false);
        btnThanhToan.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnThanhToan.putClientProperty("JButton.buttonType", "roundRect");
        btnThanhToan.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnThanhToan.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnThanhToan.setPreferredSize(new Dimension(100, 44));
        btnThanhToan.addActionListener(e -> thanhToan());
        duoi.add(btnThanhToan);
        duoi.add(Box.createVerticalStrut(6));

        btnBoChon = new JButton("Bỏ chọn tất cả");
        btnBoChon.setFont(new Font(FONT, Font.PLAIN, 13));
        btnBoChon.setForeground(CHU_DEN);
        btnBoChon.setBackground(Color.WHITE);
        btnBoChon.setFocusPainted(false);
        btnBoChon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBoChon.putClientProperty("JButton.buttonType", "roundRect");
        btnBoChon.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnBoChon.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btnBoChon.addActionListener(e -> boChonTatCa());
        duoi.add(btnBoChon);

        the.add(duoi, BorderLayout.SOUTH);
        return the;
    }

    // --- Combo bắp nước (tùy chọn) ---
    private void veKhuCombo() {
        khuCombo.removeAll();
        nhanSoLuongCombo.clear();
        if (dsCombo.isEmpty()) {
            khuCombo.revalidate();
            return;
        }
        JLabel tieuDe = new JLabel("Combo bắp nước (tùy chọn)");
        tieuDe.setFont(new Font(FONT, Font.BOLD, 13));
        tieuDe.setForeground(CHU_DEN);
        tieuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        tieuDe.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0));
        khuCombo.add(tieuDe);

        for (Combo cb : dsCombo) {
            soLuongCombo.putIfAbsent(cb.ma, 0);
            khuCombo.add(taoDongCombo(cb));
        }
        khuCombo.revalidate();
        khuCombo.repaint();
    }

    private JPanel taoDongCombo(Combo cb) {
        JPanel dong = new JPanel(new BorderLayout(8, 0));
        dong.setOpaque(false);
        dong.setAlignmentX(Component.LEFT_ALIGNMENT);
        dong.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel ten = new JPanel();
        ten.setOpaque(false);
        ten.setLayout(new BoxLayout(ten, BoxLayout.Y_AXIS));
        JLabel l1 = new JLabel(cb.ten);
        l1.setFont(new Font(FONT, Font.PLAIN, 13));
        l1.setForeground(CHU_DEN);
        JLabel l2 = new JLabel(tien(cb.gia));
        l2.setFont(new Font(FONT, Font.PLAIN, 11));
        l2.setForeground(CHU_XAM);
        ten.add(l1);
        ten.add(l2);
        dong.add(ten, BorderLayout.CENTER);
        dong.setToolTipText(cb.moTa);

        JButton tru = nutVuong("-");
        JButton cong = nutVuong("+");
        JLabel so = new JLabel(String.valueOf(soLuongCombo.getOrDefault(cb.ma, 0)), SwingConstants.CENTER);
        so.setFont(new Font(FONT, Font.BOLD, 14));
        so.setPreferredSize(new Dimension(24, 26));
        nhanSoLuongCombo.put(cb.ma, so);
        tru.addActionListener(e -> doiSoLuongCombo(cb.ma, -1));
        cong.addActionListener(e -> doiSoLuongCombo(cb.ma, +1));

        JPanel stepper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        stepper.setOpaque(false);
        stepper.add(tru);
        stepper.add(so);
        stepper.add(cong);
        dong.add(stepper, BorderLayout.EAST);
        return dong;
    }

    private JButton nutVuong(String chu) {
        JButton b = new JButton(chu);
        b.setFont(new Font(FONT, Font.BOLD, 14));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setBackground(Color.WHITE);
        b.setForeground(CHU_DEN);
        b.setPreferredSize(new Dimension(26, 26));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private void doiSoLuongCombo(String ma, int delta) {
        int moi = Math.max(0, Math.min(TOI_DA_COMBO, soLuongCombo.getOrDefault(ma, 0) + delta));
        soLuongCombo.put(ma, moi);
        JLabel l = nhanSoLuongCombo.get(ma);
        if (l != null) l.setText(String.valueOf(moi));
        capNhatHoaDon();
    }

    private void datLaiCombo() {
        for (String ma : new ArrayList<>(soLuongCombo.keySet())) {
            soLuongCombo.put(ma, 0);
            JLabel l = nhanSoLuongCombo.get(ma);
            if (l != null) l.setText("0");
        }
    }

    // --- Cập nhật phần hóa đơn mỗi khi chọn/bỏ ghế hoặc đổi combo ---
    private void capNhatHoaDon() {
        if (suatDangChon == null) {
            lblPhim.setText("Chưa chọn suất chiếu");
            lblThongTin.setText(" ");
        } else {
            SuatBan s = suatDangChon;
            lblPhim.setText(htmlRong(s.tenPhim, 225));
            lblThongTin.setText(htmlRong(s.tenPhong + "  \u2022  " + s.gioBatDau.format(HH) + " \u2013 "
                    + s.gioKetThuc.format(HH) + "  \u2022  " + s.ngay.format(DD), 225));
        }

        listHoaDon.removeAll();
        long tong = 0;

        List<Ghe> dsGhe = gheDaChonSapXep();
        if (dsGhe.isEmpty()) {
            JLabel l = new JLabel("Chưa chọn ghế nào");
            l.setFont(new Font(FONT, Font.PLAIN, 13));
            l.setForeground(CHU_XAM);
            l.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 0));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            listHoaDon.add(l);
        }
        for (Ghe g : dsGhe) {
            long gia = suatDangChon.giaTheoLoai(g.loai);
            tong += gia;
            listHoaDon.add(dongHoaDon("Ghế " + g.hang + g.so + "  (" + tenLoai(g.loai) + ")", tien(gia)));
        }
        for (Combo cb : dsCombo) {
            int n = soLuongCombo.getOrDefault(cb.ma, 0);
            if (n > 0) {
                long thanhTien = cb.gia * n;
                tong += thanhTien;
                listHoaDon.add(dongHoaDon(cb.ten + " x" + n, tien(thanhTien)));
            }
        }

        lblTong.setText(tien(tong));
        btnThanhToan.setEnabled(!dsGhe.isEmpty());
        btnBoChon.setEnabled(!dsGhe.isEmpty());
        listHoaDon.revalidate();
        listHoaDon.repaint();
    }

    private JPanel dongHoaDon(String trai, String phai) {
        JPanel dong = new JPanel(new BorderLayout(8, 0));
        dong.setOpaque(false);
        dong.setAlignmentX(Component.LEFT_ALIGNMENT);
        dong.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel l1 = new JLabel(trai);
        l1.setFont(new Font(FONT, Font.PLAIN, 13));
        l1.setForeground(CHU_DEN);
        JLabel l2 = new JLabel(phai);
        l2.setFont(new Font(FONT, Font.PLAIN, 13));
        l2.setForeground(CHU_DEN);
        dong.add(l1, BorderLayout.CENTER);
        dong.add(l2, BorderLayout.EAST);
        return dong;
    }

    private List<Ghe> gheDaChonSapXep() {
        List<Ghe> ds = new ArrayList<>(gheDangChon.values());
        ds.sort(Comparator.comparing((Ghe g) -> g.hang).thenComparingInt(g -> g.so));
        return ds;
    }

    private void boChonTatCa() {
        gheDangChon.clear();
        datLaiCombo();
        if (soDoHienTai != null) veSoDoGhe(soDoHienTai); // vẽ lại để bỏ tô màu ghế đang chọn
        capNhatHoaDon();
    }

    // ===================== THANH TOÁN =====================
    // Mo hop thoai thanh toan (thanh vien, khuyen mai, phuong thuc, tien thua, QR chuyen khoan)
    private void thanhToan() {
        if (suatDangChon == null || gheDangChon.isEmpty()) return;

        final SuatBan s = suatDangChon;
        final List<Ghe> dsGhe = gheDaChonSapXep();
        final List<String> dsMaGhe = new ArrayList<>();
        StringBuilder tenGhe = new StringBuilder();
        for (Ghe g : dsGhe) {
            dsMaGhe.add(g.maGhe);
            if (tenGhe.length() > 0) tenGhe.append(", ");
            tenGhe.append(g.hang).append(g.so).append(" (").append(tenLoai(g.loai)).append(")");
        }

        final Map<String, Integer> combo = new LinkedHashMap<>();
        StringBuilder tenCombo = new StringBuilder();
        for (Combo cb : dsCombo) {
            int n = soLuongCombo.getOrDefault(cb.ma, 0);
            if (n > 0) {
                combo.put(cb.ma, n);
                if (tenCombo.length() > 0) tenCombo.append(", ");
                tenCombo.append(cb.ten).append(" x").append(n);
            }
        }
        String thongTinSuat = s.tenPhong + "  \u2022  " + s.gioBatDau.format(HH) + " \u2013 "
                + s.gioKetThuc.format(HH) + "  \u2022  " + s.ngay.format(DD);

        btnThanhToan.setEnabled(false);
        ThanhToanDialog hop = new ThanhToanDialog(SwingUtilities.getWindowAncestor(this), thanhToanController,
                idNhanVien, s.maSuat, s.tenPhim, doTuoiTheoPhim.get(s.tenPhim), thongTinSuat,
                dsMaGhe, tenGhe.toString(), combo, tenCombo.toString());
        hop.setVisible(true);   // hop thoai modal: chay tiep o day khi da dong

        if (hop.ketQua != null) {
            datLaiCombo();
            taiSuat(ngay, s.maSuat, false);   // tai lai de cap nhat ghe da ban
        } else if (hop.gheBiBan) {
            taiSuat(ngay, s.maSuat, false);
        } else {
            capNhatHoaDon();                  // bam Huy: giu nguyen lua chon, bat lai nut
        }
    }

    // ===================== TIỆN ÍCH =====================
    private <T> void chayNen(Callable<T> viec, Consumer<T> khiXong) {
        chayNen(viec, khiXong, this::baoLoi);
    }

    private <T> void chayNen(Callable<T> viec, Consumer<T> khiXong, Consumer<Throwable> khiLoi) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return viec.call();
            }

            @Override
            protected void done() {
                try {
                    khiXong.accept(get());
                } catch (ExecutionException e) {
                    Throwable goc = (e.getCause() != null) ? e.getCause() : e;
                    goc.printStackTrace();
                    khiLoi.accept(goc);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void baoLoi(Throwable t) {
        JOptionPane.showMessageDialog(this, "Lỗi: " + t.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private JLabel tieuDeThe(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, Font.BOLD, 18));
        l.setForeground(CHU_DEN);
        return l;
    }

    private JLabel nhanThongBao(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, Font.PLAIN, 14));
        l.setForeground(CHU_XAM);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(10, 2, 0, 0));
        return l;
    }

    private String tien(long v) {
        return dinhDangSo.format(v) + "đ";
    }

    private static String tenLoai(String loai) {
        if ("VIP".equals(loai)) return "VIP";
        if ("Doi".equals(loai)) return "Đôi";
        return "Thường";
    }

    private static String htmlRong(String text, int px) {
        String an = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><div style='width:" + px + "px'>" + an + "</div></html>";
    }

    // Thẻ trắng bo góc có viền mảnh
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
}