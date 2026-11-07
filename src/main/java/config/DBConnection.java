package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Doi lai thong tin nay cho dung voi may ban:
    // - URL: ten database da tao trong phpMyAdmin (quan_ly_rap_chieu_phim)
    // - USER: mac dinh XAMPP la "root"
    // - PASSWORD: mac dinh XAMPP la rong "" (khong co mat khau)
    private static final String URL = "jdbc:mysql://localhost:3306/quan_ly_rap_chieu_phim?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        try {
            // Nap driver MySQL (bat buoc truoc khi ket noi)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Khong tim thay MySQL Driver. Kiem tra lai pom.xml", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

}