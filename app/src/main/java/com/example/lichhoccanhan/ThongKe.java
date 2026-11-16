package com.example.lichhoccanhan;

import static com.example.lichhoccanhan.TienIch.*;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;

/** Vẽ tab Thống kê vào một LinearLayout có sẵn, đọc từ bảng hoat_dong. */
public class ThongKe {

    private static final String[] KHOANG_KY = {
            "01/10 → 31/01", "01/02 → 31/05", "01/06 → 30/09"};

    private final Context ctx;
    private final LinearLayout khung;

    int kyHienTai;      // 0 = HK1, 1 = HK2, 2 = HK3
    int kyChon;
    boolean moDanhSach = false;
    String namHoc;      // ví dụ "(2026-2027)"

    /** Số liệu gộp của các hoạt động cùng tên. */
    static class Nhom {
        String ten;
        int loai;
        String lop = "";
        String diaDiem = "";
        int phutMoiBuoi;      // thời lượng một buổi (phút)
        int buoiDa, buoiTong; // số buổi
        long phutDa, phutTong;
    }

    public ThongKe(Context ctx, LinearLayout khung) {
        this.ctx = ctx;
        this.khung = khung;

        String hk = hocKyMacDinh();               // "HK1 (2026-2027)"
        kyHienTai = hk.charAt(2) - '1';
        namHoc = hk.substring(hk.indexOf('('));
        kyChon = kyHienTai;
    }

    private String tenKy(int i) {
        return "HK" + (i + 1) + " " + namHoc;
    }

    private String gio(long phut) {
        long h = phut / 60, m = phut % 60;
        if (h == 0) return m + " phút";
        if (m == 0) return h + " giờ";
        return h + " giờ " + m + " phút";
    }

    private static String ngayStr(Calendar c) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    // ====================== Vẽ ======================

    public void ve() {
        khung.removeAllViews();

        TextView tieuDe = taoText(ctx, "Thống kê", 30, CHU_DAM, true);
        tieuDe.setLayoutParams(lp(ctx, WRAP, WRAP, 4, 16, 0, 16));
        khung.addView(tieuDe);

        khung.addView(veThanhChonKy());
        if (moDanhSach) khung.addView(veDanhSachKy());

        if (kyChon != kyHienTai) {
            veTrong("Không có lịch học", "Chưa có hoạt động cho học kỳ này.");
            return;
        }

        LichDatabase db = new LichDatabase(ctx);
        ArrayList<HoatDong> tatCa = db.layTatCaHoatDong();
        db.close();

        // ---- Khoảng học kỳ ----
        Calendar[] ky = khoangHocKy();
        Calendar now = Calendar.getInstance();
        Calendar homNay = ngayThang(now.get(Calendar.YEAR),
                now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        int[] demTong = demThu(ky[0], ky[1]);
        int[] demDa = demThu(ky[0], homNay);
        String tuNgay = ngayStr(ky[0]), denNgay = ngayStr(ky[1]);
        String homNayStr = ngayStr(homNay);
        String nhanKy = "HK" + (kyChon + 1);

        // ---- Gộp theo tên ----
        LinkedHashMap<String, Nhom> map = new LinkedHashMap<>();
        for (HoatDong h : tatCa) {
            // Hoạt động có ghi học kỳ khác thì không tính
            String hk = h.hocKy == null ? "" : h.hocKy.trim().toUpperCase(Locale.ROOT);
            if (!hk.isEmpty() && !hk.startsWith(nhanKy)) continue;

            int buoiDa, buoiTong;
            if (h.laLapTuan()) {
                if (h.thu < 0 || h.thu > 6) continue;
                buoiDa = demDa[h.thu];
                buoiTong = demTong[h.thu];
            } else {
                if (h.ngay.compareTo(tuNgay) < 0 || h.ngay.compareTo(denNgay) > 0) continue;
                buoiTong = 1;
                buoiDa = h.ngay.compareTo(homNayStr) <= 0 ? 1 : 0;
            }
            int dai = h.ketThuc - h.batDau;

            String khoa = h.ten.trim().toLowerCase(Locale.ROOT) + "|" + h.loai;
            Nhom n = map.get(khoa);
            if (n == null) {
                n = new Nhom();
                n.ten = h.ten.trim();
                n.loai = h.loai;
                map.put(khoa, n);
            }
            if (n.lop.isEmpty()) n.lop = h.lop;
            if (n.diaDiem.isEmpty()) n.diaDiem = h.diaDiem;
            n.phutMoiBuoi = Math.max(n.phutMoiBuoi, dai);
            n.buoiDa += buoiDa;
            n.buoiTong += buoiTong;
            n.phutDa += (long) buoiDa * dai;
            n.phutTong += (long) buoiTong * dai;
        }

        if (map.isEmpty()) {
            veTrong("Không có lịch học", "Hãy thêm hoạt động ở màn hình chính.");
            return;
        }

        ArrayList<Nhom> ds = new ArrayList<>(map.values());
        Collections.sort(ds, (a, b) -> a.ten.compareToIgnoreCase(b.ten));

        int tongDa = 0, tongTong = 0;
        for (Nhom n : ds) {
            tongDa += n.buoiDa;
            tongTong += n.buoiTong;
        }

        // ---- 3 ô số liệu ----
        LinearLayout hang = new LinearLayout(ctx);
        hang.setOrientation(LinearLayout.HORIZONTAL);
        hang.setLayoutParams(lp(ctx, MATCH, WRAP, 0, 0, 0, 16));
        hang.addView(taoO(String.valueOf(ds.size()), "Hoạt động", true));
        hang.addView(taoO(String.valueOf(tongDa), "Đã học", true));
        hang.addView(taoO(String.valueOf(tongTong - tongDa), "Còn lại", false));
        khung.addView(hang);

        // ---- Theo loại ----
        khung.addView(veTheoLoai(ds));

        // ---- Từng hoạt động ----
        for (Nhom n : ds) khung.addView(taoTheNhom(n));

        TextView ghiChu = taoText(ctx,
                "Số liệu là ước tính theo lịch học kỳ mặc định "
                        + "(HK1: 01/10 - 31/01, HK2: 01/02 - 31/05, "
                        + "HK3: 01/06 - 30/09). Chưa trừ ngày nghỉ lễ, "
                        + "Tết, tuần thi. \"Đã học\" tính đến hết hôm nay.",
                13, CHU_MO, false);
        ghiChu.setPadding(dp(ctx, 4), 0, dp(ctx, 4), 0);
        khung.addView(ghiChu);
    }

    /** Thẻ tổng hợp theo loại: số hoạt động và số giờ cả kỳ. */
    private View veTheoLoai(ArrayList<Nhom> ds) {
        LinearLayout the = taoThe(ctx, 18);

        TextView t = taoText(ctx, "Theo loại", 19, CHU_DAM, true);
        t.setLayoutParams(lp(ctx, WRAP, WRAP, 0, 0, 0, 6));
        the.addView(t);

        for (int loai = 0; loai < HoatDong.TEN_LOAI.length; loai++) {
            int soHd = 0, buoi = 0;
            long phutDa = 0, phutTong = 0;
            for (Nhom n : ds) {
                if (n.loai != loai) continue;
                soHd++;
                buoi += n.buoiDa;
                phutDa += n.phutDa;
                phutTong += n.phutTong;
            }
            if (soHd == 0) continue;
            the.addView(dong(HoatDong.TEN_LOAI[loai],
                    soHd + " hoạt động · " + buoi + " buổi · "
                            + gio(phutDa) + " / " + gio(phutTong)));
        }
        return the;
    }

    /** Thẻ một hoạt động, thu gọn, bấm để mở chi tiết. */
    private View taoTheNhom(Nhom n) {
        LinearLayout the = taoThe(ctx, 18);
        the.setClickable(true);
        the.setFocusable(true);

        LinearLayout dau = new LinearLayout(ctx);
        dau.setOrientation(LinearLayout.HORIZONTAL);
        dau.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout chu = new LinearLayout(ctx);
        chu.setOrientation(LinearLayout.VERTICAL);
        chu.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView ten = taoText(ctx, n.ten, 18, CHU_DAM, true);
        ten.setSingleLine(true);
        ten.setEllipsize(TextUtils.TruncateAt.END);
        TextView buoi = taoText(ctx,
                n.buoiDa + " / " + n.buoiTong + " buổi", 15, CHU_DAM, true);
        buoi.setPadding(0, dp(ctx, 2), 0, 0);
        chu.addView(ten);
        chu.addView(buoi);

        TextView mui = taoText(ctx, "▾", 18, CHU_XAM, true);
        dau.addView(chu);
        dau.addView(mui);

        LinearLayout ct = new LinearLayout(ctx);
        ct.setOrientation(LinearLayout.VERTICAL);
        ct.setVisibility(View.GONE);
        ct.setPadding(0, dp(ctx, 10), 0, 0);
        ct.addView(dong("Loại", HoatDong.TEN_LOAI[n.loai]));
        ct.addView(dong("Mỗi buổi", gio(n.phutMoiBuoi)));
        ct.addView(dong("Đã học", gio(n.phutDa)));
        ct.addView(dong("Cả học kỳ", gio(n.phutTong)));
        ct.addView(dong("Còn lại", (n.buoiTong - n.buoiDa) + " buổi"));
        ct.addView(dong("Lớp", n.lop.isEmpty() ? "Chưa nhập" : n.lop));
        ct.addView(dong("Địa điểm", n.diaDiem.isEmpty() ? "Chưa nhập" : n.diaDiem));

        the.addView(dau);
        the.addView(ct);
        the.setOnClickListener(v -> {
            boolean mo = ct.getVisibility() == View.VISIBLE;
            ct.setVisibility(mo ? View.GONE : View.VISIBLE);
            mui.setText(mo ? "▾" : "▴");
        });
        return the;
    }

    // ====================== Thanh chọn học kỳ ======================

    private View veThanhChonKy() {
        LinearLayout the = taoThe(ctx, 18);
        the.setClickable(true);
        the.setFocusable(true);
        the.setOnClickListener(v -> {
            moDanhSach = !moDanhSach;
            ve();
        });

        LinearLayout d = new LinearLayout(ctx);
        d.setOrientation(LinearLayout.HORIZONTAL);
        d.setGravity(Gravity.CENTER_VERTICAL);

        TextView ten = taoText(ctx, tenKy(kyChon), 20, CHU_DAM, true);
        ten.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView mui = taoText(ctx, moDanhSach ? "▴" : "▾", 20, TIM, true);
        d.addView(ten);
        d.addView(mui);
        the.addView(d);

        String khoang;
        if (kyChon == kyHienTai) {
            SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            Calendar[] k = khoangHocKy();
            khoang = f.format(k[0].getTime()) + "  →  " + f.format(k[1].getTime());
        } else {
            khoang = KHOANG_KY[kyChon];
        }
        TextView tvKhoang = taoText(ctx, khoang, 14, CHU_XAM, true);
        tvKhoang.setPadding(0, dp(ctx, 4), 0, 0);
        the.addView(tvKhoang);
        return the;
    }

    private View veDanhSachKy() {
        LinearLayout ds = new LinearLayout(ctx);
        ds.setOrientation(LinearLayout.VERTICAL);
        ds.setLayoutParams(lp(ctx, MATCH, WRAP, 0, 0, 0, 16));

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            boolean dangChon = (i == kyChon);

            TextView o = taoText(ctx, tenKy(i), 17,
                    dangChon ? CHU_TREN_NEON : CHU_DAM, true);
            o.setGravity(Gravity.CENTER_VERTICAL);
            o.setPadding(dp(ctx, 20), 0, dp(ctx, 16), 0);
            o.setBackground(nen(ctx, dangChon ? TIM : THE, 16));
            o.setLayoutParams(lp(ctx, MATCH, dp(ctx, 52), 0, 0, 0, 8));
            o.setOnClickListener(v -> {
                kyChon = idx;
                moDanhSach = false;
                ve();
            });
            ds.addView(o);
        }
        return ds;
    }

    // ====================== Thành phần nhỏ ======================

    private void veTrong(String tieuDe, String phu) {
        LinearLayout k = new LinearLayout(ctx);
        k.setOrientation(LinearLayout.VERTICAL);
        k.setGravity(Gravity.CENTER);
        k.setPadding(0, dp(ctx, 56), 0, 0);

        TextView t = taoText(ctx, tieuDe, 20, CHU_DAM, true);
        t.setGravity(Gravity.CENTER);
        TextView p = taoText(ctx, phu, 15, CHU_XAM, false);
        p.setGravity(Gravity.CENTER);
        p.setPadding(0, dp(ctx, 6), 0, 0);

        k.addView(t);
        k.addView(p);
        khung.addView(k);
    }

    private View taoO(String so, String nhan, boolean coLe) {
        LinearLayout o = new LinearLayout(ctx);
        o.setOrientation(LinearLayout.VERTICAL);
        o.setGravity(Gravity.CENTER);
        o.setPadding(0, dp(ctx, 16), 0, dp(ctx, 16));
        o.setBackground(nen(ctx, THE, 18));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, WRAP, 1f);
        p.setMargins(0, 0, coLe ? dp(ctx, 10) : 0, 0);
        o.setLayoutParams(p);

        TextView tvSo = taoText(ctx, so, 28, TIM, true);
        tvSo.setGravity(Gravity.CENTER);
        tvSo.setLayoutParams(new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView tvNhan = taoText(ctx, nhan, 13, CHU_XAM, true);
        tvNhan.setGravity(Gravity.CENTER);
        tvNhan.setLayoutParams(new LinearLayout.LayoutParams(MATCH, WRAP));

        o.addView(tvSo);
        o.addView(tvNhan);
        return o;
    }

    private View dong(String nhan, String giaTri) {
        LinearLayout hang = new LinearLayout(ctx);
        hang.setOrientation(LinearLayout.HORIZONTAL);
        hang.setLayoutParams(lp(ctx, MATCH, WRAP, 0, 4, 0, 0));

        TextView tvNhan = taoText(ctx, nhan, 14, CHU_XAM, true);
        tvNhan.setLayoutParams(new LinearLayout.LayoutParams(dp(ctx, 96), WRAP));
        TextView tvGiaTri = taoText(ctx, giaTri, 15, CHU_DAM, true);
        tvGiaTri.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));

        hang.addView(tvNhan);
        hang.addView(tvGiaTri);
        return hang;
    }
}