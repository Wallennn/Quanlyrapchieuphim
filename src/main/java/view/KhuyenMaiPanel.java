package view;

import dao.KhuyenMaiDAO;
import dao.KhuyenMaiDAO.DaDuocDungException;
import dao.KhuyenMaiDAO.KhuyenMai;
import dao.KhuyenMaiDAO.TrungMaException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * Man "Khuyen mai" cua quan tri vien: tao chuong trinh giam gia tu dong (vd: Happy Wednesday)
 * hoac ma giam gia khach nhap (vd: HSSV), loc theo thu / thoi gian / don toi thieu, gioi han luot dung.
 */
public class KhuyenMaiPanel extends JPanel {

    private static final String FONT = "Segoe UI";
    private static final Color NEN_KEM    = new Color(0xFB, 0xF6, 0xEF);
    private static final Color NEN_HEADER = new Color(0x3A, 0x30, 0x28);
    private static final Color CHU_XAM    = new Color(0x8A, 0x87, 0x82);
    private static final Color CHU_DAM    = new Color(0x2E, 0x2A, 0x26);
    private static final Color VIEN       = new Color(0xEA, 0xDF, 0xCF);
    private static final Color XANH       = new Color(0x2E, 0x7D, 0x32);
    private static final Color DO         = new Color(0xC6, 0x28, 0x28);
    private static final Color CAM        = new Color(0xB2, 0x6A, 0x00);
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] TEN_THU = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};   // thu 1..7 (ISO)
    private static final int COT_TRANG_THAI = 8;

    private final KhuyenMaiDAO dao = new KhuyenMaiDAO();
    private final NumberFormat so = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Mã", "Tên chương trình", "Cách dùng", "Mức giảm", "Áp dụng cho", "Điều kiện",
                "Thời gian", "Đã dùng", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable bang = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField txtTim = new JTextField();
    private final JButton btnThem = nut("+  Thêm khuyến mãi", true);
    private final JButton btnSua = nut("Sửa", false);
    private final JButton btnDung = nut("Tạm dừng", false);
    private final JButton btnXoa = nut("Xóa", false);
    private final JButton btnLamMoi = nut("Làm mới", false);
    private final JLabel lblDem = new JLabel(" ");

    private List<KhuyenMai> ds = new ArrayList<>();

    public KhuyenMaiPanel() {
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);

        JPanel tren = new JPanel(new BorderLayout());
        tren.setOpaque(false);
        JLabel tieuDe = new JLabel("Khuyến mãi");
        tieuDe.setFont(new Font(FONT, Font.BOLD, 24));
        tieuDe.setForeground(CHU_DAM);
        tren.add(tieuDe, BorderLayout.WEST);

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        phai.setOpaque(false);
        txtTim.setFont(new Font(FONT, Font.PLAIN, 13));
        txtTim.setPreferredSize(new Dimension(220, 34));
        txtTim.putClientProperty("JTextField.placeholderText", "Tìm theo mã, tên...");
        phai.add(txtTim);
        phai.add(btnThem);
        phai.add(btnSua);
        phai.add(btnDung);
        phai.add(btnXoa);
        phai.add(btnLamMoi);
        tren.add(phai, BorderLayout.EAST);
        add(tren, BorderLayout.NORTH);

        dungBang();
        JScrollPane cuon = new JScrollPane(bang);
        cuon.setBorder(BorderFactory.createLineBorder(VIEN, 1));
        cuon.getViewport().setBackground(Color.WHITE);
        add(cuon, BorderLayout.CENTER);

        lblDem.setFont(new Font(FONT, Font.PLAIN, 12));
        lblDem.setForeground(CHU_XAM);
        add(lblDem, BorderLayout.SOUTH);

        btnThem.addActionListener(e -> moHopThoai(null));
        btnSua.addActionListener(e -> suaDong());
        btnDung.addActionListener(e -> doiTrangThai());
        btnXoa.addActionListener(e -> xoaDong());
        btnLamMoi.addActionListener(e -> taiLai(null));
        txtTim.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { loc(); }
            @Override public void removeUpdate(DocumentEvent e) { loc(); }
            @Override public void changedUpdate(DocumentEvent e) { loc(); }
        });
        bang.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) capNhatNut();
        });
        bang.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && bang.rowAtPoint(e.getPoint()) >= 0) suaDong();
            }
        });

        capNhatNut();
        taiLai(null);
    }

    // ===================== BANG =====================
    private void dungBang() {
        bang.setRowHeight(44);
        bang.setFont(new Font(FONT, Font.PLAIN, 13));
        bang.setShowVerticalLines(false);
        bang.setGridColor(new Color(0xF0, 0xE9, 0xDD));
        bang.setFillsViewportHeight(true);
        bang.setSelectionBackground(new Color(0xF3, 0xE6, 0xD3));
        bang.setSelectionForeground(CHU_DAM);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.setRowSorter(sorter);
        bang.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        DefaultTableCellRenderer dauCot = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
                setBackground(NEN_HEADER);
                setForeground(Color.WHITE);
                setFont(new Font(FONT, Font.BOLD, 13));
                return this;
            }

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(NEN_HEADER);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        bang.getTableHeader().setDefaultRenderer(dauCot);
        bang.getTableHeader().setReorderingAllowed(false);
        bang.getTableHeader().setPreferredSize(new Dimension(0, 40));

        DefaultTableCellRenderer o = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
                setForeground(CHU_DAM);
                setFont(new Font(FONT, Font.PLAIN, 13));
                setToolTipText(v == null ? null : v.toString());
                if (c == COT_TRANG_THAI) {
                    String tt = String.valueOf(v);
                    setFont(new Font(FONT, Font.BOLD, 13));
                    setForeground("Đang chạy".equals(tt) ? XANH
                            : ("Tạm dừng".equals(tt) ? CHU_XAM
                            : ("Chưa bắt đầu".equals(tt) ? CAM : DO)));
                }
                return this;
            }
        };
        int[] rong = {70, 230, 90, 140, 110, 160, 160, 70, 100};
        for (int c = 0; c < bang.getColumnCount(); c++) {
            bang.getColumnModel().getColumn(c).setCellRenderer(o);
            bang.getColumnModel().getColumn(c).setPreferredWidth(rong[c]);
        }
    }

    private static JButton nut(String chu, boolean chinh) {
        JButton b = new JButton(chu);
        b.setFont(new Font(FONT, chinh ? Font.BOLD : Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setBackground(chinh ? NEN_HEADER : Color.WHITE);
        b.setForeground(chinh ? Color.WHITE : CHU_DAM);
        b.setMargin(new Insets(0, 10, 0, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(chinh ? 160 : 90, 34));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    // ===================== DU LIEU =====================
    private String trangThai(KhuyenMai k) {
        LocalDate hn = LocalDate.now();
        if (!k.hoatDong) return "Tạm dừng";
        if (hn.isBefore(k.tuNgay)) return "Chưa bắt đầu";
        if (hn.isAfter(k.denNgay)) return "Hết hạn";
        if (k.gioiHan != null && k.daDung >= k.gioiHan) return "Hết lượt";
        return "Đang chạy";
    }

    private String mucGiam(KhuyenMai k) {
        if (KhuyenMaiDAO.PHAN_TRAM.equals(k.loaiGiam)) {
            return k.giaTri + "%" + (k.giamToiDa != null ? " (tối đa " + so.format(k.giamToiDa) + "đ)" : "");
        }
        return so.format(k.giaTri) + "đ";
    }

    private String dieuKien(KhuyenMai k) {
        List<String> p = new ArrayList<>();
        if (k.ngayTrongTuan != null && !k.ngayTrongTuan.isEmpty()) p.add(KhuyenMaiDAO.moTaThu(k.ngayTrongTuan));
        if (k.donToiThieu > 0) p.add("đơn từ " + so.format(k.donToiThieu) + "đ");
        return p.isEmpty() ? "—" : String.join(" • ", p);
    }

    private void taiLai(String chonMa) {
        lblDem.setText("Đang tải...");
        new SwingWorker<List<KhuyenMai>, Void>() {
            @Override
            protected List<KhuyenMai> doInBackground() throws Exception {
                return dao.layTatCa();
            }

            @Override
            protected void done() {
                try {
                    ds = get();
                    model.setRowCount(0);
                    for (KhuyenMai k : ds) {
                        model.addRow(new Object[]{
                            k.ma, k.ten,
                            k.tuDong() ? "Tự động" : "Mã: " + k.maNhap,
                            mucGiam(k),
                            KhuyenMaiDAO.CHO_TONG.equals(k.apDungCho) ? "Toàn hóa đơn" : "Tiền vé",
                            dieuKien(k),
                            k.tuNgay.format(NGAY) + " – " + k.denNgay.format(NGAY),
                            k.gioiHan == null ? String.valueOf(k.daDung) : k.daDung + "/" + k.gioiHan,
                            trangThai(k)});
                    }
                    capNhatChanTrang();
                    if (chonMa != null) {
                        for (int i = 0; i < ds.size(); i++) {
                            if (ds.get(i).ma.equals(chonMa)) {
                                int dong = bang.convertRowIndexToView(i);
                                if (dong >= 0) {
                                    bang.setRowSelectionInterval(dong, dong);
                                    bang.scrollRectToVisible(bang.getCellRect(dong, 0, true));
                                }
                                break;
                            }
                        }
                    }
                    capNhatNut();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    lblDem.setText("Lỗi tải danh sách: " + e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void loc() {
        String q = txtTim.getText().trim();
        sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?iu)" + Pattern.quote(q), 0, 1));
        capNhatChanTrang();
        capNhatNut();
    }

    private void capNhatChanTrang() {
        long chay = ds.stream().filter(k -> "Đang chạy".equals(trangThai(k))).count();
        lblDem.setText(bang.getRowCount() + "/" + ds.size() + " chương trình  •  " + chay + " đang chạy"
                + "  •  Mỗi hóa đơn áp dụng tối đa 1 khuyến mãi (mã khách nhập, nếu không có thì khuyến mãi tự động có lợi nhất)"
                + "  •  Thứ tính theo ngày chiếu của suất");
    }

    private KhuyenMai dongDangChon() {
        int dong = bang.getSelectedRow();
        if (dong < 0) return null;
        int goc = bang.convertRowIndexToModel(dong);
        return (goc >= 0 && goc < ds.size()) ? ds.get(goc) : null;
    }

    private void capNhatNut() {
        KhuyenMai k = dongDangChon();
        btnSua.setEnabled(k != null);
        btnXoa.setEnabled(k != null);
        btnDung.setEnabled(k != null);
        btnDung.setText(k != null && !k.hoatDong ? "Kích hoạt" : "Tạm dừng");
    }

    private void suaDong() {
        KhuyenMai k = dongDangChon();
        if (k != null) moHopThoai(k);
    }

    private void doiTrangThai() {
        KhuyenMai k = dongDangChon();
        if (k == null) return;
        final String ma = k.ma;
        final boolean batLai = !k.hoatDong;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                dao.datTrangThai(ma, batLai);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    taiLai(ma);
                } catch (ExecutionException e) {
                    e.getCause().printStackTrace();
                    JOptionPane.showMessageDialog(KhuyenMaiPanel.this, e.getCause().getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void xoaDong() {
        KhuyenMai k = dongDangChon();
        if (k == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "Xóa khuyến mãi \"" + k.ten + "\" (" + k.ma + ")?",
                "Xóa khuyến mãi", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        final String ma = k.ma;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                dao.xoa(ma);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    taiLai(null);
                } catch (ExecutionException e) {
                    Throwable g = e.getCause();
                    if (!(g instanceof DaDuocDungException)) g.printStackTrace();
                    JOptionPane.showMessageDialog(KhuyenMaiPanel.this, g.getMessage(),
                            "Không xóa được", JOptionPane.WARNING_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void moHopThoai(KhuyenMai sua) {
        new HopThoai(SwingUtilities.getWindowAncestor(this), sua).setVisible(true);
    }

    // ===================== HOP THOAI THEM / SUA =====================
    private class HopThoai extends JDialog {
        private final KhuyenMai dangSua;

        private final JTextField txtMa = new JTextField(), txtTen = new JTextField(), txtMoTa = new JTextField();
        private final JRadioButton rbTuDong = new JRadioButton("Tự động áp dụng", true);
        private final JRadioButton rbNhapMa = new JRadioButton("Khách nhập mã:");
        private final JTextField txtMaNhap = new JTextField();
        private final JComboBox<String> cboLoai = new JComboBox<>(new String[]{"Giảm theo phần trăm (%)", "Giảm số tiền (đ)"});
        private final JTextField txtGiaTri = new JTextField();
        private final JComboBox<String> cboApDung = new JComboBox<>(new String[]{"Chỉ tiền vé", "Toàn hóa đơn (vé + combo)"});
        private final JTextField txtToiDa = new JTextField(), txtToiThieu = new JTextField();
        private final JCheckBox[] chkThu = new JCheckBox[7];
        private final JSpinner spTu = taoNgay(LocalDate.now()), spDen = taoNgay(LocalDate.now().plusMonths(1));
        private final JCheckBox chkGioiHan = new JCheckBox("Giới hạn lượt dùng:");
        private final JSpinner spLuot = new JSpinner(new SpinnerNumberModel(100, 1, 1_000_000, 1));
        private final JCheckBox chkHoatDong = new JCheckBox("Đang kích hoạt", true);
        private final JLabel lblLoi = new JLabel(" ");
        private final JButton btnLuu = nut(" ", true);

        HopThoai(Window chu, KhuyenMai dangSua) {
            super(chu, dangSua == null ? "Thêm khuyến mãi" : "Sửa khuyến mãi", ModalityType.APPLICATION_MODAL);
            this.dangSua = dangSua;
            boolean sua = dangSua != null;

            JPanel nen = new JPanel(new GridBagLayout());
            nen.setBackground(NEN_KEM);
            nen.setBorder(BorderFactory.createEmptyBorder(18, 24, 16, 24));
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridwidth = 2;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.weightx = 1;
            g.anchor = GridBagConstraints.WEST;

            JLabel tieuDe = new JLabel(sua ? "Sửa khuyến mãi" : "Thêm khuyến mãi mới");
            tieuDe.setFont(new Font(FONT, Font.BOLD, 20));
            tieuDe.setForeground(CHU_DAM);
            g.insets = new Insets(0, 0, 12, 0);
            nen.add(tieuDe, g);

            for (JTextField t : new JTextField[]{txtMa, txtTen, txtMoTa, txtMaNhap, txtGiaTri, txtToiDa, txtToiThieu}) {
                t.setFont(new Font(FONT, Font.PLAIN, 14));
                t.setPreferredSize(new Dimension(200, 34));
            }
            txtMa.setEditable(!sua);
            txtToiDa.putClientProperty("JTextField.placeholderText", "Để trống = không giới hạn");
            txtToiThieu.putClientProperty("JTextField.placeholderText", "0 = không yêu cầu");
            cboLoai.setFont(new Font(FONT, Font.PLAIN, 13));
            cboApDung.setFont(new Font(FONT, Font.PLAIN, 13));
            spLuot.setFont(new Font(FONT, Font.PLAIN, 13));

            g.gridwidth = 1;
            g.insets = new Insets(0, 0, 8, 12);
            g.gridx = 0;
            g.weightx = 0.35;
            g.gridy = 1;
            nen.add(truong("Mã khuyến mãi (viết hoa, không dấu)", txtMa), g);
            g.gridx = 1;
            g.weightx = 0.65;
            g.insets = new Insets(0, 0, 8, 0);
            nen.add(truong("Tên chương trình", txtTen), g);

            g.gridx = 0;
            g.gridwidth = 2;
            g.gridy = 2;
            nen.add(truong("Mô tả (hiện cho nhân viên)", txtMoTa), g);

            // cách áp dụng
            ButtonGroup nhom = new ButtonGroup();
            nhom.add(rbTuDong);
            nhom.add(rbNhapMa);
            for (JRadioButton rb : new JRadioButton[]{rbTuDong, rbNhapMa}) {
                rb.setOpaque(false);
                rb.setFont(new Font(FONT, Font.PLAIN, 13));
            }
            JPanel dongCach = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            dongCach.setOpaque(false);
            dongCach.add(rbTuDong);
            dongCach.add(rbNhapMa);
            txtMaNhap.setPreferredSize(new Dimension(140, 32));
            txtMaNhap.setEnabled(false);
            dongCach.add(txtMaNhap);
            g.gridy = 3;
            nen.add(truong("Cách áp dụng", dongCach), g);
            rbTuDong.addActionListener(e -> txtMaNhap.setEnabled(false));
            rbNhapMa.addActionListener(e -> {
                txtMaNhap.setEnabled(true);
                txtMaNhap.requestFocusInWindow();
            });

            // mức giảm
            g.gridy = 4;
            g.gridwidth = 1;
            g.gridx = 0;
            g.insets = new Insets(0, 0, 8, 12);
            nen.add(truong("Loại giảm", cboLoai), g);
            g.gridx = 1;
            g.insets = new Insets(0, 0, 8, 0);
            nen.add(truong("Giá trị (% hoặc số tiền)", txtGiaTri), g);

            g.gridy = 5;
            g.gridx = 0;
            g.insets = new Insets(0, 0, 8, 12);
            nen.add(truong("Áp dụng cho", cboApDung), g);
            g.gridx = 1;
            g.insets = new Insets(0, 0, 8, 0);
            nen.add(truong("Giảm tối đa (đ) - chỉ khi giảm %", txtToiDa), g);

            g.gridy = 6;
            g.gridx = 0;
            g.gridwidth = 2;
            nen.add(truong("Hóa đơn tối thiểu (đ)", txtToiThieu), g);

            // thứ trong tuần
            JPanel dongThu = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            dongThu.setOpaque(false);
            for (int i = 0; i < 7; i++) {
                chkThu[i] = new JCheckBox(TEN_THU[i]);
                chkThu[i].setOpaque(false);
                chkThu[i].setFont(new Font(FONT, Font.PLAIN, 13));
                dongThu.add(chkThu[i]);
            }
            g.gridy = 7;
            nen.add(truong("Thứ áp dụng theo ngày chiếu (không chọn = mọi ngày)", dongThu), g);

            // thời gian
            JPanel dongNgay = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            dongNgay.setOpaque(false);
            dongNgay.add(new JLabel("Từ"));
            dongNgay.add(spTu);
            dongNgay.add(new JLabel("đến"));
            dongNgay.add(spDen);
            g.gridy = 8;
            nen.add(truong("Thời gian áp dụng (theo ngày chiếu)", dongNgay), g);

            // giới hạn + kích hoạt
            JPanel dongGh = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            dongGh.setOpaque(false);
            chkGioiHan.setOpaque(false);
            chkGioiHan.setFont(new Font(FONT, Font.PLAIN, 13));
            spLuot.setPreferredSize(new Dimension(100, 30));
            spLuot.setEnabled(false);
            chkGioiHan.addActionListener(e -> spLuot.setEnabled(chkGioiHan.isSelected()));
            chkHoatDong.setOpaque(false);
            chkHoatDong.setFont(new Font(FONT, Font.PLAIN, 13));
            dongGh.add(chkGioiHan);
            dongGh.add(spLuot);
            dongGh.add(Box.createHorizontalStrut(20));
            dongGh.add(chkHoatDong);
            g.gridy = 9;
            nen.add(dongGh, g);

            lblLoi.setFont(new Font(FONT, Font.PLAIN, 13));
            lblLoi.setForeground(DO);
            g.gridy = 10;
            g.insets = new Insets(4, 0, 4, 0);
            nen.add(lblLoi, g);

            btnLuu.setText(sua ? "Lưu thay đổi" : "Thêm khuyến mãi");
            btnLuu.setPreferredSize(new Dimension(170, 34));
            JButton btnHuy = nut("Hủy", false);
            JPanel nutBam = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            nutBam.setOpaque(false);
            nutBam.add(btnHuy);
            nutBam.add(btnLuu);
            g.gridy = 11;
            nen.add(nutBam, g);

            btnHuy.addActionListener(e -> dispose());
            btnLuu.addActionListener(e -> luu());

            if (sua) napDuLieu();

            setContentPane(nen);
            getRootPane().setDefaultButton(btnLuu);
            pack();
            setResizable(false);
            setLocationRelativeTo(chu);
        }

        private void napDuLieu() {
            KhuyenMai k = dangSua;
            txtMa.setText(k.ma);
            txtTen.setText(k.ten);
            txtMoTa.setText(k.moTa == null ? "" : k.moTa);
            if (!k.tuDong()) {
                rbNhapMa.setSelected(true);
                txtMaNhap.setEnabled(true);
                txtMaNhap.setText(k.maNhap);
            }
            cboLoai.setSelectedIndex(KhuyenMaiDAO.PHAN_TRAM.equals(k.loaiGiam) ? 0 : 1);
            txtGiaTri.setText(String.valueOf(k.giaTri));
            cboApDung.setSelectedIndex(KhuyenMaiDAO.CHO_TONG.equals(k.apDungCho) ? 1 : 0);
            txtToiDa.setText(k.giamToiDa == null ? "" : String.valueOf(k.giamToiDa));
            txtToiThieu.setText(String.valueOf(k.donToiThieu));
            for (int i = 0; i < 7; i++) chkThu[i].setSelected(k.apDungThu(i + 1) && k.ngayTrongTuan != null && !k.ngayTrongTuan.isEmpty());
            spTu.setValue(Date.from(k.tuNgay.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            spDen.setValue(Date.from(k.denNgay.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            if (k.gioiHan != null) {
                chkGioiHan.setSelected(true);
                spLuot.setEnabled(true);
                spLuot.setValue(k.gioiHan);
            }
            chkHoatDong.setSelected(k.hoatDong);
        }

        private JSpinner taoNgay(LocalDate d) {
            JSpinner sp = new JSpinner(new SpinnerDateModel(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                    null, null, java.util.Calendar.DAY_OF_MONTH));
            sp.setEditor(new JSpinner.DateEditor(sp, "dd/MM/yyyy"));
            sp.setFont(new Font(FONT, Font.PLAIN, 13));
            sp.setPreferredSize(new Dimension(120, 30));
            return sp;
        }

        private LocalDate ngay(JSpinner sp) {
            return ((Date) sp.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        private JPanel truong(String nhan, JComponent o) {
            JPanel p = new JPanel(new BorderLayout(0, 3));
            p.setOpaque(false);
            JLabel l = new JLabel(nhan);
            l.setFont(new Font(FONT, Font.PLAIN, 12));
            l.setForeground(CHU_XAM);
            p.add(l, BorderLayout.NORTH);
            p.add(o, BorderLayout.CENTER);
            return p;
        }

        private Long soTien(String chu) {
            String s = chu == null ? "" : chu.replaceAll("[^0-9]", "");
            if (s.isEmpty() || s.length() > 9) return null;
            return Long.parseLong(s);
        }

        private void luu() {
            final boolean sua = dangSua != null;
            String ma = txtMa.getText().trim().toUpperCase();
            String ten = txtTen.getText().trim();
            boolean phanTram = cboLoai.getSelectedIndex() == 0;

            if (!sua && !ma.matches("[A-Z0-9_]{2,20}")) {
                lblLoi.setText("Mã khuyến mãi 2–20 ký tự: chữ hoa không dấu, số, dấu _");
                return;
            }
            if (ten.isEmpty() || ten.length() > 150) {
                lblLoi.setText("Vui lòng nhập tên chương trình (tối đa 150 ký tự).");
                return;
            }
            String maNhap = null;
            if (rbNhapMa.isSelected()) {
                maNhap = txtMaNhap.getText().trim();
                if (!maNhap.matches("[A-Za-z0-9_\\-]{3,30}")) {
                    lblLoi.setText("Mã khách nhập 3–30 ký tự: chữ không dấu, số, dấu _ -");
                    return;
                }
            }
            Long giaTri = soTien(txtGiaTri.getText());
            if (giaTri == null || giaTri <= 0 || (phanTram && giaTri > 100)) {
                lblLoi.setText(phanTram ? "Giá trị giảm phải từ 1 đến 100 (%)." : "Số tiền giảm phải lớn hơn 0.");
                return;
            }
            Long toiDa = null;
            if (phanTram && !txtToiDa.getText().trim().isEmpty()) {
                toiDa = soTien(txtToiDa.getText());
                if (toiDa == null || toiDa <= 0) {
                    lblLoi.setText("Giảm tối đa phải lớn hơn 0 (hoặc để trống).");
                    return;
                }
            }
            Long toiThieu = txtToiThieu.getText().trim().isEmpty() ? Long.valueOf(0) : soTien(txtToiThieu.getText());
            if (toiThieu == null) {
                lblLoi.setText("Hóa đơn tối thiểu không hợp lệ.");
                return;
            }
            LocalDate tu = ngay(spTu), den = ngay(spDen);
            if (tu.isAfter(den)) {
                lblLoi.setText("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
                return;
            }
            StringBuilder thu = new StringBuilder();
            int dem = 0;
            for (int i = 0; i < 7; i++) {
                if (chkThu[i].isSelected()) {
                    if (thu.length() > 0) thu.append(",");
                    thu.append(i + 1);
                    dem++;
                }
            }

            final KhuyenMai k = new KhuyenMai();
            k.ma = sua ? dangSua.ma : ma;
            k.ten = ten;
            k.moTa = txtMoTa.getText().trim().isEmpty() ? null : txtMoTa.getText().trim();
            k.maNhap = maNhap;
            k.loaiGiam = phanTram ? KhuyenMaiDAO.PHAN_TRAM : KhuyenMaiDAO.SO_TIEN;
            k.giaTri = giaTri;
            k.apDungCho = cboApDung.getSelectedIndex() == 1 ? KhuyenMaiDAO.CHO_TONG : KhuyenMaiDAO.CHO_VE;
            k.giamToiDa = toiDa;
            k.donToiThieu = toiThieu;
            k.ngayTrongTuan = (dem == 0 || dem == 7) ? null : thu.toString();
            k.tuNgay = tu;
            k.denNgay = den;
            k.gioiHan = chkGioiHan.isSelected() ? (Integer) spLuot.getValue() : null;
            k.hoatDong = chkHoatDong.isSelected();

            lblLoi.setText(" ");
            btnLuu.setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    if (sua) dao.capNhat(k); else dao.them(k);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        dispose();
                        taiLai(k.ma);
                    } catch (ExecutionException e) {
                        btnLuu.setEnabled(true);
                        Throwable g = e.getCause();
                        if (g instanceof TrungMaException) {
                            lblLoi.setText(g.getMessage());
                        } else {
                            g.printStackTrace();
                            lblLoi.setText("Lỗi: " + g.getMessage());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }.execute();
        }
    }
}