package controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Ghi file Excel (.xlsx) nhieu sheet, khong can thu vien ngoai (tu dong goi XML + ZIP).
 * Dong dau cua moi sheet la tieu de (nen nau, chu trang, co dinh khi cuon).
 * Gia tri o: String -> chu, Long/Integer/Double -> so (co dau phan cach nghin), PhanTram -> %.
 */
public class XuatExcel {

    /** Gia tri phan tram (0..1) de ghi dung dinh dang % trong Excel. */
    public static class PhanTram {
        public final double giaTri;

        public PhanTram(double giaTri) {
            this.giaTri = giaTri;
        }
    }

    public static class Sheet {
        public final String ten;
        public final List<Object[]> hang = new ArrayList<>();

        public Sheet(String ten, Object... tieuDe) {
            this.ten = ten;
            this.hang.add(tieuDe);
        }

        public Sheet them(Object... o) {
            hang.add(o);
            return this;
        }
    }

    public static void ghi(File file, List<Sheet> sheets) throws IOException {
        if (sheets.isEmpty()) throw new IOException("Không có dữ liệu để xuất");
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            // [Content_Types].xml
            StringBuilder ct = new StringBuilder(XML_DAU);
            ct.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
              .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
              .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
              .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
              .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
            for (int i = 1; i <= sheets.size(); i++) {
                ct.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                  .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
            }
            ct.append("</Types>");
            ghiMuc(zip, "[Content_Types].xml", ct.toString());

            ghiMuc(zip, "_rels/.rels", XML_DAU
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");

            // workbook + quan he
            StringBuilder wb = new StringBuilder(XML_DAU);
            wb.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
              .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
            StringBuilder rel = new StringBuilder(XML_DAU);
            rel.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
            for (int i = 1; i <= sheets.size(); i++) {
                wb.append("<sheet name=\"").append(esc(tenSheet(sheets.get(i - 1).ten))).append("\" sheetId=\"").append(i)
                  .append("\" r:id=\"rId").append(i).append("\"/>");
                rel.append("<Relationship Id=\"rId").append(i)
                   .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                   .append(i).append(".xml\"/>");
            }
            wb.append("</sheets></workbook>");
            rel.append("<Relationship Id=\"rId").append(sheets.size() + 1)
               .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
               .append("</Relationships>");
            ghiMuc(zip, "xl/workbook.xml", wb.toString());
            ghiMuc(zip, "xl/_rels/workbook.xml.rels", rel.toString());
            ghiMuc(zip, "xl/styles.xml", STYLES);

            for (int i = 1; i <= sheets.size(); i++) {
                ghiMuc(zip, "xl/worksheets/sheet" + i + ".xml", xmlSheet(sheets.get(i - 1)));
            }
        }
    }

    // ===================== XML =====================
    private static final String XML_DAU = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";

    // kieu o: 0 = chu, 1 = tieu de, 2 = so #,##0, 3 = phan tram 0.0%
    private static final String STYLES = XML_DAU
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"0.0%\"/></numFmts>"
            + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font></fonts>"
            + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF3A3028\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"4\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\">"
            + "<alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
            + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";

    private static String xmlSheet(Sheet sh) {
        int soCot = 0;
        for (Object[] h : sh.hang) soCot = Math.max(soCot, h.length);
        int[] rong = new int[soCot];
        for (Object[] h : sh.hang) {
            for (int c = 0; c < h.length; c++) {
                int dai = (h[c] instanceof Number) ? 14 : (h[c] == null ? 0 : String.valueOf(h[c]).length() + 2);
                rong[c] = Math.max(rong[c], dai);
            }
        }

        StringBuilder sb = new StringBuilder(XML_DAU);
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        sb.append("<sheetViews><sheetView workbookViewId=\"0\">")
          .append("<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
        sb.append("<cols>");
        for (int c = 0; c < soCot; c++) {
            sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
              .append("\" width=\"").append(Math.min(Math.max(rong[c], 10), 55)).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");
        for (int r = 0; r < sh.hang.size(); r++) {
            Object[] h = sh.hang.get(r);
            sb.append("<row r=\"").append(r + 1).append("\">");
            for (int c = 0; c < h.length; c++) {
                String ref = cot(c) + (r + 1);
                Object v = h[c];
                if (v == null) continue;
                if (r == 0) {
                    chuoi(sb, ref, String.valueOf(v), 1);
                } else if (v instanceof PhanTram) {
                    sb.append("<c r=\"").append(ref).append("\" s=\"3\"><v>").append(((PhanTram) v).giaTri).append("</v></c>");
                } else if (v instanceof Number) {
                    sb.append("<c r=\"").append(ref).append("\" s=\"2\"><v>").append(v).append("</v></c>");
                } else {
                    chuoi(sb, ref, String.valueOf(v), 0);
                }
            }
            sb.append("</row>");
        }
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    private static void chuoi(StringBuilder sb, String ref, String text, int kieu) {
        sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\" s=\"").append(kieu)
          .append("\"><is><t xml:space=\"preserve\">").append(esc(text)).append("</t></is></c>");
    }

    /** 0 -> A, 25 -> Z, 26 -> AA ... */
    private static String cot(int i) {
        StringBuilder s = new StringBuilder();
        for (int n = i; n >= 0; n = n / 26 - 1) s.insert(0, (char) ('A' + n % 26));
        return s.toString();
    }

    private static String tenSheet(String ten) {
        String t = ten.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        if (t.isEmpty()) t = "Sheet";
        return t.length() > 31 ? t.substring(0, 31) : t;
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                default:
                    // bo ky tu dieu khien khong hop le trong XML
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static void ghiMuc(ZipOutputStream zip, String ten, String noiDung) throws IOException {
        zip.putNextEntry(new ZipEntry(ten));
        zip.write(noiDung.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}