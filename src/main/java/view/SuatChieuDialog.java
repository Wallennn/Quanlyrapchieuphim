package view;

import config.DBConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Hop thoai Them / Sua lich suat chieu.
 *  - Ma suat: tu dong theo CSDL (SUAT + so lon nhat + 1), khong cho nhap tay.
 *  - Ma phim, Ma phong, Ngay chieu, Gio bat dau, Gia ve: ComboBox.
 *  - Gio ket thuc: tu tinh = gio bat dau + thoi luong phim.
 *  - Khung gio da co suat chieu cua cung phong, cung ngay -> to xam, khong chon duoc.
 *
 */
public class SuatChieuDialog extends JDialog {

    // ===================== CAU HINH CSDL =====================
    private static final String T_SUAT = "suat_chieu";
    private static final String T_PHIM = "phim";
    private static final String T_PHONG = "phong_chieu";
    private static final String COT_TEN_PHIM = "ten_phim";       // cot ten phim
    private static final String COT_THOI_LUONG = "thoi_luong";   // phut
    private static final String COT_TEN_PHONG = "ten_phong";     // cot ten phong (neu khong co, de "ma_phong")
    private static final String PREFIX = "SUAT";
    // ==========================================================

    private static final int BUOC_PHUT = 5;                      // buoc khung gio
    private static final LocalTime GIO_MO = LocalTime.of(8, 0);
    private static final LocalTime GIO_DONG = LocalTime.of(23, 30);
    private static final int SO_NGAY = 60;                       // so ngay cho phep chon
    private static final int NGHI_GIUA_SUAT = 0;                 // phut don phong giua 2 suat

        private final String maSuatCu;     // null = them moi
    private boolean daLuu = false;

    private final JTextField txtMa = new JTextField();
    private final JComboBox<Item> cbPhim = new JComboBox<>();
    private final JComboBox<Item> cbPhong = new JComboBox<>();
    private final JComboBox<LocalDate> cbNgay = new JComboBox<>();
    private final JComboBox<LocalTime> cbBatDau = new JComboBox<>();
    private final JTextField txtKetThuc = new JTextField();
    private final JComboBox<Integer> cbThuong = new JComboBox<>();
    private final JComboBox<Integer> cbVip = new JComboBox<>();
    private final JComboBox<Integer> cbDoi = new JComboBox<>();

    private final List<int[]> khungDaCo = new ArrayList<>();     // [batDauPhut, ketThucPhut]
    private int thoiLuong = 120;
    private boolean dangNap = false;

    private static class Item {
        final String ma, nhan; final int thoiLuong;
        Item(String ma, String nhan, int tl) { this.ma = ma; this.nhan = nhan; this.thoiLuong = tl; }
        @Override public String toString() { return nhan; }
    }

    /** @return true neu da luu. maSuatCu = null de them moi. */
    public static boolean hien(Window owner, String maSuatCu) {
        SuatChieuDialog d = new SuatChieuDialog(owner, maSuatCu);
        d.setVisible(true);
        return d.daLuu;
    }

    private SuatChieuDialog(Window owner, String maSuatCu) {
        super(owner, maSuatCu == null ? "Thêm lịch suất chiếu" : "Sửa lịch suất chiếu", ModalityType.APPLICATION_MODAL);
        this.maSuatCu = maSuatCu;
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
        setMinimumSize(new Dimension(480, getHeight()));
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

        txtMa.setEditable(false);
        txtKetThuc.setEditable(false);
        for (JComboBox<Integer> c : java.util.Arrays.asList(cbThuong, cbVip, cbDoi)) c.setEditable(true);

        String[] nhan = {"Mã suất", "Mã phim", "Mã phòng", "Ngày chiếu", "Giờ bắt đầu", "Giờ kết thúc",
                "Giá vé thường", "Giá vé vip", "Giá vé đôi"};
        JComponent[] ctl = {txtMa, cbPhim, cbPhong, cbNgay, cbBatDau, txtKetThuc, cbThuong, cbVip, cbDoi};
        for (int i = 0; i < nhan.length; i++) {
            g.gridy = i; g.gridx = 0; g.weightx = 0;
            form.add(new JLabel(nhan[i]), g);
            g.gridx = 1; g.weightx = 1;
            ctl[i].setPreferredSize(new Dimension(300, 30));
            form.add(ctl[i], g);
        }

        // renderer to xam khung gio da co suat
        cbBatDau.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foc) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, v, i, sel, foc);
                if (v instanceof LocalTime) {
                    LocalTime t = (LocalTime) v;
                    c.setText(t.toString());
                    if (biTrung(t)) {
                        c.setText(t + "  (đã có suất)");
                        c.setForeground(Color.GRAY);
                        c.setBackground(new Color(0xDDDDDD));
                    }
                }
                return c;
            }
        });

        // khong cho chon khung gio da trung
        cbBatDau.addActionListener(e -> {
            if (dangNap) return;
            LocalTime t = (LocalTime) cbBatDau.getSelectedItem();
            if (t != null && biTrung(t)) {
                dangNap = true;
                chonKhungTrongDauTien();
                dangNap = false;
            }
            capNhatKetThuc();
        });
        cbPhim.addActionListener(e -> { if (!dangNap) { capNhatThoiLuong(); napKhungGio(); } });
        cbPhong.addActionListener(e -> { if (!dangNap) napKhungGio(); });
        cbNgay.addActionListener(e -> { if (!dangNap) napKhungGio(); });

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

    // --------------------------- NAP DU LIEU ---------------------------
    private void napDuLieu() throws SQLException {
        dangNap = true;
        try (Connection c = DBConnection.getConnection()) {
            try (Statement st = c.createStatement()) {
                // phim
                try (ResultSet rs = st.executeQuery("SELECT ma_phim, " + COT_TEN_PHIM + " AS ten, " + COT_THOI_LUONG
                        + " AS tl FROM " + T_PHIM + " ORDER BY ma_phim")) {
                    while (rs.next()) {
                        int tl = rs.getInt("tl");
                        cbPhim.addItem(new Item(rs.getString("ma_phim"),
                                rs.getString("ma_phim") + " - " + rs.getString("ten"), tl > 0 ? tl : 120));
                    }
                }
                // phong
                String cotPhong = COT_TEN_PHONG.equals("ma_phong") ? "ma_phong" : "ma_phong, " + COT_TEN_PHONG;
                try (ResultSet rs = st.executeQuery("SELECT " + cotPhong + " FROM " + T_PHONG + " ORDER BY ma_phong")) {
                    while (rs.next()) {
                        String ma = rs.getString("ma_phong");
                        String nhan = COT_TEN_PHONG.equals("ma_phong") ? ma : ma + " - " + rs.getString(COT_TEN_PHONG);
                        cbPhong.addItem(new Item(ma, nhan, 0));
                    }
                }
                // gia ve da dung (goi y)
                napGia(st, "gia_ve_thuong", cbThuong);
                napGia(st, "gia_ve_vip", cbVip);
                napGia(st, "gia_ve_doi", cbDoi);
            }

            // ngay
            LocalDate homNay = LocalDate.now();
            for (int i = 0; i < SO_NGAY; i++) cbNgay.addItem(homNay.plusDays(i));

            if (maSuatCu == null) {
                txtMa.setText(sinhMaMoi(c));
            } else {
                try (PreparedStatement ps = c.prepareStatement("SELECT * FROM " + T_SUAT + " WHERE ma_suat=?")) {
                    ps.setString(1, maSuatCu);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Không tìm thấy suất " + maSuatCu);
                        txtMa.setText(maSuatCu);
                        chonItem(cbPhim, rs.getString("ma_phim"));
                        chonItem(cbPhong, rs.getString("ma_phong"));
                        LocalDate ng = rs.getDate("ngay_chieu").toLocalDate();
                        if (((DefaultComboBoxModel<LocalDate>) cbNgay.getModel()).getIndexOf(ng) < 0) cbNgay.insertItemAt(ng, 0);
                        cbNgay.setSelectedItem(ng);
                        cbThuong.setSelectedItem(rs.getInt("gia_ve_thuong"));
                        cbVip.setSelectedItem(rs.getInt("gia_ve_vip"));
                        cbDoi.setSelectedItem(rs.getInt("gia_ve_doi"));
                        dangNap = false;
                        capNhatThoiLuong();
                        napKhungGio();
                        dangNap = true;
                        LocalTime bdCu = rs.getTime("gio_bat_dau").toLocalTime().withSecond(0);
                        int vt = 0;   // gio le (khong nam tren buoc 5 phut) van phai hien dung
                        while (vt < cbBatDau.getItemCount() && cbBatDau.getItemAt(vt).isBefore(bdCu)) vt++;
                        if (vt >= cbBatDau.getItemCount() || !cbBatDau.getItemAt(vt).equals(bdCu)) cbBatDau.insertItemAt(bdCu, vt);
                        cbBatDau.setSelectedItem(bdCu);
                    }
                }
            }
        } finally {
            dangNap = false;
        }
        if (maSuatCu == null) { capNhatThoiLuong(); napKhungGio(); }
        else capNhatKetThuc();
    }

    private void napGia(Statement st, String cot, JComboBox<Integer> cb) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT DISTINCT " + cot + " FROM " + T_SUAT + " ORDER BY " + cot)) {
            while (rs.next()) cb.addItem(rs.getInt(1));
        }
    }

    private static void chonItem(JComboBox<Item> cb, String ma) {
        for (int i = 0; i < cb.getItemCount(); i++)
            if (cb.getItemAt(i).ma.equals(ma)) { cb.setSelectedIndex(i); return; }
    }

    /** Ma moi = PREFIX + (so lon nhat trong CSDL + 1). */
    private String sinhMaMoi(Connection c) throws SQLException {
        int max = 0;
        try (PreparedStatement ps = c.prepareStatement("SELECT ma_suat FROM " + T_SUAT + " WHERE ma_suat LIKE ?")) {
            ps.setString(1, PREFIX + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String s = rs.getString(1).substring(PREFIX.length());
                    try { max = Math.max(max, Integer.parseInt(s)); } catch (NumberFormatException ignore) { }
                }
            }
        }
        return PREFIX + (max + 1);
    }

    // --------------------------- KHUNG GIO ---------------------------
    private void capNhatThoiLuong() {
        Item p = (Item) cbPhim.getSelectedItem();
        if (p != null) thoiLuong = p.thoiLuong;
    }

    /** Nap lai danh sach gio + danh sach suat da co cua phong/ngay dang chon. */
    private void napKhungGio() {
        Item phong = (Item) cbPhong.getSelectedItem();
        LocalDate ngay = (LocalDate) cbNgay.getSelectedItem();
        if (phong == null || ngay == null) return;

        khungDaCo.clear();
        String sql = "SELECT gio_bat_dau, gio_ket_thuc FROM " + T_SUAT
                + " WHERE ma_phong=? AND ngay_chieu=?" + (maSuatCu != null ? " AND ma_suat<>?" : "");
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phong.ma);
            ps.setDate(2, Date.valueOf(ngay));
            if (maSuatCu != null) ps.setString(3, maSuatCu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    khungDaCo.add(new int[]{phut(rs.getTime(1).toLocalTime()), phut(rs.getTime(2).toLocalTime())});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        LocalTime giu = (LocalTime) cbBatDau.getSelectedItem();
        dangNap = true;
        cbBatDau.removeAllItems();
        for (LocalTime t = GIO_MO; !t.isAfter(GIO_DONG); t = t.plusMinutes(BUOC_PHUT)) {
            cbBatDau.addItem(t);
        }
        if (giu != null && !biTrung(giu)) cbBatDau.setSelectedItem(giu);
        else chonKhungTrongDauTien();
        dangNap = false;
        capNhatKetThuc();
    }

    private void chonKhungTrongDauTien() {
        for (int i = 0; i < cbBatDau.getItemCount(); i++) {
            if (!biTrung(cbBatDau.getItemAt(i))) { cbBatDau.setSelectedIndex(i); return; }
        }
    }

    /** Suat [t, t+thoiLuong) co de len suat da co (cong them thoi gian don phong) khong? */
    private boolean biTrung(LocalTime t) {
        int b = phut(t), k = b + thoiLuong;
        for (int[] s : khungDaCo) {
            if (b < s[1] + NGHI_GIUA_SUAT && k + NGHI_GIUA_SUAT > s[0]) return true;
        }
        return false;
    }

    private void capNhatKetThuc() {
        LocalTime t = (LocalTime) cbBatDau.getSelectedItem();
        txtKetThuc.setText(t == null ? "" : t.plusMinutes(thoiLuong).withSecond(0).toString());
        cbBatDau.repaint();
    }

    private static int phut(LocalTime t) { return t.getHour() * 60 + t.getMinute(); }

    // --------------------------- LUU ---------------------------
    private void luu() {
        Item phim = (Item) cbPhim.getSelectedItem();
        Item phong = (Item) cbPhong.getSelectedItem();
        LocalDate ngay = (LocalDate) cbNgay.getSelectedItem();
        LocalTime bd = (LocalTime) cbBatDau.getSelectedItem();
        if (phim == null || phong == null || ngay == null || bd == null) {
            loi("Vui lòng chọn đầy đủ thông tin."); return;
        }
        if (bd.plusMinutes(thoiLuong).isBefore(bd)) { loi("Suất chiếu vượt quá nửa đêm."); return; }
        int gThuong, gVip, gDoi;
        try {
            gThuong = so(cbThuong); gVip = so(cbVip); gDoi = so(cbDoi);
        } catch (NumberFormatException e) { loi("Giá vé phải là số nguyên không âm."); return; }

        napKhungGio();   // lam moi du lieu moi nhat truoc khi kiem tra
        if (biTrung(bd)) { loi("Khung giờ này vừa có suất chiếu khác trong phòng. Vui lòng chọn giờ khác."); return; }

        LocalTime kt = bd.plusMinutes(thoiLuong);
        try (Connection c = DBConnection.getConnection()) {
            String ma = txtMa.getText();
            if (maSuatCu == null) {
                // tranh trung ma neu co nguoi khac them cung luc
                try (PreparedStatement ck = c.prepareStatement("SELECT 1 FROM " + T_SUAT + " WHERE ma_suat=?")) {
                    ck.setString(1, ma);
                    try (ResultSet rs = ck.executeQuery()) { if (rs.next()) ma = sinhMaMoi(c); }
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO " + T_SUAT
                        + " (ma_suat, ma_phim, ma_phong, ngay_chieu, gio_bat_dau, gio_ket_thuc, gia_ve_thuong, gia_ve_vip, gia_ve_doi)"
                        + " VALUES (?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, ma);
                    dat(ps, 2, phim, phong, ngay, bd, kt, gThuong, gVip, gDoi);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement("UPDATE " + T_SUAT
                        + " SET ma_phim=?, ma_phong=?, ngay_chieu=?, gio_bat_dau=?, gio_ket_thuc=?, gia_ve_thuong=?, gia_ve_vip=?, gia_ve_doi=?"
                        + " WHERE ma_suat=?")) {
                    dat(ps, 1, phim, phong, ngay, bd, kt, gThuong, gVip, gDoi);
                    ps.setString(9, maSuatCu);
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

    private static void dat(PreparedStatement ps, int i, Item phim, Item phong, LocalDate ngay,
                            LocalTime bd, LocalTime kt, int a, int b, int c) throws SQLException {
        ps.setString(i, phim.ma);
        ps.setString(i + 1, phong.ma);
        ps.setDate(i + 2, Date.valueOf(ngay));
        ps.setTime(i + 3, Time.valueOf(bd.withSecond(0)));
        ps.setTime(i + 4, Time.valueOf(kt.withSecond(0)));
        ps.setInt(i + 5, a);
        ps.setInt(i + 6, b);
        ps.setInt(i + 7, c);
    }

    private static int so(JComboBox<Integer> cb) {
        Object o = cb.getEditor().getItem();
        int v = Integer.parseInt(String.valueOf(o).trim());
        if (v < 0) throw new NumberFormatException();
        return v;
    }

    private void loi(String m) { JOptionPane.showMessageDialog(this, m, "Lỗi", JOptionPane.WARNING_MESSAGE); }
}