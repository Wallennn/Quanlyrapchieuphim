package view;

import dao.NhanVienDAO;
import dao.NhanVienDAO.NhanVien;
import dao.NhanVienDAO.RangBuocException;
import dao.NhanVienDAO.TenDangNhapTrungException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * Màn "Quản lý nhân viên" của quản trị viên:
 * xem danh sách tài khoản, tìm kiếm, thêm mới (mật khẩu được băm Argon2i), sửa,
 * đặt lại mật khẩu và khóa / mở khóa tài khoản.
 */
public class NhanVienPanel extends JPanel {

    private static final String FONT = "Segoe UI";
    private static final Color NAU_DAM    = new Color(0x8A, 0x6D, 0x4A);
    private static final Color NEN_KEM    = new Color(0xFB, 0xF6, 0xEF);
    private static final Color NEN_HEADER = new Color(0x3A, 0x30, 0x28);
    private static final Color CHU_XAM    = new Color(0x8A, 0x87, 0x82);
    private static final Color CHU_DAM    = new Color(0x2E, 0x2A, 0x26);
    private static final Color VIEN       = new Color(0xEA, 0xDF, 0xCF);
    private static final Color XANH       = new Color(0x2E, 0x7D, 0x32);
    private static final Color DO         = new Color(0xC6, 0x28, 0x28);
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int COT_ID = 0, COT_TRANG_THAI = 4, COT_NGAY_TAO = 5, COT_DN_CUOI = 6;
    private static final int MK_TOI_THIEU = 6;
    private static final String CHU_NV = "Nhân viên", CHU_QL = "Quản lý";

    private final NhanVienDAO dao;
    private final long idHienTai;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Id", "Tên đăng nhập", "Họ tên", "Vai trò", "Trạng thái", "Ngày tạo", "Lần đăng nhập cuối"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) { return c == COT_ID ? Long.class : String.class; }
    };
    private final JTable bang = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField txtTim = new JTextField();
    private final JButton btnThem = nut("+  Thêm nhân viên", true);
    private final JButton btnSua = nut("Sửa", false);
    private final JButton btnKhoa = nut("Khóa", false);
    private final JButton btnLamMoi = nut("Làm mới", false);
    private final JLabel lblDem = new JLabel(" ");

    private List<NhanVien> ds = new ArrayList<>();

    public NhanVienPanel(long idQuanLyDangNhap) {
        this.idHienTai = idQuanLyDangNhap;
        this.dao = new NhanVienDAO(idQuanLyDangNhap);

        setLayout(new BorderLayout(0, 14));
        setOpaque(false);

        // ----- Hàng trên: tiêu đề + tìm kiếm + nút -----
        JPanel tren = new JPanel(new BorderLayout());
        tren.setOpaque(false);
        JLabel tieuDe = new JLabel("Quản lý nhân viên");
        tieuDe.setFont(new Font(FONT, Font.BOLD, 24));
        tieuDe.setForeground(CHU_DAM);
        tren.add(tieuDe, BorderLayout.WEST);

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        phai.setOpaque(false);
        txtTim.setFont(new Font(FONT, Font.PLAIN, 13));
        txtTim.setPreferredSize(new Dimension(260, 34));
        txtTim.putClientProperty("JTextField.placeholderText", "Tìm theo tên đăng nhập, họ tên...");
        phai.add(txtTim);
        phai.add(btnThem);
        phai.add(btnSua);
        phai.add(btnKhoa);
        phai.add(btnLamMoi);
        tren.add(phai, BorderLayout.EAST);
        add(tren, BorderLayout.NORTH);

        // ----- Bảng -----
        dungBang();
        JScrollPane cuon = new JScrollPane(bang);
        cuon.setBorder(BorderFactory.createLineBorder(VIEN, 1));
        cuon.getViewport().setBackground(Color.WHITE);
        add(cuon, BorderLayout.CENTER);

        // ----- Chân trang -----
        lblDem.setFont(new Font(FONT, Font.PLAIN, 12));
        lblDem.setForeground(CHU_XAM);
        add(lblDem, BorderLayout.SOUTH);

        // ----- Sự kiện -----
        btnThem.addActionListener(e -> moHopThoai(null));
        btnSua.addActionListener(e -> suaDong());
        btnKhoa.addActionListener(e -> khoaHoacMo());
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

    // ===================== GIAO DIỆN BẢNG =====================
    private void dungBang() {
        bang.setRowHeight(40);
        bang.setFont(new Font(FONT, Font.PLAIN, 13));
        bang.setShowVerticalLines(false);
        bang.setGridColor(new Color(0xF0, 0xE9, 0xDD));
        bang.setFillsViewportHeight(true);
        bang.setSelectionBackground(new Color(0xF3, 0xE6, 0xD3));
        bang.setSelectionForeground(CHU_DAM);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.setRowSorter(sorter);
        sorter.setSortable(COT_NGAY_TAO, false);
        sorter.setSortable(COT_DN_CUOI, false);

        DefaultTableCellRenderer dauCot = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
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
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
                String chu = (v == null) ? "" : v.toString();
                setForeground(CHU_DAM);
                setFont(new Font(FONT, Font.PLAIN, 13));
                if (c == COT_TRANG_THAI) {
                    setForeground("Hoạt động".equals(chu) ? XANH : DO);
                    setFont(new Font(FONT, Font.BOLD, 13));
                } else if (c == COT_DN_CUOI && "Chưa đăng nhập".equals(chu)) {
                    setForeground(CHU_XAM);
                }
                return this;
            }
        };
        for (int c = 0; c < bang.getColumnCount(); c++) {
            bang.getColumnModel().getColumn(c).setCellRenderer(o);
        }
        bang.getColumnModel().getColumn(COT_ID).setMaxWidth(70);
    }

    private static JButton nut(String chu, boolean chinh) {
        JButton b = new JButton(chu);
        b.setFont(new Font(FONT, chinh ? Font.BOLD : Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setBackground(chinh ? NEN_HEADER : Color.WHITE);
        b.setForeground(chinh ? Color.WHITE : CHU_DAM);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(0, 10, 0, 10));
        b.setPreferredSize(new Dimension(chinh ? 170 : 90, 34));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    // ===================== DỮ LIỆU =====================
    /** Tải lại danh sách. chonId != null thì chọn sẵn dòng của tài khoản đó. */
    private void taiLai(Long chonId) {
        lblDem.setText("Đang tải...");
        new SwingWorker<List<NhanVien>, Void>() {
            @Override
            protected List<NhanVien> doInBackground() throws Exception {
                return dao.layTatCa();
            }

            @Override
            protected void done() {
                try {
                    ds = get();
                    model.setRowCount(0);
                    for (NhanVien n : ds) {
                        model.addRow(new Object[]{
                            n.id, n.tenDangNhap, n.hoTen, tenVaiTro(n.vaiTro),
                            n.hoatDong ? "Hoạt động" : "Đã khóa",
                            dinhDang(n.ngayTao, ""),
                            dinhDang(n.lanDangNhapCuoi, "Chưa đăng nhập")
                        });
                    }
                    capNhatChanTrang();
                    if (chonId != null) {
                        for (int i = 0; i < ds.size(); i++) {
                            if (ds.get(i).id == chonId) {
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
        sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?iu)" + Pattern.quote(q), 1, 2));
        capNhatChanTrang();
        capNhatNut();
    }

    private void capNhatChanTrang() {
        int hien = bang.getRowCount();
        int hoatDong = 0;
        for (NhanVien n : ds) if (n.hoatDong) hoatDong++;
        lblDem.setText(hien + "/" + ds.size() + " tài khoản  •  " + hoatDong
                + " đang hoạt động  •  Mật khẩu được mã hóa (Argon2) nên không hiển thị.");
    }

    private NhanVien dongDangChon() {
        int dong = bang.getSelectedRow();
        if (dong < 0) return null;
        int goc = bang.convertRowIndexToModel(dong);
        return (goc >= 0 && goc < ds.size()) ? ds.get(goc) : null;
    }

    private void capNhatNut() {
        NhanVien n = dongDangChon();
        btnSua.setEnabled(n != null);
        btnKhoa.setEnabled(n != null);
        btnKhoa.setText(n != null && !n.hoatDong ? "Mở khóa" : "Khóa");
    }

    private static String tenVaiTro(String vaiTro) {
        return NhanVienDAO.VT_QUAN_LY.equals(vaiTro) ? CHU_QL : CHU_NV;
    }

    private static String dinhDang(LocalDateTime t, String khiTrong) {
        return t == null ? khiTrong : t.format(NGAY_GIO);
    }

    // ===================== THAO TÁC =====================
    private void suaDong() {
        NhanVien n = dongDangChon();
        if (n != null) moHopThoai(n);
    }

    private void khoaHoacMo() {
        NhanVien n = dongDangChon();
        if (n == null) return;
        final boolean moKhoa = !n.hoatDong;
        if (!moKhoa) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Khóa tài khoản \"" + n.tenDangNhap + "\" (" + n.hoTen + ")?\n"
                            + "Người này sẽ không đăng nhập được cho tới khi được mở khóa.",
                    "Khóa tài khoản", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
        }
        final long id = n.id;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                dao.datTrangThai(id, moKhoa);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    taiLai(id);
                } catch (ExecutionException e) {
                    Throwable g = e.getCause();
                    if (!(g instanceof RangBuocException)) g.printStackTrace();
                    JOptionPane.showMessageDialog(NhanVienPanel.this, g.getMessage(),
                            "Không thực hiện được", JOptionPane.WARNING_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void moHopThoai(NhanVien sua) {
        new HopThoai(SwingUtilities.getWindowAncestor(this), sua).setVisible(true);
    }

    // ===================== HỘP THOẠI THÊM / SỬA =====================
    private class HopThoai extends JDialog {
        private final NhanVien dangSua;   // null = thêm mới

        private final JTextField txtTen = new JTextField();
        private final JTextField txtHoTen = new JTextField();
        private final JComboBox<String> cboVaiTro = new JComboBox<>(new String[]{CHU_NV, CHU_QL});
        private final JCheckBox chkHoatDong = new JCheckBox("Cho phép đăng nhập", true);
        private final JPasswordField txtMk = new JPasswordField();
        private final JPasswordField txtMk2 = new JPasswordField();
        private final JCheckBox chkHien = new JCheckBox("Hiện mật khẩu");
        private final JLabel lblLoi = new JLabel(" ");
        private final JButton btnLuu = nut(" ", true);
        private final char kyTuAn;

        HopThoai(Window chu, NhanVien dangSua) {
            super(chu, dangSua == null ? "Thêm nhân viên" : "Sửa nhân viên", ModalityType.APPLICATION_MODAL);
            this.dangSua = dangSua;
            this.kyTuAn = txtMk.getEchoChar();
            boolean sua = dangSua != null;

            JPanel nen = new JPanel(new GridBagLayout());
            nen.setBackground(NEN_KEM);
            nen.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.weightx = 1;
            g.anchor = GridBagConstraints.WEST;
            g.insets = new Insets(0, 0, 4, 0);

            JLabel tieuDe = new JLabel(sua ? "Sửa nhân viên" : "Thêm nhân viên mới");
            tieuDe.setFont(new Font(FONT, Font.BOLD, 20));
            tieuDe.setForeground(CHU_DAM);
            g.insets = new Insets(0, 0, 14, 0);
            nen.add(tieuDe, g);

            txtTen.setText(sua ? dangSua.tenDangNhap : "");
            txtTen.setEditable(!sua);
            txtHoTen.setText(sua ? dangSua.hoTen : "");
            cboVaiTro.setSelectedItem(sua && NhanVienDAO.VT_QUAN_LY.equals(dangSua.vaiTro) ? CHU_QL : CHU_NV);
            chkHoatDong.setSelected(!sua || dangSua.hoatDong);
            chkHoatDong.setOpaque(false);
            chkHoatDong.setFont(new Font(FONT, Font.PLAIN, 13));
            chkHien.setOpaque(false);
            chkHien.setFont(new Font(FONT, Font.PLAIN, 12));
            for (JComponent c : new JComponent[]{txtTen, txtHoTen, txtMk, txtMk2, cboVaiTro}) {
                c.setFont(new Font(FONT, Font.PLAIN, 14));
                c.setPreferredSize(new Dimension(380, 36));
            }

            nen.add(truong("Tên đăng nhập" + (sua ? " (không đổi được)" : ""), txtTen), cot(g, 0));
            nen.add(truong("Họ tên", txtHoTen), cot(g, 0));
            nen.add(truong("Vai trò", cboVaiTro), cot(g, 0));
            nen.add(chkHoatDong, cot(g, 10));
            nen.add(truong(sua ? "Mật khẩu mới (để trống nếu không đổi)" : "Mật khẩu (tối thiểu "
                    + MK_TOI_THIEU + " ký tự)", txtMk), cot(g, 0));
            nen.add(truong("Nhập lại mật khẩu", txtMk2), cot(g, 0));
            nen.add(chkHien, cot(g, 8));

            lblLoi.setFont(new Font(FONT, Font.PLAIN, 13));
            lblLoi.setForeground(DO);
            nen.add(lblLoi, cot(g, 6));

            btnLuu.setText(sua ? "Lưu thay đổi" : "Thêm nhân viên");
            JButton btnHuy = nut("Hủy", false);
            JPanel nutBam = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            nutBam.setOpaque(false);
            nutBam.add(btnHuy);
            nutBam.add(btnLuu);
            nen.add(nutBam, cot(g, 4));

            chkHien.addActionListener(e -> {
                char c = chkHien.isSelected() ? (char) 0 : kyTuAn;
                txtMk.setEchoChar(c);
                txtMk2.setEchoChar(c);
            });
            btnHuy.addActionListener(e -> dispose());
            btnLuu.addActionListener(e -> luu());

            setContentPane(nen);
            getRootPane().setDefaultButton(btnLuu);
            pack();
            setResizable(false);
            setLocationRelativeTo(chu);
            SwingUtilities.invokeLater(() -> (sua ? txtHoTen : txtTen).requestFocusInWindow());
        }

        private GridBagConstraints cot(GridBagConstraints g, int cachDuoi) {
            g.insets = new Insets(0, 0, cachDuoi == 0 ? 10 : cachDuoi, 0);
            return g;
        }

        private JPanel truong(String nhan, JComponent o) {
            JPanel p = new JPanel(new BorderLayout(0, 4));
            p.setOpaque(false);
            JLabel l = new JLabel(nhan);
            l.setFont(new Font(FONT, Font.PLAIN, 13));
            l.setForeground(CHU_XAM);
            p.add(l, BorderLayout.NORTH);
            p.add(o, BorderLayout.CENTER);
            return p;
        }

        private void loi(String thongBao) {
            lblLoi.setText(thongBao);
        }

        private void luu() {
            final boolean sua = dangSua != null;
            final String ten = txtTen.getText().trim();
            final String hoTen = txtHoTen.getText().trim();
            final String vaiTro = CHU_QL.equals(cboVaiTro.getSelectedItem())
                    ? NhanVienDAO.VT_QUAN_LY : NhanVienDAO.VT_NHAN_VIEN;
            final boolean hoatDong = chkHoatDong.isSelected();
            final char[] mk = txtMk.getPassword();
            final char[] mk2 = txtMk2.getPassword();
            final boolean coMk = mk.length > 0;

            if (!sua && !ten.matches("[A-Za-z0-9_.\\-]{3,50}")) {
                loi("Tên đăng nhập 3–50 ký tự: chữ không dấu, số, dấu . _ -");
                txtTen.requestFocusInWindow();
                return;
            }
            if (hoTen.isEmpty() || hoTen.length() > 100) {
                loi("Vui lòng nhập họ tên (tối đa 100 ký tự).");
                txtHoTen.requestFocusInWindow();
                return;
            }
            if (!sua || coMk) {
                if (mk.length < MK_TOI_THIEU) {
                    loi("Mật khẩu tối thiểu " + MK_TOI_THIEU + " ký tự.");
                    txtMk.requestFocusInWindow();
                    return;
                }
                if (!Arrays.equals(mk, mk2)) {
                    loi("Mật khẩu nhập lại không khớp.");
                    txtMk2.requestFocusInWindow();
                    return;
                }
            }
            Arrays.fill(mk2, '\0');

            lblLoi.setText(" ");
            btnLuu.setEnabled(false);
            new SwingWorker<Long, Void>() {
                @Override
                protected Long doInBackground() throws Exception {
                    if (sua) {
                        dao.capNhat(dangSua.id, hoTen, vaiTro, hoatDong, coMk ? mk : null);
                        return dangSua.id;
                    }
                    return dao.them(ten, hoTen, vaiTro, hoatDong, mk);
                }

                @Override
                protected void done() {
                    try {
                        Long id = get();
                        dispose();
                        taiLai(id);
                    } catch (ExecutionException e) {
                        btnLuu.setEnabled(true);
                        Throwable g = e.getCause();
                        if (g instanceof TenDangNhapTrungException || g instanceof RangBuocException) {
                            loi(g.getMessage());
                        } else {
                            g.printStackTrace();
                            loi("Lỗi: " + g.getMessage());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }.execute();
        }
    }
}