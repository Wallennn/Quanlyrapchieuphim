package view;

import config.DBConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Hop thoai Them / Sua phim.
 *  - Ma phim: tu dong theo CSDL (PHIM + so lon nhat + 1), khong cho nhap tay.
 *  - The loai: tick chon (lay tu cac the loai da co) + o nhap the loai khac.
 *  - Thoi luong: o so (phut). Do tuoi, Trang thai: combo box.
 *  - Ngay khoi chieu, Dao dien, Poster: combo box.
 */
public class PhimDialog extends JDialog {

    private static final String PREFIX = "PHIM";
    private static final int SO_CHU_SO = 2;            // PHIM01, PHIM02, ...
    private static final String[] DO_TUOI = {"P", "K", "T13", "T16", "T18"};
    private static final String[] TRANG_THAI = {"Dang chieu", "Sap chieu", "Ngung chieu"};

    private final String maCu;                         // null = them moi
    private boolean daLuu = false;

    private final JTextField txtMa = new JTextField();
    private final JTextField txtTen = new JTextField();
    private final JPanel pnlTheLoai = new JPanel(new WrapLayout());
    private final List<JCheckBox> cacTheLoai = new ArrayList<>();
    private final JTextField txtTheLoaiKhac = new JTextField();
    private final JSpinner spThoiLuong = new JSpinner(new SpinnerNumberModel(120, 1, 600, 5));
    private final JComboBox<String> cbDoTuoi = new JComboBox<>(DO_TUOI);
    private final JComboBox<LocalDate> cbNgay = new JComboBox<>();
    private final JComboBox<String> cbDaoDien = new JComboBox<>();
    private final JTextArea txtMoTa = new JTextArea(3, 20);
    private final JComboBox<String> cbPoster = new JComboBox<>();
    private final JComboBox<String> cbTrangThai = new JComboBox<>(TRANG_THAI);

    /** @return true neu da luu. maCu = null de them moi. */
    public static boolean hien(Window owner, String maCu) {
        PhimDialog d = new PhimDialog(owner, maCu);
        d.setVisible(true);
        return d.daLuu;
    }

    private PhimDialog(Window owner, String maCu) {
        super(owner, maCu == null ? "Thêm phim" : "Sửa phim", ModalityType.APPLICATION_MODAL);
        this.maCu = maCu;
        try {
            dungGiaoDien();
            napDuLieu();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(owner, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    // --------------------------- GIAO DIEN ---------------------------
    private void dungGiaoDien() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;

        txtMa.setEditable(false);
        cbDaoDien.setEditable(true);
        cbPoster.setEditable(true);
        txtMoTa.setLineWrap(true);
        txtMoTa.setWrapStyleWord(true);
        txtTheLoaiKhac.setToolTipText("Thể loại khác, cách nhau bằng dấu phẩy");

        cbNgay.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, v, i, s, f);
                if (v == null) c.setText("— chưa có —");
                return c;
            }
        });

        JPanel theLoai = new JPanel(new BorderLayout(0, 4));
        JScrollPane cuonTL = new JScrollPane(pnlTheLoai);
        cuonTL.setPreferredSize(new Dimension(300, 80));
        theLoai.add(cuonTL, BorderLayout.CENTER);
        JPanel khac = new JPanel(new BorderLayout(6, 0));
        khac.add(new JLabel("Khác:"), BorderLayout.WEST);
        khac.add(txtTheLoaiKhac, BorderLayout.CENTER);
        theLoai.add(khac, BorderLayout.SOUTH);

        JScrollPane cuonMoTa = new JScrollPane(txtMoTa);
        cuonMoTa.setPreferredSize(new Dimension(300, 70));

        String[] nhan = {"Mã phim", "Tên phim", "Thể loại", "Thời lượng (phút)", "Độ tuổi",
                "Ngày khởi chiếu", "Đạo diễn", "Mô tả", "Poster", "Trạng thái"};
        JComponent[] ctl = {txtMa, txtTen, theLoai, spThoiLuong, cbDoTuoi,
                cbNgay, cbDaoDien, cuonMoTa, cbPoster, cbTrangThai};
        for (int i = 0; i < nhan.length; i++) {
            g.gridy = i; g.gridx = 0; g.weightx = 0;
            form.add(new JLabel(nhan[i]), g);
            g.gridx = 1; g.weightx = 1;
            if (ctl[i] == theLoai || ctl[i] == cuonMoTa) { /* giu chieu cao rieng */ }
            else ctl[i].setPreferredSize(new Dimension(300, 30));
            form.add(ctl[i], g);
        }

        JButton ok = new JButton("OK"), huy = new JButton("Cancel");
        ok.addActionListener(e -> luu());
        huy.addActionListener(e -> dispose());
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        nut.add(ok); nut.add(huy);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(nut, BorderLayout.SOUTH);
    }

    // --------------------------- NAP DU LIEU ---------------------------
    private void napDuLieu() throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            Set<String> theLoai = new LinkedHashSet<>();
            Set<String> daoDien = new LinkedHashSet<>();
            Set<String> poster = new LinkedHashSet<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT the_loai, dao_dien, poster FROM phim ORDER BY ma_phim")) {
                while (rs.next()) {
                    String tl = rs.getString(1);
                    if (tl != null) for (String s : tl.split(",")) if (!s.trim().isEmpty()) theLoai.add(s.trim());
                    String dd = rs.getString(2);
                    if (dd != null && !dd.trim().isEmpty()) daoDien.add(dd.trim());
                    String p = rs.getString(3);
                    if (p != null && !p.trim().isEmpty()) poster.add(p.trim());
                }
            }
            for (String s : theLoai) {
                JCheckBox cb = new JCheckBox(s);
                cacTheLoai.add(cb);
                pnlTheLoai.add(cb);
            }
            for (String s : daoDien) cbDaoDien.addItem(s);
            for (String s : poster) cbPoster.addItem(s);

            LocalDate homNay = LocalDate.now();
            cbNgay.addItem(null);
            for (int i = -60; i <= 180; i++) cbNgay.addItem(homNay.plusDays(i));
            cbNgay.setSelectedItem(homNay);

            if (maCu == null) {
                String ma = sinhMaMoi(c);
                txtMa.setText(ma);
                cbPoster.setSelectedItem(ma.toLowerCase() + ".png");   // goi y ten file poster
                cbDaoDien.setSelectedItem(null);
                cbTrangThai.setSelectedItem("Sap chieu");
            } else {
                try (PreparedStatement ps = c.prepareStatement("SELECT * FROM phim WHERE ma_phim=?")) {
                    ps.setString(1, maCu);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Không tìm thấy phim " + maCu);
                        txtMa.setText(maCu);
                        txtTen.setText(rs.getString("ten_phim"));
                        chonTheLoai(rs.getString("the_loai"));
                        spThoiLuong.setValue(rs.getInt("thoi_luong"));
                        cbDoTuoi.setSelectedItem(rs.getString("do_tuoi"));
                        Date nk = rs.getDate("ngay_khoi_chieu");
                        if (nk == null) cbNgay.setSelectedItem(null);
                        else {
                            LocalDate d = nk.toLocalDate();
                            if (((DefaultComboBoxModel<LocalDate>) cbNgay.getModel()).getIndexOf(d) < 0) cbNgay.insertItemAt(d, 1);
                            cbNgay.setSelectedItem(d);
                        }
                        cbDaoDien.setSelectedItem(rs.getString("dao_dien"));
                        txtMoTa.setText(rs.getString("mo_ta"));
                        cbPoster.setSelectedItem(rs.getString("poster"));
                        cbTrangThai.setSelectedItem(rs.getString("trang_thai"));
                    }
                }
            }
        }
    }

    private void chonTheLoai(String chuoi) {
        List<String> khac = new ArrayList<>();
        if (chuoi != null) {
            for (String s : chuoi.split(",")) {
                s = s.trim();
                if (s.isEmpty()) continue;
                boolean thay = false;
                for (JCheckBox cb : cacTheLoai) if (cb.getText().equalsIgnoreCase(s)) { cb.setSelected(true); thay = true; }
                if (!thay) khac.add(s);
            }
        }
        txtTheLoaiKhac.setText(String.join(", ", khac));
    }

    /** Ma moi = PREFIX + (so lon nhat trong CSDL + 1), dem du SO_CHU_SO chu so. */
    private String sinhMaMoi(Connection c) throws SQLException {
        int max = 0;
        try (PreparedStatement ps = c.prepareStatement("SELECT ma_phim FROM phim WHERE ma_phim LIKE ?")) {
            ps.setString(1, PREFIX + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try { max = Math.max(max, Integer.parseInt(rs.getString(1).substring(PREFIX.length()))); }
                    catch (NumberFormatException ignore) { }
                }
            }
        }
        return PREFIX + String.format("%0" + SO_CHU_SO + "d", max + 1);
    }

    // --------------------------- LUU ---------------------------
    private static String chuoi(JComboBox<String> cb) {
        Object o = cb.isEditable() ? cb.getEditor().getItem() : cb.getSelectedItem();
        String s = o == null ? "" : o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private void luu() {
        String ten = txtTen.getText().trim();
        if (ten.isEmpty()) { loi("Vui lòng nhập tên phim."); return; }

        Set<String> tl = new LinkedHashSet<>();
        for (JCheckBox cb : cacTheLoai) if (cb.isSelected()) tl.add(cb.getText());
        for (String s : txtTheLoaiKhac.getText().split(",")) if (!s.trim().isEmpty()) tl.add(s.trim());
        String theLoai = tl.isEmpty() ? null : String.join(", ", tl);

        try { spThoiLuong.commitEdit(); } catch (java.text.ParseException e) { loi("Thời lượng không hợp lệ."); return; }
        int thoiLuong = ((Number) spThoiLuong.getValue()).intValue();
        if (thoiLuong <= 0) { loi("Thời lượng phải lớn hơn 0."); return; }

        LocalDate ngay = (LocalDate) cbNgay.getSelectedItem();
        String moTa = txtMoTa.getText().trim();

        try (Connection c = DBConnection.getConnection()) {
            if (maCu == null) {
                String ma = txtMa.getText();
                try (PreparedStatement ck = c.prepareStatement("SELECT 1 FROM phim WHERE ma_phim=?")) {
                    ck.setString(1, ma);
                    try (ResultSet rs = ck.executeQuery()) { if (rs.next()) ma = sinhMaMoi(c); }   // tranh trung ma
                }
                String sql = "INSERT INTO phim (ma_phim, ten_phim, the_loai, thoi_luong, do_tuoi, ngay_khoi_chieu, dao_dien, mo_ta, poster, trang_thai)"
                        + " VALUES (?,?,?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, ma);
                    dat(ps, 2, ten, theLoai, thoiLuong, ngay, moTa);
                    ps.executeUpdate();
                }
            } else {
                String sql = "UPDATE phim SET ten_phim=?, the_loai=?, thoi_luong=?, do_tuoi=?, ngay_khoi_chieu=?, dao_dien=?, mo_ta=?, poster=?, trang_thai=? WHERE ma_phim=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    dat(ps, 1, ten, theLoai, thoiLuong, ngay, moTa);
                    ps.setString(10, maCu);
                    ps.executeUpdate();
                }
            }
            daLuu = true;
            dispose();
        } catch (SQLException e) {
            e.printStackTrace();
            loi("Lỗi lưu dữ liệu: " + e.getMessage());
        }
    }

    /** Gan 9 tham so: ten, the loai, thoi luong, do tuoi, ngay, dao dien, mo ta, poster, trang thai (bat dau tu vi tri i). */
    private void dat(PreparedStatement ps, int i, String ten, String theLoai, int thoiLuong, LocalDate ngay, String moTa) throws SQLException {
        ps.setString(i, ten);
        ps.setString(i + 1, theLoai);
        ps.setInt(i + 2, thoiLuong);
        ps.setString(i + 3, (String) cbDoTuoi.getSelectedItem());
        if (ngay == null) ps.setNull(i + 4, Types.DATE); else ps.setDate(i + 4, Date.valueOf(ngay));
        ps.setString(i + 5, chuoi(cbDaoDien));
        ps.setString(i + 6, moTa.isEmpty() ? null : moTa);
        ps.setString(i + 7, chuoi(cbPoster));
        ps.setString(i + 8, (String) cbTrangThai.getSelectedItem());
    }

    private void loi(String m) { JOptionPane.showMessageDialog(this, m, "Lỗi", JOptionPane.WARNING_MESSAGE); }

    /** FlowLayout tu xuong dong de cac checkbox the loai khong bi tran ngang. */
    private static class WrapLayout extends FlowLayout {
        WrapLayout() { super(FlowLayout.LEFT, 6, 2); }
        @Override public Dimension preferredLayoutSize(Container t) { return tinh(t); }
        @Override public Dimension minimumLayoutSize(Container t) { return tinh(t); }
        private Dimension tinh(Container t) {
            synchronized (t.getTreeLock()) {
                int rong = t.getParent() != null ? t.getParent().getWidth() : 0;
                if (rong <= 0) rong = 300;
                Insets in = t.getInsets();
                int toiDa = rong - in.left - in.right - getHgap() * 2;
                int x = 0, y = in.top + getVgap(), hangCao = 0, wMax = 0;
                for (Component c : t.getComponents()) {
                    if (!c.isVisible()) continue;
                    Dimension d = c.getPreferredSize();
                    if (x > 0 && x + d.width > toiDa) { y += hangCao + getVgap(); x = 0; hangCao = 0; }
                    x += d.width + getHgap();
                    wMax = Math.max(wMax, x);
                    hangCao = Math.max(hangCao, d.height);
                }
                return new Dimension(wMax, y + hangCao + getVgap() + in.bottom);
            }
        }
    }
}