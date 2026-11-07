package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// 1 dong trong danh sach hoa don o man "In ve"
public final class HoaDonTomTat {
    private final String maHd;
    private final String tenPhim;
    private final LocalDate ngayChieu;
    private final LocalTime gioBatDau;
    private final String tenPhong;
    private final int soVe;
    private final double tongTien;
    private final String trangThai;
    private final LocalDateTime ngayLap;
    private final int soCombo;   // so combo bap nuoc mua kem trong hoa don (0 = khong co)

    public HoaDonTomTat(String maHd, String tenPhim, LocalDate ngayChieu, LocalTime gioBatDau, String tenPhong, int soVe, double tongTien, String trangThai, LocalDateTime ngayLap) {
        this(maHd, tenPhim, ngayChieu, gioBatDau, tenPhong, soVe, tongTien, trangThai, ngayLap, 0);
    }

    public HoaDonTomTat(String maHd, String tenPhim, LocalDate ngayChieu, LocalTime gioBatDau, String tenPhong, int soVe, double tongTien, String trangThai, LocalDateTime ngayLap, int soCombo) {
        this.maHd = maHd;
        this.tenPhim = tenPhim;
        this.ngayChieu = ngayChieu;
        this.gioBatDau = gioBatDau;
        this.tenPhong = tenPhong;
        this.soVe = soVe;
        this.tongTien = tongTien;
        this.trangThai = trangThai;
        this.ngayLap = ngayLap;
        this.soCombo = soCombo;
    }

    public String maHd() {
        return maHd;
    }

    public String tenPhim() {
        return tenPhim;
    }

    public LocalDate ngayChieu() {
        return ngayChieu;
    }

    public LocalTime gioBatDau() {
        return gioBatDau;
    }

    public String tenPhong() {
        return tenPhong;
    }

    public int soVe() {
        return soVe;
    }

    public double tongTien() {
        return tongTien;
    }

    public String trangThai() {
        return trangThai;
    }

    public LocalDateTime ngayLap() {
        return ngayLap;
    }

    public int soCombo() {
        return soCombo;
    }
}