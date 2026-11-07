package view;

import com.formdev.flatlaf.FlatLightLaf;
import controller.DangNhapController;
import model.TaiKhoan;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.prefs.Preferences;

public class DangNhap extends JFrame {

    private JTextField txtTenDangNhap;
    private JPasswordField txtMatKhau;
    private JCheckBox chkGhiNho;
    private JButton btnDangNhap;

    private final DangNhapController dangNhapController = new DangNhapController();

    // Preferences: noi luu nhung thong tin nho (nhu "Ghi nho dang nhap") tren may nguoi dung.
    // Du lieu nay duoc Java tu luu vao System Registry (Windows) hoac file cau hinh (Mac/Linux),
    // khong can ban tu tao file de luu.
    private final Preferences prefs = Preferences.userNodeForPackage(DangNhap.class);
    private static final String KEY_TEN_DANG_NHAP = "ghiNho_tenDangNhap";
    private static final String KEY_MAT_KHAU = "ghiNho_matKhau";
    private static final String KEY_CO_GHI_NHO = "ghiNho_coGhiNho";

    private static final Color MAU_NAU_DAM   = new Color(0x8A, 0x6D, 0x4A);
    private static final Color MAU_NAU_NHAT  = new Color(0xB2, 0x90, 0x6B);
    private static final Color MAU_NEN_KEM   = new Color(0xFB, 0xF6, 0xEF);
    private static final Color MAU_CHU_XAM   = new Color(0x8A, 0x87, 0x82);

    public DangNhap() {
        FlatLightLaf.setup();
        SwingUtilities.updateComponentTreeUI(this);
        initUI();
    }

    private void initUI() {
        setTitle("Đăng nhập - Quản lý rạp chiếu phim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 760);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panelGoc = new JPanel(new GridLayout(1, 2));
        setContentPane(panelGoc);

        // ===== BEN TRAI: Anh nen =====
        JPanel panelTrai = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                try {
                    Image img = new ImageIcon(getClass().getResource("/images/rap_nen.jpg")).getImage();
                    g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
                    g.setColor(new Color(0, 0, 0, 90));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } catch (Exception e) {
                    g.setColor(new Color(0x3A, 0x30, 0x28));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        panelTrai.setLayout(null);

        JLabel lblCauTrich1 = new JLabel("Phim hay");
        lblCauTrich1.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        lblCauTrich1.setForeground(Color.WHITE);
        lblCauTrich1.setBounds(50, 60, 300, 35);
        panelTrai.add(lblCauTrich1);

        JLabel lblCauTrich2 = new JLabel("Kết nối cảm xúc");
        lblCauTrich2.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        lblCauTrich2.setForeground(Color.WHITE);
        lblCauTrich2.setBounds(50, 95, 300, 35);
        panelTrai.add(lblCauTrich2);

        // ===== BEN PHAI: Form dang nhap =====
        JPanel panelPhai = new JPanel(null);
        panelPhai.setBackground(MAU_NEN_KEM);

        JLabel lblTieuDe = new JLabel("QUẢN LÝ RẠP CHIẾU PHIM", SwingConstants.CENTER);
        lblTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTieuDe.setForeground(MAU_NAU_DAM);
        lblTieuDe.setBounds(70, 180, 460, 35);
        panelPhai.add(lblTieuDe);

        JLabel lblChaoMung = new JLabel("Chào mừng trở lại!", SwingConstants.CENTER);
        lblChaoMung.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblChaoMung.setForeground(new Color(0x2E, 0x2A, 0x26));
        lblChaoMung.setBounds(70, 250, 460, 35);
        panelPhai.add(lblChaoMung);

        JLabel lblMoTa = new JLabel("Đăng nhập để tiếp tục quản lý hệ thống", SwingConstants.CENTER);
        lblMoTa.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblMoTa.setForeground(MAU_CHU_XAM);
        lblMoTa.setBounds(70, 285, 460, 25);
        panelPhai.add(lblMoTa);

        // O nhap Ten dang nhap
        txtTenDangNhap = new JTextField();
        txtTenDangNhap.putClientProperty("JTextField.placeholderText", "Tên đăng nhập");
        txtTenDangNhap.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtTenDangNhap.setBounds(70, 350, 460, 45);
        panelPhai.add(txtTenDangNhap);

        // O nhap Mat khau - thu nho chieu rong (425 thay vi 460) de chua cho nut mat
        txtMatKhau = new JPasswordField();
        txtMatKhau.putClientProperty("JTextField.placeholderText", "Mật khẩu");
        txtMatKhau.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtMatKhau.setBounds(70, 410, 425, 45);
        panelPhai.add(txtMatKhau);

        // ===== Nut con mat: Hien / An mat khau =====
        final char kyTuAn = txtMatKhau.getEchoChar();
        final boolean[] dangHien = {false};

        JButton btnXemMatKhau = new JButton("\uD83D\uDC41");
        btnXemMatKhau.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btnXemMatKhau.setBounds(500, 410, 30, 45);
        btnXemMatKhau.setBorderPainted(false);
        btnXemMatKhau.setContentAreaFilled(false);
        btnXemMatKhau.setFocusPainted(false);
        btnXemMatKhau.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnXemMatKhau.addActionListener(e -> {
            if (dangHien[0]) {
                txtMatKhau.setEchoChar(kyTuAn);
                btnXemMatKhau.setText("\uD83D\uDC41");
            } else {
                txtMatKhau.setEchoChar((char) 0);
                btnXemMatKhau.setText("\uD83D\uDE48");
            }
            dangHien[0] = !dangHien[0];
        });
        panelPhai.add(btnXemMatKhau);

        // Ghi nho dang nhap
        chkGhiNho = new JCheckBox("Ghi nhớ đăng nhập");
        chkGhiNho.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chkGhiNho.setForeground(new Color(0x4A, 0x46, 0x40));
        chkGhiNho.setOpaque(false);
        chkGhiNho.setBounds(70, 468, 220, 25);
        panelPhai.add(chkGhiNho);

        // Da bo nhan "Quen mat khau?" theo yeu cau

        // Nut Dang nhap
        btnDangNhap = new JButton("ĐĂNG NHẬP");
        btnDangNhap.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDangNhap.setForeground(Color.WHITE);
        btnDangNhap.setBackground(MAU_NAU_NHAT);
        btnDangNhap.setFocusPainted(false);
        btnDangNhap.setBounds(70, 515, 460, 50);
        btnDangNhap.addActionListener(this::btnDangNhapActionPerformed);
        panelPhai.add(btnDangNhap);

        panelGoc.add(panelTrai);
        panelGoc.add(panelPhai);

        // Dien san thong tin da ghi nho tu lan truoc (neu co)
        loadThongTinDaGhiNho();
    }

    // Doc thong tin da luu (neu co) va dien san vao form
    private void loadThongTinDaGhiNho() {
        boolean coGhiNho = prefs.getBoolean(KEY_CO_GHI_NHO, false);
        if (coGhiNho) {
            txtTenDangNhap.setText(prefs.get(KEY_TEN_DANG_NHAP, ""));
            txtMatKhau.setText(prefs.get(KEY_MAT_KHAU, ""));
            chkGhiNho.setSelected(true);
        }
    }

    // Luu hoac xoa thong tin dang nhap tuy theo checkbox co duoc tick hay khong
    private void xuLyGhiNho(String tenDangNhap, String matKhau) {
        if (chkGhiNho.isSelected()) {
            prefs.put(KEY_TEN_DANG_NHAP, tenDangNhap);
            prefs.put(KEY_MAT_KHAU, matKhau);
            prefs.putBoolean(KEY_CO_GHI_NHO, true);
        } else {
            prefs.remove(KEY_TEN_DANG_NHAP);
            prefs.remove(KEY_MAT_KHAU);
            prefs.putBoolean(KEY_CO_GHI_NHO, false);
        }
    }

    private void btnDangNhapActionPerformed(ActionEvent evt) {
        String tenDangNhap = txtTenDangNhap.getText().trim();
        String matKhau = new String(txtMatKhau.getPassword());

        if (tenDangNhap.isEmpty() || matKhau.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!",
                    "Thiếu thông tin",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            TaiKhoan tk = dangNhapController.dangNhap(tenDangNhap, matKhau);

            if (tk == null) {
                JOptionPane.showMessageDialog(this,
                        "Sai tên đăng nhập hoặc mật khẩu, hoặc tài khoản đã bị khóa!",
                        "Đăng nhập thất bại",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Luu hoac xoa thong tin dang nhap tuy theo checkbox "Ghi nho dang nhap"
            xuLyGhiNho(tenDangNhap, matKhau);

            // Dieu huong sang man hinh tiep theo tuy vai tro
          if (tk.getVaiTro().equals("QUAN_LY")) {
            new TrangChuQuanLy(tk).setVisible(true);
           } else {
            new TrangChuNhanVien(tk).setVisible(true);
}
            this.dispose(); // dong man hinh Dang nhap lai sau khi da mo Trang chu

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi kết nối cơ sở dữ liệu!\n" + e.getMessage(),
                    "Lỗi hệ thống",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DangNhap().setVisible(true));
    }
} 