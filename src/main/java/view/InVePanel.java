package view;

import javax.swing.*;
import java.awt.*;

// Man hinh "In ve": 2 tab
//   1) In ve + bap nuoc : hoa don co ve (kem phieu bap nuoc neu co mua combo cung hoa don)
//   2) Bap nuoc rieng   : hoa don chi co combo, khong ve (kem thong tin khach hang / diem tich luy)
public class InVePanel extends JPanel {

    // maHdChonSan: ma hoa don can mo san (vi du vua ban ve xong), null neu khong co
    public InVePanel(String maHdChonSan) {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        JLabel lblTieuDe = new JLabel("In vé");
        lblTieuDe.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTieuDe.setForeground(InVeKemBapNuocPanel.MAU_CHU_DAM);
        lblTieuDe.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        add(lblTieuDe, BorderLayout.NORTH);

        InVeKemBapNuocPanel tabVe = new InVeKemBapNuocPanel(maHdChonSan);
        BapNuocRiengPanel tabBapNuoc = new BapNuocRiengPanel(maHdChonSan);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("In vé + bắp nước", tabVe);
        tabs.addTab("Bắp nước riêng", tabBapNuoc);
        if (tabBapNuoc.daChonDuoc()) {
            tabs.setSelectedIndex(1);   // ma hoa don truyen vao la hoa don bap nuoc rieng
        }
        add(tabs, BorderLayout.CENTER);
    }
}