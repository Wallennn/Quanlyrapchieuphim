package view;

import config.DBConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

/**
 * Hop thoai Them / Sua phong chieu.
 *  - Ma phong: tu dong theo CSDL (P + so lon nhat + 1), khong cho nhap tay.
 *  - Ten phong: tu goi y "Phong chieu N" theo ma (van sua duoc).
 *  - So hang, So cot: combo box (co the go so khac).
 */
public class PhongChieuDialog extends JDialog {

    private static final String PREFIX = "P";
    private static final int SO_CHU_SO = 2;               // P01, P02, ...
    private static final int HANG_MIN = 3, HANG_MAX = 20;
    private static final int COT_MIN = 4, COT_MAX = 30;

    private final String maCu;                            // null = them moi
    private boolean daLuu = false;
    private int hangCu = 0, cotCu = 0;

    private final JTextField txtMa = new JTextField();
    private final JTextField txtTen = new JTextField();
    private final JComboBox<Integer> cbHang = new JComboBox<>();
    private final JComboBox<Integer> cbCot = new JComboBox<>();

    /** @return true neu da luu. maCu = null de them moi. */
    public static boolean hien(Window owner, String maCu) {
        PhongChieuDialog d = new PhongChieuDialog(owner, maCu);
        d.setVisible(true);
        return d.daLuu;
    }

    private PhongChieuDialog(Window owner, String maCu) {
        super(owner, maCu == null ? "Thêm phòng chiếu" : "Sửa phòng chiếu", ModalityType.APPLICATION_MODAL);
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

    private void dungGiaoDien() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 4);
        g.fill = GridBagConstraints.HORIZONTAL;

        txtMa.setEditable(false);
        cbHang.setEditable(true);
        cbCot.setEditable(true);
        for (int i = HANG_MIN; i <= HANG_MAX; i++) cbHang.addItem(i);
        for (int i = COT_MIN; i <= COT_MAX; i++) cbCot.addItem(i);

        String[] nhan = {"Mã phòng", "Tên phòng", "Số hàng ghế", "Số cột ghế"};
        JComponent[] ctl = {txtMa, txtTen, cbHang, cbCot};
        for (int i = 0; i < nhan.length; i++) {
            g.gridy = i; g.gridx = 0; g.weightx = 0;
            form.add(new JLabel(nhan[i]), g);
            g.gridx = 1; g.weightx = 1;
            ctl[i].setPreferredSize(new Dimension(280, 30));
            form.add(ctl[i], g);
        }

        JButton ok = new JButton("OK"), huy = new JButton("Cancel");
        ok.addActionListener(e -> luu());
        huy.addActionListener(e -> dispose());
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        nut.add(ok); nut.add(huy);
        getRootPane().setDefaultButton(ok);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(nut, BorderLayout.SOUTH);
    }

    private void napDuLieu() throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            if (maCu == null) {
                int so = soTiepTheo(c);
                txtMa.setText(PREFIX + String.format("%0" + SO_CHU_SO + "d", so));
                txtTen.setText("Phòng chiếu " + so);
                cbHang.setSelectedItem(8);        // mac dinh giong cac phong hien co
                cbCot.setSelectedItem(10);
            } else {
                try (PreparedStatement ps = c.prepareStatement("SELECT * FROM phong_chieu WHERE ma_phong=?")) {
                    ps.setString(1, maCu);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Không tìm thấy phòng " + maCu);
                        txtMa.setText(maCu);
                        txtTen.setText(rs.getString("ten_phong"));
                        hangCu = rs.getInt("so_hang");
                        cotCu = rs.getInt("so_cot");
                        cbHang.setSelectedItem(hangCu);
                        cbCot.setSelectedItem(cotCu);
                    }
                }
            }
        }
    }

    /** So lon nhat trong ma phong hien co + 1. */
    private int soTiepTheo(Connection c) throws SQLException {
        int max = 0;
        try (PreparedStatement ps = c.prepareStatement("SELECT ma_phong FROM phong_chieu WHERE ma_phong LIKE ?")) {
            ps.setString(1, PREFIX + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try { max = Math.max(max, Integer.parseInt(rs.getString(1).substring(PREFIX.length()))); }
                    catch (NumberFormatException ignore) { }
                }
            }
        }
        return max + 1;
    }

    private static int so(JComboBox<Integer> cb) {
        int v = Integer.parseInt(String.valueOf(cb.getEditor().getItem()).trim());
        if (v <= 0) throw new NumberFormatException();
        return v;
    }

    private void luu() {
        String ten = txtTen.getText().trim();
        if (ten.isEmpty()) { loi("Vui lòng nhập tên phòng."); return; }
        int hang, cot;
        try { hang = so(cbHang); cot = so(cbCot); }
        catch (NumberFormatException e) { loi("Số hàng và số cột phải là số nguyên dương."); return; }

        if (maCu != null && (hang < hangCu || cot < cotCu)) {
            int xn = JOptionPane.showConfirmDialog(this,
                    "Bạn đang giảm kích thước phòng. Các ghế nằm ngoài sơ đồ mới sẽ không còn được dùng khi bán vé.\nTiếp tục?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (xn != JOptionPane.YES_OPTION) return;
        }

        try (Connection c = DBConnection.getConnection()) {
            if (maCu == null) {
                String ma = txtMa.getText();
                try (PreparedStatement ck = c.prepareStatement("SELECT 1 FROM phong_chieu WHERE ma_phong=?")) {
                    ck.setString(1, ma);
                    try (ResultSet rs = ck.executeQuery()) {
                        if (rs.next()) ma = PREFIX + String.format("%0" + SO_CHU_SO + "d", soTiepTheo(c));   // tranh trung ma
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO phong_chieu (ma_phong, ten_phong, so_hang, so_cot) VALUES (?,?,?,?)")) {
                    ps.setString(1, ma);
                    ps.setString(2, ten);
                    ps.setInt(3, hang);
                    ps.setInt(4, cot);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE phong_chieu SET ten_phong=?, so_hang=?, so_cot=? WHERE ma_phong=?")) {
                    ps.setString(1, ten);
                    ps.setInt(2, hang);
                    ps.setInt(3, cot);
                    ps.setString(4, maCu);
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

    private void loi(String m) { JOptionPane.showMessageDialog(this, m, "Lỗi", JOptionPane.WARNING_MESSAGE); }
}