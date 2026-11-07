package model;

import java.time.LocalDateTime;

// 1 hoa don BAP NUOC RIENG (khong co ve): dong trong danh sach + thong tin khach hang de xem truoc / in
public final class HoaDonBapNuoc {
    private final String maHd;
    private final LocalDateTime ngayLap;
    private final int soCombo;
    private final double tongTien;
    private final String trangThai;
    private final String tenKhach;       // null = khach le (khong nhap SDT)
    private final String soDienThoai;
    private final String hang;           // hang thanh vien hien tai (Dong / Bac / Vang / ...)
    private final int diemHienCo;        // diem hien co cua khach
    private final int diemTichHd;        // diem duoc cong tu hoa don nay
    private final String tenNhanVien;

    public HoaDonBapNuoc(String maHd, LocalDateTime ngayLap, int soCombo, double tongTien, String trangThai,
                         String tenKhach, String soDienThoai, String hang, int diemHienCo, int diemTichHd,
                         String tenNhanVien) {
        this.maHd = maHd;
        this.ngayLap = ngayLap;
        this.soCombo = soCombo;
        this.tongTien = tongTien;
        this.trangThai = trangThai;
        this.tenKhach = tenKhach;
        this.soDienThoai = soDienThoai;
        this.hang = hang;
        this.diemHienCo = diemHienCo;
        this.diemTichHd = diemTichHd;
        this.tenNhanVien = tenNhanVien;
    }

    public String maHd() { return maHd; }
    public LocalDateTime ngayLap() { return ngayLap; }
    public int soCombo() { return soCombo; }
    public double tongTien() { return tongTien; }
    public String trangThai() { return trangThai; }
    public String tenKhach() { return tenKhach; }
    public String soDienThoai() { return soDienThoai; }
    public String hang() { return hang; }
    public int diemHienCo() { return diemHienCo; }
    public int diemTichHd() { return diemTichHd; }
    public String tenNhanVien() { return tenNhanVien; }
    public boolean laThanhVien() { return tenKhach != null; }
}