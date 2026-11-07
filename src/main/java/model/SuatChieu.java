package model;

import java.time.LocalDate;
import java.time.LocalTime;

public class SuatChieu {

    private String maSuat;
    private String maPhim;
    private String maPhong;
    private LocalDate ngayChieu;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private double giaVeThuong;
    private double giaVeVip;
    private double giaVeDoi;

    public SuatChieu() {
    }

    public String getMaSuat() {
        return maSuat;
    }

    public void setMaSuat(String maSuat) {
        this.maSuat = maSuat;
    }

    public String getMaPhim() {
        return maPhim;
    }

    public void setMaPhim(String maPhim) {
        this.maPhim = maPhim;
    }

    public String getMaPhong() {
        return maPhong;
    }

    public void setMaPhong(String maPhong) {
        this.maPhong = maPhong;
    }

    public LocalDate getNgayChieu() {
        return ngayChieu;
    }

    public void setNgayChieu(LocalDate ngayChieu) {
        this.ngayChieu = ngayChieu;
    }

    public LocalTime getGioBatDau() {
        return gioBatDau;
    }

    public void setGioBatDau(LocalTime gioBatDau) {
        this.gioBatDau = gioBatDau;
    }

    public LocalTime getGioKetThuc() {
        return gioKetThuc;
    }

    public void setGioKetThuc(LocalTime gioKetThuc) {
        this.gioKetThuc = gioKetThuc;
    }

    public double getGiaVeThuong() {
        return giaVeThuong;
    }

    public void setGiaVeThuong(double giaVeThuong) {
        this.giaVeThuong = giaVeThuong;
    }

    public double getGiaVeVip() {
        return giaVeVip;
    }

    public void setGiaVeVip(double giaVeVip) {
        this.giaVeVip = giaVeVip;
    }

    public double getGiaVeDoi() {
        return giaVeDoi;
    }

    public void setGiaVeDoi(double giaVeDoi) {
        this.giaVeDoi = giaVeDoi;
    }
}