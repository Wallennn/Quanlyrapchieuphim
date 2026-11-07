package view;

import config.DBConnection;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Man hinh quan ly DUNG CHUNG cho 1 bang trong database: xem, tim kiem, them, sua, xoa.
 * Tu doc cau truc bang (ten cot, khoa chinh, cot tu tang) nen khong can biet truoc cac cot.
 * Dung cho: tai_khoan (nhan vien), phim, phong_chieu, suat_chieu.
 */
public class BangDuLieuPanel extends JPanel {

    private static final String FONT = "Segoe UI";
    private static final Color MAU_NEN_HEADER = new Color(0x3A, 0x30, 0x28);
    private static final Color MAU_CHU_XAM    = new Color(0x8A, 0x87, 0x82);
    private static final Color MAU_CHU_DAM    = new Color(0x2E, 0x2A, 0x26);
    private static final Color MAU_VIEN       = new Color(0xEA, 0xDF, 0xCF);
    private static final Color MAU_DO         = new Color(0xC6, 0x28, 0x28);

    /** Thong tin 1 cot cua bang. */
    private static class Cot {
        String ten, nhan;
        boolean tuTang, choNull, khoaChinh, an;
    }

    private static class DuLieu {
        final List<Cot> cot = new ArrayList<>();
        final List<String[]> dong = new ArrayList<>();
    }

    private final String tenBang, tieuDe, orderBy, ghiChu;
    private final boolean choThem, choSua, choXoa;

    private List<Cot> cacCot = new ArrayList<>();
    private List<String[]> cacDong = new ArrayList<>();
    private final List<Integer> cotHienThi = new ArrayList<>();   // vi tri cot (trong cacCot) dang hien tren bang

    private final DefaultTableModel model = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable bang = new JTable(model);
    private final JTextField txtTim = new JTextField(16);
    private final JLabel lblThongBao = new JLabel(" ");
    private final JButton btnThem = nut("+ Thêm");
    private final JButton btnSua = nut("Sửa");
    private final JButton btnXoa = nut("Xóa");
    private final JButton btnLamMoi = nut("Làm mới");

    /**
     * @param tenBang  ten bang trong database (vi du "phim")
     * @param orderBy  menh de ORDER BY (vi du "ma_phim"), co the null
     * @param ghiChu   dong chu nho hien o duoi bang, co the null
     */
    public BangDuLieuPanel(String tenBang, String tieuDe, String orderBy,
                           boolean choThem, boolean choSua, boolean choXoa, String ghiChu) {
        this.tenBang = tenBang;
        this.tieuDe = tieuDe;
        this.orderBy = orderBy;
        this.choThem = choThem;
        this.choSua = choSua;
        this.choXoa = choXoa;
        this.ghiChu = ghiChu;

        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        add(taoThanhTren(), BorderLayout.NORTH);
        add(taoBang(), BorderLayout.CENTER);

        lblThongBao.setFont(new Font(FONT, Font.PLAIN, 12));
        lblThongBao.setForeground(MAU_CHU_XAM);
        add(lblThongBao, BorderLayout.SOUTH);

        btnThem.setVisible(choThem);
        btnSua.setVisible(choSua);
        btnXoa.setVisible(choXoa);
        btnThem.addActionListener(e -> moFormThem());
        btnSua.addActionListener(e -> moFormSua());
        btnXoa.addActionListener(e -> xoaDong());
        btnLamMoi.addActionListener(e -> taiDuLieu());
        bang.getSelectionModel().addListSelectionListener(e -> capNhatNut());
        capNhatNut();
        taiDuLieu();
    }

    // ===================== GIAO DIEN =====================
    private JPanel taoThanhTren() {
        JPanel thanh = new JPanel(new BorderLayout());
        thanh.setOpaque(false);
        JLabel lbl = new JLabel(tieuDe);
        lbl.setFont(new Font(FONT, Font.BOLD, 24));
        lbl.setForeground(MAU_CHU_DAM);
        thanh.add(lbl, BorderLayout.WEST);

        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        phai.setOpaque(false);
        JLabel lblTim = new JLabel("Tìm:");
        lblTim.setFont(new Font(FONT, Font.PLAIN, 13));
        lblTim.setForeground(MAU_CHU_XAM);
        txtTim.setFont(new Font(FONT, Font.PLAIN, 13));
        txtTim.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { loc(); }
            @Override public void removeUpdate(DocumentEvent e) { loc(); }
            @Override public void changedUpdate(DocumentEvent e) { loc(); }
        });
        phai.add(lblTim);
        phai.add(txtTim);
        phai.add(btnThem);
        phai.add(btnSua);
        phai.add(btnXoa);
        phai.add(btnLamMoi);
        thanh.add(phai, BorderLayout.EAST);
        return thanh;
    }

    private JButton nut(String chu) {
        JButton b = new JButton(chu);
        b.setFont(new Font(FONT, Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setBackground(Color.WHITE);
        b.setForeground(MAU_CHU_DAM);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private JScrollPane taoBang() {
        bang.setRowHeight(34);
        bang.setFont(new Font(FONT, Font.PLAIN, 13));
        bang.setShowVerticalLines(false);
        bang.setGridColor(new Color(0xF0, 0xE9, 0xDD));
        bang.setFillsViewportHeight(true);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.setSelectionBackground(new Color(0xF3, 0xE6, 0xD3));
        bang.setSelectionForeground(MAU_CHU_DAM);
        bang.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

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
        bang.getTableHeader().setPreferredSize(new Dimension(0, 38));
        bang.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && choSua) moFormSua();
            }
        });

        JScrollPane cuon = new JScrollPane(bang);
        cuon.setBorder(BorderFactory.createLineBorder(MAU_VIEN, 1));
        cuon.getViewport().setBackground(Color.WHITE);
        return cuon;
    }

    private void loc() {
        if (bang.getRowSorter() instanceof TableRowSorter) {
            @SuppressWarnings("unchecked")
            TableRowSorter<DefaultTableModel> s = (TableRowSorter<DefaultTableModel>) bang.getRowSorter();
            String t = txtTim.getText().trim();
            s.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(t)));
        }
        capNhatThongBao();
    }

    private void capNhatNut() {
        boolean coDong = bang.getSelectedRow() >= 0;
        boolean coKhoa = cacCot.stream().anyMatch(c -> c.khoaChinh);
        btnSua.setEnabled(coDong && coKhoa);
        btnXoa.setEnabled(coDong && coKhoa);
    }

    private void capNhatThongBao() {
        String s = bang.getRowCount() + " / " + cacDong.size() + " dòng";
        if (ghiChu != null && !ghiChu.isEmpty()) s += "   •   " + ghiChu;
        lblThongBao.setForeground(MAU_CHU_XAM);
        lblThongBao.setText(s);
    }

    private void baoLoi(String s) {
        lblThongBao.setForeground(MAU_DO);
        lblThongBao.setText(s);
    }

    // ===================== DOC DU LIEU =====================
    private static boolean laCotNhayCam(String ten) {
        String t = ten.toLowerCase();
        return t.contains("mat_khau") || t.contains("password") || t.contains("hash");
    }

    private static String nhanDep(String ten) {
        String s = ten.replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private DuLieu docDuLieu() throws SQLException {
        DuLieu d = new DuLieu();
        try (Connection c = DBConnection.getConnection()) {
            Set<String> khoa = new HashSet<>();
            DatabaseMetaData dm = c.getMetaData();
            try (ResultSet rs = dm.getPrimaryKeys(c.getCatalog(), null, tenBang)) {
                while (rs.next()) khoa.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
            String sql = "SELECT * FROM `" + tenBang + "`" + (orderBy != null ? " ORDER BY " + orderBy : "") + " LIMIT 2000";
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                boolean[] laThoiGian = new boolean[n];
                for (int i = 1; i <= n; i++) {
                    Cot ct = new Cot();
                    ct.ten = md.getColumnName(i);
                    ct.nhan = nhanDep(ct.ten);
                    ct.tuTang = md.isAutoIncrement(i);
                    ct.choNull = md.isNullable(i) != ResultSetMetaData.columnNoNulls;
                    ct.khoaChinh = khoa.contains(ct.ten.toLowerCase());
                    ct.an = laCotNhayCam(ct.ten);
                    d.cot.add(ct);
                    laThoiGian[i - 1] = md.getColumnType(i) == Types.TIMESTAMP;
                }
                while (rs.next()) {
                    String[] r = new String[n];
                    for (int i = 1; i <= n; i++) {
                        String v = rs.getString(i);
                        if (v != null && laThoiGian[i - 1] && v.endsWith(".0")) v = v.substring(0, v.length() - 2);
                        r[i - 1] = v;
                    }
                    d.dong.add(r);
                }
            }
        }
        return d;
    }

    private void taiDuLieu() {
        chayNen(this::docDuLieu, d -> {
            cacCot = d.cot;
            cacDong = d.dong;
            cotHienThi.clear();
            Vector<String> tieuDeCot = new Vector<>();
            for (int i = 0; i < cacCot.size(); i++) {
                if (!cacCot.get(i).an) {
                    cotHienThi.add(i);
                    tieuDeCot.add(cacCot.get(i).nhan);
                }
            }
            model.setRowCount(0);
            model.setColumnIdentifiers(tieuDeCot);
            for (String[] r : cacDong) {
                Object[] hang = new Object[cotHienThi.size()];
                for (int k = 0; k < cotHienThi.size(); k++) hang[k] = r[cotHienThi.get(k)] == null ? "" : r[cotHienThi.get(k)];
                model.addRow(hang);
            }
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
            bang.setRowSorter(sorter);
            for (int k = 0; k < bang.getColumnCount(); k++) {
                int rong = Math.max(90, Math.min(260, bang.getColumnModel().getColumn(k).getHeaderValue().toString().length() * 11 + 30));
                for (int r = 0; r < Math.min(model.getRowCount(), 60); r++) {
                    Object v = model.getValueAt(r, k);
                    if (v != null) rong = Math.max(rong, Math.min(300, v.toString().length() * 8 + 28));
                }
                bang.getColumnModel().getColumn(k).setPreferredWidth(rong);
            }
            loc();
            capNhatNut();
            if (choSua || choXoa) {
                if (cacCot.stream().noneMatch(c -> c.khoaChinh)) baoLoi("Bảng này không có khóa chính nên không sửa/xóa được.");
            }
        });
    }

    // ===================== THEM / SUA / XOA =====================
    private int dongDangChon() {
        int v = bang.getSelectedRow();
        return v < 0 ? -1 : bang.convertRowIndexToModel(v);
    }

    private void moFormThem() {
        if ("phong_chieu".equals(tenBang)) {
            if (PhongChieuDialog.hien(SwingUtilities.getWindowAncestor(this), null)) {
                taiDuLieu();
                lblThongBao.setForeground(new Color(0x2E, 0x7D, 0x32));
                lblThongBao.setText("Đã thêm.");
            }
            return;
        }
        if ("phim".equals(tenBang)) {
            if (PhimDialog.hien(SwingUtilities.getWindowAncestor(this), null)) {
                taiDuLieu();
                lblThongBao.setForeground(new Color(0x2E, 0x7D, 0x32));
                lblThongBao.setText("Đã thêm.");
            }
            return;
        }
        if ("suat_chieu".equals(tenBang)) {
            if (SuatChieuDialog.hien(SwingUtilities.getWindowAncestor(this), null)) {
                taiDuLieu();
                lblThongBao.setForeground(new Color(0x2E, 0x7D, 0x32));
                lblThongBao.setText("Đã thêm.");
            }
            return;
        }
        Map<Cot, JTextField> o = new LinkedHashMap<>();
        JPanel form = taoForm(o, null);
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(form), "Thêm " + tieuDe.toLowerCase(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        List<String> ten = new ArrayList<>();
        List<String> giaTri = new ArrayList<>();
        for (Map.Entry<Cot, JTextField> e : o.entrySet()) {
            String t = e.getValue().getText().trim();
            if (!t.isEmpty()) {
                ten.add(e.getKey().ten);
                giaTri.add(t);
            }
        }
        if (ten.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa nhập thông tin nào.");
            return;
        }
        StringBuilder sql = new StringBuilder("INSERT INTO `" + tenBang + "` (");
        StringBuilder dau = new StringBuilder();
        for (int i = 0; i < ten.size(); i++) {
            sql.append(i > 0 ? ", " : "").append('`').append(ten.get(i)).append('`');
            dau.append(i > 0 ? ", ?" : "?");
        }
        sql.append(") VALUES (").append(dau).append(')');
        thucThi(sql.toString(), giaTri, "Đã thêm.");
    }

    private void moFormSua() {
        int i = dongDangChon();
        if (i < 0) return;
        String[] goc = cacDong.get(i);
        if ("phong_chieu".equals(tenBang)) {
            for (int k = 0; k < cacCot.size(); k++) {
                if (!"ma_phong".equals(cacCot.get(k).ten)) continue;
                if (PhongChieuDialog.hien(SwingUtilities.getWindowAncestor(this), goc[k])) {
                    taiDuLieu();
                    lblThongBao.setForeground(new Color(0x2E, 0x7D, 0x32));
                    lblThongBao.setText("Đã cập nhật.");
                }
                return;
            }
        }
        if ("phim".equals(tenBang)) {
            for (int k = 0; k < cacCot.size(); k++) {
                if (!"ma_phim".equals(cacCot.get(k).ten)) continue;
                if (PhimDialog.hien(SwingUtilities.getWindowAncestor(this), goc[k])) {
                    taiDuLieu();
                    lblThongBao.setForeground(new Color(0x2E, 0x7D, 0x32));
                    lblThongBao.setText("Đã cập nhật.");
                }
                return;
            }
        }
        if ("suat_chieu".equals(tenBang)) {
            for (int k = 0; k < cacCot.size(); k++) {
                if (!"ma_suat".equals(cacCot.get(k).ten)) continue;
                if (SuatChieuDialog.hien(SwingUtilities.getWindowAncestor(this), goc[k])) {
                    taiDuLieu();
                    lblThongBao.setForeground(new Color(0x2E, 0x7D, 0x32));
                    lblThongBao.setText("Đã cập nhật.");
                }
                return;
            }
        }
        Map<Cot, JTextField> o = new LinkedHashMap<>();
        JPanel form = taoForm(o, goc);
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(form), "Sửa " + tieuDe.toLowerCase(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        List<String> giaTri = new ArrayList<>();
        StringBuilder set = new StringBuilder();
        for (Map.Entry<Cot, JTextField> e : o.entrySet()) {
            Cot c = e.getKey();
            if (c.khoaChinh || c.tuTang) continue;
            String t = e.getValue().getText().trim();
            if (t.isEmpty() && !c.choNull) continue;           // cot bat buoc ma de trong: giu nguyen
            set.append(set.length() > 0 ? ", " : "").append('`').append(c.ten).append("` = ?");
            giaTri.add(t.isEmpty() ? null : t);
        }
        if (set.length() == 0) return;
        String where = menhDeWhere(goc, giaTri);
        if (where == null) return;
        thucThi("UPDATE `" + tenBang + "` SET " + set + " WHERE " + where, giaTri, "Đã cập nhật.");
    }

    private void xoaDong() {
        int i = dongDangChon();
        if (i < 0) return;
        String[] goc = cacDong.get(i);
        List<String> giaTri = new ArrayList<>();
        String where = menhDeWhere(goc, giaTri);
        if (where == null) return;
        if (JOptionPane.showConfirmDialog(this, "Xóa dòng này? Thao tác không hoàn tác được.",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        thucThi("DELETE FROM `" + tenBang + "` WHERE " + where, giaTri, "Đã xóa.");
    }

    /** WHERE theo khoa chinh cua dong goc; bo sung gia tri vao danh sach tham so. */
    private String menhDeWhere(String[] goc, List<String> giaTri) {
        StringBuilder w = new StringBuilder();
        for (int k = 0; k < cacCot.size(); k++) {
            if (!cacCot.get(k).khoaChinh) continue;
            w.append(w.length() > 0 ? " AND " : "").append('`').append(cacCot.get(k).ten).append("` = ?");
            giaTri.add(goc[k]);
        }
        if (w.length() == 0) {
            JOptionPane.showMessageDialog(this, "Bảng này không có khóa chính nên không thao tác được.");
            return null;
        }
        return w.toString();
    }

    /** Form nhap lieu: moi cot mot o. goc == null la them moi (bo cot tu tang), nguoc lai la sua (khoa chinh bi khoa). */
    private JPanel taoForm(Map<Cot, JTextField> o, String[] goc) {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 8);
        g.anchor = GridBagConstraints.WEST;
        int dong = 0;
        for (int k = 0; k < cacCot.size(); k++) {
            Cot c = cacCot.get(k);
            if (c.an || (goc == null && c.tuTang)) continue;
            JLabel lbl = new JLabel(c.nhan);
            lbl.setFont(new Font(FONT, Font.PLAIN, 13));
            JTextField txt = new JTextField(goc != null && goc[k] != null ? goc[k] : "", 24);
            txt.setFont(new Font(FONT, Font.PLAIN, 13));
            if (goc != null && (c.khoaChinh || c.tuTang)) txt.setEnabled(false);
            g.gridx = 0;
            g.gridy = dong;
            g.fill = GridBagConstraints.NONE;
            g.weightx = 0;
            form.add(lbl, g);
            g.gridx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.weightx = 1;
            form.add(txt, g);
            o.put(c, txt);
            dong++;
        }
        form.setPreferredSize(new Dimension(460, Math.min(460, dong * 38 + 16)));
        return form;
    }

    private void thucThi(String sql, List<String> thamSo, String thanhCong) {
        chayNen(() -> {
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < thamSo.size(); i++) ps.setObject(i + 1, thamSo.get(i));
                ps.executeUpdate();
            }
            return Boolean.TRUE;
        }, ok -> {
            taiDuLieu();
            lblThongBao.setForeground(new Color(0x2E, 0x7D, 0x32));
            lblThongBao.setText(thanhCong);
        }, loi -> {
            String msg = loi.getMessage();
            if (loi instanceof SQLException) {
                int ma = ((SQLException) loi).getErrorCode();
                if (ma == 1062) msg = "Mã bị trùng với dòng đã có.";
                else if (ma == 1451) msg = "Dữ liệu này đang được dùng ở bảng khác nên không xóa được.";
                else if (ma == 1452) msg = "Giá trị tham chiếu không tồn tại (sai mã liên kết).";
            }
            JOptionPane.showMessageDialog(this, "Không thực hiện được:\n" + msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
        });
    }

    // ===================== CHAY NEN =====================
    private <T> void chayNen(Callable<T> viec, Consumer<T> khiXong) {
        chayNen(viec, khiXong, loi -> baoLoi("Lỗi: " + loi.getMessage()));
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
}