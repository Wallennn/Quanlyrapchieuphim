package view;

import controller.DoTuoiController;

import javax.swing.*;
import java.awt.*;

/** Nhan do tuoi phim (P, K, T13, T16, T18) dang "vien thuoc" co mau theo muc do. Re chuot vao de xem y nghia. */
public class DoTuoiChip extends JLabel {

    private final Color nen;

    public DoTuoiChip(String doTuoi) {
        super(doTuoi == null ? "" : doTuoi, SwingConstants.CENTER);
        this.nen = mauCua(doTuoi);
        setFont(new Font("Segoe UI", Font.BOLD, 11));
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        setOpaque(false);
        setToolTipText(DoTuoiController.moTa(doTuoi));
        setVisible(doTuoi != null && !doTuoi.isEmpty());
    }

    public static Color mauCua(String doTuoi) {
        if (doTuoi == null) return new Color(0x8A, 0x87, 0x82);
        switch (doTuoi) {
            case "P":   return new Color(0x3F, 0xA3, 0x4D);
            case "K":   return new Color(0x2F, 0x6F, 0xDE);
            case "T13": return new Color(0xE0, 0x9A, 0x1F);
            case "T16": return new Color(0xE0, 0x6A, 0x1F);
            case "T18": return new Color(0xC6, 0x28, 0x28);
            default:    return new Color(0x8A, 0x87, 0x82);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(nen);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}