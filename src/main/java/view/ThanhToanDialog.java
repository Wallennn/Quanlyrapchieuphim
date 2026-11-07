package view;

import controller.BanVeController.GheDaBanException;
import controller.DoTuoiController;
import controller.ThanhToanController;
import controller.ThanhToanController.KetQuaGia;
import controller.ThanhToanController.KetQuaThanhToan;
import controller.ThanhToanController.TienKhachDuaKhongDuException;
import controller.ThanhToanController.YeuCau;
import dao.KhachHangDAO;
import dao.KhachHangDAO.KhachHang;
import dao.KhachHangDAO.SdtTrungException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Hop thoai thanh toan khi ban ve:
 * thanh vien (tich diem / doi diem), khuyen mai, phuong thuc thanh toan, tien khach dua - tien thua,
 * ma QR chuyen khoan, kiem tra tuoi voi phim T16/T18. Moi con so lay tu ThanhToanController (tinh o server).
 */
public class ThanhToanDialog extends JDialog {

    private static final String FONT = "Segoe UI";
    private static final Color NAU_DAM    = new Color(0x8A, 0x6D, 0x4A);
    private static final Color NEN_KEM    = new Color(0xFB, 0xF6, 0xEF);
    private static final Color NEN_HEADER = new Color(0x3A, 0x30, 0x28);
    private static final Color CHU_XAM    = new Color(0x8A, 0x87, 0x82);
    private static final Color CHU_DEN    = new Color(0x2E, 0x2A, 0x26);
    private static final Color VIEN       = new Color(0xE5, 0xDD, 0xD0);
    private static final Color XANH       = new Color(0x2E, 0x7D, 0x32);
    private static final Color DO         = new Color(0xC6, 0x28, 0x28);

    /** Khac null khi thanh toan thanh cong. */
    public KetQuaThanhToan ketQua;
    /** true neu co ghe vua bi quay khac ban mat (can tai lai so do ghe). */
    public boolean gheBiBan;

    private final ThanhToanController controller;
    private final long idNhanVien;
    private final String maSuat, doTuoi;
    private final List<String> dsMaGhe;
    private final Map<String, Integer> combo;
    private final NumberFormat so = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    // ----- trang thai -----
    private KetQuaGia gia;
    private String sdtDangDung, maDangDung;
    private int phien = 0;
    private boolean dangTinh = true, dangThanhToan = false, dangNap = false;
    private long qrSoTien = -1;

    // ----- thanh phan -----
    private final JTextField txtSdt = new JTextField(), txtMa = new JTextField(), txtTienDua = new JTextField();
    private final JButton btnTim = nutPhu("Tìm"), btnDangKy = nutPhu("Đăng ký thành viên"), btnApMa = nutPhu("Áp dụng");
    private final JLabel lblKhach = new JLabel(" "), lblMa = new JLabel(" "), lblThua = new JLabel(" "),
            lblMaxDiem = new JLabel(" "), lblLoi = new JLabel(" ");
    private final JSpinner spDiem = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
    private final JRadioButton rbTienMat = new JRadioButton("Tiền mặt", true),
            rbChuyenKhoan = new JRadioButton("Chuyển khoản"), rbThe = new JRadioButton("Thẻ");
    private final JCheckBox chkTuoi = new JCheckBox();
    private final JPanel khuTienMat = new JPanel(), khuTomTat = new JPanel(), khuQR = new JPanel();
    private final JLabel lblQR = new JLabel("", SwingConstants.CENTER), lblQRChu = new JLabel(" ", SwingConstants.CENTER);
    private final JButton btnXacNhan = new JButton("Xác nhận thanh toán");

    public ThanhToanDialog(Window chu, ThanhToanController controller, long idNhanVien, String maSuat,
                           String tenPhim, String doTuoi, String thongTinSuat, List<String> dsMaGhe,
                           String tenGhe, Map<String, Integer> combo, String moTaCombo) {
        super(chu, "Thanh toán", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.idNhanVien = idNhanVien;
        this.maSuat = maSuat;
        this.doTuoi = doTuoi;
        this.dsMaGhe = dsMaGhe;
        this.combo = combo;

        JPanel nen = new JPanel(new BorderLayout(18, 0));
        nen.setBackground(NEN_KEM);
        nen.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));

        // ================= CỘT TRÁI =================
        JPanel trai = new CotCuon();
        trai.setOpaque(false);
        trai.setLayout(new BoxLayout(trai, BoxLayout.Y_AXIS));

        // -- thông tin đơn
        JPanel dongPhim = new JPanel(new BorderLayout(8, 0));
        dongPhim.setOpaque(false);
        JLabel lblPhim = nhan(tenPhim, Font.BOLD, 18, CHU_DEN);
        dongPhim.add(lblPhim, BorderLayout.CENTER);
        DoTuoiChip chip = new DoTuoiChip(doTuoi);
        JPanel chipBoc = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        chipBoc.setOpaque(false);
        chipBoc.add(chip);
        dongPhim.add(chipBoc, BorderLayout.EAST);
        dongPhim.setAlignmentX(Component.LEFT_ALIGNMENT);
        trai.add(dongPhim);
        trai.add(canTrai(nhan(thongTinSuat, Font.PLAIN, 13, CHU_XAM)));
        trai.add(Box.createVerticalStrut(6));
        trai.add(canTrai(nhan(htmlRong("Ghế: " + tenGhe, 400), Font.PLAIN, 13, CHU_DEN)));
        if (moTaCombo != null && !moTaCombo.isEmpty()) {
            trai.add(canTrai(nhan(htmlRong("Combo: " + moTaCombo, 400), Font.PLAIN, 13, CHU_DEN)));
        }

        // -- kiểm tra tuổi (phim T16 / T18)
        if (DoTuoiController.canKiemTraTuoi(doTuoi)) {
            JPanel canhBao = new JPanel(new BorderLayout());
            canhBao.setBackground(new Color(0xFD, 0xEC, 0xEA));
            canhBao.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xF0, 0xB8, 0xB0), 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            chkTuoi.setText("Đã kiểm tra giấy tờ: khách đủ " + ("T18".equals(doTuoi) ? "18" : "16") + " tuổi");
            chkTuoi.setOpaque(false);
            chkTuoi.setFont(new Font(FONT, Font.BOLD, 13));
            chkTuoi.setForeground(DO);
            canhBao.add(nhan("Phim " + doTuoi + " - nhân viên phải kiểm tra tuổi khách", Font.PLAIN, 12, DO), BorderLayout.NORTH);
            canhBao.add(chkTuoi, BorderLayout.CENTER);
            canhBao.setAlignmentX(Component.LEFT_ALIGNMENT);
            canhBao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
            trai.add(Box.createVerticalStrut(10));
            trai.add(canhBao);
            chkTuoi.addActionListener(e -> capNhatNut());
        }

        // -- thành viên
        trai.add(Box.createVerticalStrut(14));
        trai.add(tieuMuc("Thành viên (tích điểm)"));
        txtSdt.putClientProperty("JTextField.placeholderText", "Số điện thoại thành viên");
        trai.add(canTrai(hang(txtSdt, btnTim)));
        trai.add(Box.createVerticalStrut(4));
        trai.add(canTrai(lblKhach));
        trai.add(canTrai(btnDangKy));
        btnDangKy.setVisible(false);
        JPanel dongDiem = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dongDiem.setOpaque(false);
        dongDiem.add(nhan("Dùng điểm:", Font.PLAIN, 13, CHU_DEN));
        spDiem.setPreferredSize(new Dimension(80, 30));
        spDiem.setEnabled(false);
        dongDiem.add(spDiem);
        dongDiem.add(lblMaxDiem);
        trai.add(canTrai(dongDiem));

        // -- khuyến mãi
        trai.add(Box.createVerticalStrut(12));
        trai.add(tieuMuc("Khuyến mãi"));
        txtMa.putClientProperty("JTextField.placeholderText", "Mã khuyến mãi (nếu có)");
        trai.add(canTrai(hang(txtMa, btnApMa)));
        trai.add(Box.createVerticalStrut(4));
        trai.add(canTrai(lblMa));

        // -- phương thức thanh toán
        trai.add(Box.createVerticalStrut(12));
        trai.add(tieuMuc("Phương thức thanh toán"));
        ButtonGroup nhom = new ButtonGroup();
        JPanel dongPt = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        dongPt.setOpaque(false);
        for (JRadioButton rb : new JRadioButton[]{rbTienMat, rbChuyenKhoan, rbThe}) {
            rb.setOpaque(false);
            rb.setFont(new Font(FONT, Font.PLAIN, 14));
            rb.setForeground(CHU_DEN);
            nhom.add(rb);
            dongPt.add(rb);
        }
        trai.add(canTrai(dongPt));

        khuTienMat.setOpaque(false);
        khuTienMat.setLayout(new BoxLayout(khuTienMat, BoxLayout.Y_AXIS));
        txtTienDua.putClientProperty("JTextField.placeholderText", "Tiền khách đưa");
        txtTienDua.setFont(new Font(FONT, Font.BOLD, 16));
        JPanel dongDua = new JPanel(new BorderLayout(8, 0));
        dongDua.setOpaque(false);
        dongDua.add(nhan("Khách đưa:", Font.PLAIN, 13, CHU_DEN), BorderLayout.WEST);
        dongDua.add(txtTienDua, BorderLayout.CENTER);
        dongDua.setAlignmentX(Component.LEFT_ALIGNMENT);
        khuTienMat.add(dongDua);
        JPanel dongNhanh = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        dongNhanh.setOpaque(false);
        JButton btnDu = nutPhu("Vừa đủ");
        btnDu.addActionListener(e -> {
            if (gia != null) txtTienDua.setText(so.format(gia.khachPhaiTra));
        });
        dongNhanh.add(btnDu);
        for (long v : new long[]{100_000, 200_000, 500_000}) {
            JButton b = nutPhu(so.format(v));
            b.addActionListener(e -> txtTienDua.setText(so.format(v)));
            dongNhanh.add(b);
        }
        dongNhanh.setAlignmentX(Component.LEFT_ALIGNMENT);
        khuTienMat.add(dongNhanh);
        lblThua.setFont(new Font(FONT, Font.BOLD, 16));
        lblThua.setAlignmentX(Component.LEFT_ALIGNMENT);
        khuTienMat.add(lblThua);
        khuTienMat.setAlignmentX(Component.LEFT_ALIGNMENT);
        trai.add(Box.createVerticalStrut(6));
        trai.add(khuTienMat);
        trai.add(Box.createVerticalGlue());

        JScrollPane cuonTrai = new JScrollPane(trai);
        cuonTrai.setBorder(null);
        cuonTrai.setOpaque(false);
        cuonTrai.getViewport().setOpaque(false);
        cuonTrai.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        cuonTrai.getVerticalScrollBar().setUnitIncrement(16);
        cuonTrai.setPreferredSize(new Dimension(440, 570));
        nen.add(cuonTrai, BorderLayout.CENTER);

        // ================= CỘT PHẢI =================
        JPanel phai = new JPanel(new BorderLayout(0, 12));
        phai.setOpaque(false);
        phai.setPreferredSize(new Dimension(290, 0));

        JPanel khungTomTat = new JPanel(new BorderLayout());
        khungTomTat.setBackground(Color.WHITE);
        khungTomTat.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VIEN, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        khungTomTat.add(nhan("Tóm tắt thanh toán", Font.BOLD, 15, CHU_DEN), BorderLayout.NORTH);
        khuTomTat.setOpaque(false);
        khuTomTat.setLayout(new BoxLayout(khuTomTat, BoxLayout.Y_AXIS));
        khuTomTat.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        khungTomTat.add(khuTomTat, BorderLayout.CENTER);
        phai.add(khungTomTat, BorderLayout.NORTH);

        khuQR.setOpaque(false);
        khuQR.setLayout(new BorderLayout(0, 4));
        khuQR.add(lblQR, BorderLayout.CENTER);
        lblQRChu.setFont(new Font(FONT, Font.PLAIN, 12));
        lblQRChu.setForeground(CHU_XAM);
        khuQR.add(lblQRChu, BorderLayout.SOUTH);
        khuQR.setVisible(false);
        phai.add(khuQR, BorderLayout.CENTER);
        nen.add(phai, BorderLayout.EAST);

        // ================= HÀNG NÚT =================
        JPanel duoi = new JPanel(new BorderLayout());
        duoi.setOpaque(false);
        duoi.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        lblLoi.setFont(new Font(FONT, Font.PLAIN, 13));
        lblLoi.setForeground(DO);
        duoi.add(lblLoi, BorderLayout.CENTER);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nut.setOpaque(false);
        JButton btnHuy = nutPhu("Hủy");
        btnHuy.setPreferredSize(new Dimension(90, 40));
        btnHuy.addActionListener(e -> dispose());
        btnXacNhan.setFont(new Font(FONT, Font.BOLD, 15));
        btnXacNhan.setForeground(Color.WHITE);
        btnXacNhan.setBackground(NEN_HEADER);
        btnXacNhan.setFocusPainted(false);
        btnXacNhan.setPreferredSize(new Dimension(220, 40));
        btnXacNhan.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnXacNhan.putClientProperty("JButton.buttonType", "roundRect");
        btnXacNhan.addActionListener(e -> xacNhan());
        nut.add(btnHuy);
        nut.add(btnXacNhan);
        duoi.add(nut, BorderLayout.EAST);
        nen.add(duoi, BorderLayout.SOUTH);

        // ================= SỰ KIỆN =================
        btnTim.addActionListener(e -> timThanhVien());
        txtSdt.addActionListener(e -> timThanhVien());
        btnDangKy.addActionListener(e -> dangKyNhanh());
        btnApMa.addActionListener(e -> apDungMa());
        txtMa.addActionListener(e -> apDungMa());
        spDiem.addChangeListener(e -> {
            if (!dangNap) capNhatGia();
        });
        java.awt.event.ActionListener doiPt = e -> {
            khuTienMat.setVisible(rbTienMat.isSelected());
            capNhatQR();
            capNhatThua();
            capNhatNut();
        };
        rbTienMat.addActionListener(doiPt);
        rbChuyenKhoan.addActionListener(doiPt);
        rbThe.addActionListener(doiPt);
        txtTienDua.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { capNhatThua(); capNhatNut(); }
            @Override public void removeUpdate(DocumentEvent e) { capNhatThua(); capNhatNut(); }
            @Override public void changedUpdate(DocumentEvent e) { capNhatThua(); capNhatNut(); }
        });

        setContentPane(nen);
        pack();
        setResizable(true);
        setLocationRelativeTo(chu);
        capNhatNut();
        capNhatGia();
    }

    // ===================== TÍNH GIÁ =====================
    private YeuCau taoYeuCau() {
        YeuCau y = new YeuCau();
        y.maSuat = maSuat;
        y.dsMaGhe = dsMaGhe;
        y.combo = combo;
        y.sdt = sdtDangDung;
        y.maNhap = maDangDung;
        y.diemDung = (Integer) spDiem.getValue();
        if (rbChuyenKhoan.isSelected()) y.phuongThuc = ThanhToanController.CHUYEN_KHOAN;
        else if (rbThe.isSelected()) y.phuongThuc = ThanhToanController.THE;
        else y.phuongThuc = ThanhToanController.TIEN_MAT;
        y.tienKhachDua = docTien(txtTienDua.getText());
        return y;
    }

    private void capNhatGia() {
        final int p = ++phien;
        dangTinh = true;
        capNhatNut();
        final YeuCau y = taoYeuCau();
        chayNen(() -> controller.tinhGia(y), g -> {
            if (p != phien) return;
            dangTinh = false;
            gia = g;
            hienGia();
        }, loi -> {
            if (p != phien) return;
            dangTinh = false;
            lblLoi.setText("Lỗi tính tiền: " + loi.getMessage());
            capNhatNut();
        });
    }

    private void hienGia() {
        lblLoi.setText(" ");

        // --- thành viên
        if (sdtDangDung == null) {
            lblKhach.setText("Khách vãng lai (không tích điểm)");
            lblKhach.setForeground(CHU_XAM);
            btnDangKy.setVisible(false);
        } else if (gia.khach != null) {
            KhachHang k = gia.khach;
            lblKhach.setText(htmlRong(k.hoTen + "  •  hạng " + KhachHangDAO.tenHang(k.hang)
                    + " (giảm " + gia.phanTramHang + "%)  •  " + so.format(k.diem) + " điểm", 400));
            lblKhach.setForeground(XANH);
            btnDangKy.setVisible(false);
        } else {
            lblKhach.setText("Số điện thoại này chưa là thành viên");
            lblKhach.setForeground(DO);
            btnDangKy.setVisible(true);
        }
        dangNap = true;
        if (gia.khach != null && gia.diemToiDa > 0) {
            spDiem.setModel(new SpinnerNumberModel(gia.diemDung, 0, gia.diemToiDa, 1));
            spDiem.setEnabled(true);
            lblMaxDiem.setText("tối đa " + so.format(gia.diemToiDa) + " điểm (= " + tien(gia.diemToiDa * (long) ThanhToanController.GIA_TRI_DIEM) + ")");
        } else {
            spDiem.setModel(new SpinnerNumberModel(0, 0, 0, 1));
            spDiem.setEnabled(false);
            lblMaxDiem.setText(gia.khach != null ? "chưa đủ điểm để dùng" : " ");
        }
        dangNap = false;

        // --- khuyến mãi
        if (gia.thongBaoMa != null) {
            lblMa.setText(htmlRong("Mã không dùng được: " + gia.thongBaoMa, 400));
            lblMa.setForeground(DO);
        } else if (gia.maKm != null) {
            boolean tuMa = maDangDung != null;
            lblMa.setText(htmlRong((tuMa ? "Đã áp dụng mã: " : "Tự động áp dụng: ") + gia.tenKm + " (-" + tien(gia.giamKm) + ")", 400));
            lblMa.setForeground(XANH);
        } else {
            lblMa.setText(htmlRong("Không có khuyến mãi áp dụng cho hóa đơn này", 400));
            lblMa.setForeground(CHU_XAM);
        }

        // --- tóm tắt
        khuTomTat.removeAll();
        khuTomTat.add(dongTomTat("Vé (" + dsMaGhe.size() + " ghế)", tien(gia.tienVe), CHU_DEN, false));
        if (gia.tienCombo > 0) khuTomTat.add(dongTomTat("Combo", tien(gia.tienCombo), CHU_DEN, false));
        khuTomTat.add(dongTomTat("Tạm tính", tien(gia.tamTinh), CHU_DEN, true));
        if (gia.giamKm > 0) khuTomTat.add(dongTomTat("Khuyến mãi", "-" + tien(gia.giamKm), XANH, false));
        if (gia.giamHang > 0) {
            khuTomTat.add(dongTomTat("Hạng " + KhachHangDAO.tenHang(gia.khach.hang) + " (" + gia.phanTramHang + "%)",
                    "-" + tien(gia.giamHang), XANH, false));
        }
        if (gia.giamDiem > 0) khuTomTat.add(dongTomTat("Dùng " + so.format(gia.diemDung) + " điểm", "-" + tien(gia.giamDiem), XANH, false));
        khuTomTat.add(Box.createVerticalStrut(8));
        JLabel nhanTra = nhan("KHÁCH PHẢI TRẢ", Font.BOLD, 12, CHU_XAM);
        nhanTra.setAlignmentX(Component.LEFT_ALIGNMENT);
        khuTomTat.add(nhanTra);
        JLabel tong = nhan(tien(gia.khachPhaiTra), Font.BOLD, 28, NAU_DAM);
        tong.setAlignmentX(Component.LEFT_ALIGNMENT);
        khuTomTat.add(tong);
        if (gia.khach != null) {
            JLabel d = nhan("Tích lũy +" + gia.diemSeTich + " điểm", Font.PLAIN, 12, CHU_XAM);
            d.setAlignmentX(Component.LEFT_ALIGNMENT);
            khuTomTat.add(d);
        }
        khuTomTat.revalidate();
        khuTomTat.repaint();

        capNhatQR();
        capNhatThua();
        capNhatNut();
    }

    private JPanel dongTomTat(String trai, String phai, Color mauPhai, boolean cachTren) {
        JPanel d = new JPanel(new BorderLayout(8, 0));
        d.setOpaque(false);
        d.setBorder(BorderFactory.createEmptyBorder(cachTren ? 8 : 2, 0, 2, 0));
        d.setAlignmentX(Component.LEFT_ALIGNMENT);
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        d.add(nhan(trai, Font.PLAIN, 13, CHU_DEN), BorderLayout.CENTER);
        d.add(nhan(phai, cachTren ? Font.BOLD : Font.PLAIN, 13, mauPhai), BorderLayout.EAST);
        return d;
    }

    // ===================== QR CHUYỂN KHOẢN (dùng util.VietQR có sẵn) =====================
    private void capNhatQR() {
        boolean hien = rbChuyenKhoan.isSelected() && gia != null;
        khuQR.setVisible(hien);
        if (!hien || qrSoTien == gia.khachPhaiTra) return;
        try {
            Image anh = util.VietQR.taoAnhQR(util.VietQR.taoChuoi(gia.khachPhaiTra, "VE " + maSuat), 200);
            lblQR.setIcon(new ImageIcon(anh));
            lblQR.setText("");
            lblQRChu.setText("Khách quét mã để chuyển " + tien(gia.khachPhaiTra));
            qrSoTien = gia.khachPhaiTra;
        } catch (Exception ex) {
            ex.printStackTrace();   // loi tao QR thi van cho xac nhan thu cong
            lblQR.setIcon(null);
            lblQR.setText("Không tạo được mã QR");
            lblQRChu.setText(" ");
        }
    }

    // ===================== TIỀN THỪA / NÚT =====================
    private void capNhatThua() {
        if (!rbTienMat.isSelected() || gia == null) return;
        long dua = docTien(txtTienDua.getText());
        if (txtTienDua.getText().trim().isEmpty()) {
            lblThua.setText(" ");
        } else if (dua >= gia.khachPhaiTra) {
            lblThua.setText("Tiền thừa: " + tien(dua - gia.khachPhaiTra));
            lblThua.setForeground(XANH);
        } else {
            lblThua.setText("Còn thiếu: " + tien(gia.khachPhaiTra - dua));
            lblThua.setForeground(DO);
        }
    }

    private void capNhatNut() {
        boolean duTuoi = !DoTuoiController.canKiemTraTuoi(doTuoi) || chkTuoi.isSelected();
        boolean duTien = !rbTienMat.isSelected() || (gia != null && docTien(txtTienDua.getText()) >= gia.khachPhaiTra);
        btnXacNhan.setEnabled(!dangTinh && !dangThanhToan && gia != null && duTuoi && duTien);
    }

    // ===================== THÀNH VIÊN / MÃ =====================
    private void timThanhVien() {
        String sdt = txtSdt.getText().trim();
        if (sdt.isEmpty()) {
            sdtDangDung = null;
            dangNap = true;
            spDiem.setValue(0);
            dangNap = false;
            capNhatGia();
            return;
        }
        if (!KhachHangDAO.sdtHopLe(sdt)) {
            lblKhach.setText("Số điện thoại gồm 10 chữ số, bắt đầu bằng 0");
            lblKhach.setForeground(DO);
            return;
        }
        sdtDangDung = sdt;
        dangNap = true;
        spDiem.setValue(0);
        dangNap = false;
        capNhatGia();
    }

    private void dangKyNhanh() {
        final String sdt = sdtDangDung;
        if (sdt == null) return;
        String ten = JOptionPane.showInputDialog(this, "Họ tên khách hàng (SĐT " + sdt + "):",
                "Đăng ký thành viên", JOptionPane.QUESTION_MESSAGE);
        if (ten == null) return;
        final String hoTen = ten.trim();
        if (hoTen.isEmpty() || hoTen.length() > 100) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập họ tên (tối đa 100 ký tự).",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }
        chayNen(() -> controller.dangKyNhanh(sdt, hoTen), k -> capNhatGia(), loi -> {
            if (loi instanceof SdtTrungException) lblLoi.setText(loi.getMessage());
            else lblLoi.setText("Lỗi đăng ký: " + loi.getMessage());
        });
    }

    private void apDungMa() {
        String ma = txtMa.getText().trim();
        maDangDung = ma.isEmpty() ? null : ma;
        capNhatGia();
    }

    // ===================== XÁC NHẬN =====================
    private void xacNhan() {
        if (!txtSdt.getText().trim().isEmpty() && !txtSdt.getText().trim().equals(sdtDangDung)) {
            JOptionPane.showMessageDialog(this, "Bạn đã nhập số điện thoại nhưng chưa bấm \"Tìm\".\n"
                    + "Hãy bấm Tìm để áp dụng thành viên, hoặc xóa số điện thoại nếu khách vãng lai.",
                    "Chưa áp dụng thành viên", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final YeuCau y = taoYeuCau();
        dangThanhToan = true;
        capNhatNut();
        lblLoi.setText("Đang xử lý...");
        chayNen(() -> controller.thanhToan(y, idNhanVien), kq -> {
            ketQua = kq;
            hienThanhCong(kq, y);
            dispose();
        }, loi -> {
            dangThanhToan = false;
            if (loi instanceof GheDaBanException) {
                gheBiBan = true;
                JOptionPane.showMessageDialog(this, "Có ghế vừa được bán ở quầy khác.\nVui lòng chọn lại ghế.",
                        "Ghế đã được bán", JOptionPane.WARNING_MESSAGE);
                dispose();
            } else if (loi instanceof TienKhachDuaKhongDuException) {
                lblLoi.setText(loi.getMessage());
                capNhatGia();   // gia co the doi (diem thanh vien da bi dung o quay khac)
            } else {
                lblLoi.setText("Lỗi: " + loi.getMessage());
                capNhatNut();
            }
        });
    }

    private void hienThanhCong(KetQuaThanhToan kq, YeuCau y) {
        KetQuaGia g = kq.gia;
        StringBuilder sb = new StringBuilder("Thanh toán thành công!\n\nMã hóa đơn: " + kq.maHd);
        sb.append("\nKhách đã trả: ").append(tien(g.khachPhaiTra));
        sb.append("\nHình thức: ").append(tenPhuongThuc(y.phuongThuc));
        if (ThanhToanController.TIEN_MAT.equals(y.phuongThuc)) {
            sb.append("\nTiền khách đưa: ").append(tien(y.tienKhachDua));
            sb.append("\nTIỀN THỪA TRẢ KHÁCH: ").append(tien(kq.tienThua));
        }
        if (g.tongGiam > 0) sb.append("\nĐã giảm: ").append(tien(g.tongGiam));
        if (kq.khachSau != null) {
            sb.append("\n\nThành viên ").append(kq.khachSau.hoTen).append(": +").append(g.diemSeTich)
              .append(" điểm, hiện có ").append(so.format(kq.khachSau.diem)).append(" điểm (hạng ")
              .append(KhachHangDAO.tenHang(kq.khachSau.hang)).append(")");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String tenPhuongThuc(String pt) {
        if (ThanhToanController.CHUYEN_KHOAN.equals(pt)) return "Chuyển khoản";
        if (ThanhToanController.THE.equals(pt)) return "Thẻ";
        return "Tiền mặt";
    }

    /** Panel trong khung cuon: luon bang be rong khung nhin nen khong bi tran ngang. */
    private static class CotCuon extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 64; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ===================== TIỆN ÍCH =====================
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
                    if (!(goc instanceof GheDaBanException) && !(goc instanceof TienKhachDuaKhongDuException)) goc.printStackTrace();
                    khiLoi.accept(goc);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private static long docTien(String chu) {
        String so = (chu == null) ? "" : chu.replaceAll("[^0-9]", "");
        if (so.isEmpty() || so.length() > 12) return 0;
        return Long.parseLong(so);
    }

    private String tien(long v) {
        return so.format(v) + "đ";
    }

    private static String htmlRong(String text, int px) {
        String an = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><div style='width:" + px + "px'>" + an + "</div></html>";
    }

    private static JLabel nhan(String text, int kieu, int co, Color mau) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, kieu, co));
        l.setForeground(mau);
        return l;
    }

    private static JLabel tieuMuc(String text) {
        JLabel l = nhan(text, Font.BOLD, 14, CHU_DEN);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return l;
    }

    private static JButton nutPhu(String chu) {
        JButton b = new JButton(chu);
        b.setFont(new Font(FONT, Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setBackground(Color.WHITE);
        b.setForeground(CHU_DEN);
        b.setMargin(new Insets(2, 10, 2, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    private static JComponent canTrai(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JPanel hang(JTextField o, JButton nut) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        o.setFont(new Font(FONT, Font.PLAIN, 14));
        o.setPreferredSize(new Dimension(200, 34));
        nut.setPreferredSize(new Dimension(86, 34));
        p.add(o, BorderLayout.CENTER);
        p.add(nut, BorderLayout.EAST);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return p;
    }
}