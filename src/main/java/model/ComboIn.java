package model;

// 1 dong combo tren hoa don (ten combo, so luong, don gia luc ban)
public final class ComboIn {
    private final String tenCombo;
    private final int soLuong;
    private final double donGia;

    public ComboIn(String tenCombo, int soLuong, double donGia) {
        this.tenCombo = tenCombo;
        this.soLuong = soLuong;
        this.donGia = donGia;
    }

    public String tenCombo() {
        return tenCombo;
    }

    public int soLuong() {
        return soLuong;
    }

    public double donGia() {
        return donGia;
    }

    public double thanhTien() {
        return soLuong * donGia;
    }
}