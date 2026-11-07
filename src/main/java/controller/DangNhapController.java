package controller;

import dao.TaiKhoanDAO;
import model.TaiKhoan;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import java.sql.SQLException;

public class DangNhapController {

    private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO();

    // Argon2 la thu vien chuyen ma hoa/kiem tra mat khau.
    // Argon2Factory.create() tao 1 "cong cu" dung chung cho ca viec ma hoa va kiem tra.
    private final Argon2 argon2 = Argon2Factory.create();

    /**
     * Kiem tra dang nhap.
     * @return doi tuong TaiKhoan neu dung thong tin, null neu sai hoac tai khoan bi khoa
     */
    public TaiKhoan dangNhap(String tenDangNhap, String matKhauNhapVao) throws SQLException {
        TaiKhoan tk = taiKhoanDAO.timTheoTenDangNhap(tenDangNhap);

        if (tk == null) {
            return null; // khong co tai khoan nay trong database
        }

        if (!tk.isTrangThai()) {
            return null; // tai khoan da bi khoa / nghi viec
        }

        // argon2.verify(hashDaLuu, matKhauNguoiDungGo):
        // ham nay tu dong so sanh mat khau vua go voi hash da luu trong database,
        // KHONG CAN giai ma hash ra - day la nguyen tac cua ma hoa 1 chieu (hashing).
        boolean dungMatKhau = argon2.verify(tk.getMatKhau(), matKhauNhapVao.toCharArray());

        if (!dungMatKhau) {
            return null; // sai mat khau
        }

        // Dang nhap thanh cong -> cap nhat lan dang nhap cuoi (khong bat buoc, co the bo qua)
        taiKhoanDAO.capNhatLanDangNhapCuoi(tk.getId());

        return tk;
    }

    // Ham tien ich: dung de TAO HASH moi khi can tao tai khoan moi (vi du man Dang ky, hoac khi them nhan vien)
    public String maHoaMatKhau(String matKhauGoc) {
        return argon2.hash(2, 65536, 1, matKhauGoc.toCharArray());
        // 3 tham so dau: so lan lap (2), bo nho dung (65536 KB = 64MB), so luong - cang cao cang an toan nhung cang cham
    }
}