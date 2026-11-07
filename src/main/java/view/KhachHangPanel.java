package view;

import dao.KhachHangDAO;
import dao.KhachHangDAO.CoHoaDonException;
import dao.KhachHangDAO.KhachHang;
import dao.KhachHangDAO.SdtTrungException;

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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * Man "Khach hang thanh vien" cua quan tri vien: danh sach, tim kiem, them / sua / xoa,
 * dieu chinh diem. Hang (Thuong / Bac / Vang) tu dong tinh theo tong chi tieu khi khach mua ve.
 */
public class KhachHangPanel extends JPanel {

    private static final String FONT = "Segoe UI";
    private static final Color NAU_DAM    = new Color(0x8A, 0x6D, 0x4A);
    private static final Color NEN_KEM    = new Color(0xFB, 0xF6, 0xEF);
    private static final Color NEN_HEADER = new Color(0x3A, 0x30, 0x28);
    private static final Color CHU_XAM    = new Color(0x8A, 0x87, 0x82);
    private static final Color CHU_DAM    = new Color(0x2E, 0x2A, 0x26);
    private static final Color VIEN       = new Color(0xEA, 0xDF, 0xCF);
    private static final Color DO         = new Color(0xC6, 0x28, 0x28);
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final int COT_ID = 0, COT_HANG = 3, COT_DIEM = 4, COT_CHI_TIEU = 5;

    private final KhachHangDAO dao = new KhachHangDAO();
    private final NumberFormat so = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Id", "Số điện thoại", "Họ tên", "Hạng", "Điểm", "Tổng chi tiêu", "Ngày đăng ký"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) { return c == COT_ID ? Long.class : String.class; }
    };
    private final JTable bang = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField txtTim = new JTextField();
    private final JButton btnThem = nut("+  Thêm khách hàng", true);
    private final JButton btnSua = nut("Sửa", false);
    private final JButton btnXoa = nut("Xóa", false);
    private final JButton btnLamMoi = nut("Làm mới", false);
    private final JLabel lblDem = new JLabel(" ");

    private List<KhachHang> ds = new ArrayList<>();

    public KhachHangPanel() {
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);

        JPanel tren = new JPanel(new BorderLayout());
        tren.setOpaque(false);
        JLabel tieuDe = new JLabel("Khách hàng thành viên");
        tieuDe.setFont(new Font(FONT, Font.BOLD, 24));
        tieuDe.setForeground(CHU_DAM);
        tren.add(tieuDe, BorderLayout.WEST);

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        phai.setOpaque(false);
        txtTim.setFont(new Font(FONT, Font.PLAIN, 13));
        txtTim.setPreferredSize(new Dimension(260, 34));
        txtTim.putClientProperty("JTextField.placeholderText", "Tìm theo số điện thoại, họ tên...");
        phai.add(txtTim);
        phai.add(btnThem);
        phai.add(btnSua);
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
        bang.setRowHeight(40);
        bang.setFont(new Font(FONT, Font.PLAIN, 13));
        bang.setShowVerticalLines(false);
        bang.setGridColor(new Color(0xF0, 0xE9, 0xDD));
        bang.setFillsViewportHeight(true);
        bang.setSelectionBackground(new Color(0xF3, 0xE6, 0xD3));
        bang.setSelectionForeground(CHU_DAM);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.setRowSorter(sorter);

        DefaultTableCellRenderer dauCot = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                setHorizontalAlignment(c >= COT_DIEM && c <= COT_CHI_TIEU ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
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
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
                setHorizontalAlignment(c >= COT_DIEM && c <= COT_CHI_TIEU ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setForeground(CHU_DAM);
                setFont(new Font(FONT, Font.PLAIN, 13));
                if (c == COT_HANG) {
                    String h = String.valueOf(v);
                    setFont(new Font(FONT, Font.BOLD, 13));
                    setForeground("Vàng".equals(h) ? new Color(0xB8, 0x86, 0x0B)
                            : ("Bạc".equals(h) ? new Color(0x6B, 0x75, 0x80) : CHU_XAM));
                }
                return this;
            }
        };
        for (int c = 0; c < bang.getColumnCount(); c++) bang.getColumnModel().getColumn(c).setCellRenderer(o);
        bang.getColumnModel().getColumn(COT_ID).setMaxWidth(70);
    }

    private static JButton nut(String chu, boolean chinh) {
        JButton b = new JButton(chu);
        b.setFont(new Font(FONT, chinh ? Font.BOLD : Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setBackground(chinh ? NEN_HEADER : Color.WHITE);
        b.setForeground(chinh ? Color.WHITE : CHU_DAM);
        b.setMargin(new Insets(0, 10, 0, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(chinh ? 175 : 80, 34));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    // ===================== DU LIEU =====================
    private void taiLai(Long chonId) {
        lblDem.setText("Đang tải...");
        new SwingWorker<List<KhachHang>, Void>() {
            @Override
            protected List<KhachHang> doInBackground() throws Exception {
                return dao.layTatCa();
            }

            @Override
            protected void done() {
                try {
                    ds = get();
                    model.setRowCount(0);
                    for (KhachHang k : ds) {
                        model.addRow(new Object[]{k.id, k.soDienThoai, k.hoTen, KhachHangDAO.tenHang(k.hang),
                            so.format(k.diem), so.format(k.tongChiTieu) + "đ",
                            k.ngayTao == null ? "" : k.ngayTao.format(NGAY)});
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
        long vang = ds.stream().filter(k -> KhachHangDAO.HANG_VANG.equals(k.hang)).count();
        long bac = ds.stream().filter(k -> KhachHangDAO.HANG_BAC.equals(k.hang)).count();
        lblDem.setText(bang.getRowCount() + "/" + ds.size() + " thành viên  •  " + vang + " hạng Vàng, " + bac
                + " hạng Bạc  •  Hạng tự động: Bạc từ " + so.format(KhachHangDAO.NGUONG_BAC) + "đ, Vàng từ "
                + so.format(KhachHangDAO.NGUONG_VANG) + "đ tổng chi tiêu  •  1 điểm = 1.000đ khi dùng, cộng 1 điểm / 20.000đ");
    }

    private KhachHang dongDangChon() {
        int dong = bang.getSelectedRow();
        if (dong < 0) return null;
        int goc = bang.convertRowIndexToModel(dong);
        return (goc >= 0 && goc < ds.size()) ? ds.get(goc) : null;
    }

    private void capNhatNut() {
        boolean co = dongDangChon() != null;
        btnSua.setEnabled(co);
        btnXoa.setEnabled(co);
    }

    private void suaDong() {
        KhachHang k = dongDangChon();
        if (k != null) moHopThoai(k);
    }

    private void xoaDong() {
        KhachHang k = dongDangChon();
        if (k == null) return;
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa thành viên " + k.hoTen + " (" + k.soDienThoai + ")?\nĐiểm tích lũy sẽ mất.",
                "Xóa khách hàng", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        final long id = k.id;
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                dao.xoa(id);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    taiLai(null);
                } catch (ExecutionException e) {
                    Throwable g = e.getCause();
                    if (!(g instanceof CoHoaDonException)) g.printStackTrace();
                    JOptionPane.showMessageDialog(KhachHangPanel.this, g.getMessage(),
                            "Không xóa được", JOptionPane.WARNING_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void moHopThoai(KhachHang sua) {
        new HopThoai(SwingUtilities.getWindowAncestor(this), sua).setVisible(true);
    }

    // ===================== HOP THOAI THEM / SUA =====================
    private class HopThoai extends JDialog {
        private final KhachHang dangSua;
        private final JTextField txtSdt = new JTextField();
        private final JTextField txtHoTen = new JTextField();
        private final JSpinner spDiem = new JSpinner(new SpinnerNumberModel(0, 0, 10_000_000, 1));
        private final JLabel lblLoi = new JLabel(" ");
        private final JButton btnLuu = nut(" ", true);

        HopThoai(Window chu, KhachHang dangSua) {
            super(chu, dangSua == null ? "Thêm khách hàng" : "Sửa khách hàng", ModalityType.APPLICATION_MODAL);
            this.dangSua = dangSua;
            boolean sua = dangSua != null;

            JPanel nen = new JPanel(new GridBagLayout());
            nen.setBackground(NEN_KEM);
            nen.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.weightx = 1;
            g.anchor = GridBagConstraints.WEST;

            JLabel tieuDe = new JLabel(sua ? "Sửa khách hàng" : "Thêm khách hàng mới");
            tieuDe.setFont(new Font(FONT, Font.BOLD, 20));
            tieuDe.setForeground(CHU_DAM);
            g.insets = new Insets(0, 0, 14, 0);
            nen.add(tieuDe, g);

            txtSdt.setText(sua ? dangSua.soDienThoai : "");
            txtHoTen.setText(sua ? dangSua.hoTen : "");
            spDiem.setValue(sua ? dangSua.diem : 0);
            for (JComponent c : new JComponent[]{txtSdt, txtHoTen, spDiem}) {
                c.setFont(new Font(FONT, Font.PLAIN, 14));
                c.setPreferredSize(new Dimension(360, 36));
            }

            g.insets = new Insets(0, 0, 10, 0);
            nen.add(truong("Số điện thoại (10 chữ số, bắt đầu bằng 0)", txtSdt), g);
            nen.add(truong("Họ tên", txtHoTen), g);
            if (sua) {
                nen.add(truong("Điểm tích lũy (điều chỉnh khi cần)", spDiem), g);
                JLabel thongTin = new JLabel("Hạng hiện tại: " + KhachHangDAO.tenHang(dangSua.hang)
                        + "  •  Tổng chi tiêu: " + so.format(dangSua.tongChiTieu) + "đ");
                thongTin.setFont(new Font(FONT, Font.PLAIN, 13));
                thongTin.setForeground(CHU_XAM);
                nen.add(thongTin, g);
            }

            lblLoi.setFont(new Font(FONT, Font.PLAIN, 13));
            lblLoi.setForeground(DO);
            g.insets = new Insets(6, 0, 6, 0);
            nen.add(lblLoi, g);

            btnLuu.setText(sua ? "Lưu thay đổi" : "Thêm khách hàng");
            btnLuu.setPreferredSize(new Dimension(170, 34));
            JButton btnHuy = nut("Hủy", false);
            JPanel nutBam = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            nutBam.setOpaque(false);
            nutBam.add(btnHuy);
            nutBam.add(btnLuu);
            g.insets = new Insets(4, 0, 0, 0);
            nen.add(nutBam, g);

            btnHuy.addActionListener(e -> dispose());
            btnLuu.addActionListener(e -> luu());

            setContentPane(nen);
            getRootPane().setDefaultButton(btnLuu);
            pack();
            setResizable(false);
            setLocationRelativeTo(chu);
            SwingUtilities.invokeLater(() -> txtSdt.requestFocusInWindow());
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

        private void luu() {
            final boolean sua = dangSua != null;
            final String sdt = txtSdt.getText().trim();
            final String hoTen = txtHoTen.getText().trim();
            final int diem = (Integer) spDiem.getValue();

            if (!KhachHangDAO.sdtHopLe(sdt)) {
                lblLoi.setText("Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.");
                txtSdt.requestFocusInWindow();
                return;
            }
            if (hoTen.isEmpty() || hoTen.length() > 100) {
                lblLoi.setText("Vui lòng nhập họ tên (tối đa 100 ký tự).");
                txtHoTen.requestFocusInWindow();
                return;
            }
            lblLoi.setText(" ");
            btnLuu.setEnabled(false);
            new SwingWorker<Long, Void>() {
                @Override
                protected Long doInBackground() throws Exception {
                    if (sua) {
                        dao.capNhat(dangSua.id, sdt, hoTen, diem);
                        return dangSua.id;
                    }
                    return dao.them(sdt, hoTen).id;
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
                        if (g instanceof SdtTrungException) {
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