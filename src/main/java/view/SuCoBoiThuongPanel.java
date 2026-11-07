package view;

import dao.SuCoDAO;
import dao.SuCoDAO.DongXuLy;
import dao.SuCoDAO.HoaDonAnhHuong;
import dao.SuCoDAO.KetQuaXuLy;
import dao.SuCoDAO.LichSuSuCo;
import dao.SuCoDAO.LoaiSuCo;
import dao.SuCoDAO.SuatSuCo;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Man hinh "Su co & boi thuong": chon suat bi su co -> chon hoa don can xu ly
// (hoan tien hoac doi sang suat khac) -> xac nhan. Ben phai la lich su cac su co gan day.
public class SuCoBoiThuongPanel extends JPanel {

    private static final Color MAU_NAU_DAM    = new Color(0x8A, 0x6D, 0x4A);
    private static final Color MAU_NAU_TOI    = new Color(0x6B, 0x52, 0x3B);
    private static final Color MAU_NEN_KEM    = new Color(0xFB, 0xF6, 0xEF);
    private static final Color MAU_NEN_HEADER = new Color(0x3A, 0x30, 0x28);
    private static final Color MAU_CHU_XAM    = new Color(0x8A, 0x87, 0x82);
    private static final Color MAU_CHU_DAM    = new Color(0x2E, 0x2A, 0x26);
    private static final Color MAU_VIEN       = new Color(0xEA, 0xDF, 0xCF);
    private static final Color MAU_DO         = new Color(0xC6, 0x28, 0x28);

    private static final String HT_HOAN = "Hoàn tiền";
    private static final String HT_DOI = "Đổi suất khác";

    private static final int COT_CHON = 0;
    private static final int COT_HINH_THUC = 4;

    private final SuCoDAO dao;
    private final long idNhanVien;

    private final JComboBox<SuatSuCo> cboSuat = new JComboBox<>();
    private final JComboBox<LoaiSuCo> cboLoai = new JComboBox<>(LoaiSuCo.tatCa());
    private final JTextArea txtMoTa = new JTextArea(3, 20);
    private final JComboBox<SuatSuCo> cboSuatMoi = new JComboBox<>();
    private final JCheckBox chkTatCa = new JCheckBox("Chọn tất cả", true);
    private final JLabel lblTomTat = new JLabel(" ");
    private final JLabel lblThongBao = new JLabel(" ");
    private final JButton btnXacNhan = new JButton("Xác nhận xử lý sự cố");

    private final DefaultTableModel moHinhHoaDon;
    private final JTable bangHoaDon;
    private List<HoaDonAnhHuong> cacHoaDon = new ArrayList<>();

    private final DefaultTableModel moHinhLichSu;
    private final JTable bangLichSu;

    public SuCoBoiThuongPanel(long idNhanVien) {
        this(idNhanVien, new SuCoDAO());
    }

    SuCoBoiThuongPanel(long idNhanVien, SuCoDAO dao) {
        this.idNhanVien = idNhanVien;
        this.dao = dao;

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        moHinhHoaDon = new DefaultTableModel(new String[]{"Xử lý", "Mã HĐ", "Số vé", "Tổng tiền", "Hình thức"}, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == COT_CHON ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == COT_CHON || col == COT_HINH_THUC;
            }
        };
        bangHoaDon = new JTable(moHinhHoaDon);

        moHinhLichSu = new DefaultTableModel(new String[]{"Mã sự cố", "Phim", "Loại", "Số HĐ", "Đã hoàn", "Lúc xử lý"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        bangLichSu = new JTable(moHinhLichSu);

        JLabel lblTieuDe = new JLabel("Sự cố & bồi thường");
        lblTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTieuDe.setForeground(MAU_CHU_DAM);
        add(lblTieuDe, BorderLayout.NORTH);

        JPanel giua = new JPanel(new GridBagLayout());
        giua.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.gridy = 0;
        g.weighty = 1.0;
        g.gridx = 0;
        g.weightx = 0.54;
        g.insets = new Insets(0, 0, 0, 20);
        giua.add(taoPanelLapPhieu(), g);
        g.gridx = 1;
        g.weightx = 0.46;
        g.insets = new Insets(0, 0, 0, 0);
        giua.add(taoPanelLichSu(), g);
        add(giua, BorderLayout.CENTER);

        // Nap du lieu truoc, gan su kien sau (de viec them muc vao o chon khong kich hoat su kien)
        taiSuatHomNay();
        cboSuat.addActionListener(e -> taiTheoSuatDangChon());
        taiTheoSuatDangChon();
        taiLichSu();
    }

    // ===== Ben trai: lap phieu su co =====
    private JPanel taoPanelLapPhieu() {
        JPanel the = new JPanel(new BorderLayout(0, 10));
        the.setBackground(Color.WHITE);
        the.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        the.setPreferredSize(new Dimension(600, 100));

        // --- Phan tren: form ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 10, 0);
        JLabel lblTieuDe = new JLabel("Lập phiếu sự cố");
        lblTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTieuDe.setForeground(MAU_CHU_DAM);
        form.add(lblTieuDe, c);

        cboSuat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboLoai.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtMoTa.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtMoTa.setLineWrap(true);
        txtMoTa.setWrapStyleWord(true);
        JScrollPane cuonMoTa = new JScrollPane(txtMoTa);
        cuonMoTa.setBorder(BorderFactory.createLineBorder(MAU_VIEN, 1));

        themDong(form, 1, "Suất chiếu bị sự cố", cboSuat);
        themDong(form, 2, "Loại sự cố", cboLoai);
        themDong(form, 3, "Mô tả (nếu có)", cuonMoTa);
        the.add(form, BorderLayout.NORTH);

        // --- Giua: bang hoa don bi anh huong ---
        JPanel giua = new JPanel(new BorderLayout(0, 6));
        giua.setOpaque(false);

        JLabel lblBang = new JLabel("Hóa đơn bị ảnh hưởng — chọn hóa đơn cần xử lý và hình thức:");
        lblBang.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblBang.setForeground(MAU_CHU_XAM);
        chkTatCa.setOpaque(false);
        chkTatCa.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chkTatCa.addActionListener(e -> {
            for (int i = 0; i < moHinhHoaDon.getRowCount(); i++) {
                moHinhHoaDon.setValueAt(chkTatCa.isSelected(), i, COT_CHON);
            }
        });
        JPanel dongBang = new JPanel(new BorderLayout());
        dongBang.setOpaque(false);
        dongBang.add(lblBang, BorderLayout.WEST);
        dongBang.add(chkTatCa, BorderLayout.EAST);
        giua.add(dongBang, BorderLayout.NORTH);

        kieuBang(bangHoaDon);
        bangHoaDon.getColumnModel().getColumn(0).setPreferredWidth(50);
        bangHoaDon.getColumnModel().getColumn(1).setPreferredWidth(150);
        bangHoaDon.getColumnModel().getColumn(2).setPreferredWidth(55);
        bangHoaDon.getColumnModel().getColumn(3).setPreferredWidth(100);
        bangHoaDon.getColumnModel().getColumn(4).setPreferredWidth(130);
        JComboBox<String> cboHinhThuc = new JComboBox<>(new String[]{HT_HOAN, HT_DOI});
        cboHinhThuc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bangHoaDon.getColumnModel().getColumn(COT_HINH_THUC).setCellEditor(new DefaultCellEditor(cboHinhThuc));
        DefaultTableCellRenderer giuaO = new DefaultTableCellRenderer();
        giuaO.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i <= 4; i++) {
            bangHoaDon.getColumnModel().getColumn(i).setCellRenderer(giuaO);
        }
        moHinhHoaDon.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                capNhatTomTat();
            }
        });

        JScrollPane cuon = new JScrollPane(bangHoaDon);
        cuon.setBorder(BorderFactory.createLineBorder(MAU_VIEN, 1));
        cuon.getViewport().setBackground(Color.WHITE);
        giua.add(cuon, BorderLayout.CENTER);
        the.add(giua, BorderLayout.CENTER);

        // --- Duoi: suat doi sang + tom tat + nut ---
        JPanel duoi = new JPanel(new GridBagLayout());
        duoi.setOpaque(false);
        GridBagConstraints d = new GridBagConstraints();
        d.fill = GridBagConstraints.HORIZONTAL;
        d.anchor = GridBagConstraints.WEST;
        cboSuatMoi.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        themDong(duoi, 0, "Đổi sang suất", cboSuatMoi);

        lblTomTat.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTomTat.setForeground(MAU_NAU_TOI);
        d.gridx = 0;
        d.gridy = 1;
        d.gridwidth = 2;
        d.insets = new Insets(8, 0, 0, 0);
        duoi.add(lblTomTat, d);

        lblThongBao.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblThongBao.setForeground(MAU_DO);
        d.gridy = 2;
        d.insets = new Insets(2, 0, 0, 0);
        duoi.add(lblThongBao, d);

        btnXacNhan.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnXacNhan.setForeground(Color.WHITE);
        btnXacNhan.setBackground(MAU_NAU_TOI);
        btnXacNhan.setFocusPainted(false);
        btnXacNhan.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnXacNhan.addActionListener(e -> xacNhan());
        d.gridy = 3;
        d.ipady = 8;
        d.insets = new Insets(10, 0, 0, 0);
        duoi.add(btnXacNhan, d);

        the.add(duoi, BorderLayout.SOUTH);
        return the;
    }

    private void themDong(JPanel p, int dong, String nhan, JComponent o) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = dong;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 8, 12);
        JLabel lbl = new JLabel(nhan);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(MAU_CHU_XAM);
        p.add(lbl, c);

        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 8, 0);
        p.add(o, c);
    }

    // ===== Ben phai: lich su su co =====
    private JPanel taoPanelLichSu() {
        JPanel the = new JPanel(new BorderLayout(0, 10));
        the.setBackground(Color.WHITE);
        the.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAU_VIEN, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        the.setPreferredSize(new Dimension(520, 100));

        JLabel lblTieuDe = new JLabel("Sự cố gần đây");
        lblTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTieuDe.setForeground(MAU_CHU_DAM);
        the.add(lblTieuDe, BorderLayout.NORTH);

        kieuBang(bangLichSu);
        int[] doRong = {135, 130, 90, 50, 90, 105};
        for (int i = 0; i < doRong.length; i++) {
            bangLichSu.getColumnModel().getColumn(i).setPreferredWidth(doRong[i]);
        }
        DefaultTableCellRenderer giuaO = new DefaultTableCellRenderer();
        giuaO.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 2; i < doRong.length; i++) {
            bangLichSu.getColumnModel().getColumn(i).setCellRenderer(giuaO);
        }
        JScrollPane cuon = new JScrollPane(bangLichSu);
        cuon.setBorder(BorderFactory.createLineBorder(MAU_VIEN, 1));
        cuon.getViewport().setBackground(Color.WHITE);
        the.add(cuon, BorderLayout.CENTER);
        return the;
    }

    // Kieu chung cho hai bang: tieu de nau dam chu trang, dong cao 34
    private void kieuBang(JTable bang) {
        bang.setRowHeight(34);
        bang.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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
        bang.getTableHeader().setPreferredSize(new Dimension(0, 36));
    }

    // ===== Nap du lieu =====
    private void taiSuatHomNay() {
        try {
            for (SuatSuCo s : dao.laySuatHomNay()) {
                cboSuat.addItem(s);
            }
            if (cboSuat.getItemCount() == 0) {
                lblThongBao.setText("Hôm nay chưa có suất chiếu nào.");
            }
        } catch (SQLException e) {
            lblThongBao.setText("Lỗi khi tải suất chiếu: " + e.getMessage());
        }
    }

    // Khi doi suat bi su co: tai lai danh sach hoa don + danh sach suat co the doi sang
    private void taiTheoSuatDangChon() {
        SuatSuCo suat = (SuatSuCo) cboSuat.getSelectedItem();

        moHinhHoaDon.setRowCount(0);
        cacHoaDon = new ArrayList<>();
        cboSuatMoi.removeAllItems();

        if (suat != null) {
            try {
                cacHoaDon = dao.layHoaDonCoVe(suat.maSuat());
                DateTimeFormatter dinhDang = DateTimeFormatter.ofPattern("HH:mm dd/MM");
                for (HoaDonAnhHuong hd : cacHoaDon) {
                    moHinhHoaDon.addRow(new Object[]{
                        Boolean.TRUE, hd.maHd(), String.valueOf(hd.soVe()), dinhDangTien(hd.tongTien()) + "đ", HT_HOAN});
                }
                for (SuatSuCo s : dao.laySuatCoTheDoiSang(suat.maSuat())) {
                    cboSuatMoi.addItem(s);
                }
                lblThongBao.setText(cacHoaDon.isEmpty() ? "Suất này chưa có hóa đơn vé nào cần xử lý." : " ");
            } catch (SQLException e) {
                lblThongBao.setText("Lỗi khi tải dữ liệu: " + e.getMessage());
            }
        }
        chkTatCa.setSelected(true);
        capNhatTomTat();
    }

    private void taiLichSu() {
        moHinhLichSu.setRowCount(0);
        try {
            DateTimeFormatter dinhDang = DateTimeFormatter.ofPattern("HH:mm dd/MM");
            for (LichSuSuCo ls : dao.layLichSuGanDay()) {
                moHinhLichSu.addRow(new Object[]{
                    ls.maSuCo(),
                    ls.tenPhim() + " " + ls.gioBatDau().format(DateTimeFormatter.ofPattern("HH:mm")),
                    LoaiSuCo.tenTuMa(ls.loaiSuCo()),
                    ls.soHoaDon(),
                    dinhDangTien(ls.tongHoan()) + "đ",
                    ls.ngayXuLy() != null ? ls.ngayXuLy().format(dinhDang) : ""
                });
            }
        } catch (SQLException e) {
            lblThongBao.setText("Lỗi khi tải lịch sử: " + e.getMessage());
        }
    }

    // ===== Tom tat lua chon + bat/tat nut =====
    private void capNhatTomTat() {
        int soHoan = 0, soDoi = 0;
        double tienHoan = 0;
        for (int i = 0; i < moHinhHoaDon.getRowCount() && i < cacHoaDon.size(); i++) {
            if (!Boolean.TRUE.equals(moHinhHoaDon.getValueAt(i, COT_CHON))) continue;
            if (HT_DOI.equals(moHinhHoaDon.getValueAt(i, COT_HINH_THUC))) {
                soDoi++;
            } else {
                soHoan++;
                tienHoan += cacHoaDon.get(i).tongTien();
            }
        }

        cboSuatMoi.setEnabled(soDoi > 0);
        lblTomTat.setText(soHoan + soDoi == 0 ? " "
                : "Hoàn tiền: " + soHoan + " hóa đơn (" + dinhDangTien(tienHoan) + "đ)   •   Đổi suất: " + soDoi + " hóa đơn");

        boolean duocXacNhan = soHoan + soDoi > 0 && cboSuat.getSelectedItem() != null
                && (soDoi == 0 || cboSuatMoi.getItemCount() > 0);
        btnXacNhan.setEnabled(duocXacNhan);
        if (soDoi > 0 && cboSuatMoi.getItemCount() == 0) {
            lblThongBao.setText("Không có suất nào còn chiếu hôm nay hoặc ngày mai để đổi sang — hãy chọn hoàn tiền.");
        } else if (lblThongBao.getText().startsWith("Không có suất nào")) {
            lblThongBao.setText(" ");
        }
    }

    // ===== Xac nhan xu ly =====
    private void xacNhan() {
        if (bangHoaDon.isEditing()) {
            bangHoaDon.getCellEditor().stopCellEditing();   // chot gia tri o dang go/chon
        }
        SuatSuCo suat = (SuatSuCo) cboSuat.getSelectedItem();
        LoaiSuCo loai = (LoaiSuCo) cboLoai.getSelectedItem();
        SuatSuCo suatMoi = (SuatSuCo) cboSuatMoi.getSelectedItem();
        if (suat == null || loai == null) {
            return;
        }

        List<DongXuLy> cacDong = new ArrayList<>();
        int soHoan = 0, soDoi = 0;
        double tienHoan = 0;
        for (int i = 0; i < moHinhHoaDon.getRowCount() && i < cacHoaDon.size(); i++) {
            if (!Boolean.TRUE.equals(moHinhHoaDon.getValueAt(i, COT_CHON))) continue;
            HoaDonAnhHuong hd = cacHoaDon.get(i);
            if (HT_DOI.equals(moHinhHoaDon.getValueAt(i, COT_HINH_THUC))) {
                if (suatMoi == null) {
                    JOptionPane.showMessageDialog(this, "Hãy chọn suất để đổi sang.");
                    return;
                }
                cacDong.add(new DongXuLy(hd.maHd(), SuCoDAO.VE_SUAT_KHAC, suatMoi.maSuat()));
                soDoi++;
            } else {
                cacDong.add(new DongXuLy(hd.maHd(), SuCoDAO.HOAN_TIEN, null));
                soHoan++;
                tienHoan += hd.tongTien();
            }
        }
        if (cacDong.isEmpty()) {
            return;
        }

        StringBuilder hoi = new StringBuilder();
        hoi.append("Sự cố: ").append(loai.ten()).append("\nSuất: ").append(suat).append("\n\n");
        if (soHoan > 0) {
            hoi.append("• Hoàn tiền ").append(soHoan).append(" hóa đơn, tổng ").append(dinhDangTien(tienHoan)).append("đ\n");
        }
        if (soDoi > 0) {
            hoi.append("• Đổi ").append(soDoi).append(" hóa đơn sang suất: ").append(suatMoi).append("\n");
        }
        hoi.append("\nThao tác này không hoàn tác được. Xác nhận?");
        int chon = JOptionPane.showConfirmDialog(this, hoi.toString(), "Xác nhận xử lý sự cố",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (chon != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            KetQuaXuLy kq = dao.xuLySuCo(suat.maSuat(), loai.ma(), txtMoTa.getText(), idNhanVien, cacDong);
            JOptionPane.showMessageDialog(this,
                    "Đã ghi nhận sự cố " + kq.maSuCo() + "\n"
                            + "Hoàn tiền: " + kq.soHoanTien() + " hóa đơn (" + dinhDangTien(kq.tongHoan()) + "đ)\n"
                            + "Đổi suất: " + kq.soDoiSuat() + " hóa đơn",
                    "Xử lý thành công", JOptionPane.INFORMATION_MESSAGE);
            txtMoTa.setText("");
            taiTheoSuatDangChon();
            taiLichSu();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Không xử lý được, chưa có thay đổi nào được lưu:\n" + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 59000 -> "59.000"
    private static String dinhDangTien(double so) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(so);
    }
}