package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Thong tin 1 ve (1 ghe) de in ra giay. loaiGhe: Thuong / VIP / Doi; trangThai: Da thanh toan / Da huy / Da hoan tien
public final class VeIn {
    private final String maHd;
    private final String tenPhim;
    private final String tenPhong;
    private final LocalDate ngayChieu;
    private final LocalTime gioBatDau;
    private final LocalTime gioKetThuc;
    private final String hangGhe;
    private final int soGhe;
    private final String loaiGhe;
    private final double giaVe;
    private final LocalDateTime ngayLap;
    private final String tenNhanVien;
    private final String trangThai;

    public VeIn(String maHd, String tenPhim, String tenPhong, LocalDate ngayChieu, LocalTime gioBatDau, LocalTime gioKetThuc, String hangGhe, int soGhe, String loaiGhe, double giaVe, LocalDateTime ngayLap, String tenNhanVien, String trangThai) {
        this.maHd = maHd;
        this.tenPhim = tenPhim;
        this.tenPhong = tenPhong;
        this.ngayChieu = ngayChieu;
        this.gioBatDau = gioBatDau;
        this.gioKetThuc = gioKetThuc;
        this.hangGhe = hangGhe;
        this.soGhe = soGhe;
        this.loaiGhe = loaiGhe;
        this.giaVe = giaVe;
        this.ngayLap = ngayLap;
        this.tenNhanVien = tenNhanVien;
        this.trangThai = trangThai;
    }

    public String maHd() {
        return maHd;
    }

    public String tenPhim() {
        return tenPhim;
    }

    public String tenPhong() {
        return tenPhong;
    }

    public LocalDate ngayChieu() {
        return ngayChieu;
    }

    public LocalTime gioBatDau() {
        return gioBatDau;
    }

    public LocalTime gioKetThuc() {
        return gioKetThuc;
    }

    public String hangGhe() {
        return hangGhe;
    }

    public int soGhe() {
        return soGhe;
    }

    public String loaiGhe() {
        return loaiGhe;
    }

    public double giaVe() {
        return giaVe;
    }

    public LocalDateTime ngayLap() {
        return ngayLap;
    }

    public String tenNhanVien() {
        return tenNhanVien;
    }

    public String trangThai() {
        return trangThai;
    }
}