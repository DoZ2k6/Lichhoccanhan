package com.example.lichhoccanhan;

import static com.example.lichhoccanhan.TienIch.*;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.os.Build;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.widget.NumberPicker;
/** Thêm / sửa một hoạt động có giờ bắt đầu và kết thúc. */
public class HoatDongActivity extends AppCompatActivity {

    LichDatabase database;
    EditText edtTen, edtLop, edtDiaDiem, edtHocKy, edtGhiChu;
    LinearLayout khungLoai, khungLap, khungThu, khungChonThu, khungChonNgay;
    TextView tvNgay, tvBatDau, tvKetThuc;
    TextView tvThoiLuong;
    LinearLayout khungThoiLuong;

    HoatDong sua;                 // null = đang thêm mới
    int loai = HoatDong.HOC;
    boolean lapTuan = true;
    boolean[] thuChon = new boolean[7];
    String ngay = "";             // "yyyy-MM-dd"
    int batDau = 480;             // 08:00
    int ketThuc = 570;            // 09:30

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT));
        setContentView(R.layout.activity_hoat_dong);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.ime());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        database = new LichDatabase(this);

        edtTen = findViewById(R.id.edtTen);
        edtLop = findViewById(R.id.edtLop);
        edtDiaDiem = findViewById(R.id.edtDiaDiem);
        edtHocKy = findViewById(R.id.edtHocKy);
        edtGhiChu = findViewById(R.id.edtGhiChu);
        khungLoai = findViewById(R.id.khungLoai);
        khungLap = findViewById(R.id.khungLap);
        khungThu = findViewById(R.id.khungThu);
        khungChonThu = findViewById(R.id.khungChonThu);
        khungChonNgay = findViewById(R.id.khungChonNgay);
        tvNgay = findViewById(R.id.tvNgay);
        tvBatDau = findViewById(R.id.tvBatDau);
        tvKetThuc = findViewById(R.id.tvKetThuc);
        tvThoiLuong = findViewById(R.id.tvThoiLuong);
        khungThoiLuong = findViewById(R.id.khungThoiLuong);
        TextView tvTieuDe = findViewById(R.id.tvTieuDe);
        TextView btnLuu = findViewById(R.id.btnLuu);
        TextView btnXoa = findViewById(R.id.btnXoa);

        // ---- Chế độ sửa hay thêm ----
        long id = getIntent().getLongExtra("id", -1);
        if (id >= 0) {
            sua = database.layHoatDong(id);
            if (sua == null) {
                finish();
                return;
            }
        }

        if (sua != null) {
            edtTen.setText(sua.ten);
            edtLop.setText(sua.lop);
            edtDiaDiem.setText(sua.diaDiem);
            edtHocKy.setText(sua.hocKy);
            edtGhiChu.setText(sua.ghiChu);
            loai = sua.loai;
            batDau = sua.batDau;
            ketThuc = sua.ketThuc;
            lapTuan = sua.laLapTuan();
            if (lapTuan && sua.thu >= 0 && sua.thu <= 6) thuChon[sua.thu] = true;
            if (!lapTuan) ngay = sua.ngay;
            tvTieuDe.setText("Sửa hoạt động");
            btnLuu.setText("Lưu");
            btnXoa.setVisibility(View.VISIBLE);
        } else {
            int thu = getIntent().getIntExtra("thu", -1);
            if (thu < 0 || thu > 6) {
                thu = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7;
            }
            thuChon[thu] = true;
        }

        veLoai();
        veLap();
        veThu();
        capNhatNgay();
        veThoiLuong();
        capNhatGio();

        findViewById(R.id.btnQuayLai).setOnClickListener(v -> finish());
        tvNgay.setOnClickListener(v -> chonNgay());
        tvBatDau.setOnClickListener(v -> chonGio(true));
        tvKetThuc.setOnClickListener(v -> chonGio(false));
        btnLuu.setOnClickListener(v -> luu());
        btnXoa.setOnClickListener(v -> {
            database.xoaHoatDong(sua.id);
            Toast.makeText(this, "Đã xóa hoạt động", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // ====================== Các hàng chip ======================

    private TextView taoChip(String s, boolean bat) {
        TextView c = taoText(this, s, 14, bat ? CHU_TREN_NEON : CHU_DAM, true);
        c.setGravity(Gravity.CENTER);
        c.setBackground(nen(this, bat ? TIM : THE, 22));
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(0, dp(this, 44), 1f);
        p.setMargins(dp(this, 3), 0, dp(this, 3), 0);
        c.setLayoutParams(p);
        return c;
    }

    private void veLoai() {
        khungLoai.removeAllViews();
        for (int i = 0; i < HoatDong.TEN_LOAI.length; i++) {
            final int idx = i;
            TextView c = taoChip(HoatDong.TEN_LOAI[i], loai == i);
            c.setOnClickListener(v -> {
                loai = idx;
                veLoai();
            });
            khungLoai.addView(c);
        }
    }

    private void veLap() {
        khungLap.removeAllViews();
        String[] ten = {"Hằng tuần", "Một ngày"};
        for (int i = 0; i < 2; i++) {
            final boolean tuan = (i == 0);
            TextView c = taoChip(ten[i], lapTuan == tuan);
            c.setOnClickListener(v -> {
                lapTuan = tuan;
                veLap();
            });
            khungLap.addView(c);
        }
        khungChonThu.setVisibility(lapTuan ? View.VISIBLE : View.GONE);
        khungChonNgay.setVisibility(lapTuan ? View.GONE : View.VISIBLE);
    }

    private void veThu() {
        khungThu.removeAllViews();
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            TextView c = taoChip(TEN_THU[i], thuChon[i]);
            c.setOnClickListener(v -> {
                thuChon[idx] = !thuChon[idx];
                veThu();
            });
            khungThu.addView(c);
        }
    }

    // ====================== Chọn ngày và giờ ======================

    private static String ngayHienThi(String s) {
        if (s == null || s.length() != 10) return "Chọn ngày";
        return s.substring(8, 10) + "/" + s.substring(5, 7) + "/" + s.substring(0, 4);
    }

    private void capNhatNgay() {
        tvNgay.setText(ngay.isEmpty() ? "Chọn ngày" : ngayHienThi(ngay));
        tvNgay.setTextColor(ngay.isEmpty() ? CHU_MO : CHU_DAM);
    }

    private String thoiLuongChu(int phut) {
        int h = phut / 60, m = phut % 60;
        if (h == 0) return m + " phút";
        if (m == 0) return h + " giờ";
        return h + " giờ " + m + " phút";
    }

    private void capNhatGio() {
        tvBatDau.setText(HoatDong.gio(batDau));
        tvKetThuc.setText(HoatDong.gio(ketThuc));

        int dai = ketThuc - batDau;
        if (dai > 0) {
            tvThoiLuong.setText("Thời lượng: " + thoiLuongChu(dai));
            tvThoiLuong.setTextColor(CHU_XAM);
        } else {
            tvThoiLuong.setText("Giờ kết thúc phải sau giờ bắt đầu");
            tvThoiLuong.setTextColor(DO_TRUNG);
        }
        veThoiLuong();
    }

    /** Hàng nút chọn nhanh thời lượng: đặt giờ kết thúc theo giờ bắt đầu. */
    private void veThoiLuong() {
        khungThoiLuong.removeAllViews();
        int[] phut = {30, 60, 90, 120, 180};
        String[] ten = {"30p", "1 giờ", "1,5 giờ", "2 giờ", "3 giờ"};
        for (int i = 0; i < phut.length; i++) {
            final int p = phut[i];
            TextView c = taoChip(ten[i], ketThuc - batDau == p);
            c.setTextSize(13);
            c.setOnClickListener(v -> {
                ketThuc = Math.min(batDau + p, 24 * 60 - 1);
                capNhatGio();
            });
            khungThoiLuong.addView(c);
        }
    }

    private static String ngayStr(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }

    private static Calendar tuNgayStr(String s) {
        Calendar c = Calendar.getInstance();
        if (s != null && s.length() == 10) {
            try {
                c.set(Integer.parseInt(s.substring(0, 4)),
                        Integer.parseInt(s.substring(5, 7)) - 1,
                        Integer.parseInt(s.substring(8, 10)));
            } catch (NumberFormatException ignored) {}
        }
        return c;
    }

    private void chonNgay() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_ngay, null);
        bs.setContentView(v);
        ((View) v.getParent()).setBackgroundColor(android.graphics.Color.TRANSPARENT);

        final String[] chon = {ngay};
        final Calendar thang = tuNgayStr(ngay);
        thang.set(Calendar.DAY_OF_MONTH, 1);

        // Tên các thứ
        LinearLayout tenThu = v.findViewById(R.id.khungTenThu);
        for (String t : TEN_THU) {
            TextView tv = taoText(this, t, 13, CHU_XAM, true);
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));
            tenThu.addView(tv);
        }

        // Chip chọn nhanh
        LinearLayout nhanh = v.findViewById(R.id.khungNhanh);
        String[] ten = {"Hôm nay", "Ngày mai", "Cuối tuần", "Tuần sau"};
        for (int i = 0; i < 4; i++) {
            final int k = i;
            TextView c = taoChip(ten[i], false);
            c.setTextSize(13);
            c.setOnClickListener(x -> {
                Calendar d = Calendar.getInstance();
                int viTri = (d.get(Calendar.DAY_OF_WEEK) + 5) % 7;
                if (k == 1) d.add(Calendar.DAY_OF_MONTH, 1);
                else if (k == 2) d.add(Calendar.DAY_OF_MONTH, 5 - viTri < 0 ? 6 : 5 - viTri);
                else if (k == 3) d.add(Calendar.DAY_OF_MONTH, 7);
                chon[0] = ngayStr(d);
                thang.setTime(d.getTime());
                thang.set(Calendar.DAY_OF_MONTH, 1);
                veLich(v, chon, thang);
            });
            nhanh.addView(c);
        }

        v.findViewById(R.id.btnThangTruoc).setOnClickListener(x -> {
            thang.add(Calendar.MONTH, -1);
            veLich(v, chon, thang);
        });
        v.findViewById(R.id.btnThangSau).setOnClickListener(x -> {
            thang.add(Calendar.MONTH, 1);
            veLich(v, chon, thang);
        });
        v.findViewById(R.id.btnXongNgay).setOnClickListener(x -> {
            if (chon[0].isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn một ngày",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            ngay = chon[0];
            capNhatNgay();
            bs.dismiss();
        });

        veLich(v, chon, thang);
        bs.show();
    }

    /** Vẽ lưới ngày của tháng đang xem. */
    private void veLich(View goc, String[] chon, Calendar thang) {
        ((TextView) goc.findViewById(R.id.tvThang)).setText(
                "Tháng " + (thang.get(Calendar.MONTH) + 1) + " / "
                        + thang.get(Calendar.YEAR));

        LinearLayout luoi = goc.findViewById(R.id.khungLuoi);
        luoi.removeAllViews();

        int lech = (thang.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int soNgay = thang.getActualMaximum(Calendar.DAY_OF_MONTH);
        int soHang = (lech + soNgay + 6) / 7;
        String homNay = ngayStr(Calendar.getInstance());

        for (int r = 0; r < soHang; r++) {
            LinearLayout hang = new LinearLayout(this);
            hang.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 7; col++) {
                int ngayTrongThang = r * 7 + col - lech + 1;

                LinearLayout o = new LinearLayout(this);
                o.setGravity(Gravity.CENTER);
                o.setLayoutParams(new LinearLayout.LayoutParams(0, dp(this, 44), 1f));

                if (ngayTrongThang >= 1 && ngayTrongThang <= soNgay) {
                    Calendar c = (Calendar) thang.clone();
                    c.set(Calendar.DAY_OF_MONTH, ngayTrongThang);
                    final String s = ngayStr(c);
                    boolean duocChon = s.equals(chon[0]);

                    TextView tv = taoText(this, String.valueOf(ngayTrongThang),
                            15, duocChon ? CHU_TREN_NEON : CHU_DAM, true);
                    tv.setGravity(Gravity.CENTER);
                    tv.setLayoutParams(new LinearLayout.LayoutParams(
                            dp(this, 40), dp(this, 40)));
                    android.graphics.drawable.GradientDrawable g =
                            nen(this, duocChon ? TIM : android.graphics.Color.TRANSPARENT, 20);
                    if (!duocChon && s.equals(homNay)) g.setStroke(dp(this, 2), TIM);
                    tv.setBackground(g);
                    tv.setOnClickListener(x -> {
                        chon[0] = s;
                        veLich(goc, chon, thang);
                    });
                    o.addView(tv);
                }
                hang.addView(o);
            }
            luoi.addView(hang);
        }
    }
    private NumberPicker taoCuon(Context c, int max, int giaTri) {
        NumberPicker np = new NumberPicker(c);
        np.setMinValue(0);
        np.setMaxValue(max);
        np.setValue(giaTri);
        np.setWrapSelectorWheel(true);
        np.setFormatter(v -> String.format(Locale.US, "%02d", v));
        // Tránh lỗi hiển thị số chưa định dạng ở lần đầu
        View o = np.getChildAt(0);
        if (o instanceof EditText) ((EditText) o).setFilters(new InputFilter[0]);
        if (Build.VERSION.SDK_INT >= 29) {
            np.setTextColor(TIM);
            np.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                    28, getResources().getDisplayMetrics()));
        }
        np.setLayoutParams(new LinearLayout.LayoutParams(dp(this, 90), WRAP));
        return np;
    }

    private void chonGio(boolean laBatDau) {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_gio, null);
        bs.setContentView(v);
        ((View) v.getParent()).setBackgroundColor(android.graphics.Color.TRANSPARENT);

        ((TextView) v.findViewById(R.id.tvTieuDeGio)).setText(
                laBatDau ? "Giờ bắt đầu" : "Giờ kết thúc");

        int hien = laBatDau ? batDau : ketThuc;
        Context ctx = new ContextThemeWrapper(this, R.style.CuonGioToi);
        NumberPicker gio = taoCuon(ctx, 23, hien / 60);
        NumberPicker phut = taoCuon(ctx, 59, hien % 60);
        TextView hai = taoText(this, ":", 28, CHU_DAM, true);
        hai.setPadding(dp(this, 8), 0, dp(this, 8), 0);

        LinearLayout khung = v.findViewById(R.id.khungCuon);
        khung.addView(gio);
        khung.addView(hai);
        khung.addView(phut);

        v.findViewById(R.id.btnXongGio).setOnClickListener(x -> {
            // Chốt giá trị đang gõ dở trong ô số
            gio.clearFocus();
            phut.clearFocus();
            int moi = gio.getValue() * 60 + phut.getValue();
            if (laBatDau) {
                int dai = ketThuc - batDau;          // giữ nguyên độ dài
                batDau = moi;
                ketThuc = Math.min(moi + Math.max(dai, 15), 24 * 60 - 1);
            } else {
                ketThuc = moi;
            }
            capNhatGio();
            bs.dismiss();
        });
        bs.show();
    }
    private static int thuCuaNgay(String s) {
        Calendar c = Calendar.getInstance();
        c.set(Integer.parseInt(s.substring(0, 4)),
                Integer.parseInt(s.substring(5, 7)) - 1,
                Integer.parseInt(s.substring(8, 10)));
        return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    private HoatDong tao(String ten, int thu, String ngayChon) {
        HoatDong h = new HoatDong();
        if (sua != null) h.id = sua.id;
        h.ten = ten;
        h.loai = loai;
        h.thu = thu;
        h.ngay = ngayChon;
        h.batDau = batDau;
        h.ketThuc = ketThuc;
        h.lop = edtLop.getText().toString().trim();
        h.diaDiem = edtDiaDiem.getText().toString().trim();
        h.hocKy = edtHocKy.getText().toString().trim();
        h.ghiChu = edtGhiChu.getText().toString().trim();
        return h;
    }

    private void luu() {
        String ten = edtTen.getText().toString().trim();
        if (ten.isEmpty()) {
            edtTen.setError("Vui lòng nhập tên hoạt động");
            edtTen.requestFocus();
            return;
        }
        if (ketThuc <= batDau) {
            Toast.makeText(this, "Giờ kết thúc phải sau giờ bắt đầu",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<HoatDong> ds = new ArrayList<>();
        if (lapTuan) {
            for (int t = 0; t < 7; t++) {
                if (!thuChon[t]) continue;
                HoatDong h = tao(ten, t, "");
                // Thứ gốc của hoạt động đang sửa thì cập nhật, các thứ khác thêm mới
                if (sua != null && t != sua.thu) h.id = 0;
                ds.add(h);
            }
            if (ds.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một thứ",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (ngay.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ngày",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            ds.add(tao(ten, -1, ngay));
        }

        // ---- Kiểm tra trùng giờ ----
        ArrayList<HoatDong> tatCa = database.layTatCaHoatDong();
        StringBuilder sb = new StringBuilder();
        int dem = 0;
        for (HoatDong h : ds) {
            for (HoatDong o : tatCa) {
                if (o.id == h.id && sua != null) continue; // chính nó
                if (!cungNgay(h, o) || !h.giaoNhau(o)) continue;
                dem++;
                if (dem <= 5) {
                    sb.append("\n• ").append(o.ten).append("  ")
                            .append(o.khoangGio()).append("  (")
                            .append(o.laLapTuan() ? TEN_THU_DAY[o.thu]
                                    : ngayHienThi(o.ngay)).append(")");
                }
            }
        }

        if (dem == 0) {
            ghi(ds);
            return;
        }
        if (dem > 5) sb.append("\n... và ").append(dem - 5).append(" hoạt động khác");
        new AlertDialog.Builder(this, R.style.HopThoaiToi)
                .setTitle("Trùng giờ")
                .setMessage("Hoạt động này giao nhau về giờ với:" + sb)
                .setPositiveButton("Vẫn lưu", (d, w) -> ghi(ds))
                .setNegativeButton("Sửa lại", null)
                .show();
    }

    /** Hai hoạt động có rơi vào cùng một ngày cụ thể không. */
    private boolean cungNgay(HoatDong a, HoatDong b) {
        if (a.laLapTuan() && b.laLapTuan()) return a.thu == b.thu;
        if (!a.laLapTuan() && !b.laLapTuan()) return a.ngay.equals(b.ngay);
        HoatDong lap = a.laLapTuan() ? a : b;
        HoatDong mot = a.laLapTuan() ? b : a;
        return thuCuaNgay(mot.ngay) == lap.thu;
    }

    private void ghi(ArrayList<HoatDong> ds) {
        boolean ok = true;
        boolean daSua = false;
        for (HoatDong h : ds) {
            if (sua != null && h.id == sua.id && h.id != 0) {
                if (!database.suaHoatDong(h)) ok = false;
                daSua = true;
            } else {
                h.id = 0;
                if (database.themHoatDong(h) == -1) ok = false;
            }
        }
        // Bỏ chọn thứ gốc: hoạt động gốc bị xóa (coi như đổi sang thứ khác)
        if (sua != null && !daSua) database.xoaHoatDong(sua.id);

        if (!ok) {
            Toast.makeText(this, "Lưu thất bại, vui lòng thử lại",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, sua != null ? "Đã lưu"
                : "Đã thêm " + ds.size() + " hoạt động", Toast.LENGTH_SHORT).show();
        finish();
    }
    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}