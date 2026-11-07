package controller;

// FILE NAY CHI DUNG 1 LAN DE TAO HASH THAT CHO TAI KHOAN MAU, SAU DO XOA DI
// Khong phai 1 phan chinh thuc cua chuong trinh

public class TaoHashMauTamThoi {
    public static void main(String[] args) {
        DangNhapController controller = new DangNhapController();

        System.out.println("Hash cho 'QuanLy@123': ");
        System.out.println(controller.maHoaMatKhau("QuanLy@123"));
        System.out.println();

        System.out.println("Hash cho 'NhanVien@123': ");
        System.out.println(controller.maHoaMatKhau("NhanVien@123"));
    }
}