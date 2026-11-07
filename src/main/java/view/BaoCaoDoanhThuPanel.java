package view;

import controller.BaoCaoDoanhThuController;
import controller.BaoCaoDoanhThuController.BaoCao;
import controller.BaoCaoDoanhThuController.TheoComboLe;
import controller.BaoCaoDoanhThuController.TheoGio;
import controller.BaoCaoDoanhThuController.TheoKhuyenMai;
import controller.BaoCaoDoanhThuController.TheoNgay;
import controller.BaoCaoDoanhThuController.TheoNhanVien;
import controller.BaoCaoDoanhThuController.TheoPhim;
import controller.BaoCaoDoanhThuController.TheoSuat;
import controller.BaoCaoDoanhThuController.TheoThanhToan;
import controller.XuatExcel;
import controller.XuatExcel.PhanTram;
import controller.XuatExcel.Sheet;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * Man hinh "Bao cao & thong ke": chon khoang ngay ->
 *  - the tong quan (doanh thu, hoa don, ve, combo, giam gia, hoan tien, lap day ghe)
 *  - Tab 1: ty le lap day ghe theo suat chieu
 *  - Tab 2: doanh thu theo phim (+ phim ban chay nhat)
 *  - Tab 3: bieu do doanh thu theo ngay + khung gio ban chay
 *  - Tab 4: doanh thu theo nhan vien, theo phuong thuc thanh toan, khuyen mai da dung
 *  - Nut "Xuat Excel": ghi toan bo bao cao ra file .xlsx.
 * Doanh thu = tien khach THUC TRA (da tru giam gia); hoa don da hoan tien khong tinh.
 */
public class BaoCaoDoanhThuPanel extends JPanel {

    private static final String FONT = "Segoe UI";
    private static final Color MAU_NAU_DAM    = new Color(0x8A, 0x6D, 0x4A);
    private static final Color MAU_NAU_NHAT   = new Color(0xC9, 0xB6, 0x98);
    private static final Color MAU_NEN_HEADER = new Color(0x3A, 0x30, 0x28);
    private static final Color MAU_CHU_XAM    = new Color(0x8A, 0x87, 0x82);
    private static final Color MAU_CHU_DAM    = new Color(0x2E, 0x2A, 0x26);
    private static final Color MAU_VIEN       = new Color(0xEA, 0xDF, 0xCF);
    private static final Color MAU_XANH       = new Color(0x3F, 0xA3, 0x4D);
    private static final Color MAU_DO         = new Color(0xC6, 0x28, 0x28);
    private static final DateTimeFormatter DD = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HH = DateTimeFormatter.ofPattern("HH:mm");

    private final BaoCaoDoanhThuController controller = new BaoCaoDoanhThuController();
    private final NumberFormat so = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    private final JSpinner spTu = taoNgay(LocalDate.now().minusDays(6));
    private final JSpinner spDen = taoNgay(LocalDate.now());
    private final JLabel lblTong = new JLabel("—"), lblHd = new JLabel("—"), lblVe = new JLabel("—"),
            lblCombo = new JLabel("—"), lblGiam = new JLabel("—"), lblHoan = new JLabel("—"), lblLapDay = new JLabel("—");
    private final JLabel lblTrangThai = new JLabel(" ");
    private final JCheckBox chkCoVe = new JCheckBox("Chỉ hiện suất có bán vé", true);
    private final JButton btnExcel = nut("Xuất Excel");
    private final JTextField txtTim = new JTextField();                // o tim phim (thay nut "Xem bao cao")
    private final JLabel lblKetQuaTim = new JLabel(" ");               // vd: "Hang 2/5 theo so ve"
    private final JLabel lblTopTen = new JLabel("—");                  // ten phim ban chay nhat ky nay
    private final JLabel lblTopChiTiet = new JLabel(" ");              // so ve, doanh thu cua phim do
    private JTabbedPane tabs;
    private TableRowSorter<DefaultTableModel> sorterPhim, sorterSuat, sorterVeCombo;
    private final DefaultTableModel modelVeCombo = taoModel("Ngày", "Giờ", "Phim", "Phòng", "Số vé", "Số combo", "Chi tiết combo", "Tiền combo");
    private final DefaultTableModel modelLe = taoModel("Mã", "Combo", "Số lượng", "Số hóa đơn", "Doanh thu");
    private static final String[] TEN_BANG = {"Tỷ lệ lấp đầy theo suất chiếu", "Vé + bắp nước (mua kèm vé)", "Bắp nước bán lẻ"};
    private static final String[] KHOA_BANG = {"lapday", "vecombo", "banle"};
    private final JComboBox<String> cbBang = new JComboBox<>(TEN_BANG);
    private final CardLayout theBang = new CardLayout();
    private final JPanel khungBang = new JPanel(theBang);
    private final JLabel lblGhiChuBang = new JLabel(" ");
    private boolean dangDatKhoang = false;
    private final Timer hoanXem = new Timer(500, e -> xem());          // tu tai lai sau khi doi ngay

    private final DefaultTableModel modelPhim = taoModel("Hạng", "Phim", "Số vé", "Doanh thu (vé)");
    private final DefaultTableModel modelSuat = taoModel("Ngày", "Giờ", "Phim", "Phòng", "Đã bán/Tổng", "Lấp đầy");
    private final DefaultTableModel modelNv = taoModel("Nhân viên", "Số hóa đơn", "Số vé", "Doanh thu");
    private final DefaultTableModel modelTt = taoModel("Phương thức", "Số hóa đơn", "Doanh thu", "Tỷ trọng");
    private final DefaultTableModel modelKm = taoModel("Khuyến mãi", "Lượt dùng", "Doanh thu hóa đơn");

    private final BieuDoCot bieuDoNgay = new BieuDoCot();
    private final BieuDoCot bieuDoGio = new BieuDoCot();
    private final JLabel lblGhiChuNgay = new JLabel(" "), lblGhiChuGio = new JLabel(" ");

    private BaoCao baoCaoHienTai;
    private LocalDate tuHienTai, denHienTai;

    /** Gia tri o "Lap day": giu so ghe de ve thanh thanh tien do + tooltip. */
    private static class TyLe {
        final long daBan, tong;

        TyLe(long daBan, long tong) {
            this.daBan = daBan;
            this.tong = tong;
        }

        double tyLe() {
            return tong <= 0 ? 0 : (double) daBan / tong;
        }

        @Override
        public String toString() {
            return daBan + "/" + tong + " ghế (" + Math.round(tyLe() * 100) + "%)";
        }
    }

    public BaoCaoDoanhThuPanel() {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        JPanel tren = new JPanel(new BorderLayout());
        tren.setOpaque(false);
        JLabel tieuDe = new JLabel("Báo cáo & thống kê");
        tieuDe.setFont(new Font(FONT, Font.BOLD, 22));
        tieuDe.setForeground(MAU_CHU_DAM);
        tren.add(tieuDe, BorderLayout.WEST);
        tren.add(taoBoLoc(), BorderLayout.EAST);
        add(tren, BorderLayout.NORTH);

        JPanel giua = new JPanel(new BorderLayout(0, 12));
        giua.setOpaque(false);

        JPanel hangThe = new JPanel(new GridLayout(1, 7, 10, 0));
        hangThe.setOpaque(false);
        hangThe.setPreferredSize(new Dimension(0, 84));
        hangThe.add(taoThe("Tổng doanh thu", lblTong, MAU_NAU_DAM));
        hangThe.add(taoThe("Số hóa đơn", lblHd, MAU_NAU_DAM));
        hangThe.add(taoThe("Số vé đã bán", lblVe, MAU_NAU_DAM));
        hangThe.add(taoThe("Trong đó combo", lblCombo, MAU_NAU_DAM));
        hangThe.add(taoThe("Đã giảm giá", lblGiam, MAU_XANH));
        hangThe.add(taoThe("Đã hoàn tiền (sự cố)", lblHoan, MAU_DO));
        hangThe.add(taoThe("Lấp đầy ghế", lblLapDay, MAU_NAU_DAM));
        giua.add(hangThe, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.setFont(new Font(FONT, Font.BOLD, 13));
        tabs.addTab("Lấp đầy suất chiếu", taoTabLapDay());
        tabs.addTab("Doanh thu theo phim", taoTabPhim());
        tabs.addTab("Biểu đồ", taoTabBieuDo());
        tabs.addTab("Nhân viên & thanh toán", taoTabNhanVien());
        giua.add(tabs, BorderLayout.CENTER);
        add(giua, BorderLayout.CENTER);

        lblTrangThai.setFont(new Font(FONT, Font.PLAIN, 12));
        lblTrangThai.setForeground(MAU_CHU_XAM);
        add(lblTrangThai, BorderLayout.SOUTH);

        hoanXem.setRepeats(false);
        xem();
    }

    // ===================== CAC TAB =====================
    /** Tab: chon bang can xem bang combo box (lap day / ve + bap nuoc / bap nuoc ban le), full chieu ngang. */
    private JComponent taoTabLapDay() {
        JTable bangSuat = taoBang(modelSuat, new int[]{0, 1, 3, 4}, new int[]{90, 55, 230, 75, 105, 190});
        bangSuat.getColumnModel().getColumn(5).setCellRenderer(new OTienDo());
        sorterSuat = khoaSapXep(bangSuat, modelSuat);
        JTable bangVeCombo = taoBang(modelVeCombo, new int[]{0, 1, 3, 4, 5}, new int[]{90, 55, 220, 75, 65, 80, 280, 110});
        sorterVeCombo = khoaSapXep(bangVeCombo, modelVeCombo);
        JTable bangLe = taoBang(modelLe, new int[]{0, 2, 3}, new int[]{70, 300, 110, 110, 150});
        khoaSapXep(bangLe, modelLe);

        chkCoVe.setOpaque(false);
        chkCoVe.setFont(new Font(FONT, Font.PLAIN, 13));
        chkCoVe.setForeground(MAU_CHU_XAM);
        chkCoVe.addActionListener(e -> { veBangLapDay(); veBangVeCombo(); capNhatGhiChuBang(); });

        JPanel khung = taoKhung(TEN_BANG[0], bangSuat, chkCoVe);
        BorderLayout bl = (BorderLayout) khung.getLayout();

        // tieu de -> combo box chon bang
        JPanel dau = (JPanel) bl.getLayoutComponent(BorderLayout.NORTH);
        dau.remove(((BorderLayout) dau.getLayout()).getLayoutComponent(BorderLayout.WEST));
        cbBang.setFont(new Font(FONT, Font.BOLD, 15));
        cbBang.setPreferredSize(new Dimension(330, 32));
        dau.add(cbBang, BorderLayout.WEST);

        // noi dung -> 3 bang xep chong (CardLayout)
        Component cuonSuat = bl.getLayoutComponent(BorderLayout.CENTER);
        khung.remove(cuonSuat);
        khungBang.setOpaque(false);
        khungBang.add(cuonSuat, KHOA_BANG[0]);
        khungBang.add(cuonBang(bangVeCombo), KHOA_BANG[1]);
        khungBang.add(cuonBang(bangLe), KHOA_BANG[2]);
        khung.add(khungBang, BorderLayout.CENTER);

        lblGhiChuBang.setFont(new Font(FONT, Font.PLAIN, 12));
        lblGhiChuBang.setForeground(MAU_CHU_XAM);
        khung.add(lblGhiChuBang, BorderLayout.SOUTH);

        cbBang.addActionListener(e -> {
            int k = Math.max(0, cbBang.getSelectedIndex());
            theBang.show(khungBang, KHOA_BANG[k]);
            chkCoVe.setVisible(k < 2);
            capNhatGhiChuBang();
        });
        capNhatGhiChuBang();
        return boTab(khung);
    }

    private JScrollPane cuonBang(JTable bang) {
        JScrollPane cuon = new JScrollPane(bang);
        cuon.setBorder(BorderFactory.createLineBorder(MAU_VIEN, 1));
        cuon.getViewport().setBackground(Color.WHITE);
        return cuon;
    }

    /** Dong ghi chu / tong ket duoi bang dang chon. */
    private void capNhatGhiChuBang() {
        int k = Math.max(0, cbBang.getSelectedIndex());
        BaoCao bc = baoCaoHienTai;
        if (k == 0) {
            lblGhiChuBang.setText("Lấp đầy = ghế đã bán / tổng ghế của phòng, tính theo ngày chiếu.");
        } else if (k == 1) {
            long sl = 0, tienCb = 0;
            if (bc != null) for (TheoSuat s : bc.theoSuat) { sl += s.soCombo; tienCb += s.tienCombo; }
            lblGhiChuBang.setText("Chỉ tính bắp nước mua cùng hóa đơn với vé. Tổng: " + so.format(sl) + " combo, "
                    + tien(tienCb) + " (giá niêm yết, chưa trừ giảm giá).");
        } else {
            lblGhiChuBang.setText(bc == null ? " " : "Bán lẻ (không kèm vé): " + so.format(bc.soLuongComboLe) + " combo, "
                    + so.format(bc.soHdComboLe) + " hóa đơn, " + tien(bc.tienComboLe) + ".");
        }
    }

    /** Bang "Ve + bap nuoc": moi suat chieu - so ve ban va bap nuoc mua kem ve. */
    private void veBangVeCombo() {
        modelVeCombo.setRowCount(0);
        if (baoCaoHienTai == null) return;
        List<TheoSuat> ds = new ArrayList<>(baoCaoHienTai.theoSuat);
        ds.sort(Comparator.comparing((TheoSuat s) -> s.ngay).reversed().thenComparing(s -> s.gioBatDau));
        for (TheoSuat s : ds) {
            if (chkCoVe.isSelected() && s.daBan == 0 && s.soCombo == 0) continue;
            modelVeCombo.addRow(new Object[]{
                s.ngay.format(DD), s.gioBatDau.format(HH), s.tenPhim, s.tenPhong.replace("Phòng chiếu", "Phòng"),
                so.format(s.daBan), so.format(s.soCombo),
                s.soCombo > 0 ? s.chiTietCombo : "—", s.soCombo > 0 ? tien(s.tienCombo) : "—"
            });
        }
    }

    /** Bang "Bap nuoc ban le": tung loai combo ban rieng (khong kem ve). */
    private void veBangLe() {
        modelLe.setRowCount(0);
        if (baoCaoHienTai == null) return;
        for (TheoComboLe x : baoCaoHienTai.comboLe) {
            modelLe.addRow(new Object[]{x.maCombo, x.tenCombo, so.format(x.soLuong), so.format(x.soHd), tien(x.tien)});
        }
    }

    /** Tab: doanh thu theo phim + phim ban chay nhat (full chieu ngang). */
    private JComponent taoTabPhim() {
        JTable bangPhim = taoBang(modelPhim, new int[]{0, 2, 3}, new int[]{60, 420, 100, 160});
        sorterPhim = khoaSapXep(bangPhim, modelPhim);
        lblKetQuaTim.setFont(new Font(FONT, Font.BOLD, 13));
        lblKetQuaTim.setForeground(MAU_NAU_DAM);
        JPanel khungPhim = taoKhung("Doanh thu theo phim", bangPhim, lblKetQuaTim, null);
        ganBanChay(khungPhim);
        return boTab(khungPhim);
    }

    /** Dat 1 khung vao tab, co le tren giong cac tab khac. */
    private JPanel boTab(JComponent khung) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        p.add(khung, BorderLayout.CENTER);
        return p;
    }

    /** Them khoi "Phim ban chay nhat" ngay duoi tieu de cua khung doanh thu theo phim. */
    private void ganBanChay(JPanel khung) {
        BorderLayout bl = (BorderLayout) khung.getLayout();
        Component dau = bl.getLayoutComponent(BorderLayout.NORTH);
        khung.remove(dau);

        JLabel nhanTop = new JLabel("PHIM BÁN CHẠY NHẤT");
        nhanTop.setFont(new Font(FONT, Font.BOLD, 11));
        nhanTop.setForeground(MAU_NAU_DAM);
        lblTopTen.setFont(new Font(FONT, Font.BOLD, 18));
        lblTopTen.setForeground(MAU_CHU_DAM);
        lblTopChiTiet.setFont(new Font(FONT, Font.PLAIN, 13));
        lblTopChiTiet.setForeground(MAU_CHU_XAM);

        JPanel ndung = new JPanel(new GridLayout(3, 1, 0, 2));
        ndung.setOpaque(false);
        ndung.add(nhanTop);
        ndung.add(lblTopTen);
        ndung.add(lblTopChiTiet);
        JPanel the = new JPanel(new BorderLayout());
        the.setBackground(new Color(0xF3, 0xE6, 0xD3));
        the.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, MAU_NAU_DAM),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        the.add(ndung, BorderLayout.CENTER);

        JPanel bac = new JPanel(new BorderLayout(0, 10));
        bac.setOpaque(false);
        bac.add(dau, BorderLayout.NORTH);
        bac.add(the, BorderLayout.CENTER);
        khung.add(bac, BorderLayout.NORTH);
    }

    private JComponent taoTabBieuDo() {
        return chia2(khungBieuDo("Doanh thu theo ngày", bieuDoNgay, lblGhiChuNgay),
                khungBieuDo("Khung giờ bán chạy (số vé theo giờ chiếu)", bieuDoGio, lblGhiChuGio), 0.55);
    }

    private JComponent taoTabNhanVien() {
        JTable bangNv = taoBang(modelNv, new int[]{1, 2, 3}, new int[]{200, 90, 70, 120});
        JTable bangTt = taoBang(modelTt, new int[]{1, 2, 3}, new int[]{140, 90, 120, 80});
        JTable bangKm = taoBang(modelKm, new int[]{1, 2}, new int[]{260, 80, 130});
        JPanel phai = new JPanel(new GridLayout(2, 1, 0, 12));
        phai.setOpaque(false);
        phai.add(taoKhung("Theo phương thức thanh toán", bangTt, null));
        phai.add(taoKhung("Khuyến mãi đã dùng", bangKm, null));
        return chia2(taoKhung("Doanh thu theo nhân viên", bangNv, null), phai, 0.5);
    }

    private JPanel chia2(JComponent trai, JComponent phai, double tiLeTrai) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;
        g.gridy = 0;
        g.gridx = 0;
        g.weightx = tiLeTrai;
        g.insets = new Insets(0, 0, 0, 14);
        p.add(trai, g);
        g.gridx = 1;
        g.weightx = 1 - tiLeTrai;
        g.insets = new Insets(0, 0, 0, 0);
        p.add(phai, g);
        return p;
    }

    private JPanel khungBieuDo(String tieuDe, BieuDoCot bieuDo, JLabel ghiChu) {
        JPanel khung = new JPanel(new BorderLayout(0, 6));
        khung.setBackground(Color.WHITE);
        khung.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1),
                BorderFactory.createEmptyBorder(12, 14, 10, 14)));
        JLabel l = new JLabel(tieuDe);
        l.setFont(new Font(FONT, Font.BOLD, 16));
        l.setForeground(MAU_CHU_DAM);
        khung.add(l, BorderLayout.NORTH);
        khung.add(bieuDo, BorderLayout.CENTER);
        ghiChu.setFont(new Font(FONT, Font.PLAIN, 13));
        ghiChu.setForeground(MAU_CHU_XAM);
        khung.add(ghiChu, BorderLayout.SOUTH);
        return khung;
    }

    // ===================== GIAO DIEN CHUNG =====================
    private static DefaultTableModel taoModel(String... cot) {
        return new DefaultTableModel(cot, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JSpinner taoNgay(LocalDate ngay) {
        JSpinner sp = new JSpinner(new SpinnerDateModel(Date.from(ngay.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                null, null, java.util.Calendar.DAY_OF_MONTH));
        sp.setEditor(new JSpinner.DateEditor(sp, "dd/MM/yyyy"));
        sp.setFont(new Font(FONT, Font.PLAIN, 13));
        sp.setPreferredSize(new Dimension(120, 30));
        return sp;
    }

    private JPanel taoBoLoc() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setOpaque(false);
        p.add(nhan("Từ ngày"));
        p.add(spTu);
        p.add(nhan("Đến ngày"));
        p.add(spDen);
        txtTim.setFont(new Font(FONT, Font.PLAIN, 13));
        txtTim.setPreferredSize(new Dimension(170, 30));
        txtTim.putClientProperty("JTextField.placeholderText", "Tìm phim...");
        txtTim.putClientProperty("JTextField.showClearButton", true);
        txtTim.setToolTipText("Gõ tên phim (không cần dấu) để xem hạng, số vé và doanh thu của phim đó");
        txtTim.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { locTheoPhim(); }
            @Override public void removeUpdate(DocumentEvent e) { locTheoPhim(); }
            @Override public void changedUpdate(DocumentEvent e) { locTheoPhim(); }
        });
        // doi ngay thi tu tai lai bao cao (khong con nut "Xem bao cao")
        javax.swing.event.ChangeListener doiNgay = e -> {
            if (!dangDatKhoang) hoanXem.restart();
        };
        spTu.addChangeListener(doiNgay);
        spDen.addChangeListener(doiNgay);
        JButton btnHomNay = nut("Hôm nay");
        btnHomNay.addActionListener(e -> datKhoang(LocalDate.now(), LocalDate.now()));
        JButton bt7 = nut("7 ngày");
        bt7.addActionListener(e -> datKhoang(LocalDate.now().minusDays(6), LocalDate.now()));
        JButton btThang = nut("Tháng này");
        btThang.addActionListener(e -> datKhoang(LocalDate.now().withDayOfMonth(1), LocalDate.now()));
        btnExcel.setBackground(MAU_NEN_HEADER);
        btnExcel.setForeground(Color.WHITE);
        btnExcel.setFont(new Font(FONT, Font.BOLD, 13));
        btnExcel.addActionListener(e -> xuatExcel());
        p.add(txtTim);
        p.add(btnHomNay);
        p.add(bt7);
        p.add(btThang);
        p.add(btnExcel);
        return p;
    }

    private JLabel nhan(String chu) {
        JLabel l = new JLabel(chu);
        l.setFont(new Font(FONT, Font.PLAIN, 13));
        l.setForeground(MAU_CHU_XAM);
        return l;
    }

    private static JButton nut(String chu) {
        JButton b = new JButton(chu);
        b.setFont(new Font(FONT, Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setBackground(Color.WHITE);
        b.setForeground(MAU_CHU_DAM);
        b.setMargin(new Insets(2, 10, 2, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private JPanel taoThe(String tieuDe, JLabel giaTri, Color mauSo) {
        JPanel the = new JPanel();
        the.setLayout(new BoxLayout(the, BoxLayout.Y_AXIS));
        the.setBackground(Color.WHITE);
        the.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 8)));
        JLabel t = new JLabel(tieuDe);
        t.setFont(new Font(FONT, Font.PLAIN, 12));
        t.setForeground(MAU_CHU_XAM);
        giaTri.setFont(new Font(FONT, Font.BOLD, 18));
        giaTri.setForeground(mauSo);
        the.add(t);
        the.add(Box.createVerticalStrut(6));
        the.add(giaTri);
        return the;
    }

    private JPanel taoKhung(String tieuDe, JTable bang, JComponent gocPhai) {
        return taoKhung(tieuDe, bang, gocPhai, null);
    }

    /** Khung trang co tieu de; goc phai (vd: o tich loc) va dong ghi chu o duoi la tuy chon. */
    private JPanel taoKhung(String tieuDe, JTable bang, JComponent gocPhai, JComponent duoi) {
        JPanel khung = new JPanel(new BorderLayout(0, 8));
        khung.setBackground(Color.WHITE);
        khung.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JPanel dau = new JPanel(new BorderLayout());
        dau.setOpaque(false);
        JLabel l = new JLabel(tieuDe);
        l.setFont(new Font(FONT, Font.BOLD, 16));
        l.setForeground(MAU_CHU_DAM);
        dau.add(l, BorderLayout.WEST);
        if (gocPhai != null) dau.add(gocPhai, BorderLayout.EAST);
        khung.add(dau, BorderLayout.NORTH);

        JScrollPane cuon = new JScrollPane(bang);
        cuon.setBorder(BorderFactory.createLineBorder(MAU_VIEN, 1));
        cuon.getViewport().setBackground(Color.WHITE);
        khung.add(cuon, BorderLayout.CENTER);
        if (duoi != null) khung.add(duoi, BorderLayout.SOUTH);
        return khung;
    }

    /** Chi dung de LOC theo ten phim (tat sap xep khi bam tieu de cot). */
    private TableRowSorter<DefaultTableModel> khoaSapXep(JTable bang, DefaultTableModel model) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        for (int c = 0; c < model.getColumnCount(); c++) sorter.setSortable(c, false);
        bang.setRowSorter(sorter);
        return sorter;
    }

    /** Bang co dinh dang chung; re chuot len o nao se hien day du noi dung o do. */
    private JTable taoBang(DefaultTableModel model, int[] cotCanGiua, int[] rongCot) {
        JTable bang = new JTable(model) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int r = rowAtPoint(e.getPoint()), c = columnAtPoint(e.getPoint());
                if (r < 0 || c < 0) return null;
                Object v = getValueAt(r, c);
                return v == null ? null : v.toString();
            }
        };
        bang.setRowHeight(32);
        bang.setFont(new Font(FONT, Font.PLAIN, 13));
        bang.setShowVerticalLines(false);
        bang.setGridColor(new Color(0xF0, 0xE9, 0xDD));
        bang.setFillsViewportHeight(true);
        bang.setSelectionBackground(new Color(0xF3, 0xE6, 0xD3));
        bang.setSelectionForeground(MAU_CHU_DAM);

        DefaultTableCellRenderer dauCot = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(MAU_NEN_HEADER);
                setForeground(Color.WHITE);
                setFont(new Font(FONT, Font.BOLD, 13));
                return this;
            }

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(MAU_NEN_HEADER);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        bang.getTableHeader().setDefaultRenderer(dauCot);
        bang.getTableHeader().setReorderingAllowed(false);
        bang.getTableHeader().setPreferredSize(new Dimension(0, 36));

        DefaultTableCellRenderer giua = new DefaultTableCellRenderer();
        giua.setHorizontalAlignment(SwingConstants.CENTER);
        for (int c : cotCanGiua) bang.getColumnModel().getColumn(c).setCellRenderer(giua);
        for (int c = 0; c < rongCot.length; c++) bang.getColumnModel().getColumn(c).setPreferredWidth(rongCot[c]);
        return bang;
    }

    /** O "Lap day": thanh tien do + phan tram. Xanh khi >= 70%, nau khi >= 30%, nhat khi thap hon. */
    private static class OTienDo extends JComponent implements TableCellRenderer {
        private double tyLe;
        private boolean chon;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            tyLe = (v instanceof TyLe) ? ((TyLe) v).tyLe() : 0;
            chon = sel;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (chon) {
                g2.setColor(new Color(0xF3, 0xE6, 0xD3));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            int le = 10, chuRong = 46, cao = 12;
            int rong = Math.max(10, getWidth() - le * 2 - chuRong);
            int y = (getHeight() - cao) / 2;
            g2.setColor(new Color(0xEF, 0xE7, 0xDA));
            g2.fillRoundRect(le, y, rong, cao, cao, cao);
            int day = (int) Math.round(rong * Math.min(1.0, tyLe));
            if (day > 0) {
                g2.setColor(tyLe >= 0.7 ? MAU_XANH : (tyLe >= 0.3 ? MAU_NAU_DAM : MAU_NAU_NHAT));
                g2.fillRoundRect(le, y, Math.max(day, cao), cao, cao, cao);
            }
            g2.setFont(new Font(FONT, Font.BOLD, 12));
            g2.setColor(MAU_CHU_DAM);
            String chu = Math.round(tyLe * 100) + "%";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(chu, getWidth() - le - fm.stringWidth(chu),
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ===================== BIEU DO COT (tu ve, khong can thu vien) =====================
    private static class BieuDoCot extends JComponent {
        private String[] nhan = new String[0];
        private double[] giaTri = new double[0];
        private String[] chuGiaTri = new String[0];
        private String[] chuTip = new String[0];
        private Function<Double, String> dinhDangTruc = v -> String.valueOf(Math.round(v));
        private Color mau = MAU_NAU_DAM;
        private int nhanMoiN = 1;
        private int dangTro = -1;
        private Rectangle[] khungCot = new Rectangle[0];

        BieuDoCot() {
            setPreferredSize(new Dimension(300, 200));
            ToolTipManager.sharedInstance().registerComponent(this);
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int idx = -1;
                    for (int i = 0; i < khungCot.length; i++) {
                        if (khungCot[i] != null && khungCot[i].contains(e.getPoint())) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx != dangTro) {
                        dangTro = idx;
                        setToolTipText(idx >= 0 ? chuTip[idx] : null);
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    dangTro = -1;
                    repaint();
                }
            });
        }

        void datDuLieu(String[] nhan, double[] giaTri, String[] chuGiaTri, String[] chuTip,
                       Function<Double, String> dinhDangTruc, Color mau, int nhanMoiN) {
            this.nhan = nhan;
            this.giaTri = giaTri;
            this.chuGiaTri = chuGiaTri;
            this.chuTip = chuTip;
            this.dinhDangTruc = dinhDangTruc;
            this.mau = mau;
            this.nhanMoiN = Math.max(1, nhanMoiN);
            this.khungCot = new Rectangle[nhan.length];
            this.dangTro = -1;
            repaint();
        }

        private static double lamTronLen(double max) {
            if (max <= 0) return 1;
            double mu = Math.pow(10, Math.floor(Math.log10(max)));
            double f = max / mu;
            double nice = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
            return nice * mu;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int trai = 58, phai = 8, tren = 22, duoi = 28;
            int rongV = w - trai - phai, caoV = h - tren - duoi;

            double max = 0;
            for (double v : giaTri) max = Math.max(max, v);
            if (nhan.length == 0 || max <= 0 || rongV < 40 || caoV < 40) {
                g2.setFont(new Font(FONT, Font.PLAIN, 14));
                g2.setColor(MAU_CHU_XAM);
                String t = "Chưa có dữ liệu trong khoảng ngày này";
                g2.drawString(t, (w - g2.getFontMetrics().stringWidth(t)) / 2, h / 2);
                g2.dispose();
                return;
            }
            double tran = lamTronLen(max);

            // luoi ngang + nhan truc tung
            g2.setFont(new Font(FONT, Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i <= 4; i++) {
                int y = tren + caoV - (int) Math.round(caoV * i / 4.0);
                g2.setColor(i == 0 ? new Color(0xCF, 0xC4, 0xB2) : new Color(0xF0, 0xE9, 0xDD));
                g2.drawLine(trai, y, trai + rongV, y);
                String t = dinhDangTruc.apply(tran * i / 4.0);
                g2.setColor(MAU_CHU_XAM);
                g2.drawString(t, trai - 6 - fm.stringWidth(t), y + fm.getAscent() / 2 - 1);
            }

            int n = nhan.length;
            double rongOCot = (double) rongV / n;
            int rongCot = (int) Math.max(3, Math.min(54, rongOCot * 0.62));
            int viTriMax = 0;
            for (int i = 0; i < n; i++) if (giaTri[i] > giaTri[viTriMax]) viTriMax = i;
            // chi hien so tren dau cot khi cot du rong cho so do (cot cao nhat va cot dang tro luon hien)
            g2.setFont(new Font(FONT, Font.BOLD, 11));
            int doRongSo = 0;
            for (int i = 0; i < n; i++) doRongSo = Math.max(doRongSo, g2.getFontMetrics().stringWidth(chuGiaTri[i]));

            for (int i = 0; i < n; i++) {
                int x = trai + (int) Math.round(rongOCot * i + (rongOCot - rongCot) / 2.0);
                int cao = (int) Math.round(caoV * giaTri[i] / tran);
                int y = tren + caoV - cao;
                khungCot[i] = new Rectangle(trai + (int) Math.round(rongOCot * i), tren, (int) Math.ceil(rongOCot), caoV);

                if (cao > 0) {
                    Color c = (i == viTriMax) ? MAU_XANH : mau;
                    if (i == dangTro) c = c.darker();
                    g2.setColor(c);
                    g2.fillRoundRect(x, y, rongCot, cao, 6, 6);
                    if (cao > 6) g2.fillRect(x, y + cao / 2, rongCot, cao - cao / 2);   // day cot vuong, dinh bo tron
                }

                // gia tri tren dau cot (neu du cho, hoac la cot cao nhat / cot dang tro)
                boolean hienGiaTri = giaTri[i] > 0 && (rongOCot >= doRongSo + 8 || i == viTriMax || i == dangTro);
                if (hienGiaTri) {
                    g2.setFont(new Font(FONT, Font.BOLD, 11));
                    FontMetrics f2 = g2.getFontMetrics();
                    String t = chuGiaTri[i];
                    g2.setColor(MAU_CHU_DAM);
                    g2.drawString(t, x + (rongCot - f2.stringWidth(t)) / 2, Math.max(tren - 4, y - 4));
                }
                // nhan truc hoanh
                if (i % nhanMoiN == 0) {
                    g2.setFont(new Font(FONT, Font.PLAIN, 11));
                    FontMetrics f3 = g2.getFontMetrics();
                    g2.setColor(MAU_CHU_XAM);
                    String t = nhan[i];
                    g2.drawString(t, x + (rongCot - f3.stringWidth(t)) / 2, tren + caoV + 16);
                }
            }
            g2.dispose();
        }
    }

    // ===================== XU LY =====================
    private void datKhoang(LocalDate tu, LocalDate den) {
        dangDatKhoang = true;   // tranh tai lai 2 lan (moi spinner doi la 1 su kien)
        spTu.setValue(Date.from(tu.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        spDen.setValue(Date.from(den.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        dangDatKhoang = false;
        hoanXem.stop();
        xem();
    }

    // ===================== TIM PHIM =====================
    /** Bo dau tieng Viet + chu thuong, de "quy an" khop duoc "Quỷ Ăn". */
    private static String boDau(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }

    private static RowFilter<DefaultTableModel, Integer> locTheoCot(String tuKhoa, int cot) {
        return new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                return boDau(String.valueOf(e.getValue(cot))).contains(tuKhoa);
            }
        };
    }

    /** Loc bang "Doanh thu theo phim" va "Lap day theo suat" theo ten phim dang go. */
    private void locTheoPhim() {
        String q = boDau(txtTim.getText().trim());
        if (sorterPhim != null) sorterPhim.setRowFilter(q.isEmpty() ? null : locTheoCot(q, 1));
        if (sorterSuat != null) sorterSuat.setRowFilter(q.isEmpty() ? null : locTheoCot(q, 2));
        if (sorterVeCombo != null) sorterVeCombo.setRowFilter(q.isEmpty() ? null : locTheoCot(q, 2));
        if (!q.isEmpty() && tabs != null) tabs.setSelectedIndex(1);   // chuyen ve tab doanh thu theo phim
        capNhatKetQuaTim();
    }

    private void capNhatKetQuaTim() {
        String q = txtTim.getText().trim();
        int hien = (sorterPhim == null) ? 0 : sorterPhim.getViewRowCount();
        if (q.isEmpty()) {
            lblKetQuaTim.setText(" ");
        } else if (hien == 0) {
            lblKetQuaTim.setText("Không có phim nào khớp (hoặc chưa bán vé trong kỳ)");
            lblKetQuaTim.setForeground(MAU_DO);
        } else if (hien == 1) {
            Object hang = modelPhim.getValueAt(sorterPhim.convertRowIndexToModel(0), 0);
            lblKetQuaTim.setText("Hạng " + hang + "/" + modelPhim.getRowCount() + " theo số vé bán");
            lblKetQuaTim.setForeground(MAU_NAU_DAM);
        } else {
            lblKetQuaTim.setText("Tìm thấy " + hien + " phim");
            lblKetQuaTim.setForeground(MAU_NAU_DAM);
        }
    }

    /** Xep hang phim theo so ve ban (bang nhau thi theo doanh thu). */
    private static List<TheoPhim> xepHangPhim(BaoCao bc) {
        List<TheoPhim> ds = new ArrayList<>(bc.theoPhim);
        ds.sort(Comparator.comparingLong((TheoPhim p) -> p.soVe).reversed()
                .thenComparing(Comparator.comparingLong((TheoPhim p) -> p.tien).reversed()));
        return ds;
    }

    private static LocalDate layNgay(JSpinner sp) {
        return ((Date) sp.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String tien(long v) {
        return so.format(v) + "đ";
    }

    private String phanTram(double tyLe) {
        return String.format(Locale.forLanguageTag("vi-VN"), "%.1f%%", tyLe * 100);
    }

    private static String tenPhuongThuc(String pt) {
        if ("Chuyen khoan".equals(pt)) return "Chuyển khoản";
        if ("The".equals(pt)) return "Thẻ";
        return "Tiền mặt";
    }

    private void xem() {
        final LocalDate tu = layNgay(spTu), den = layNgay(spDen);
        if (tu.isAfter(den)) {
            lblTrangThai.setText("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            return;
        }
        lblTrangThai.setText("Đang tải...");
        new SwingWorker<BaoCao, Void>() {
            @Override
            protected BaoCao doInBackground() throws Exception {
                return controller.lay(tu, den);
            }

            @Override
            protected void done() {
                try {
                    BaoCao bc = get();
                    baoCaoHienTai = bc;
                    tuHienTai = tu;
                    denHienTai = den;
                    hienKetQua(bc, tu, den);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    lblTrangThai.setText("Lỗi: " + e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void hienKetQua(BaoCao bc, LocalDate tu, LocalDate den) {
        lblTong.setText(tien(bc.tongTien));
        lblHd.setText(so.format(bc.soHd));
        lblVe.setText(so.format(bc.soVe));
        lblCombo.setText(tien(bc.tienCombo));
        lblGiam.setText(tien(bc.tongGiamGia));
        lblHoan.setText(tien(bc.tienHoan));
        lblLapDay.setText(bc.tongGhe > 0 ? phanTram(bc.tyLeLapDay()) : "—");
        lblLapDay.setToolTipText("Đã bán " + so.format(bc.gheDaBan) + " / " + so.format(bc.tongGhe)
                + " ghế của các suất chiếu trong kỳ (tính theo ngày chiếu)");
        lblGiam.setToolTipText(bc.soHdCoGiam + " hóa đơn có giảm giá (khuyến mãi, hạng thành viên, đổi điểm)");

        modelPhim.setRowCount(0);
        List<TheoPhim> xepHang = xepHangPhim(bc);
        for (int i = 0; i < xepHang.size(); i++) {
            TheoPhim p = xepHang.get(i);
            modelPhim.addRow(new Object[]{String.valueOf(i + 1), p.tenPhim, so.format(p.soVe), tien(p.tien)});
        }
        if (xepHang.isEmpty()) {
            lblTopTen.setText("Chưa có vé bán trong kỳ");
            lblTopTen.setToolTipText(null);
            lblTopChiTiet.setText(" ");
        } else {
            TheoPhim top = xepHang.get(0);
            lblTopTen.setText(top.tenPhim);
            lblTopTen.setToolTipText(top.tenPhim);
            lblTopChiTiet.setText(so.format(top.soVe) + " vé  •  " + tien(top.tien));
        }
        veBangLapDay();
        veBangVeCombo();
        veBangLe();
        capNhatGhiChuBang();
        capNhatKetQuaTim();

        modelNv.setRowCount(0);
        for (TheoNhanVien n : bc.theoNhanVien) {
            modelNv.addRow(new Object[]{n.hoTen, so.format(n.soHd), so.format(n.soVe), tien(n.tien)});
        }
        modelTt.setRowCount(0);
        long tongTt = 0;
        for (TheoThanhToan p : bc.theoThanhToan) tongTt += p.tien;
        for (TheoThanhToan p : bc.theoThanhToan) {
            modelTt.addRow(new Object[]{tenPhuongThuc(p.phuongThuc), so.format(p.soHd), tien(p.tien),
                tongTt > 0 ? phanTram((double) p.tien / tongTt) : "—"});
        }
        modelKm.setRowCount(0);
        for (TheoKhuyenMai k : bc.theoKhuyenMai) {
            modelKm.addRow(new Object[]{k.ten + " (" + k.ma + ")", so.format(k.luot), tien(k.tien)});
        }

        veBieuDo(bc, tu, den);

        lblTrangThai.setText("Từ " + tu.format(DD) + " đến " + den.format(DD)
                + "   •   Doanh thu = tiền khách thực trả (đã trừ giảm giá), chỉ tính hóa đơn đã thanh toán"
                + "   •   Sự cố: hoàn tiền " + so.format(bc.soHdHoan) + " hóa đơn, đổi suất "
                + so.format(bc.soHdDoiSuat) + " hóa đơn");
    }

    private static String trieu(double v) {
        if (v <= 0) return "0";
        if (v >= 1_000_000) {
            double m = v / 1_000_000.0;
            String s = (m >= 100 || Math.abs(m - Math.round(m)) < 0.05)
                    ? String.valueOf(Math.round(m)) : String.format(Locale.forLanguageTag("vi-VN"), "%.1f", m);
            return s + "tr";
        }
        return Math.round(v / 1000.0) + "k";
    }

    private void veBieuDo(BaoCao bc, LocalDate tu, LocalDate den) {
        // ----- Doanh thu theo ngay (lien tuc tu ngay bat dau den ngay ket thuc) -----
        Map<LocalDate, Long> tienNgay = new HashMap<>();
        for (TheoNgay n : bc.theoNgay) tienNgay.put(n.ngay, n.tien);
        List<LocalDate> cacNgay = new ArrayList<>();
        long soNgay = java.time.temporal.ChronoUnit.DAYS.between(tu, den) + 1;
        if (soNgay <= 90) {
            for (LocalDate d = tu; !d.isAfter(den); d = d.plusDays(1)) cacNgay.add(d);
        } else {   // khoang rat dai: chi ve nhung ngay co doanh thu
            cacNgay.addAll(tienNgay.keySet());
            Collections.sort(cacNgay);
        }
        int n = cacNgay.size();
        String[] nhan = new String[n], chuGt = new String[n], tip = new String[n];
        double[] gt = new double[n];
        LocalDate ngayCao = null;
        long tienCao = 0;
        for (int i = 0; i < n; i++) {
            long t = tienNgay.getOrDefault(cacNgay.get(i), 0L);
            gt[i] = t;
            nhan[i] = cacNgay.get(i).format(DM);
            chuGt[i] = trieu(t);
            tip[i] = cacNgay.get(i).format(DD) + ": " + tien(t);
            if (t > tienCao) {
                tienCao = t;
                ngayCao = cacNgay.get(i);
            }
        }
        bieuDoNgay.datDuLieu(nhan, gt, chuGt, tip, BaoCaoDoanhThuPanel::trieu, MAU_NAU_DAM, (int) Math.ceil(n / 14.0));
        lblGhiChuNgay.setText(ngayCao == null ? "Chưa có doanh thu trong kỳ"
                : "Ngày cao nhất: " + ngayCao.format(DD) + " (" + tien(tienCao) + ")  •  trung bình "
                + tien(bc.tongTien / Math.max(1, soNgay)) + "/ngày");

        // ----- Khung gio ban chay (gio bat dau suat chieu) -----
        int gioMin = 8, gioMax = 23;
        for (TheoGio g : bc.theoGio) {
            gioMin = Math.min(gioMin, g.gio);
            gioMax = Math.max(gioMax, g.gio);
        }
        int m = gioMax - gioMin + 1;
        String[] nh = new String[m], cg = new String[m], tp = new String[m];
        double[] v = new double[m];
        Map<Integer, TheoGio> theoGio = new HashMap<>();
        for (TheoGio g : bc.theoGio) theoGio.put(g.gio, g);
        int gioCao = -1;
        long veCao = 0;
        for (int i = 0; i < m; i++) {
            int gio = gioMin + i;
            TheoGio g = theoGio.get(gio);
            long soVe = g == null ? 0 : g.soVe;
            v[i] = soVe;
            nh[i] = String.format("%02dh", gio);
            cg[i] = String.valueOf(soVe);
            tp[i] = String.format("Suất %02dh: %s vé, doanh thu %s", gio, so.format(soVe), tien(g == null ? 0 : g.tien));
            if (soVe > veCao) {
                veCao = soVe;
                gioCao = gio;
            }
        }
        bieuDoGio.datDuLieu(nh, v, cg, tp, d -> String.valueOf(Math.round(d)), new Color(0xB2, 0x90, 0x6B), 1);
        lblGhiChuGio.setText(gioCao < 0 ? "Chưa có vé bán trong kỳ"
                : "Khung giờ bán chạy nhất: suất " + String.format("%02dh", gioCao) + " (" + so.format(veCao) + " vé)");
    }

    /** Do lai bang lap day theo suat (co the chi hien suat da co ve). */
    private void veBangLapDay() {
        modelSuat.setRowCount(0);
        if (baoCaoHienTai == null) return;
        for (TheoSuat s : baoCaoHienTai.theoSuat) {
            if (chkCoVe.isSelected() && s.daBan == 0) continue;
            modelSuat.addRow(new Object[]{
                s.ngay.format(DD), s.gioBatDau.format(HH), s.tenPhim, s.tenPhong.replace("Phòng chiếu", "Phòng"),
                s.daBan + " / " + s.tongGhe, new TyLe(s.daBan, s.tongGhe)
            });
        }
    }

    // ===================== XUAT EXCEL =====================
    private void xuatExcel() {
        if (baoCaoHienTai == null) {
            lblTrangThai.setText("Chưa có dữ liệu để xuất. Hãy chọn khoảng ngày để tải báo cáo trước.");
            return;
        }
        JFileChooser hop = new JFileChooser();
        hop.setDialogTitle("Lưu báo cáo Excel");
        hop.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        hop.setSelectedFile(new File("bao_cao_" + tuHienTai + "_" + denHienTai + ".xlsx"));
        if (hop.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = hop.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".xlsx")) f = new File(f.getParentFile(), f.getName() + ".xlsx");
        if (f.exists() && JOptionPane.showConfirmDialog(this, "File \"" + f.getName() + "\" đã tồn tại. Ghi đè?",
                "Ghi đè file", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try {
            XuatExcel.ghi(f, taoSheets(baoCaoHienTai, tuHienTai, denHienTai));
            JOptionPane.showMessageDialog(this, "Đã xuất báo cáo:\n" + f.getAbsolutePath(),
                    "Xuất Excel", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không ghi được file.\nNếu file đang mở trong Excel, hãy đóng lại rồi thử lại.\n\n"
                    + e.getMessage(), "Lỗi xuất Excel", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<Sheet> taoSheets(BaoCao bc, LocalDate tu, LocalDate den) {
        List<Sheet> ds = new ArrayList<>();

        ds.add(new Sheet("Tổng quan", "Chỉ tiêu", "Giá trị")
                .them("Từ ngày", tu.format(DD))
                .them("Đến ngày", den.format(DD))
                .them("Tổng doanh thu (đ)", bc.tongTien)
                .them("Số hóa đơn", bc.soHd)
                .them("Số vé đã bán", bc.soVe)
                .them("Doanh thu combo (đ)", bc.tienCombo)
                .them("Tổng giảm giá (đ)", bc.tongGiamGia)
                .them("Số hóa đơn có giảm giá", bc.soHdCoGiam)
                .them("Đã hoàn tiền do sự cố (đ)", bc.tienHoan)
                .them("Số hóa đơn hoàn tiền", bc.soHdHoan)
                .them("Số hóa đơn đổi sang suất khác", bc.soHdDoiSuat)
                .them("Ghế đã bán (các suất chiếu trong kỳ)", bc.gheDaBan)
                .them("Tổng ghế (các suất chiếu trong kỳ)", bc.tongGhe)
                .them("Tỷ lệ lấp đầy", new PhanTram(bc.tyLeLapDay())));

        Sheet sNgay = new Sheet("Theo ngày", "Ngày", "Số hóa đơn", "Số vé", "Doanh thu (đ)");
        List<TheoNgay> tang = new ArrayList<>(bc.theoNgay);
        Collections.reverse(tang);
        for (TheoNgay n : tang) sNgay.them(n.ngay.format(DD), n.soHd, n.soVe, n.tien);
        ds.add(sNgay);

        Sheet sPhim = new Sheet("Theo phim", "Hạng (theo số vé)", "Phim", "Số vé", "Doanh thu vé (đ)");
        List<TheoPhim> hangPhim = xepHangPhim(bc);
        for (int i = 0; i < hangPhim.size(); i++) {
            TheoPhim p = hangPhim.get(i);
            sPhim.them(i + 1, p.tenPhim, p.soVe, p.tien);
        }
        ds.add(sPhim);

        Sheet sSuat = new Sheet("Lấp đầy theo suất", "Ngày chiếu", "Giờ", "Phim", "Phòng", "Ghế đã bán", "Tổng ghế", "Tỷ lệ lấp đầy");
        for (TheoSuat s : bc.theoSuat) {
            sSuat.them(s.ngay.format(DD), s.gioBatDau.format(HH), s.tenPhim, s.tenPhong, s.daBan, s.tongGhe, new PhanTram(s.tyLe()));
        }
        ds.add(sSuat);

        Sheet sVeCb = new Sheet("Vé + bắp nước", "Ngày chiếu", "Giờ", "Phim", "Phòng", "Số vé", "Số combo kèm vé", "Chi tiết combo", "Tiền combo (đ)");
        List<TheoSuat> theoGioChieu = new ArrayList<>(bc.theoSuat);
        theoGioChieu.sort(Comparator.comparing((TheoSuat s) -> s.ngay).reversed().thenComparing(s -> s.gioBatDau));
        for (TheoSuat s : theoGioChieu) {
            sVeCb.them(s.ngay.format(DD), s.gioBatDau.format(HH), s.tenPhim, s.tenPhong, s.daBan, s.soCombo, s.chiTietCombo, s.tienCombo);
        }
        ds.add(sVeCb);

        Sheet sLe = new Sheet("Bắp nước bán lẻ", "Mã combo", "Combo", "Số lượng", "Số hóa đơn", "Doanh thu (đ)");
        for (TheoComboLe x : bc.comboLe) sLe.them(x.maCombo, x.tenCombo, x.soLuong, x.soHd, x.tien);
        ds.add(sLe);

        Sheet sGio = new Sheet("Khung giờ", "Giờ chiếu", "Số vé", "Doanh thu (đ)");
        for (TheoGio g : bc.theoGio) sGio.them(String.format("%02dh", g.gio), g.soVe, g.tien);
        ds.add(sGio);

        Sheet sNv = new Sheet("Theo nhân viên", "Nhân viên", "Số hóa đơn", "Số vé", "Doanh thu (đ)");
        for (TheoNhanVien n : bc.theoNhanVien) sNv.them(n.hoTen, n.soHd, n.soVe, n.tien);
        ds.add(sNv);

        long tongTt = 0;
        for (TheoThanhToan p : bc.theoThanhToan) tongTt += p.tien;
        Sheet sTt = new Sheet("Thanh toán", "Phương thức", "Số hóa đơn", "Doanh thu (đ)", "Tỷ trọng");
        for (TheoThanhToan p : bc.theoThanhToan) {
            sTt.them(tenPhuongThuc(p.phuongThuc), p.soHd, p.tien, new PhanTram(tongTt > 0 ? (double) p.tien / tongTt : 0));
        }
        ds.add(sTt);

        Sheet sKm = new Sheet("Khuyến mãi", "Mã", "Tên khuyến mãi", "Lượt dùng", "Doanh thu hóa đơn (đ)");
        for (TheoKhuyenMai k : bc.theoKhuyenMai) sKm.them(k.ma, k.ten, k.luot, k.tien);
        ds.add(sKm);
        return ds;
    }
}