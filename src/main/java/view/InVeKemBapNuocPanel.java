package view;

import dao.InVeDAO;
import model.ComboIn;
import model.HoaDonTomTat;
import model.VeIn;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Tab "In ve + bap nuoc": tim hoa don co ve -> xem truoc cac ve (moi ghe 1 ve) va phieu bap nuoc di kem (neu co) -> in ra may in
public class InVeKemBapNuocPanel extends JPanel {

    // Kich thuoc 1 ve (don vi diem anh, 1 diem = 1/72 inch)
    static final int VE_RONG = 360;
    static final int VE_CAO = 180;
    static final int KHOANG_VE = 14;

    static final Color MAU_NAU_DAM    = new Color(0x8A, 0x6D, 0x4A);
    static final Color MAU_NAU_NHAT   = new Color(0xB2, 0x90, 0x6B);
    static final Color MAU_NAU_TOI    = new Color(0x6B, 0x52, 0x3B);
    static final Color MAU_NEN_KEM    = new Color(0xFB, 0xF6, 0xEF);
    static final Color MAU_NEN_HEADER = new Color(0x3A, 0x30, 0x28);
    static final Color MAU_CHU_XAM    = new Color(0x8A, 0x87, 0x82);
    static final Color MAU_CHU_DAM    = new Color(0x2E, 0x2A, 0x26);
    static final Color MAU_VIEN       = new Color(0xEA, 0xDF, 0xCF);

    private static final String DA_THANH_TOAN = "Da thanh toan";

    private final InVeDAO dao;

    private final JTextField txtTimKiem = new JTextField(16);
    private final JCheckBox chkHomNay = new JCheckBox("Chỉ hôm nay", true);
    private final JLabel lblKetQua = new JLabel(" ");
    private final DefaultTableModel moHinhBang;
    private final JTable bang;

    private final JPanel panelDanhSachVe = new JPanel();
    private final JLabel lblThongTinHd = new JLabel("Chọn một hóa đơn bên trái để xem vé và bắp nước");
    private final JButton btnInTatCa = new JButton("In tất cả");
    private final JButton btnInDaChon = new JButton("In vé đã chọn");

    private List<HoaDonTomTat> cacHoaDon = new ArrayList<>();
    private List<VeIn> cacVe = new ArrayList<>();
    private List<ComboIn> cacCombo = new ArrayList<>();
    private final List<MucIn> cacMuc = new ArrayList<>();   // cac ve + phieu bap nuoc (neu co) hien trong phan xem truoc
    private HoaDonTomTat hdDangChon;
    private int veDangChon = -1;   // chi so muc dang duoc chon trong preview, -1 = chua chon

    // maHdChonSan: ma hoa don can mo san (vi du vua ban ve xong), null neu khong co
    public InVeKemBapNuocPanel(String maHdChonSan) {
        this(maHdChonSan, new InVeDAO());
    }

    InVeKemBapNuocPanel(String maHdChonSan, InVeDAO dao) {
        this.dao = dao;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        moHinhBang = new DefaultTableModel(
                new String[]{"Mã HĐ", "Phim", "Suất chiếu", "Phòng", "Số vé", "Bắp nước", "Tổng tiền", "Trạng thái", "Giờ lập"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bang = new JTable(moHinhBang);

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
        giua.add(taoPanelVe(), g);
        add(giua, BorderLayout.CENTER);

        capNhatNut();

        if (maHdChonSan != null && !maHdChonSan.isBlank()) {
            txtTimKiem.setText(maHdChonSan);
            chkHomNay.setSelected(false);
            timKiem();
            chonDongTheoMa(maHdChonSan);
        } else {
            timKiem();
        }
    }

    // ===== Thanh tren: tieu de + o tim kiem =====
    private JPanel taoThanhTren() {
        JPanel thanh = new JPanel(new BorderLayout());
        thanh.setOpaque(false);

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        phai.setOpaque(false);

        JLabel lblTim = new JLabel("Mã hóa đơn / tên phim:");
        lblTim.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTim.setForeground(MAU_CHU_XAM);

        txtTimKiem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtTimKiem.addActionListener(e -> timKiem());   // bam Enter cung tim

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

    // ===== Ben trai: bang danh sach hoa don =====
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

        // Tieu de bang: nen nau dam, chu trang
        DefaultTableCellRenderer dauCot = new DefaultTableCellRenderer();
        dauCot.setHorizontalAlignment(SwingConstants.CENTER);
        dauCot.setBackground(MAU_NEN_HEADER);
        dauCot.setForeground(Color.WHITE);
        dauCot.setFont(new Font("Segoe UI", Font.BOLD, 13));
        bang.getTableHeader().setDefaultRenderer(dauCot);
        bang.getTableHeader().setReorderingAllowed(false);
        bang.getTableHeader().setPreferredSize(new Dimension(0, 38));

        int[] doRong = {135, 120, 95, 65, 45, 70, 85, 115, 90};
        for (int i = 0; i < doRong.length; i++) {
            bang.getColumnModel().getColumn(i).setPreferredWidth(doRong[i]);
        }

        DefaultTableCellRenderer giuaO = new DefaultTableCellRenderer();
        giuaO.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i : new int[]{0, 2, 3, 4, 5, 6, 8}) {
            bang.getColumnModel().getColumn(i).setCellRenderer(giuaO);
        }
        bang.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
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
                    taiVe(cacHoaDon.get(dong));
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

    // ===== Ben phai: xem truoc cac ve + nut in =====
    private JPanel taoPanelVe() {
        JPanel the = new JPanel(new BorderLayout(0, 10));
        the.setBackground(Color.WHITE);
        the.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        the.setPreferredSize(new Dimension(420, 100));

        JPanel dau = new JPanel();
        dau.setOpaque(false);
        dau.setLayout(new BoxLayout(dau, BoxLayout.Y_AXIS));
        JLabel lblTieuDe = new JLabel("Xem trước vé & bắp nước");
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

        panelDanhSachVe.setLayout(new BoxLayout(panelDanhSachVe, BoxLayout.Y_AXIS));
        panelDanhSachVe.setBackground(MAU_NEN_KEM);
        panelDanhSachVe.setBorder(BorderFactory.createEmptyBorder(12, 8, 4, 8));
        JScrollPane cuon = new JScrollPane(panelDanhSachVe);
        cuon.setBorder(BorderFactory.createLineBorder(MAU_VIEN, 1));
        cuon.getVerticalScrollBar().setUnitIncrement(16);
        the.add(cuon, BorderLayout.CENTER);

        JPanel duoi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        duoi.setOpaque(false);

        btnInDaChon.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnInDaChon.setFocusPainted(false);
        btnInDaChon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnInDaChon.addActionListener(e -> {
            if (veDangChon >= 0 && veDangChon < cacMuc.size()) {
                inVe(List.of(cacMuc.get(veDangChon)));
            }
        });

        btnInTatCa.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnInTatCa.setForeground(Color.WHITE);
        btnInTatCa.setBackground(MAU_NAU_TOI);
        btnInTatCa.setFocusPainted(false);
        btnInTatCa.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnInTatCa.addActionListener(e -> inVe(new ArrayList<>(cacMuc)));

        duoi.add(btnInDaChon);
        duoi.add(btnInTatCa);
        the.add(duoi, BorderLayout.SOUTH);
        return the;
    }

    // ===== Xu ly du lieu =====
    private void timKiem() {
        try {
            cacHoaDon = dao.timHoaDon(txtTimKiem.getText(), chkHomNay.isSelected());
        } catch (SQLException e) {
            cacHoaDon = new ArrayList<>();
            JOptionPane.showMessageDialog(this, "Lỗi khi tải hóa đơn: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

        DateTimeFormatter dinhDangSuat = DateTimeFormatter.ofPattern("HH:mm dd/MM");
        DateTimeFormatter dinhDangLap = DateTimeFormatter.ofPattern("HH:mm dd/MM");

        moHinhBang.setRowCount(0);
        for (HoaDonTomTat hd : cacHoaDon) {
            moHinhBang.addRow(new Object[]{
                hd.maHd(),
                hd.tenPhim(),
                hd.ngayChieu().atTime(hd.gioBatDau()).format(dinhDangSuat),
                hd.tenPhong() == null ? "" : hd.tenPhong().replace("Phòng chiếu ", "Phòng "),
                hd.soVe(),
                hd.soCombo() > 0 ? hd.soCombo() + " combo" : "—",
                dinhDangTien(hd.tongTien()),
                nhanTrangThai(hd.trangThai()),
                hd.ngayLap() != null ? hd.ngayLap().format(dinhDangLap) : ""
            });
        }

        lblKetQua.setText(cacHoaDon.isEmpty()
                ? "Không có hóa đơn nào" + (chkHomNay.isSelected() ? " hôm nay (bỏ chọn \"Chỉ hôm nay\" để xem các ngày trước)." : ".")
                : "Tìm thấy " + cacHoaDon.size() + " hóa đơn. Bấm vào một dòng để xem vé và bắp nước.");

        cacVe = new ArrayList<>();
        cacCombo = new ArrayList<>();
        hdDangChon = null;
        hienThiVe();
        lblThongTinHd.setText("Chọn một hóa đơn bên trái để xem vé và bắp nước");
    }

    private void chonDongTheoMa(String maHd) {
        for (int i = 0; i < cacHoaDon.size(); i++) {
            if (cacHoaDon.get(i).maHd().equalsIgnoreCase(maHd.trim())) {
                bang.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    private void taiVe(HoaDonTomTat hd) {
        hdDangChon = hd;
        try {
            cacVe = dao.layVeTheoHoaDon(hd.maHd());
            cacCombo = hd.soCombo() > 0 ? dao.layComboTheoHoaDon(hd.maHd()) : new ArrayList<>();
        } catch (SQLException e) {
            cacVe = new ArrayList<>();
            cacCombo = new ArrayList<>();
            JOptionPane.showMessageDialog(this, "Lỗi khi tải vé: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

        // Xep cac muc xem truoc: tat ca ve, roi den phieu bap nuoc (neu hoa don co combo)
        cacMuc.clear();
        for (VeIn ve : cacVe) {
            cacMuc.add(new MucVe(ve));
        }
        if (!cacCombo.isEmpty() && !cacVe.isEmpty()) {
            VeIn dau = cacVe.get(0);
            cacMuc.add(new MucCombo(dau.maHd(), dau.ngayLap(), dau.tenNhanVien(), cacCombo));
        }
        hienThiVe();

        String tomTat = hd.maHd() + "  •  " + hd.soVe() + " vé"
                + (hd.soCombo() > 0 ? "  •  " + hd.soCombo() + " combo" : "")
                + "  •  " + dinhDangTien(hd.tongTien()) + "đ";
        if (cacVe.isEmpty()) {
            lblThongTinHd.setText("<html>" + tomTat + "<br><font color='#C62828'>Hóa đơn này không có vé nào.</font></html>");
        } else if (!DA_THANH_TOAN.equals(hd.trangThai())) {
            lblThongTinHd.setText("<html>" + tomTat + "<br><font color='#C62828'>Hóa đơn "
                    + nhanTrangThai(hd.trangThai()).toLowerCase() + " - không thể in.</font></html>");
        } else {
            lblThongTinHd.setText(tomTat + "  •  Bấm vào vé/phiếu để chọn in riêng");
        }
    }

    private void hienThiVe() {
        veDangChon = -1;
        panelDanhSachVe.removeAll();
        for (int i = 0; i < cacMuc.size(); i++) {
            TheMuc the = new TheMuc(cacMuc.get(i), i);
            the.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelDanhSachVe.add(the);
            panelDanhSachVe.add(Box.createVerticalStrut(KHOANG_VE));
        }
        panelDanhSachVe.revalidate();
        panelDanhSachVe.repaint();
        capNhatNut();
    }

    private void capNhatNut() {
        boolean duocIn = !cacMuc.isEmpty() && hdDangChon != null && DA_THANH_TOAN.equals(hdDangChon.trangThai());
        btnInTatCa.setEnabled(duocIn);
        btnInDaChon.setEnabled(duocIn && veDangChon >= 0);
    }

    // Bam vao 1 ve/phieu de chon (bam lan nua de bo chon)
    private void chonVe(int chiSo) {
        veDangChon = (veDangChon == chiSo) ? -1 : chiSo;
        panelDanhSachVe.repaint();
        capNhatNut();
    }

    // ===== In ra may in =====
    private void inVe(List<MucIn> danhSach) {
        if (danhSach.isEmpty()) {
            return;
        }
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("In ve " + (hdDangChon != null ? hdDangChon.maHd() : ""));
        job.setPrintable(new InVePrintable(danhSach));

        if (job.printDialog()) {   // hien hop thoai chon may in (co the chon "Microsoft Print to PDF")
            try {
                job.print();
                JOptionPane.showMessageDialog(this,
                        "Đã gửi lệnh in " + danhSach.size() + " phiếu (vé / bắp nước).",
                        "In vé", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi in: " + e.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 1 muc in/xem truoc: 1 ve hoac 1 phieu bap nuoc
    interface MucIn {
        int cao();

        void ve(Graphics2D g, int x, int y);
    }

    static final class MucVe implements MucIn {
        private final VeIn ve;

        MucVe(VeIn ve) {
            this.ve = ve;
        }

        @Override
        public int cao() {
            return VE_CAO;
        }

        @Override
        public void ve(Graphics2D g, int x, int y) {
            veTicket(g, ve, x, y);
        }
    }

    static final class MucCombo implements MucIn {
        private final String maHd;
        private final java.time.LocalDateTime ngayLap;
        private final String tenNhanVien;
        private final List<ComboIn> combo;

        MucCombo(String maHd, java.time.LocalDateTime ngayLap, String tenNhanVien, List<ComboIn> combo) {
            this.maHd = maHd;
            this.ngayLap = ngayLap;
            this.tenNhanVien = tenNhanVien;
            this.combo = combo;
        }

        @Override
        public int cao() {
            return PhieuBapNuoc.chieuCao(combo.size(), false, false);
        }

        @Override
        public void ve(Graphics2D g, int x, int y) {
            PhieuBapNuoc.ve(g, x, y, "PHIẾU BẮP NƯỚC", maHd, ngayLap, tenNhanVien, false, null, combo);
        }
    }

    // Xep nhieu muc (ve + phieu bap nuoc) len cac trang giay (muc nao khong vua trang nay thi xuong trang sau)
    static class InVePrintable implements Printable {
        private final List<MucIn> danhSach;

        InVePrintable(List<MucIn> danhSach) {
            this.danhSach = danhSach;
        }

        @Override
        public int print(Graphics g, PageFormat pf, int chiSoTrang) {
            double tyLe = Math.min(1.0, pf.getImageableWidth() / VE_RONG);   // thu nho neu giay hep hon ve
            double caoTrang = pf.getImageableHeight() / tyLe;                 // chieu cao kha dung (toa do logic)

            // chia trang
            List<List<MucIn>> cacTrang = new ArrayList<>();
            List<MucIn> hienTai = new ArrayList<>();
            double y = 0;
            for (MucIn m : danhSach) {
                if (!hienTai.isEmpty() && y + m.cao() > caoTrang) {
                    cacTrang.add(hienTai);
                    hienTai = new ArrayList<>();
                    y = 0;
                }
                hienTai.add(m);
                y += m.cao() + KHOANG_VE;
            }
            if (!hienTai.isEmpty()) {
                cacTrang.add(hienTai);
            }

            if (chiSoTrang >= cacTrang.size()) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.scale(tyLe, tyLe);

            int vi = 0;
            for (MucIn m : cacTrang.get(chiSoTrang)) {
                m.ve(g2, 0, vi);
                vi += m.cao() + KHOANG_VE;
            }
            g2.dispose();
            return PAGE_EXISTS;
        }
    }

    // ===== Thanh phan hien thi 1 muc (ve hoac phieu bap nuoc) trong phan xem truoc =====
    private class TheMuc extends JComponent {
        private final MucIn muc;
        private final int chiSo;

        TheMuc(MucIn muc, int chiSo) {
            this.muc = muc;
            this.chiSo = chiSo;
            Dimension d = new Dimension(VE_RONG, muc.cao());
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    chonVe(TheMuc.this.chiSo);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            muc.ve(g2, 0, 0);
            if (veDangChon == chiSo) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xE0, 0x9B, 0x2D));
                g2.setStroke(new BasicStroke(3f));
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, VE_RONG - 3, muc.cao() - 3, 16, 16));
            }
            g2.dispose();
        }
    }

    // ===== VE 1 CAI VE (dung chung cho xem truoc va in) =====
    // Ve gom phan chinh ben trai (phim, gio, phong, gia) va cuong ve ben phai (so ghe lon + ma vach)
    static void veTicket(Graphics2D g0, VeIn ve, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(x, y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = VE_RONG, h = VE_CAO;
        int cuong = 104;               // be rong phan cuong ve ben phai
        int chinh = w - cuong;         // be rong phan chinh ben trai
        int cao = 32;                  // chieu cao thanh tieu de

        Shape khung = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 16, 16);
        g.setColor(Color.WHITE);
        g.fill(khung);

        Shape clipCu = g.getClip();
        g.clip(khung);

        // Thanh tieu de
        g.setColor(MAU_NEN_HEADER);
        g.fillRect(0, 0, w, cao);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g.drawString("RẠP CHIẾU PHIM", 14, 21);
        g.setColor(MAU_NAU_NHAT);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        String nhan = "VÉ XEM PHIM";
        g.drawString(nhan, w - 14 - g.getFontMetrics().stringWidth(nhan), 21);

        // Nen cuong ve
        g.setColor(MAU_NEN_KEM);
        g.fillRect(chinh, cao, cuong, h - cao);
        g.setClip(clipCu);

        // Vien ngoai + duong dut giua ve va cuong
        g.setColor(MAU_NAU_NHAT);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(khung);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
        g.drawLine(chinh, cao, chinh, h - 6);

        // Ten phim
        g.setColor(MAU_CHU_DAM);
        g.setFont(new Font("Segoe UI", Font.BOLD, 17));
        g.drawString(catChu(ve.tenPhim(), g.getFontMetrics(), chinh - 28), 14, 58);

        // Cac dong thong tin
        DateTimeFormatter dinhDangGio = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dinhDangNgay = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dongThongTin(g, "Ngày chiếu", ve.ngayChieu().format(dinhDangNgay), 14, 80, false);
        dongThongTin(g, "Giờ chiếu", ve.gioBatDau().format(dinhDangGio) + " - " + ve.gioKetThuc().format(dinhDangGio), 14, 98, true);
        dongThongTin(g, "Phòng", ve.tenPhong(), 14, 116, false);
        dongThongTin(g, "Giá vé", dinhDangTien(ve.giaVe()) + "đ", 14, 134, true);

        // Chan ve (nho)
        g.setColor(MAU_CHU_XAM);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g.drawString("Mã HĐ: " + ve.maHd(), 14, 154);
        String nguoiLap = "Lập: " + (ve.ngayLap() != null ? ve.ngayLap().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "")
                + (ve.tenNhanVien() != null ? "  •  NV: " + ve.tenNhanVien() : "");
        g.drawString(catChu(nguoiLap, g.getFontMetrics(), chinh - 28), 14, 168);

        // Cuong ve: so ghe to + loai ghe + ma vach
        int giua = chinh + cuong / 2;
        g.setColor(MAU_CHU_XAM);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        veChuGiua(g, "GHẾ", giua, 56);

        g.setColor(MAU_NAU_TOI);
        g.setFont(new Font("Segoe UI", Font.BOLD, 34));
        veChuGiua(g, ve.hangGhe() + ve.soGhe(), giua, 94);

        g.setColor(MAU_NAU_DAM);
        g.setFont(new Font("Segoe UI", Font.BOLD, 12));
        veChuGiua(g, tenLoaiGhe(ve.loaiGhe()), giua, 114);

        veMaVach(g, ve.maHd() + ve.hangGhe() + ve.soGhe(), chinh + 14, 126, cuong - 28, 34);

        g.dispose();
    }

    private static void dongThongTin(Graphics2D g, String nhan, String giaTri, int x, int y, boolean dam) {
        g.setColor(MAU_CHU_XAM);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g.drawString(nhan + ":", x, y);
        g.setColor(MAU_CHU_DAM);
        g.setFont(new Font("Segoe UI", dam ? Font.BOLD : Font.PLAIN, 12));
        g.drawString(giaTri, x + 80, y);
    }

    static void veChuGiua(Graphics2D g, String chu, int giuaX, int y) {
        g.drawString(chu, giuaX - g.getFontMetrics().stringWidth(chu) / 2, y);
    }

    // Ma vach trang tri sinh tu chuoi (ma hoa don + ghe), khong phai ma vach chuan de quet
    private static void veMaVach(Graphics2D g, String hatGiong, int x, int y, int rong, int cao) {
        g.setColor(MAU_CHU_DAM);
        int vt = x;
        int i = 0;
        while (vt < x + rong) {
            int beRong = 1 + ((hatGiong.charAt(i % hatGiong.length()) + i * 7) % 3);
            beRong = Math.min(beRong, x + rong - vt);
            if (i % 2 == 0) {
                g.fillRect(vt, y, beRong, cao);
            }
            vt += beRong;
            i++;
        }
    }

    // Cat chu cho vua be rong, them "..." neu qua dai
    static String catChu(String chu, FontMetrics fm, int rongToiDa) {
        if (chu == null) return "";
        if (fm.stringWidth(chu) <= rongToiDa) return chu;
        String ba = "...";
        int n = chu.length();
        while (n > 1 && fm.stringWidth(chu.substring(0, n) + ba) > rongToiDa) {
            n--;
        }
        return chu.substring(0, n) + ba;
    }

    private static String tenLoaiGhe(String loai) {
        if ("VIP".equals(loai)) return "GHẾ VIP";
        if ("Doi".equals(loai)) return "GHẾ ĐÔI";
        return "GHẾ THƯỜNG";
    }

    private static String nhanTrangThai(String trangThai) {
        if ("Da thanh toan".equals(trangThai)) return "Đã thanh toán";
        if ("Da huy".equals(trangThai)) return "Đã hủy";
        if ("Da hoan tien".equals(trangThai)) return "Đã hoàn tiền";
        return trangThai == null ? "" : trangThai;
    }

    // 70000 -> "70.000"
    static String dinhDangTien(double so) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(so);
    }
}