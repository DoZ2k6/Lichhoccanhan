package com.example.lichhoccanhan;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

/** Các hằng số, mô hình dữ liệu và hàm dùng chung cho các màn hình. */
public final class TienIch {

    private TienIch() {}

    public static final int SO_TIET = 6;

    public static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    public static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    // ---- Màu (giao diện tối) ----
    public static final int TIM = Color.rgb(18, 216, 184);          // màu nhấn (teal)
    public static final int CHU_DAM = Color.WHITE;                  // chữ chính
    public static final int CHU_XAM = Color.rgb(154, 159, 174);     // chữ phụ
    public static final int CHU_MO = Color.rgb(107, 112, 128);      // chữ mờ, "Chưa nhập"
    public static final int CHU_TREN_NEON = Color.BLACK;            // chữ nằm trên nền teal
    public static final int XANH_DAM = Color.rgb(18, 216, 184);     // tên môn
    public static final int XANH_NHAT = Color.rgb(14, 58, 58);      // nền ô có môn
    public static final int XANH_LA = Color.rgb(24, 119, 70);
    public static final int MINT = Color.rgb(223, 245, 231);
    public static final int O_TRONG = Color.rgb(34, 37, 48);        // ô trống
    public static final int NEN = Color.rgb(18, 20, 28);            // nền màn hình
    public static final int THE = Color.argb(38, 255, 255, 255);    // xám trong suốt
    public static final int VIEN = Color.rgb(42, 45, 58);           // viền
    public static final int DO_TRUNG = Color.rgb(255, 92, 92);   // báo trùng lịch

    public static final String[] TEN_THU = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
    public static final String[] TEN_THU_DAY = {
            "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm",
            "Thứ sáu", "Thứ bảy", "Chủ nhật"};

    // ====================== Mô hình dữ liệu ======================

    /** Một tiết học. */
    public static class Tiet {
        public String tenMon;
        public String tenLop;
        public String phong;
        public boolean isTrung = false;   // đánh dấu tiết bị trùng khi nhập file

    }

    /** Một môn học = các tiết liền nhau cùng tên trong một buổi. */
    public static class NhomMon {
        public String ten;
        public String lop = "";
        public String phong = "";
        public int dau;   // chỉ số tiết đầu (0 ... SO_TIET - 1)
        public int cuoi;  // chỉ số tiết cuối
    }

    public interface KhiChon {
        void chon(int viTri);
    }

    private static boolean cungMon(Tiet a, Tiet b) {
        return a.tenMon.trim().equalsIgnoreCase(b.tenMon.trim());
    }

    /** Nhóm môn chứa tiết thứ idx (buoi[idx] không được null). */
    public static NhomMon nhom(Tiet[] buoi, int idx) {
        int dau = idx;
        int cuoi = idx;
        while (dau > 0 && buoi[dau - 1] != null
                && cungMon(buoi[dau - 1], buoi[idx])) dau--;
        while (cuoi < buoi.length - 1 && buoi[cuoi + 1] != null
                && cungMon(buoi[cuoi + 1], buoi[idx])) cuoi++;

        NhomMon n = new NhomMon();
        n.ten = buoi[idx].tenMon.trim();
        n.dau = dau;
        n.cuoi = cuoi;

        // Lớp và phòng lấy ở tiết đầu tiên có nhập trong nhóm
        for (int k = dau; k <= cuoi; k++) {
            if (n.lop.isEmpty() && buoi[k].tenLop != null) {
                n.lop = buoi[k].tenLop.trim();
            }
            if (n.phong.isEmpty() && buoi[k].phong != null) {
                n.phong = buoi[k].phong.trim();
            }
        }
        return n;
    }

    /** Gom các tiết của một buổi thành danh sách môn học. */
    public static ArrayList<NhomMon> gomMon(Tiet[] buoi) {
        ArrayList<NhomMon> ds = new ArrayList<>();
        int i = 0;
        while (i < buoi.length) {
            if (buoi[i] == null) {
                i++;
                continue;
            }
            NhomMon n = nhom(buoi, i);
            ds.add(n);
            i = n.cuoi + 1;
        }
        return ds;
    }

    /** "2" hoặc "1-3". */
    public static String tietChu(NhomMon n) {
        return tietChu(n.dau, n.cuoi);
    }

    /** "2" hoặc "1-3" từ chỉ số tiết đầu và cuối (bắt đầu từ 0). */
    public static String tietChu(int dau, int cuoi) {
        return dau == cuoi
                ? String.valueOf(dau + 1)
                : (dau + 1) + "-" + (cuoi + 1);
    }

    /** Các đoạn tiết trống liền nhau của một buổi: mỗi phần tử là {dau, cuoi}. */
    public static ArrayList<int[]> gomTietTrong(Tiet[] buoi) {
        ArrayList<int[]> ds = new ArrayList<>();
        int i = 0;
        while (i < buoi.length) {
            if (buoi[i] != null) {
                i++;
                continue;
            }
            int dau = i;
            while (i + 1 < buoi.length && buoi[i + 1] == null) i++;
            ds.add(new int[]{dau, i});
            i++;
        }
        return ds;
    }
    /** Ngày (năm, tháng 0-11, ngày) lúc 00:00. */
    public static Calendar ngayThang(int nam, int thang, int ngay) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(nam, thang, ngay);
        return c;
    }

    /** Khoảng ngày của học kỳ hiện tại: {ngày bắt đầu, ngày kết thúc}. */
    public static Calendar[] khoangHocKy() {
        Calendar h = Calendar.getInstance();
        int nam = h.get(Calendar.YEAR);
        int thang = h.get(Calendar.MONTH);

        Calendar tu, den;
        if (thang >= Calendar.OCTOBER) {            // tháng 10 - 12
            tu = ngayThang(nam, Calendar.OCTOBER, 1);
            den = ngayThang(nam + 1, Calendar.JANUARY, 31);
        } else if (thang == Calendar.JANUARY) {     // tháng 1
            tu = ngayThang(nam - 1, Calendar.OCTOBER, 1);
            den = ngayThang(nam, Calendar.JANUARY, 31);
        } else if (thang <= Calendar.MAY) {         // tháng 2 - 5
            tu = ngayThang(nam, Calendar.FEBRUARY, 1);
            den = ngayThang(nam, Calendar.MAY, 31);
        } else {                                    // tháng 6 - 9
            tu = ngayThang(nam, Calendar.JUNE, 1);
            den = ngayThang(nam, Calendar.SEPTEMBER, 30);
        }
        return new Calendar[]{tu, den};
    }

    /** Đếm số ngày của từng thứ (0 = T2 ... 6 = CN) trong khoảng [tu, den]. */
    public static int[] demThu(Calendar tu, Calendar den) {
        int[] kq = new int[7];
        Calendar c = (Calendar) tu.clone();
        while (!c.after(den)) {
            kq[(c.get(Calendar.DAY_OF_WEEK) + 5) % 7]++;
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return kq;
    }
    public static String hocKyMacDinh() {
        Calendar c = Calendar.getInstance();
        int nam = c.get(Calendar.YEAR);
        int thang = c.get(Calendar.MONTH); // 0 = tháng 1

        if (thang >= Calendar.OCTOBER) {            // tháng 10 - 12
            return "HK1 (" + nam + "-" + (nam + 1) + ")";
        } else if (thang == Calendar.JANUARY) {     // tháng 1
            return "HK1 (" + (nam - 1) + "-" + nam + ")";
        } else if (thang <= Calendar.MAY) {         // tháng 2 - 5
            return "HK2 (" + (nam - 1) + "-" + nam + ")";
        } else {                                    // tháng 6 - 9
            return "HK3 (" + (nam - 1) + "-" + nam + ")";
        }
    }

    /** Số tiết đã có môn của từng thứ. */
    public static int[] demTheoThu(Tiet[][][] bang) {
        int[] kq = new int[7];
        for (int t = 0; t < 7; t++) {
            for (int b = 0; b < 2; b++) {
                for (int i = 0; i < SO_TIET; i++) {
                    if (bang[t][b][i] != null) kq[t]++;
                }
            }
        }
        return kq;
    }

    public static boolean coDuLieu(Tiet[][][] bang) {
        for (int n : demTheoThu(bang)) {
            if (n > 0) return true;
        }
        return false;
    }

    /** Bảng thời khóa biểu rỗng: bang[thu][buoi][tiet]. */
    public static Tiet[][][] taoBangRong() {
        return new Tiet[7][2][SO_TIET];
    }
    // ====================== Tiện ích giao diện ======================

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static GradientDrawable nen(Context c, int mau, float banKinhDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(mau);
        g.setCornerRadius(dp(c, banKinhDp));
        return g;
    }

    public static TextView taoText(Context c, String s, float sp,
                                   int mau, boolean dam) {
        TextView tv = new TextView(c);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(mau);
        tv.setTypeface(null, dam ? Typeface.BOLD : Typeface.NORMAL);
        return tv;
    }

    /** w, h là MATCH/WRAP hoặc pixel; các lề tính bằng dp. */
    public static LinearLayout.LayoutParams lp(Context c, int w, int h,
                                               float l, float t, float r, float b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return p;
    }

    public static LinearLayout taoThe(Context c, float padDp) {
        LinearLayout the = new LinearLayout(c);
        the.setOrientation(LinearLayout.VERTICAL);
        the.setBackgroundResource(R.drawable.bg_card);
        the.setPadding(dp(c, padDp), dp(c, padDp), dp(c, padDp), dp(c, padDp));
        the.setLayoutParams(lp(c, MATCH, WRAP, 0, 0, 0, 16));
        return the;
    }

    /** Hàng chọn thứ T2 ... CN, dùng chung cho cả hai màn hình. */
    public static void veHangNgay(Context c, LinearLayout khung,
                                  int[] soTiet, int chon, KhiChon cb) {
        khung.removeAllViews();

        for (int i = 0; i < 7; i++) {
            final int vt = i;
            boolean dangChon = (i == chon);

            String noiDung = TEN_THU[i];
            if (soTiet[i] > 0) noiDung += "\n" + soTiet[i];
            noiDung += "\n•";

            TextView tv = taoText(c, noiDung, 14,
                    dangChon ? CHU_TREN_NEON : CHU_DAM, true);
            tv.setGravity(Gravity.CENTER);
            tv.setBackground(nen(c, dangChon ? TIM : THE, 20));

            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(0, dp(c, 78), 1f);
            p.setMargins(dp(c, 3), 0, dp(c, 3), 0);
            tv.setLayoutParams(p);

            tv.setOnClickListener(v -> cb.chon(vt));
            khung.addView(tv);
        }
    }
}