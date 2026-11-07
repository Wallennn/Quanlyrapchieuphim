package view;

import config.DBConnection;
import controller.PhimController;
import model.Phim;
import model.SuatChieu;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Man hinh "Danh sach phim" kieu TRANG CHU WEBSITE rap phim:
 *  - Banner lon (carousel tu chuyen) gioi thieu cac phim dang chieu hom nay: nen mo tu poster, gio chieu, nut "Dat ve ngay".
 *  - Luoi the poster (tu chia cot theo be rong cua so): do tuoi, thoi luong, the loai, cac gio chieu.
 *  - Bam vao the phim de xem chi tiet (mo ta, dao dien, ngay khoi chieu, gio chieu); bam gio chieu de mo man Ban ve.
 */
public class TrangPhimWeb extends JScrollPane {

    private static final String FONT = "Segoe UI";
    private static final Color NAU_DAM = new Color(0x8A, 0x6D, 0x4A);
    private static final Color NAU_NHAT = new Color(0xB2, 0x90, 0x6B);
    private static final Color NAU_TOI = new Color(0x6B, 0x52, 0x3B);
    private static final Color NEN_HEADER = new Color(0x3A, 0x30, 0x28);
    private static final Color CHU_DAM = new Color(0x2E, 0x2A, 0x26);
    private static final Color CHU_XAM = new Color(0x8A, 0x87, 0x82);
    private static final Color VIEN = new Color(0xEA, 0xDF, 0xCF);

    private static final int HERO_CAO = 340;
    private static final int GAP = 22;            // khoang cach giua cac the phim
    private static final int THE_MIN = 200;       // be rong toi thieu 1 the phim
    private static final int THE_MAX = 300;       // be rong toi da 1 the phim (cua so qua rong thi can giua)
    private static final int INFO_CAO = 168;      // chieu cao phan thong tin (duoi poster)
    private static final DateTimeFormatter HH = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] THU = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ nhật"};

    private final Consumer<String> moBanVe;   // nhan ma suat -> mo man Ban ve
    private final Map<String, String> doTuoiTheoPhim;

    /** 1 phim + cac suat chieu cua 1 ngay. */
    private static final class Muc {
        final Phim phim;
        final List<SuatChieu> suat;
        final String doTuoi;
        final String nhanNgay;   // "hôm nay" / "ngày mai"

        Muc(Phim phim, List<SuatChieu> suat, String doTuoi, String nhanNgay) {
            this.phim = phim;
            this.suat = suat;
            this.doTuoi = doTuoi;
            this.nhanNgay = nhanNgay;
        }
    }

    /** Thanh phan tinh chieu cao theo be rong (de trang tu xep lai khi doi kich thuoc cua so). */
    private interface ChieuCaoTheoRong {
        int cao(int rong);
    }

    public TrangPhimWeb(Consumer<String> moBanVe) {
        this.moBanVe = moBanVe;
        this.doTuoiTheoPhim = layDoTuoi();

        PhimController pc = new PhimController();
        List<Muc> homNay = new ArrayList<>();
        List<Muc> ngayMai = new ArrayList<>();
        String loiHomNay = null, loiNgayMai = null;

        try {
            for (Map.Entry<Phim, List<SuatChieu>> e : pc.layDanhSachPhimVaSuatChieuHomNay().entrySet()) {
                homNay.add(new Muc(e.getKey(), e.getValue(), doTuoiTheoPhim.get(e.getKey().getMaPhim()), "hôm nay"));
            }
        } catch (SQLException ex) {
            loiHomNay = "Lỗi khi tải phim hôm nay: " + ex.getMessage();
        }
        try {
            Set<String> daCo = new HashSet<>();
            for (Muc m : homNay) daCo.add(m.phim.getMaPhim());
            for (Map.Entry<Phim, List<SuatChieu>> e : pc.layDanhSachPhimVaSuatChieuNgayMai().entrySet()) {
                if (daCo.contains(e.getKey().getMaPhim())) continue;   // phim da chieu hom nay thi khong de xuat lai
                ngayMai.add(new Muc(e.getKey(), e.getValue(), doTuoiTheoPhim.get(e.getKey().getMaPhim()), "ngày mai"));
            }
        } catch (SQLException ex) {
            loiNgayMai = "Lỗi khi tải phim ngày mai: " + ex.getMessage();
        }

        // Banner: toi da 5 phim hom nay (neu hom nay khong co thi lay phim ngay mai)
        List<Muc> nguonBanner = homNay.isEmpty() ? ngayMai : homNay;
        List<Muc> slides = new ArrayList<>(nguonBanner.subList(0, Math.min(5, nguonBanner.size())));

        Trang trang = new Trang();
        trang.add(new Hero(slides));
        trang.add(khoangTrong(26));

        LocalDate ngay = LocalDate.now();
        trang.add(new TieuDeMuc("Đang chiếu hôm nay", dinhDangNgay(ngay) + "  •  " + homNay.size() + " phim"));
        trang.add(khoangTrong(8));
        trang.add(noiDungMuc(homNay, loiHomNay, "Hôm nay không có phim nào chiếu."));
        trang.add(khoangTrong(26));

        LocalDate mai = ngay.plusDays(1);
        trang.add(new TieuDeMuc("Đề xuất - chiếu ngày mai", dinhDangNgay(mai) + "  •  " + ngayMai.size() + " phim"));
        trang.add(khoangTrong(8));
        trang.add(noiDungMuc(ngayMai, loiNgayMai, "Chưa có phim mới nào cho ngày mai."));
        trang.add(khoangTrong(24));

        setViewportView(trang);
        setBorder(null);
        setOpaque(false);
        getViewport().setOpaque(false);
        getVerticalScrollBar().setUnitIncrement(24);
    }

    private static String dinhDangNgay(LocalDate d) {
        return THU[d.getDayOfWeek().getValue() - 1] + ", " + d.format(NGAY);
    }

    private Component noiDungMuc(List<Muc> ds, String loi, String khiTrong) {
        if (loi != null) return new ThongBao(loi, new Color(0xC6, 0x28, 0x28));
        if (ds.isEmpty()) return new ThongBao(khiTrong, CHU_XAM);
        return new LuoiPhim(ds);
    }

    private static Component khoangTrong(int cao) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(1, cao));
        return p;
    }

    private static Map<String, String> layDoTuoi() {
        Map<String, String> kq = new HashMap<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT ma_phim, do_tuoi FROM phim")) {
            while (rs.next()) {
                kq.put(rs.getString(1), rs.getString(2));
            }
        } catch (SQLException e) {
            // khong co do tuoi thi bo qua nhan do tuoi
        }
        return kq;
    }

    // =====================================================================
    //  TRANG (noi dung cuon): xep doc, tracks be rong viewport
    // =====================================================================
    private static final class Trang extends JPanel implements Scrollable {
        Trang() {
            setOpaque(false);
            setLayout(new LayoutManager() {
                @Override public void addLayoutComponent(String n, Component c) { }
                @Override public void removeLayoutComponent(Component c) { }

                private int rongHienTai(Container c) {
                    Container cha = c.getParent();
                    int w = (cha instanceof JViewport) ? cha.getWidth() : c.getWidth();
                    return w > 0 ? w : 1000;
                }

                private int cao(Component c, int w) {
                    return (c instanceof ChieuCaoTheoRong) ? ((ChieuCaoTheoRong) c).cao(w) : c.getPreferredSize().height;
                }

                @Override public Dimension preferredLayoutSize(Container c) {
                    int w = rongHienTai(c), h = 0;
                    for (Component k : c.getComponents()) h += cao(k, w);
                    return new Dimension(w, h);
                }

                @Override public Dimension minimumLayoutSize(Container c) {
                    return new Dimension(300, 100);
                }

                @Override public void layoutContainer(Container c) {
                    int w = c.getWidth(), y = 0;
                    for (Component k : c.getComponents()) {
                        int h = cao(k, w);
                        k.setBounds(0, y, w, h);
                        y += h;
                    }
                }
            });
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return (int) (r.height * 0.9); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static final class ThongBao extends JLabel implements ChieuCaoTheoRong {
        ThongBao(String chu, Color mau) {
            super(chu);
            setFont(new Font(FONT, Font.PLAIN, 14));
            setForeground(mau);
        }

        @Override public int cao(int rong) { return 36; }
    }

    // =====================================================================
    //  TIEU DE MUC: thanh nau + ten muc + ngay/so phim
    // =====================================================================
    private static final class TieuDeMuc extends JComponent implements ChieuCaoTheoRong {
        private final String tieuDe, phuDe;

        TieuDeMuc(String tieuDe, String phuDe) {
            this.tieuDe = tieuDe;
            this.phuDe = phuDe;
        }

        @Override public int cao(int rong) { return 44; }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr.create();
            hint(g);
            g.setColor(NAU_NHAT);
            g.fillRoundRect(0, 8, 5, 28, 5, 5);
            g.setColor(CHU_DAM);
            g.setFont(new Font(FONT, Font.BOLD, 22));
            g.drawString(tieuDe, 16, 30);
            g.setFont(new Font(FONT, Font.PLAIN, 13));
            g.setColor(CHU_XAM);
            g.drawString(phuDe, getWidth() - g.getFontMetrics().stringWidth(phuDe), 29);
            g.setColor(VIEN);
            g.drawLine(0, 43, getWidth(), 43);
            g.dispose();
        }
    }

    // =====================================================================
    //  LUOI THE PHIM: tu chia so cot theo be rong
    // =====================================================================
    private final class LuoiPhim extends JPanel implements ChieuCaoTheoRong {
        LuoiPhim(List<Muc> ds) {
            setOpaque(false);
            for (Muc m : ds) add(new TheFilm(m));
            setLayout(new LayoutManager() {
                @Override public void addLayoutComponent(String n, Component c) { }
                @Override public void removeLayoutComponent(Component c) { }
                @Override public Dimension preferredLayoutSize(Container c) {
                    int w = c.getWidth() > 0 ? c.getWidth() : 1000;
                    return new Dimension(w, cao(w));
                }
                @Override public Dimension minimumLayoutSize(Container c) { return new Dimension(THE_MIN, 100); }
                @Override public void layoutContainer(Container c) {
                    int w = c.getWidth();
                    int cot = soCot(w), rong = rongThe(w, cot), caoThe = caoThe(rong);
                    int leTrai = Math.max(0, (w - (cot * rong + (cot - 1) * GAP)) / 2);   // can giua khi the da dat be rong toi da
                    int i = 0;
                    for (Component k : c.getComponents()) {
                        int hang = i / cot, vc = i % cot;
                        k.setBounds(leTrai + vc * (rong + GAP), hang * (caoThe + GAP), rong, caoThe);
                        i++;
                    }
                }
            });
        }

        // So cot: nhieu nhat co the theo be rong, nhung khong qua so phim -> the gian ra lap day hang (khong de trong ben phai)
        private int soCot(int w) {
            int toiDa = Math.max(1, (w + GAP) / (THE_MIN + GAP));
            return Math.max(1, Math.min(getComponentCount(), toiDa));
        }

        private int rongThe(int w, int cot) {
            return Math.min(THE_MAX, Math.max(THE_MIN - 40, (w - GAP * (cot - 1)) / cot));
        }

        private int caoThe(int rong) { return (int) (rong * 1.42) + INFO_CAO; }

        @Override public int cao(int w) {
            int n = getComponentCount();
            if (n == 0) return 0;
            int cot = soCot(w), rong = rongThe(w, cot);
            int hang = (n + cot - 1) / cot;
            return hang * caoThe(rong) + (hang - 1) * GAP;
        }
    }

    // =====================================================================
    //  THE PHIM (poster + thong tin + gio chieu)
    // =====================================================================
    private final class TheFilm extends JComponent {
        private final Muc muc;
        private final BufferedImage anh;
        private final List<ChipGio> chips = new ArrayList<>();
        private final ChipGio chipThem = new ChipGio("", KIEU_THUONG, false, 58, 26, null);
        private boolean hover;

        TheFilm(Muc muc) {
            this.muc = muc;
            this.anh = docPoster(muc.phim.getPoster());
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            for (SuatChieu sc : muc.suat) {
                ChipGio c = new ChipGio(sc.getGioBatDau().format(HH), KIEU_THUONG, daQua(sc), 58, 26,
                        () -> moBanVe.accept(sc.getMaSuat()));
                if (daQua(sc)) c.setToolTipText("Đã qua giờ chiếu");
                chips.add(c);
                add(c);
            }
            add(chipThem);
            chipThem.setVisible(false);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) {
                    Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), TheFilm.this);
                    hover = contains(p);
                    repaint();
                }
                @Override public void mouseClicked(MouseEvent e) { hienChiTiet(TheFilm.this, muc); }
            });
        }

        @Override public void doLayout() {
            int w = getWidth();
            int gap = 6, cw = 58, ch = 26;
            int x0 = 5 + 14, usable = w - 10 - 28;
            int moiHang = Math.max(1, (usable + gap) / (cw + gap));
            int toiDa = moiHang * 2;
            int n = chips.size();
            boolean them = n > toiDa;
            int hien = them ? toiDa - 1 : n;
            int yChip = 4 + (getHeight() - 12 - INFO_CAO) + 96;   // duoi ten phim + the loai
            for (int i = 0; i < n; i++) {
                ChipGio c = chips.get(i);
                c.setVisible(i < hien);
                c.setBounds(x0 + (i % moiHang) * (cw + gap), yChip + (i / moiHang) * (ch + 6), cw, ch);
            }
            chipThem.setVisible(them);
            if (them) {
                chipThem.setChu("+" + (n - hien));
                StringBuilder sb = new StringBuilder("Còn: ");
                for (int i = hien; i < n; i++) sb.append(i > hien ? ", " : "").append(muc.suat.get(i).getGioBatDau().format(HH));
                chipThem.setToolTipText(sb.toString());
                chipThem.setBounds(x0 + (hien % moiHang) * (cw + gap), yChip + (hien / moiHang) * (ch + 6), cw, ch);
            }
        }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr.create();
            hint(g);
            int rx = 5, ry = 4, rw = getWidth() - 10, rh = getHeight() - 12;
            int ph = rh - INFO_CAO;
            RoundRectangle2D.Float the = new RoundRectangle2D.Float(rx, ry, rw, rh, 18, 18);

            // bong do
            for (int i = 4; i >= 1; i--) {
                g.setColor(new Color(60, 40, 20, hover ? 16 : 9));
                g.fill(new RoundRectangle2D.Float(rx - i + 1, ry - i + 3, rw + 2 * i - 2, rh + 2 * i - 2, 18 + i, 18 + i));
            }
            g.setColor(Color.WHITE);
            g.fill(the);

            // poster (cat theo khung the, phong nhe khi re chuot)
            Shape clipCu = g.getClip();
            g.clip(the);
            if (anh != null) {
                veCover(g, anh, rx, ry, rw, ph, hover ? 1.06 : 1.0);
            } else {
                g.setPaint(new GradientPaint(0, ry, new Color(0xEA, 0xDF, 0xCF), 0, ry + ph, new Color(0xD3, 0xBC, 0x9A)));
                g.fillRect(rx, ry, rw, ph);
                String ch = muc.phim.getTenPhim() != null && !muc.phim.getTenPhim().isEmpty()
                        ? muc.phim.getTenPhim().substring(0, 1).toUpperCase() : "?";
                g.setColor(NAU_NHAT);
                g.setFont(new Font(FONT, Font.BOLD, 64));
                g.drawString(ch, rx + (rw - g.getFontMetrics().stringWidth(ch)) / 2, ry + ph / 2 + 22);
            }
            g.setPaint(new GradientPaint(0, ry + ph - 60, new Color(0, 0, 0, 0), 0, ry + ph, new Color(0, 0, 0, 90)));
            g.fillRect(rx, ry + ph - 60, rw, 60);
            g.setClip(clipCu);

            // nhan tren poster: do tuoi (trai) + thoi luong (phai)
            if (muc.doTuoi != null && !muc.doTuoi.isEmpty()) {
                veVien(g, muc.doTuoi, rx + 10, ry + 10, 22, DoTuoiChip.mauCua(muc.doTuoi), Color.WHITE,
                        new Font(FONT, Font.BOLD, 12));
            }
            String tl = muc.phim.getThoiLuong() + " phút";
            g.setFont(new Font(FONT, Font.BOLD, 12));
            int wtl = g.getFontMetrics().stringWidth(tl) + 22;
            veVien(g, tl, rx + rw - 10 - wtl, ry + ph - 32, 22, new Color(0, 0, 0, 150), Color.WHITE,
                    new Font(FONT, Font.BOLD, 12));

            // vien
            g.setColor(hover ? NAU_NHAT : VIEN);
            g.setStroke(new BasicStroke(hover ? 2f : 1f));
            g.draw(the);

            // ten phim (toi da 2 dong) + the loai
            int y = ry + ph + 26;
            g.setFont(new Font(FONT, Font.BOLD, 15));
            g.setColor(CHU_DAM);
            List<String> dong = ngat(muc.phim.getTenPhim(), g.getFontMetrics(), rw - 28, 2);
            for (String d : dong) {
                g.drawString(d, rx + 14, y);
                y += 19;
            }
            g.setFont(new Font(FONT, Font.PLAIN, 12));
            g.setColor(CHU_XAM);
            String theLoai = muc.phim.getTheLoai() != null ? muc.phim.getTheLoai() : "";
            g.drawString(catDuoi(theLoai, g.getFontMetrics(), rw - 28), rx + 14, y + 2);

            if (muc.suat.isEmpty()) {
                g.setFont(new Font(FONT, Font.ITALIC, 12));
                g.drawString("Không có suất chiếu", rx + 14, ry + ph + 96 + 17);
            }
            g.dispose();
        }
    }

    // =====================================================================
    //  CHIP GIO CHIEU / NUT DAT VE (dang vien thuoc)
    // =====================================================================
    private static final int KIEU_THUONG = 0, KIEU_TOI = 1, KIEU_CHINH = 2;

    private static final class ChipGio extends JComponent {
        private String chu;
        private final int kieu;
        private final boolean daQua;
        private boolean hover;

        ChipGio(String chu, int kieu, boolean daQua, int w, int h, Runnable khiBam) {
            this.chu = chu;
            this.kieu = kieu;
            this.daQua = daQua;
            Dimension d = new Dimension(w, h);
            setPreferredSize(d);
            setSize(d);
            if (khiBam != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { khiBam.run(); }
                });
            }
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setChu(String chu) {
            this.chu = chu;
            repaint();
        }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr.create();
            hint(g);
            int w = getWidth(), h = getHeight();
            Color nen, vien, mau;
            if (kieu == KIEU_CHINH) {
                nen = hover ? NAU_DAM : NAU_NHAT;
                vien = nen;
                mau = Color.WHITE;
            } else if (kieu == KIEU_TOI) {
                nen = hover ? Color.WHITE : new Color(255, 255, 255, 30);
                vien = new Color(255, 255, 255, daQua ? 90 : 190);
                mau = hover ? CHU_DAM : new Color(255, 255, 255, daQua ? 150 : 255);
            } else {
                nen = hover && !daQua ? NAU_DAM : Color.WHITE;
                vien = daQua ? new Color(0xD8, 0xD2, 0xC8) : NAU_NHAT;
                mau = hover && !daQua ? Color.WHITE : (daQua ? new Color(0xA8, 0xA3, 0x9B) : NAU_DAM);
            }
            g.setColor(nen);
            g.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            g.setColor(vien);
            g.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            g.setColor(mau);
            g.setFont(new Font(FONT, kieu == KIEU_CHINH ? Font.BOLD : Font.PLAIN, kieu == KIEU_CHINH ? 14 : 13));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(chu, (w - fm.stringWidth(chu)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    private static boolean daQua(SuatChieu sc) {
        return sc.getNgayChieu() != null && sc.getNgayChieu().equals(LocalDate.now())
                && sc.getGioBatDau().isBefore(LocalTime.now());
    }

    // =====================================================================
    //  BANNER (carousel tu chuyen)
    // =====================================================================
    private final class Hero extends JComponent implements ChieuCaoTheoRong {
        private static final int PW = 190, PH = 270, LE_TRAI = 64, LE_PHAI = 72;

        private final List<Muc> slides;
        private final List<ChipGio> chips = new ArrayList<>();
        private int idx = 0;
        private final javax.swing.Timer timer;

        Hero(List<Muc> slides) {
            this.slides = slides;
            timer = new javax.swing.Timer(6500, e -> {
                if (this.slides.size() > 1) datSlide((idx + 1) % this.slides.size());
            });

            addHierarchyListener(e -> {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing() && this.slides.size() > 1) timer.start(); else timer.stop();
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { timer.stop(); }
                @Override public void mouseExited(MouseEvent e) {
                    Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), Hero.this);
                    if (!contains(p) && isShowing() && Hero.this.slides.size() > 1) timer.start();
                }
                @Override public void mouseClicked(MouseEvent e) { khiBam(e.getPoint()); }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    boolean tay = trungMuc(e.getPoint()) != 0;
                    setCursor(Cursor.getPredefinedCursor(tay ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
            });
            if (!slides.isEmpty()) datSlide(0);
        }

        @Override public int cao(int rong) { return HERO_CAO; }

        private void datSlide(int i) {
            idx = i;
            for (ChipGio c : chips) remove(c);
            chips.clear();
            Muc m = slides.get(i);
            int dem = 0;
            SuatChieu dauTien = null;
            for (SuatChieu sc : m.suat) {
                if (dem >= 6) break;
                ChipGio c = new ChipGio(sc.getGioBatDau().format(HH), KIEU_TOI, daQua(sc), 64, 32,
                        () -> moBanVe.accept(sc.getMaSuat()));
                chips.add(c);
                add(c);
                if (dauTien == null && !daQua(sc)) dauTien = sc;
                dem++;
            }
            if (dauTien == null && !m.suat.isEmpty()) dauTien = m.suat.get(0);
            if (dauTien != null) {
                final SuatChieu chon = dauTien;
                ChipGio nut = new ChipGio("Đặt vé ngay", KIEU_CHINH, false, 140, 36, () -> moBanVe.accept(chon.getMaSuat()));
                chips.add(nut);
                add(nut);
            }
            doLayout();
            repaint();
        }

        @Override public void doLayout() {
            int y = getHeight() - 78;
            int x = LE_TRAI;
            for (ChipGio c : chips) {
                boolean nut = c.getPreferredSize().width == 140;
                if (nut) x += 8;
                c.setBounds(x, nut ? y - 2 : y, c.getPreferredSize().width, c.getPreferredSize().height);
                x += c.getPreferredSize().width + 8;
            }
        }

        // 0 = khong trung; -1 = mui ten trai; 1 = mui ten phai; 2 = poster; 100+i = cham thu i
        private int trungMuc(Point p) {
            if (slides.isEmpty()) return 0;
            int w = getWidth(), h = getHeight();
            if (slides.size() > 1) {
                if (p.distance(30, h / 2.0) <= 22) return -1;
                if (p.distance(w - 30, h / 2.0) <= 22) return 1;
                int x0 = w / 2 - (slides.size() * 18) / 2;
                for (int i = 0; i < slides.size(); i++) {
                    if (p.distance(x0 + i * 18 + 9, h - 18) <= 9) return 100 + i;
                }
            }
            int px = w - LE_PHAI - PW, py = (h - PH) / 2;
            if (new Rectangle(px, py, PW, PH).contains(p)) return 2;
            return 0;
        }

        private void khiBam(Point p) {
            int k = trungMuc(p);
            if (k == -1) datSlide((idx - 1 + slides.size()) % slides.size());
            else if (k == 1) datSlide((idx + 1) % slides.size());
            else if (k >= 100) datSlide(k - 100);
            else if (k == 2) hienChiTiet(this, slides.get(idx));
        }

        @Override protected void paintComponent(Graphics gr) {
            Graphics2D g = (Graphics2D) gr.create();
            hint(g);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D.Float khung = new RoundRectangle2D.Float(0, 0, w, h, 26, 26);
            g.clip(khung);

            if (slides.isEmpty()) {
                g.setPaint(new GradientPaint(0, 0, NEN_HEADER, w, h, NAU_TOI));
                g.fillRect(0, 0, w, h);
                g.setColor(Color.WHITE);
                g.setFont(new Font(FONT, Font.BOLD, 26));
                String s = "Hôm nay chưa có suất chiếu";
                g.drawString(s, (w - g.getFontMetrics().stringWidth(s)) / 2, h / 2);
                g.dispose();
                return;
            }

            Muc m = slides.get(idx);
            BufferedImage anh = docPoster(m.phim.getPoster());

            // nen: poster lam mo + lop phu toi
            if (anh != null) {
                veCover(g, lamMo(anh), 0, 0, w, h, 1.15);
            } else {
                g.setPaint(new GradientPaint(0, 0, NEN_HEADER, w, h, NAU_TOI));
                g.fillRect(0, 0, w, h);
            }
            g.setPaint(new GradientPaint(0, 0, new Color(24, 18, 12, 238), w, 0, new Color(24, 18, 12, 120)));
            g.fillRect(0, 0, w, h);
            g.setPaint(new GradientPaint(0, h - 120, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 120)));
            g.fillRect(0, h - 120, w, 120);

            // poster ben phai
            int px = w - LE_PHAI - PW, py = (h - PH) / 2;
            RoundRectangle2D.Float khungP = new RoundRectangle2D.Float(px, py, PW, PH, 16, 16);
            for (int i = 6; i >= 1; i--) {
                g.setColor(new Color(0, 0, 0, 22));
                g.fill(new RoundRectangle2D.Float(px - i, py - i + 6, PW + 2 * i, PH + 2 * i, 16 + i, 16 + i));
            }
            Shape clipCu = g.getClip();
            g.clip(khungP);
            if (anh != null) {
                veCover(g, anh, px, py, PW, PH, 1.0);
            } else {
                g.setColor(new Color(0xEA, 0xDF, 0xCF));
                g.fillRect(px, py, PW, PH);
            }
            g.setClip(clipCu);
            g.setColor(new Color(255, 255, 255, 120));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(khungP);

            // chu ben trai
            int x0 = LE_TRAI, textW = Math.min(px - 36 - x0, 680);
            g.setColor(new Color(0xE3, 0xC8, 0x9C));
            g.setFont(new Font(FONT, Font.BOLD, 12));
            g.fillOval(x0, 42, 8, 8);
            g.drawString(("ĐANG CHIẾU " + m.nhanNgay).toUpperCase(), x0 + 16, 50);

            g.setColor(Color.WHITE);
            g.setFont(new Font(FONT, Font.BOLD, 34));
            List<String> tieuDe = ngat(m.phim.getTenPhim(), g.getFontMetrics(), textW, 2);
            int y = 92;
            for (String d : tieuDe) {
                g.drawString(d, x0, y);
                y += 40;
            }
            y -= 6;

            // do tuoi + the loai + thoi luong
            int x = x0;
            if (m.doTuoi != null && !m.doTuoi.isEmpty()) {
                x += veVien(g, m.doTuoi, x, y - 16, 22, DoTuoiChip.mauCua(m.doTuoi), Color.WHITE,
                        new Font(FONT, Font.BOLD, 12)) + 10;
            }
            g.setFont(new Font(FONT, Font.PLAIN, 14));
            g.setColor(new Color(0xE6, 0xDC, 0xCB));
            String meta = (m.phim.getTheLoai() != null ? m.phim.getTheLoai() + "  •  " : "") + m.phim.getThoiLuong() + " phút";
            g.drawString(catDuoi(meta, g.getFontMetrics(), textW - (x - x0)), x, y);

            // mo ta
            String moTa = m.phim.getMoTa();
            if (moTa != null && !moTa.isBlank() && !moTa.toLowerCase().contains("cập nhật")) {
                g.setFont(new Font(FONT, Font.PLAIN, 14));
                g.setColor(new Color(0xD8, 0xCF, 0xC0));
                int dongToiDa = tieuDe.size() > 1 ? 2 : 3;
                int yy = y + 28;
                for (String d : ngat(moTa, g.getFontMetrics(), textW, dongToiDa)) {
                    g.drawString(d, x0, yy);
                    yy += 21;
                }
            }

            // mui ten + cham chuyen slide
            if (slides.size() > 1) {
                veMuiTen(g, 30, h / 2, true);
                veMuiTen(g, w - 30, h / 2, false);
                int dx = w / 2 - (slides.size() * 18) / 2;
                for (int i = 0; i < slides.size(); i++) {
                    g.setColor(i == idx ? Color.WHITE : new Color(255, 255, 255, 90));
                    int r = i == idx ? 10 : 8;
                    g.fillOval(dx + i * 18 + 9 - r / 2, h - 18 - r / 2, r, r);
                }
            }
            g.dispose();
        }

        private void veMuiTen(Graphics2D g, int cx, int cy, boolean trai) {
            g.setColor(new Color(255, 255, 255, 45));
            g.fillOval(cx - 20, cy - 20, 40, 40);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int d = trai ? 1 : -1;
            g.drawLine(cx + 4 * d, cy - 9, cx - 4 * d, cy);
            g.drawLine(cx - 4 * d, cy, cx + 4 * d, cy + 9);
        }
    }

    // =====================================================================
    //  CHI TIET PHIM (hop thoai khi bam vao the / poster banner)
    // =====================================================================
    private void hienChiTiet(Component goc, Muc m) {
        Window cha = SwingUtilities.getWindowAncestor(goc);
        JDialog dlg = new JDialog(cha, m.phim.getTenPhim(), Dialog.ModalityType.APPLICATION_MODAL);

        BufferedImage anh = docPoster(m.phim.getPoster());
        JComponent poster = new JComponent() {
            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g = (Graphics2D) gr.create();
                hint(g);
                RoundRectangle2D.Float k = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16);
                g.clip(k);
                if (anh != null) {
                    veCover(g, anh, 0, 0, getWidth(), getHeight(), 1.0);
                } else {
                    g.setColor(new Color(0xEA, 0xDF, 0xCF));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                g.dispose();
            }
        };
        poster.setPreferredSize(new Dimension(240, 350));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel lblTen = new JLabel("<html><div style='width:360px'>" + thoat(m.phim.getTenPhim()) + "</div></html>");
        lblTen.setFont(new Font(FONT, Font.BOLD, 24));
        lblTen.setForeground(CHU_DAM);
        lblTen.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(lblTen);
        info.add(Box.createVerticalStrut(10));

        JPanel hangMeta = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        hangMeta.setOpaque(false);
        hangMeta.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (m.doTuoi != null && !m.doTuoi.isEmpty()) hangMeta.add(new DoTuoiChip(m.doTuoi));
        JLabel lblMeta = new JLabel((m.phim.getTheLoai() != null ? m.phim.getTheLoai() + "  •  " : "")
                + m.phim.getThoiLuong() + " phút");
        lblMeta.setFont(new Font(FONT, Font.PLAIN, 13));
        lblMeta.setForeground(CHU_XAM);
        hangMeta.add(lblMeta);
        info.add(hangMeta);
        info.add(Box.createVerticalStrut(10));

        info.add(dongChiTiet("Đạo diễn", m.phim.getDaoDien()));
        if (m.phim.getNgayKhoiChieu() != null) {
            info.add(dongChiTiet("Khởi chiếu", m.phim.getNgayKhoiChieu().format(NGAY)));
        }
        info.add(Box.createVerticalStrut(8));

        JTextArea moTa = new JTextArea(m.phim.getMoTa() != null ? m.phim.getMoTa() : "");
        moTa.setLineWrap(true);
        moTa.setWrapStyleWord(true);
        moTa.setEditable(false);
        moTa.setOpaque(false);
        moTa.setFont(new Font(FONT, Font.PLAIN, 13));
        moTa.setForeground(CHU_DAM);
        JScrollPane cuon = new JScrollPane(moTa);
        cuon.setBorder(null);
        cuon.setOpaque(false);
        cuon.getViewport().setOpaque(false);
        cuon.setAlignmentX(Component.LEFT_ALIGNMENT);
        cuon.setPreferredSize(new Dimension(380, 100));
        cuon.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        info.add(cuon);
        info.add(Box.createVerticalStrut(12));

        JLabel lblSuat = new JLabel("Suất chiếu " + m.nhanNgay + ":");
        lblSuat.setFont(new Font(FONT, Font.BOLD, 13));
        lblSuat.setForeground(NAU_DAM);
        lblSuat.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(lblSuat);
        info.add(Box.createVerticalStrut(6));

        JPanel hangGio = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hangGio.setOpaque(false);
        hangGio.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel gioWrap = new JPanel(new GridLayout(0, 5, 8, 8));
        gioWrap.setOpaque(false);
        for (SuatChieu sc : m.suat) {
            ChipGio c = new ChipGio(sc.getGioBatDau().format(HH), KIEU_THUONG, daQua(sc), 64, 30, () -> {
                dlg.dispose();
                moBanVe.accept(sc.getMaSuat());
            });
            c.setPreferredSize(new Dimension(64, 30));
            gioWrap.add(c);
        }
        hangGio.add(gioWrap);
        info.add(hangGio);
        info.add(Box.createVerticalGlue());

        JButton dong = new JButton("Đóng");
        dong.setFocusPainted(false);
        dong.addActionListener(e -> dlg.dispose());
        JPanel chan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        chan.setOpaque(false);
        chan.add(dong);

        JPanel goc2 = new JPanel(new BorderLayout(24, 0));
        goc2.setBackground(Color.WHITE);
        goc2.setBorder(BorderFactory.createEmptyBorder(24, 24, 18, 24));
        goc2.add(poster, BorderLayout.WEST);
        goc2.add(info, BorderLayout.CENTER);
        goc2.add(chan, BorderLayout.SOUTH);

        dlg.setContentPane(goc2);
        dlg.pack();
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(cha);
        dlg.setVisible(true);
    }

    private static JComponent dongChiTiet(String nhan, String giaTri) {
        JLabel l = new JLabel("<html><span style='color:#8A8782'>" + nhan + ":</span> "
                + thoat(giaTri == null || giaTri.isBlank() ? "Đang cập nhật" : giaTri) + "</html>");
        l.setFont(new Font(FONT, Font.PLAIN, 13));
        l.setForeground(CHU_DAM);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        return l;
    }

    private static String thoat(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // =====================================================================
    //  TIEN ICH VE
    // =====================================================================
    private static void hint(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    /** Ve nhan dang vien thuoc, tra ve be rong da ve. */
    private static int veVien(Graphics2D g, String chu, int x, int y, int h, Color nen, Color mau, Font f) {
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(chu) + 18;
        g.setColor(nen);
        g.fillRoundRect(x, y, w, h, h, h);
        g.setColor(mau);
        g.drawString(chu, x + 9, y + (h - fm.getHeight()) / 2 + fm.getAscent());
        return w;
    }

    /** Ve anh phu kin khung (cat bot phan thua), zoom >= 1 de phong to nhe. */
    private static void veCover(Graphics2D g0, BufferedImage a, int x, int y, int w, int h, double zoom) {
        Graphics2D g = (Graphics2D) g0.create();
        g.clipRect(x, y, w, h);
        double t = Math.max((double) w / a.getWidth(), (double) h / a.getHeight()) * zoom;
        int dw = (int) Math.round(a.getWidth() * t), dh = (int) Math.round(a.getHeight() * t);
        g.drawImage(a, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, null);
        g.dispose();
    }

    /** Ngat chu thanh toi da n dong theo be rong, dong cuoi them "..." neu con du. */
    private static List<String> ngat(String s, FontMetrics fm, int rongToiDa, int soDongToiDa) {
        List<String> dong = new ArrayList<>();
        if (s == null || s.isBlank()) return dong;
        StringBuilder cur = new StringBuilder();
        boolean catBot = false;
        for (String t : s.trim().split("\\s+")) {
            String thu = cur.length() == 0 ? t : cur + " " + t;
            if (cur.length() == 0 || fm.stringWidth(thu) <= rongToiDa) {
                cur.setLength(0);
                cur.append(thu);
            } else {
                dong.add(cur.toString());
                cur.setLength(0);
                cur.append(t);
                if (dong.size() == soDongToiDa) {
                    catBot = true;
                    break;
                }
            }
        }
        if (!catBot && cur.length() > 0) dong.add(cur.toString());
        if (catBot && !dong.isEmpty()) {
            int cuoi = dong.size() - 1;
            dong.set(cuoi, dong.get(cuoi) + "…");
        }
        for (int i = 0; i < dong.size(); i++) dong.set(i, catDuoi(dong.get(i), fm, rongToiDa));
        return dong;
    }

    private static String catDuoi(String s, FontMetrics fm, int rongToiDa) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= rongToiDa) return s;
        int n = s.length();
        while (n > 1 && fm.stringWidth(s.substring(0, n) + "…") > rongToiDa) n--;
        return s.substring(0, n) + "…";
    }

    // =====================================================================
    //  POSTER (doc tu src/main/resources/images/posters/, co cache)
    // =====================================================================
    private static final Map<String, Optional<BufferedImage>> CACHE_ANH = new HashMap<>();
    private static final Map<BufferedImage, BufferedImage> CACHE_MO = new HashMap<>();

    private static BufferedImage docPoster(String tenFile) {
        if (tenFile == null || tenFile.trim().isEmpty()) return null;
        return CACHE_ANH.computeIfAbsent(tenFile.trim(), t -> Optional.ofNullable(docAnh(t))).orElse(null);
    }

    private static BufferedImage docAnh(String ten) {
        int cham = ten.lastIndexOf('.');
        String goc = cham > 0 ? ten.substring(0, cham) : ten;
        for (String t : new String[]{ten, goc + ".png", goc + ".jpg", goc + ".jpeg", goc + ".PNG", goc + ".JPG"}) {
            URL url = TrangPhimWeb.class.getResource("/images/posters/" + t);
            if (url == null) continue;
            try {
                BufferedImage a = ImageIO.read(url);
                if (a != null) return a;
            } catch (Exception e) {
                System.err.println("[POSTER] Loi doc file " + t + ": " + e.getMessage());
            }
        }
        return null;
    }

    /** Lam mo anh bang cach thu nho roi phong to (noi suy mem) - dung cho nen banner. */
    private static BufferedImage lamMo(BufferedImage a) {
        return CACHE_MO.computeIfAbsent(a, goc -> {
            int w = Math.max(8, goc.getWidth() / 18), h = Math.max(8, goc.getHeight() / 18);
            BufferedImage nho = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = nho.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(goc, 0, 0, w, h, null);
            g.dispose();
            return nho;
        });
    }
}