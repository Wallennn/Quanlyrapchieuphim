package util;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Tao ma VietQR (chuan EMV/NAPAS) de khach quet bang app ngan hang.
 * Chi can thu vien ZXing "core" (file core-3.5.x.jar)
 */
public class VietQR {

    // ====== THONG TIN TAI KHOAN NHAN TIEN CUA RAP ======
    public static final String BIN_NGAN_HANG = "970418";     // BIDV
    public static final String SO_TAI_KHOAN  = "7500983144";

    private static String tlv(String id, String giaTri) {
        return id + String.format("%02d", giaTri.length()) + giaTri;
    }

    /** Chuoi VietQR cho tai khoan cua rap, da dien san so tien + noi dung. */
    public static String taoChuoi(long soTien, String noiDung) {
        return taoChuoi(BIN_NGAN_HANG, SO_TAI_KHOAN, soTien, noiDung);
    }

    public static String taoChuoi(String bin, String stk, long soTien, String noiDung) {
        String taiKhoan = tlv("00", bin) + tlv("01", stk);
        String nganHang = tlv("00", "A000000727") + tlv("01", taiKhoan) + tlv("02", "QRIBFTTA");

        StringBuilder sb = new StringBuilder();
        sb.append(tlv("00", "01"));
        sb.append(tlv("01", soTien > 0 ? "12" : "11"));
        sb.append(tlv("38", nganHang));
        sb.append(tlv("53", "704"));
        if (soTien > 0) sb.append(tlv("54", String.valueOf(soTien)));
        sb.append(tlv("58", "VN"));
        String nd = boDau(noiDung);
        if (!nd.isEmpty()) sb.append(tlv("62", tlv("08", nd)));
        sb.append("6304");
        sb.append(crc16(sb.toString()));
        return sb.toString();
    }

    /** Bo dau tieng Viet, chi giu chu/so/khoang trang, toi da 25 ky tu. */
    public static String boDau(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .replaceAll("[^A-Za-z0-9 ]", "")
                .trim();
        return t.length() > 25 ? t.substring(0, 25) : t;
    }

    private static String crc16(String s) {
        int crc = 0xFFFF;
        for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = ((crc & 0x8000) != 0) ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

    /** Ve chuoi thanh anh QR (kichThuoc x kichThuoc px). */
    public static BufferedImage taoAnhQR(String chuoi, int kichThuoc) throws Exception {
        java.util.Map<com.google.zxing.EncodeHintType, Object> hints = new java.util.HashMap<>();
        hints.put(com.google.zxing.EncodeHintType.MARGIN, 1);
        com.google.zxing.common.BitMatrix bm = new com.google.zxing.qrcode.QRCodeWriter()
                .encode(chuoi, com.google.zxing.BarcodeFormat.QR_CODE, kichThuoc, kichThuoc, hints);
        int w = bm.getWidth(), h = bm.getHeight();
        BufferedImage anh = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                anh.setRGB(x, y, bm.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        return anh;
    }
}