package view;

import com.formdev.flatlaf.FlatLightLaf;
import controller.PhimController;
import model.Phim;
import model.SuatChieu;
import model.TaiKhoan;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrangChuNhanVien extends JFrame {

    private TaiKhoan taiKhoanDangNhap;
    private JPanel panelNoiDung;   // vung hien thi noi dung thay doi theo menu dang chon
    private JButton[] cacNutMenu;  // mang luu cac nut menu ngang de doi mau khi chon

    private final PhimController phimController = new PhimController();

    private static final Color MAU_NAU_DAM   = new Color(0x8A, 0x6D, 0x4A);
    private static final Color MAU_NAU_NHAT  = new Color(0xB2, 0x90, 0x6B);
    private static final Color MAU_NEN_KEM   = new Color(0xFB, 0xF6, 0xEF);
    private static final Color MAU_CHU_XAM   = new Color(0x8A, 0x87, 0x82);
    private static final Color MAU_NEN_HEADER = new Color(0x3A, 0x30, 0x28);

    private static final String[] TEN_CAC_MUC_MENU = {
        "Trang chủ",
        "Danh sách phim",
        "Bán vé",
        "Combo bắp nước",
        "In vé",
        "Sự cố & bồi thường"
    };

    public TrangChuNhanVien(TaiKhoan taiKhoan) {
        this.taiKhoanDangNhap = taiKhoan;
        FlatLightLaf.setup();
        SwingUtilities.updateComponentTreeUI(this);
        initUI();
    }

    private void initUI() {
        setTitle("Trang chủ Nhân viên - Quản lý rạp chiếu phim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 800);
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel panelGoc = new JPanel(new BorderLayout());
        panelGoc.setBackground(MAU_NEN_KEM);
        setContentPane(panelGoc);

        panelGoc.add(taoHeader(), BorderLayout.NORTH);

        panelNoiDung = new JPanel(new BorderLayout());
        panelNoiDung.setBackground(MAU_NEN_KEM);
        panelNoiDung.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panelGoc.add(panelNoiDung, BorderLayout.CENTER);

        hienThiTrangChu(); // mac dinh mo tab Trang chu dau tien
    }

    // ===== HEADER =====
    private JPanel taoHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(MAU_NEN_HEADER);
        header.setPreferredSize(new Dimension(0, 80));
        header.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));

        JPanel panelLogo = new JPanel();
        panelLogo.setOpaque(false);
        panelLogo.setLayout(new BoxLayout(panelLogo, BoxLayout.Y_AXIS));
        JLabel lblTenRap = new JLabel("RẠP CHIẾU PHIM");
        lblTenRap.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTenRap.setForeground(Color.WHITE);
        JLabel lblPhuDe = new JLabel("HỆ THỐNG QUẢN LÝ");
        lblPhuDe.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblPhuDe.setForeground(new Color(0xC0, 0xB0, 0xA0));
        panelLogo.add(lblTenRap);
        panelLogo.add(lblPhuDe);
        header.add(panelLogo, BorderLayout.WEST);

        JPanel panelMenu = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panelMenu.setOpaque(false);

        cacNutMenu = new JButton[TEN_CAC_MUC_MENU.length];
        for (int i = 0; i < TEN_CAC_MUC_MENU.length; i++) {
            final int chiSo = i;
            JButton nut = new JButton(TEN_CAC_MUC_MENU[i]);
            nut.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            nut.setForeground(Color.WHITE);
            nut.setBorderPainted(false);
            nut.setContentAreaFilled(false);
            nut.setFocusPainted(false);
            nut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            nut.setMargin(new Insets(8, 16, 8, 16));
            nut.addActionListener(e -> chonMucMenu(chiSo));
            cacNutMenu[i] = nut;
            panelMenu.add(nut);
        }
        header.add(panelMenu, BorderLayout.CENTER);

        JPanel panelPhai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        panelPhai.setOpaque(false);

        JLabel lblTenNguoiDung = new JLabel(
                (taiKhoanDangNhap != null ? taiKhoanDangNhap.getHoTen() : "Người dùng")
        );
        lblTenNguoiDung.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblTenNguoiDung.setForeground(Color.WHITE);

        JButton btnDangXuat = new JButton("Đăng xuất");
        btnDangXuat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnDangXuat.setForeground(Color.WHITE);
        btnDangXuat.setBorderPainted(true);
        btnDangXuat.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        btnDangXuat.setContentAreaFilled(false);
        btnDangXuat.setFocusPainted(false);
        btnDangXuat.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDangXuat.addActionListener(this::btnDangXuatActionPerformed);

        panelPhai.add(lblTenNguoiDung);
        panelPhai.add(btnDangXuat);
        header.add(panelPhai, BorderLayout.EAST);

        SwingUtilities.invokeLater(() -> capNhatMauNutMenu(0));

        return header;
    }

    private void capNhatMauNutMenu(int chiSoDuocChon) {
        for (int i = 0; i < cacNutMenu.length; i++) {
            if (i == chiSoDuocChon) {
                cacNutMenu[i].setContentAreaFilled(true);
                cacNutMenu[i].setBackground(MAU_NAU_NHAT);
            } else {
                cacNutMenu[i].setContentAreaFilled(false);
            }
        }
    }
//==== MENU===//
    private void chonMucMenu(int chiSo) {
        capNhatMauNutMenu(chiSo);
        switch (chiSo) {
            case 0 -> hienThiTrangChu();
            case 1 -> hienThiDanhSachPhim();
            case 2 -> hienThiBanVe(null);
            case 3 -> hienThiBanCombo();
            case 4 -> hienThiInVe(null);
            case 5 -> hienThiSuCo();
            default -> hienThiTrangChu();
        }
    }

  
        // ===== Noi dung tab In ve =====
    // maHd: ma hoa don can mo san (vi du vua ban ve xong), null neu khong co
    private void hienThiInVe(String maHd) {
        panelNoiDung.removeAll();
        try {
            panelNoiDung.add(new InVePanel(maHd), BorderLayout.CENTER);
        } catch (Exception e) {
            e.printStackTrace();
            JLabel lblLoi = new JLabel("Lỗi tải màn in vé: " + e);
            lblLoi.setForeground(Color.RED);
            lblLoi.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            panelNoiDung.add(lblLoi, BorderLayout.CENTER);
        }
        panelNoiDung.revalidate();
        panelNoiDung.repaint();
    }
    
    // ===== Noi dung tab Combo bap nuoc =====
private void hienThiBanCombo() {
    panelNoiDung.removeAll();
    try {
        panelNoiDung.add(new BanComboPanel(layIdNhanVien()), BorderLayout.CENTER);
    } catch (Exception e) {
        e.printStackTrace();
        JLabel lblLoi = new JLabel("Lỗi tải màn combo: " + e);
        lblLoi.setForeground(Color.RED);
        lblLoi.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panelNoiDung.add(lblLoi, BorderLayout.CENTER);
    }
    panelNoiDung.revalidate();
    panelNoiDung.repaint();
}

    // ===== Noi dung tab Su co & boi thuong =====
    private void hienThiSuCo() {
        panelNoiDung.removeAll();
        try {
            panelNoiDung.add(new SuCoBoiThuongPanel(layIdNhanVien()), BorderLayout.CENTER);
        } catch (Exception e) {
            e.printStackTrace();
            JLabel lblLoi = new JLabel("Lỗi tải màn sự cố: " + e);
            lblLoi.setForeground(Color.RED);
            lblLoi.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            panelNoiDung.add(lblLoi, BorderLayout.CENTER);
        }
        panelNoiDung.revalidate();
        panelNoiDung.repaint();
    }

    // ===== Noi dung tab Trang chu: dung Trangchupanel =====
    private void hienThiTrangChu() {
        System.out.println(">>> DANG VE TRANG CHU MOI");
        panelNoiDung.removeAll();
        try {
            panelNoiDung.add(
                    new Trangchupanel(taiKhoanDangNhap, this::chonMucMenu, this::docAnhPoster),
                    BorderLayout.CENTER);
        } catch (Exception e) {
            // Neu panel loi thi hien chu do thay vi man hinh trang
            e.printStackTrace();
            JLabel lblLoi = new JLabel("Lỗi tải trang chủ: " + e);
            lblLoi.setForeground(Color.RED);
            lblLoi.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            panelNoiDung.add(lblLoi, BorderLayout.CENTER);
        }
        panelNoiDung.revalidate();
        panelNoiDung.repaint();
    }

    // ===== Noi dung tab Ban ve =====
    // maSuatChonSan: ma suat can mo san (bam gio chieu o tab Danh sach phim), null neu khong co
    private void hienThiBanVe(String maSuatChonSan) {
        panelNoiDung.removeAll();
        try {
            panelNoiDung.add(new BanVePanel(layIdNhanVien(), maSuatChonSan), BorderLayout.CENTER);
        } catch (Exception e) {
            e.printStackTrace();
            JLabel lblLoi = new JLabel("Lỗi tải màn bán vé: " + e);
            lblLoi.setForeground(Color.RED);
            lblLoi.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            panelNoiDung.add(lblLoi, BorderLayout.CENTER);
        }
        panelNoiDung.revalidate();
        panelNoiDung.repaint();
    }

    // Id tai khoan dang dang nhap (ghi vao hoa don). Chay thu khong dang nhap thi dung id 2 (nv001)
    // >>> Neu model TaiKhoan dat ten khac getId() thi sua lai o day <<<
    private long layIdNhanVien() {
        return (taiKhoanDangNhap != null) ? taiKhoanDangNhap.getId() : 2L;
    }

    // ===== Noi dung tab Danh sach phim =====
    private void hienThiDanhSachPhim() {
        panelNoiDung.removeAll();
        // Giao dien kieu trang chu website: banner + luoi poster. Bam gio chieu -> mo man Ban ve voi suat do.
        panelNoiDung.add(new TrangPhimWeb(maSuat -> {
            capNhatMauNutMenu(2);               // to sang nut "Ban ve" tren menu
            hienThiBanVe(maSuat);               // mo san suat vua bam
        }), BorderLayout.CENTER);
        panelNoiDung.revalidate();
        panelNoiDung.repaint();
    }

    private JPanel taoTieuDeMuc(String tieuDe, String phuDe) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JLabel lbl1 = new JLabel(tieuDe);
        lbl1.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl1.setForeground(MAU_NAU_DAM);
        lbl1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl2 = new JLabel(phuDe);
        lbl2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl2.setForeground(MAU_CHU_XAM);
        lbl2.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lbl1);
        p.add(Box.createVerticalStrut(2));
        p.add(lbl2);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private JLabel taoNhanThongBao(String noiDung, Color mau) {
        JLabel lbl = new JLabel(noiDung);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setForeground(mau);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        return lbl;
    }

    private JPanel taoTheFilm(Phim phim, List<SuatChieu> cacSuatChieu, String nhanGio) {
        JPanel the = new JPanel(new GridBagLayout());
        the.setBackground(Color.WHITE);
        the.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5, 0xDD, 0xD0), 1),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));
        the.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        the.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 20);
        the.add(taoKhungPoster(phim), gbc);

        JPanel panelThongTin = new JPanel();
        panelThongTin.setOpaque(false);
        panelThongTin.setLayout(new BoxLayout(panelThongTin, BoxLayout.Y_AXIS));
        panelThongTin.setPreferredSize(new Dimension(300, 120));
        panelThongTin.setMaximumSize(new Dimension(300, 120));

        JLabel lblTen = new JLabel(phim.getTenPhim());
        lblTen.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTen.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTheLoai = new JLabel(
                (phim.getTheLoai() != null ? phim.getTheLoai() : "") + "  •  " + phim.getThoiLuong() + " phút"
        );
        lblTheLoai.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTheLoai.setForeground(MAU_CHU_XAM);
        lblTheLoai.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelThongTin.add(lblTen);
        panelThongTin.add(Box.createVerticalStrut(6));
        panelThongTin.add(lblTheLoai);

        if (phim.getDaoDien() != null && !phim.getDaoDien().isEmpty()) {
            JLabel lblDaoDien = new JLabel("Đạo diễn: " + phim.getDaoDien());
            lblDaoDien.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblDaoDien.setForeground(MAU_CHU_XAM);
            lblDaoDien.setAlignmentX(Component.LEFT_ALIGNMENT);
            panelThongTin.add(Box.createVerticalStrut(4));
            panelThongTin.add(lblDaoDien);
        }

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 20);
        the.add(panelThongTin, gbc);

        JPanel panelGioChieu = new JPanel();
        panelGioChieu.setOpaque(false);
        panelGioChieu.setLayout(new BoxLayout(panelGioChieu, BoxLayout.Y_AXIS));

        JLabel lblNhanGioChieu = new JLabel(nhanGio);
        lblNhanGioChieu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblNhanGioChieu.setForeground(MAU_CHU_XAM);
        lblNhanGioChieu.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelGioChieu.add(lblNhanGioChieu);
        panelGioChieu.add(Box.createVerticalStrut(8));

        JPanel panelCacGio = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panelCacGio.setOpaque(false);
        panelCacGio.setAlignmentX(Component.LEFT_ALIGNMENT);

        DateTimeFormatter dinhDangGio = DateTimeFormatter.ofPattern("HH:mm");

        if (cacSuatChieu.isEmpty()) {
            JLabel lblKhongCoSuat = new JLabel("Không có suất chiếu");
            lblKhongCoSuat.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            lblKhongCoSuat.setForeground(MAU_CHU_XAM);
            panelCacGio.add(lblKhongCoSuat);
        } else {
            for (SuatChieu sc : cacSuatChieu) {
                JButton nutGio = new JButton(sc.getGioBatDau().format(dinhDangGio));
                nutGio.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                nutGio.setForeground(MAU_NAU_DAM);
                nutGio.setBackground(MAU_NEN_KEM);
                nutGio.setBorder(BorderFactory.createLineBorder(MAU_NAU_NHAT, 1));
                nutGio.setFocusPainted(false);
                nutGio.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                nutGio.addActionListener(e -> {
                    capNhatMauNutMenu(2);               // to sang nut "Ban ve" tren menu
                    hienThiBanVe(sc.getMaSuat());       // mo san suat vua bam
                });

                panelCacGio.add(nutGio);
            }
        }

        panelGioChieu.add(panelCacGio);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        the.add(panelGioChieu, gbc);

        return the;
    }

    // ===== POSTER =====
    private JPanel taoKhungPoster(Phim phim) {
        int rong = 80, cao = 120;

        JPanel khung = new JPanel(new BorderLayout());
        khung.setPreferredSize(new Dimension(rong, cao));
        khung.setMinimumSize(new Dimension(rong, cao));
        khung.setMaximumSize(new Dimension(rong, cao));

        Image anhGoc = docAnhPoster(phim.getPoster());

        if (anhGoc != null) {
            Image anhThuNho = anhGoc.getScaledInstance(rong, cao, Image.SCALE_SMOOTH);
            khung.add(new JLabel(new ImageIcon(anhThuNho)), BorderLayout.CENTER);
            return khung;
        }

        khung.setBackground(MAU_NEN_KEM);
        khung.setBorder(BorderFactory.createLineBorder(new Color(0xE5, 0xDD, 0xD0), 1));

        String chuCaiDau = (phim.getTenPhim() != null && !phim.getTenPhim().isEmpty())
                ? phim.getTenPhim().substring(0, 1).toUpperCase()
                : "?";
        JLabel lblChuCai = new JLabel(chuCaiDau, SwingConstants.CENTER);
        lblChuCai.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblChuCai.setForeground(MAU_NAU_NHAT);
        khung.add(lblChuCai, BorderLayout.CENTER);

        return khung;
    }

    // Doc anh poster tu src/main/resources/images/posters/ (dung chung cho Danh sach phim va Trang chu)
    private Image docAnhPoster(String tenFile) {
        if (tenFile == null || tenFile.trim().isEmpty()) {
            return null;
        }

        String ten = tenFile.trim();
        int viTriCham = ten.lastIndexOf('.');
        String tenKhongDuoi = (viTriCham > 0) ? ten.substring(0, viTriCham) : ten;

        String[] cacTenThu = {
            ten,
            tenKhongDuoi + ".png",
            tenKhongDuoi + ".jpg",
            tenKhongDuoi + ".jpeg",
            tenKhongDuoi + ".PNG",
            tenKhongDuoi + ".JPG"
        };

        boolean timThayFileNhungLoi = false;

        for (String t : cacTenThu) {
            URL url = getClass().getResource("/images/posters/" + t);
            if (url == null) {
                continue;
            }
            try {
                BufferedImage anh = ImageIO.read(url);
                if (anh != null) {
                    return anh;
                }
                timThayFileNhungLoi = true;
                System.err.println("[POSTER] File ton tai nhung KHONG doc duoc (sai dinh dang that): " + t);
            } catch (Exception e) {
                timThayFileNhungLoi = true;
                System.err.println("[POSTER] Loi doc file " + t + ": " + e.getMessage());
            }
        }

        if (!timThayFileNhungLoi) {
            System.err.println("[POSTER] Khong tim thay file anh cho: " + tenFile
                    + " (kiem tra thu muc src/main/resources/images/posters/ va Clean & Build)");
        }
        return null;
    }

    private void hienThiNoiDungTamThoi(String tenChucNang) {
        panelNoiDung.removeAll();

        JLabel lbl = new JLabel("Chức năng \"" + tenChucNang + "\" đang được xây dựng...");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lbl.setForeground(MAU_CHU_XAM);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);

        panelNoiDung.add(lbl, BorderLayout.CENTER);
        panelNoiDung.revalidate();
        panelNoiDung.repaint();
    }

    private void btnDangXuatActionPerformed(ActionEvent evt) {
        int xacNhan = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đăng xuất?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION);

        if (xacNhan == JOptionPane.YES_OPTION) {
            this.dispose();
            new DangNhap().setVisible(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TrangChuNhanVien(null).setVisible(true));
    }
}