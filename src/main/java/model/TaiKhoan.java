package model;

// Lop nay chi luu DU LIEU cua 1 tai khoan, khong co logic xu ly gi ca
// (cac lop nhu the nay goi la "POJO" - Plain Old Java Object)
public class TaiKhoan {

    private long id;
    private String tenDangNhap;
    private String matKhau;      // luu HASH, khong phai mat khau goc
    private String hoTen;
    private String vaiTro;       // "QUAN_LY" hoac "NHAN_VIEN"
    private boolean trangThai;

    public TaiKhoan() {
    }

    public TaiKhoan(long id, String tenDangNhap, String matKhau, String hoTen, String vaiTro, boolean trangThai) {
        this.id = id;
        this.tenDangNhap = tenDangNhap;
        this.matKhau = matKhau;
        this.hoTen = hoTen;
        this.vaiTro = vaiTro;
        this.trangThai = trangThai;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(String vaiTro) {
        this.vaiTro = vaiTro;
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }
}