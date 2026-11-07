package model;

// 1 combo bap nuoc (1 dong trong bang combo)
public record ComboBan(
        String maCombo,
        String tenCombo,
        String moTa,
        double gia,
        String hinhAnh) {   // ten file anh trong src/main/resources/images/combos/ (co the NULL)
}