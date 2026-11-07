package view;

import dao.InVeDAO;
import model.ComboIn;
import model.HoaDonBapNuoc;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Tab "Bap nuoc rieng": hoa don chi co combo (khong ve) -> xem truoc hoa don + thong tin khach hang -> in ra may in
class BapNuocRiengPanel extends JPanel {

    private static final Color MAU_NAU_TOI = InVeKemBapNuocPanel.MAU_NAU_TOI;
    private static final Color MAU_NEN_KEM = InVeKemBapNuocPanel.MAU_NEN_KEM;
    private static final Color MAU_NEN_HEADER = InVeKemBapNuocPanel.MAU_NEN_HEADER;
    private static final Color MAU_CHU_XAM = InVeKemBapNuocPanel.MAU_CHU_XAM;
    private static final Color MAU_CHU_DAM = InVeKemBapNuocPanel.MAU_CHU_DAM;
    private static final Color MAU_VIEN = InVeKemBapNuocPanel.MAU_VIEN;
    private static final String DA_THANH_TOAN = "Da thanh toan";

    private final InVeDAO dao = new InVeDAO();

    private final JTextField txtTimKiem = new JTextField(16);
    private final JCheckBox chkHomNay = new JCheckBox("Chỉ hôm nay", true);
    private final JLabel lblKetQua = new JLabel(" ");
    private final DefaultTableModel moHinhBang = new DefaultTableModel(
            new String[]{"Mã HĐ", "Khách hàng", "SĐT", "Số combo", "Tổng tiền", "Trạng thái", "Giờ lập"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable bang = new JTable(moHinhBang);

    private final JPanel panelXemTruoc = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    private final JLabel lblThongTinHd = new JLabel("Chọn một hóa đơn bên trái để xem");
    private final JButton btnIn = new JButton("In hóa đơn");

    private List<HoaDonBapNuoc> cacHoaDon = new ArrayList<>();
    private HoaDonBapNuoc hdDangChon;
    private List<ComboIn> cacCombo = new ArrayList<>();
    private boolean daChonDuoc = false;

    BapNuocRiengPanel(String maHdChonSan) {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        add(taoThanhTren(), BorderLayout.NORTH);

        JPanel giua = new JPanel(new GridBagLayout());
        giua.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.gridy = 0;
        g.weighty = 1.0;
        g.gridx = 0;
        g.weightx = 0.6;
        g.insets = new Insets(0, 0, 0, 20);
        giua.add(taoPanelBang(), g);
        g.gridx = 1;
        g.weightx = 0.4;
        g.insets = new Insets(0, 0, 0, 0);
        giua.add(taoPanelXemTruoc(), g);
        add(giua, BorderLayout.CENTER);

        capNhatNut();

        if (maHdChonSan != null && !maHdChonSan.isBlank()) {
            txtTimKiem.setText(maHdChonSan);
            chkHomNay.setSelected(false);
            timKiem();
            for (int i = 0; i < cacHoaDon.size(); i++) {
                if (cacHoaDon.get(i).maHd().equalsIgnoreCase(maHdChonSan.trim())) {
                    bang.setRowSelectionInterval(i, i);
                    daChonDuoc = true;
                    break;
                }
            }
            if (!daChonDuoc) {     // khong phai hoa don bap nuoc rieng -> tra lai man hinh mac dinh
                txtTimKiem.setText("");
                chkHomNay.setSelected(true);
                timKiem();
            }
        } else {
            timKiem();
        }
    }

    /** True neu ma hoa don truyen vao la 1 hoa don bap nuoc rieng va da duoc chon san. */
    boolean daChonDuoc() {
        return daChonDuoc;
    }

    private JPanel taoThanhTren() {
        JPanel thanh = new JPanel(new BorderLayout());
        thanh.setOpaque(false);
        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        phai.setOpaque(false);

        JLabel lblTim = new JLabel("Mã hóa đơn / tên khách / SĐT:");
        lblTim.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTim.setForeground(MAU_CHU_XAM);

        txtTimKiem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtTimKiem.addActionListener(e -> timKiem());

        chkHomNay.setOpaque(false);
        chkHomNay.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chkHomNay.addActionListener(e -> timKiem());

        JButton btnTim = new JButton("Tìm");
        btnTim.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnTim.setForeground(Color.WHITE);
        btnTim.setBackground(MAU_NAU_TOI);
        btnTim.setFocusPainted(false);
        btnTim.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTim.addActionListener(e -> timKiem());

        JButton btnLamMoi = new JButton("Làm mới");
        btnLamMoi.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnLamMoi.setFocusPainted(false);
        btnLamMoi.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            chkHomNay.setSelected(true);
            timKiem();
        });

        phai.add(lblTim);
        phai.add(txtTimKiem);
        phai.add(chkHomNay);
        phai.add(btnTim);
        phai.add(btnLamMoi);
        thanh.add(phai, BorderLayout.EAST);
        return thanh;
    }

    private JPanel taoPanelBang() {
        JPanel the = new JPanel(new BorderLayout(0, 8));
        the.setBackground(Color.WHITE);
        the.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1),
                BorderFactory.createEmptyBorder(14, 14, 10, 14)));
        the.setPreferredSize(new Dimension(640, 100));

        bang.setRowHeight(36);
        bang.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.setShowVerticalLines(false);
        bang.setGridColor(new Color(0xF0, 0xE9, 0xDD));
        bang.setSelectionBackground(new Color(0xF3, 0xE6, 0xD3));
        bang.setSelectionForeground(MAU_CHU_DAM);
        bang.setFillsViewportHeight(true);

        DefaultTableCellRenderer dauCot = new DefaultTableCellRenderer();
        dauCot.setHorizontalAlignment(SwingConstants.CENTER);
        dauCot.setBackground(MAU_NEN_HEADER);
        dauCot.setForeground(Color.WHITE);
        dauCot.setFont(new Font("Segoe UI", Font.BOLD, 13));
        bang.getTableHeader().setDefaultRenderer(dauCot);
        bang.getTableHeader().setReorderingAllowed(false);
        bang.getTableHeader().setPreferredSize(new Dimension(0, 38));

        int[] doRong = {150, 140, 100, 70, 90, 115, 90};
        for (int i = 0; i < doRong.length; i++) {
            bang.getColumnModel().getColumn(i).setPreferredWidth(doRong[i]);
        }
        DefaultTableCellRenderer giuaO = new DefaultTableCellRenderer();
        giuaO.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i : new int[]{0, 2, 3, 4, 6}) {
            bang.getColumnModel().getColumn(i).setCellRenderer(giuaO);
        }
        bang.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                String chu = String.valueOf(v);
                setFont(getFont().deriveFont(Font.BOLD));
                if (chu.equals("Đã thanh toán")) setForeground(new Color(0x2E, 0x7D, 0x32));
                else if (chu.equals("Đã hủy")) setForeground(new Color(0xC6, 0x28, 0x28));
                else setForeground(new Color(0xB2, 0x6A, 0x00));
                return this;
            }
        });

        bang.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int dong = bang.getSelectedRow();
                if (dong >= 0 && dong < cacHoaDon.size()) {
                    taiHoaDon(cacHoaDon.get(dong));
                }
            }
        });

        JScrollPane cuon = new JScrollPane(bang);
        cuon.setBorder(BorderFactory.createLineBorder(MAU_VIEN, 1));
        cuon.getViewport().setBackground(Color.WHITE);
        the.add(cuon, BorderLayout.CENTER);

        lblKetQua.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblKetQua.setForeground(MAU_CHU_XAM);
        the.add(lblKetQua, BorderLayout.SOUTH);
        return the;
    }

    private JPanel taoPanelXemTruoc() {
        JPanel the = new JPanel(new BorderLayout(0, 10));
        the.setBackground(Color.WHITE);
        the.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        the.setPreferredSize(new Dimension(420, 100));

        JPanel dau = new JPanel();
        dau.setOpaque(false);
        dau.setLayout(new BoxLayout(dau, BoxLayout.Y_AXIS));
        JLabel lblTieuDe = new JLabel("Xem trước hóa đơn bắp nước");
        lblTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTieuDe.setForeground(MAU_CHU_DAM);
        lblTieuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblThongTinHd.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblThongTinHd.setForeground(MAU_CHU_XAM);
        lblThongTinHd.setAlignmentX(Component.LEFT_ALIGNMENT);
        dau.add(lblTieuDe);
        dau.add(Box.createVerticalStrut(2));
        dau.add(lblThongTinHd);
        the.add(dau, BorderLayout.NORTH);

        panelXemTruoc.setBackground(MAU_NEN_KEM);
        panelXemTruoc.setBorder(BorderFactory.createEmptyBorder(12, 8, 4, 8));
        JScrollPane cuon = new JScrollPane(panelXemTruoc);
        cuon.setBorder(BorderFactory.createLineBorder(MAU_VIEN, 1));
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        the.add(cuon, BorderLayout.CENTER);

        JPanel duoi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        duoi.setOpaque(false);
        btnIn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnIn.setForeground(Color.WHITE);
        btnIn.setBackground(MAU_NAU_TOI);
        btnIn.setFocusPainted(false);
        btnIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnIn.addActionListener(e -> inHoaDon());
        duoi.add(btnIn);
        the.add(duoi, BorderLayout.SOUTH);
        return the;
    }

    // ===== Du lieu =====
    private void timKiem() {
        try {
            cacHoaDon = dao.timHoaDonBapNuocRieng(txtTimKiem.getText(), chkHomNay.isSelected());
        } catch (SQLException e) {
            cacHoaDon = new ArrayList<>();
            JOptionPane.showMessageDialog(this, "Lỗi khi tải hóa đơn: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        DateTimeFormatter dinhDangLap = DateTimeFormatter.ofPattern("HH:mm dd/MM");
        moHinhBang.setRowCount(0);
        for (HoaDonBapNuoc hd : cacHoaDon) {
            moHinhBang.addRow(new Object[]{
                hd.maHd(),
                hd.laThanhVien() ? hd.tenKhach() : "Khách lẻ",
                hd.soDienThoai() != null ? hd.soDienThoai() : "—",
                hd.soCombo(),
                InVeKemBapNuocPanel.dinhDangTien(hd.tongTien()),
                nhanTrangThai(hd.trangThai()),
                hd.ngayLap() != null ? hd.ngayLap().format(dinhDangLap) : ""
            });
        }
        lblKetQua.setText(cacHoaDon.isEmpty()
                ? "Không có hóa đơn bắp nước riêng nào" + (chkHomNay.isSelected() ? " hôm nay (bỏ chọn \"Chỉ hôm nay\" để xem các ngày trước)." : ".")
                : "Tìm thấy " + cacHoaDon.size() + " hóa đơn. Bấm vào một dòng để xem.");

        hdDangChon = null;
        cacCombo = new ArrayList<>();
        hienThi();
        lblThongTinHd.setText("Chọn một hóa đơn bên trái để xem");
    }

    private void taiHoaDon(HoaDonBapNuoc hd) {
        hdDangChon = hd;
        try {
            cacCombo = dao.layComboTheoHoaDon(hd.maHd());
        } catch (SQLException e) {
            cacCombo = new ArrayList<>();
            JOptionPane.showMessageDialog(this, "Lỗi khi tải combo: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
        hienThi();

        String tomTat = hd.maHd() + "  •  " + hd.soCombo() + " combo  •  "
                + InVeKemBapNuocPanel.dinhDangTien(hd.tongTien()) + "đ";
        if (!DA_THANH_TOAN.equals(hd.trangThai())) {
            lblThongTinHd.setText("<html>" + tomTat + "<br><font color='#C62828'>Hóa đơn "
                    + nhanTrangThai(hd.trangThai()).toLowerCase() + " - không thể in.</font></html>");
        } else {
            lblThongTinHd.setText(tomTat);
        }
    }

    private void hienThi() {
        panelXemTruoc.removeAll();
        if (hdDangChon != null && !cacCombo.isEmpty()) {
            panelXemTruoc.add(new ThePhieu(hdDangChon, cacCombo));
        }
        panelXemTruoc.revalidate();
        panelXemTruoc.repaint();
        capNhatNut();
    }

    private void capNhatNut() {
        btnIn.setEnabled(hdDangChon != null && !cacCombo.isEmpty() && DA_THANH_TOAN.equals(hdDangChon.trangThai()));
    }

    // ===== In =====
    private void inHoaDon() {
        if (hdDangChon == null || cacCombo.isEmpty()) {
            return;
        }
        HoaDonBapNuoc hd = hdDangChon;
        List<ComboIn> combo = cacCombo;
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("In hoa don bap nuoc " + hd.maHd());
        job.setPrintable((Graphics g, PageFormat pf, int trang) -> {
            if (trang > 0) {
                return Printable.NO_SUCH_PAGE;
            }
            double tyLe = Math.min(1.0, pf.getImageableWidth() / InVeKemBapNuocPanel.VE_RONG);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.scale(tyLe, tyLe);
            veHoaDon(g2, 0, 0, hd, combo);
            g2.dispose();
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "Đã gửi lệnh in hóa đơn " + hd.maHd() + ".",
                        "In hóa đơn", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi in: " + e.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void veHoaDon(Graphics2D g, int x, int y, HoaDonBapNuoc hd, List<ComboIn> combo) {
        PhieuBapNuoc.ve(g, x, y, "HÓA ĐƠN BẮP NƯỚC", hd.maHd(), hd.ngayLap(), hd.tenNhanVien(), true, hd, combo);
    }

    private static class ThePhieu extends JComponent {
        private final HoaDonBapNuoc hd;
        private final List<ComboIn> combo;

        ThePhieu(HoaDonBapNuoc hd, List<ComboIn> combo) {
            this.hd = hd;
            this.combo = combo;
            Dimension d = new Dimension(InVeKemBapNuocPanel.VE_RONG,
                    PhieuBapNuoc.chieuCao(combo.size(), true, hd.laThanhVien()));
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            veHoaDon(g2, 0, 0, hd, combo);
            g2.dispose();
        }
    }

    private static String nhanTrangThai(String trangThai) {
        if ("Da thanh toan".equals(trangThai)) return "Đã thanh toán";
        if ("Da huy".equals(trangThai)) return "Đã hủy";
        if ("Da hoan tien".equals(trangThai)) return "Đã hoàn tiền";
        return trangThai == null ? "" : trangThai;
    }
}