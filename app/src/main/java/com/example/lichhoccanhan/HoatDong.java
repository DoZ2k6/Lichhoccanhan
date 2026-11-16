package com.example.lichhoccanhan;

import java.util.Locale;

/** Một hoạt động trong lịch cá nhân: có giờ bắt đầu, giờ kết thúc. */
public class HoatDong {

    public static final int HOC = 0, DAY = 1, KHAC = 2;
    public static final String[] TEN_LOAI = {"Học", "Dạy", "Khác"};

    public long id;
    public String ten = "";
    public int loai = HOC;
    public int thu = -1;          // 0 = T2 ... 6 = CN (khi lặp hằng tuần)
    public String ngay = "";      // "yyyy-MM-dd" nếu chỉ một ngày, rỗng nếu lặp tuần
    public int batDau;            // số phút tính từ 00:00 (06:45 = 405)
    public int ketThuc;
    public String lop = "";       // tùy chọn
    public String diaDiem = "";   // tùy chọn
    public String hocKy = "";     // tùy chọn
    public String ghiChu = "";    // tùy chọn

    public boolean laLapTuan() {
        return ngay == null || ngay.isEmpty();
    }

    /** Hai hoạt động giao nhau về giờ (giờ kết thúc của cái này = giờ bắt đầu cái kia thì không trùng). */
    public boolean giaoNhau(HoatDong o) {
        return batDau < o.ketThuc && o.batDau < ketThuc;
    }

    public String khoangGio() {
        return gio(batDau) + " → " + gio(ketThuc);
    }

    public String tenLoai() {
        return TEN_LOAI[Math.max(0, Math.min(loai, TEN_LOAI.length - 1))];
    }

    /** 405 -> "06:45". */
    public static String gio(int phut) {
        return String.format(Locale.US, "%02d:%02d", phut / 60, phut % 60);
    }

    // ====================== Bảng giờ của tiết ======================
    // Buổi sáng: tiết 1-3 liền nhau từ 06:45, nghỉ 15 phút, tiết 4-6 từ 09:15.
    // Buổi chiều: tiết 1 bắt đầu 12:30, mỗi tiết 45 phút, nghỉ 5 phút.
    // idx là chỉ số tiết trong buổi (0 ... 5).

    public static int batDauTiet(int buoi, int idx) {
        if (buoi == 0) {
            return idx < 3 ? 405 + idx * 45 : 555 + (idx - 3) * 45;
        }
        return 750 + idx * 50;
    }

    public static int ketThucTiet(int buoi, int idx) {
        return batDauTiet(buoi, idx) + 45;
    }
}