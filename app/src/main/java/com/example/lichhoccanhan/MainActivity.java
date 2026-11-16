package com.example.lichhoccanhan;

import static com.example.lichhoccanhan.TienIch.*;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // Khung giờ dùng để tính thời gian trống (phút trong ngày)
    private static final int GIO_MO = 6 * 60;
    private static final int GIO_DONG = 22 * 60;

    LichDatabase database;

    View khungTrong, khungNoiDung, thanhDuoi, khungTimKiem;
    LinearLayout khungNgay, khungDanhSach;
    TextView tvNgayHienTai, tvTheoNgay, tvTheoTuan;
    EditText edtTimKiem;

    boolean xemTuan = false;
    boolean dangTim = false;
    String tuKhoa = "";
    int viTriChon; // 0 = T2 ... 6 = CN

    View thanhTieuDe, khungPhuCuon;
    LinearLayout khungPhu;
    int tabHienTai = 0; // 0 = Thời khóa biểu, 1 = Thống kê, 2 = Nhắc nhở, 3 = Tôi

    private final ActivityResultLauncher<String> xinQuyenTB =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), ok -> {});

    private final ActivityResultLauncher<String[]> chonFile =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(), uri -> {
                        if (uri != null) docFileNhap(uri);
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT));
        setContentView(R.layout.activity_main);

        database = new LichDatabase(this);

        tvNgayHienTai = findViewById(R.id.tvNgayHienTai);
        khungTrong = findViewById(R.id.khungTrong);
        khungNoiDung = findViewById(R.id.khungNoiDung);
        khungNgay = findViewById(R.id.khungNgay);
        khungDanhSach = findViewById(R.id.khungDanhSach);
        tvTheoNgay = findViewById(R.id.tvTheoNgay);
        tvTheoTuan = findViewById(R.id.tvTheoTuan);
        thanhDuoi = findViewById(R.id.thanhDuoi);
        khungTimKiem = findViewById(R.id.khungTimKiem);
        edtTimKiem = findViewById(R.id.edtTimKiem);
        thanhTieuDe = findViewById(R.id.thanhTieuDe);
        khungPhuCuon = findViewById(R.id.khungPhuCuon);
        khungPhu = findViewById(R.id.khungPhu);

        final int padDuoi = thanhDuoi.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, 0);
                    thanhDuoi.setPadding(0, thanhDuoi.getPaddingTop(),
                            0, padDuoi + bars.bottom);
                    return insets;
                });

        viTriChon = viTriHomNay();

        // ---- Tìm kiếm ----
        findViewById(R.id.btnTimKiem).setOnClickListener(v -> batTatTimKiem());
        edtTimKiem.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable e) {
                tuKhoa = e.toString().trim();
                capNhat();
            }
        });

        View.OnClickListener moNhap = v -> moBangThem();
        findViewById(R.id.btnSuaLich).setOnClickListener(moNhap);
        findViewById(R.id.btnNhap).setOnClickListener(moNhap);

        View.OnClickListener quetQR = v -> Toast.makeText(this,
                "Chức năng quét mã QR sẽ được làm tiếp.",
                Toast.LENGTH_SHORT).show();
        findViewById(R.id.btnQuetQR).setOnClickListener(quetQR);
        findViewById(R.id.btnQuetQR2).setOnClickListener(quetQR);

        // ---- Thanh menu dưới: 4 tab trong cùng một màn hình ----
        findViewById(R.id.tabThoiKhoaBieu).setOnClickListener(v -> chonTab(0));
        findViewById(R.id.tabCongViec).setOnClickListener(v -> chonTab(1));
        findViewById(R.id.tabNhacNho).setOnClickListener(v -> chonTab(2));
        findViewById(R.id.tabHuongDan).setOnClickListener(v -> chonTab(3));

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (tabHienTai != 0) {
                            chonTab(0);
                        } else {
                            finish();
                        }
                    }
                });

        tvTheoNgay.setOnClickListener(v -> {
            xemTuan = false;
            capNhat();
        });
        tvTheoTuan.setOnClickListener(v -> {
            xemTuan = true;
            capNhat();
        });

        // ---- Nhắc lịch học ----
        NhacNho.taoKenh(this);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            xinQuyenTB.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        NhacNho.datLich(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tabHienTai == 2) {
            chonTab(2);
        } else {
            capNhat();
        }
    }

    private int viTriHomNay() {
        return (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    /** Ngày "yyyy-MM-dd" của thứ thu (0 = T2 ... 6 = CN) trong tuần hiện tại. */
    private String ngayCuaThu(int thu) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, thu - viTriHomNay());
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private ArrayList<HoatDong> hoatDongNgay(int thu) {
        return database.layHoatDongTheoNgay(thu, ngayCuaThu(thu));
    }

    /** Thêm hoạt động mới (thu = thứ được chọn sẵn). */
    private void moHoatDong(int thu) {
        Intent i = new Intent(this, HoatDongActivity.class);
        i.putExtra("thu", thu);
        startActivity(i);
    }

    /** Sửa / xóa một hoạt động có sẵn. */
    private void moSuaHoatDong(long id) {
        Intent i = new Intent(this, HoatDongActivity.class);
        i.putExtra("id", id);
        startActivity(i);
    }

    /** Bảng "Thêm lịch biểu": chọn tạo thủ công hoặc nhập từ file. */
    private void moBangThem() {
        BottomSheetDialog bs = new BottomSheetDialog(this);
        View v = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_them_lich, null);
        bs.setContentView(v);

        ((View) v.getParent()).setBackgroundColor(Color.TRANSPARENT);

        v.findViewById(R.id.btnTaoThuCong).setOnClickListener(x -> {
            bs.dismiss();
            moHoatDong(viTriChon);
        });
        v.findViewById(R.id.btnNhapTuFile).setOnClickListener(x -> {
            bs.dismiss();
            chonFile.launch(new String[]{
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel",
                    "application/octet-stream"});
        });
        bs.show();
    }

    // ====================== Nhập từ file ======================

    private android.app.AlertDialog.Builder hopThoai(String tieuDe, String noiDung) {
        return new android.app.AlertDialog.Builder(this, R.style.HopThoaiToi)
                .setTitle(tieuDe)
                .setMessage(noiDung);
    }

    private void docFileNhap(Uri uri) {
        Toast.makeText(this, "Đang đọc file...", Toast.LENGTH_SHORT).show();
        final android.content.Context ctx = getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            NhapFile.KetQua kq = NhapFile.docExcel(ctx, uri);
            runOnUiThread(() -> {
                if (!isFinishing()) xemTruocNhap(kq);
            });
        });
    }

    private HoatDong tuDong(NhapFile.Dong d) {
        HoatDong h = new HoatDong();
        h.ten = d.ten;
        h.loai = HoatDong.HOC;
        h.thu = d.thu;
        h.batDau = d.batDau;
        h.ketThuc = d.ketThuc;
        h.lop = d.lop;
        h.diaDiem = d.phong;
        h.hocKy = d.hocKy;
        return h;
    }
    /** Các hoạt động lặp tuần đang có mà giao giờ với hoạt động mới. */
    private ArrayList<HoatDong> timTrung(ArrayList<HoatDong> ds, HoatDong moi) {
        ArrayList<HoatDong> kq = new ArrayList<>();
        for (HoatDong o : ds) {
            if (o.laLapTuan() && o.thu == moi.thu && o.giaoNhau(moi)) kq.add(o);
        }
        return kq;
    }

    private String gomLoi(NhapFile.KetQua kq) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kq.loi.size() && i < 5; i++) {
            sb.append("\n• ").append(kq.loi.get(i));
        }
        if (kq.loi.size() > 5) {
            sb.append("\n... và ").append(kq.loi.size() - 5).append(" lỗi khác");
        }
        return sb.toString();
    }

    private void xemTruocNhap(NhapFile.KetQua kq) {
        if (kq.loiChung != null) {
            hopThoai("Không đọc được file", kq.loiChung)
                    .setPositiveButton("Đóng", null).show();
            return;
        }
        if (kq.dong.isEmpty()) {
            hopThoai("Không có dòng hợp lệ",
                    "File không có môn học nào đọc được."
                            + (kq.loi.isEmpty() ? "" : "\n" + gomLoi(kq)))
                    .setPositiveButton("Đóng", null).show();
            return;
        }

        ArrayList<HoatDong> hienCo = database.layTatCaHoatDong();
        StringBuilder trung = new StringBuilder();
        int soTrung = 0;
        for (NhapFile.Dong d : kq.dong) {
            HoatDong h = tuDong(d);
            ArrayList<HoatDong> tr = timTrung(hienCo, h);
            if (tr.isEmpty()) continue;
            soTrung++;
            if (soTrung <= 8) {
                trung.append("\n• ").append(TEN_THU_DAY[h.thu]).append(" ")
                        .append(h.khoangGio()).append(": \"").append(h.ten)
                        .append("\" trùng \"").append(tr.get(0).ten).append("\"");
            }
        }
        if (soTrung > 8) {
            trung.append("\n... và ").append(soTrung - 8).append(" dòng trùng khác");
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Đọc được ").append(kq.dong.size()).append(" dòng hợp lệ");
        if (!kq.loi.isEmpty()) {
            msg.append(", ").append(kq.loi.size()).append(" dòng lỗi (bị bỏ qua)");
        }
        msg.append(".");
        if (!kq.loi.isEmpty()) msg.append("\n").append(gomLoi(kq));

        if (soTrung == 0) {
            hopThoai("Nhập từ file", msg.toString())
                    .setPositiveButton("Nhập", (d, w) -> apDungNhap(kq, false))
                    .setNegativeButton("Hủy", null)
                    .show();
            return;
        }

        msg.append("\n\nCó ").append(soTrung)
                .append(" dòng trùng giờ với lịch hiện tại:").append(trung);
        hopThoai("Phát hiện lịch trùng", msg.toString())
                .setPositiveButton("Bỏ qua trùng", (d, w) -> apDungNhap(kq, false))
                .setNegativeButton("Ghi đè", (d, w) -> apDungNhap(kq, true))
                .setNeutralButton("Hủy", null)
                .show();
    }

    private void apDungNhap(NhapFile.KetQua kq, boolean ghiDe) {
        ArrayList<HoatDong> ds = database.layTatCaHoatDong();
        int them = 0, boQua = 0;

        for (NhapFile.Dong d : kq.dong) {
            HoatDong h = tuDong(d);
            ArrayList<HoatDong> tr = timTrung(ds, h);
            if (!tr.isEmpty()) {
                if (!ghiDe) {
                    boQua++;
                    continue;
                }
                for (HoatDong o : tr) {
                    database.xoaHoatDong(o.id);
                    ds.remove(o);
                }
            }
            long id = database.themHoatDong(h);
            if (id != -1) {
                h.id = id;
                ds.add(h);
                them++;
            }
        }

        database.themLichSuNhap("Excel", them, boQua);
        Toast.makeText(this, "Đã nhập " + them + " hoạt động"
                        + (boQua > 0 ? ", bỏ qua " + boQua + " dòng trùng" : ""),
                Toast.LENGTH_LONG).show();
        capNhat();
    }

    // ====================== Cập nhật màn hình ======================

    private void capNhat() {
        if (tabHienTai != 0) return;

        Calendar homNay = Calendar.getInstance();
        int homNayIdx = viTriHomNay();

        tvNgayHienTai.setText(TEN_THU_DAY[homNayIdx] + ", "
                + new SimpleDateFormat("dd/MM/yyyy", Locale.US)
                .format(homNay.getTime()));

        ArrayList<HoatDong> tatCa = database.layTatCaHoatDong();
        boolean chuaCoLich = tatCa.isEmpty();

        khungTrong.setVisibility(chuaCoLich ? View.VISIBLE : View.GONE);
        khungNoiDung.setVisibility(chuaCoLich ? View.GONE : View.VISIBLE);
        if (chuaCoLich) return;

        datCheDo(tvTheoNgay, !xemTuan);
        datCheDo(tvTheoTuan, xemTuan);

        // Đang tìm kiếm: hiện kết quả trên toàn bộ hoạt động
        if (dangTim && !tuKhoa.isEmpty()) {
            khungNgay.setVisibility(View.GONE);
            khungDanhSach.removeAllViews();
            veKetQuaTim(tatCa);
            return;
        }

        khungNgay.setVisibility(xemTuan ? View.GONE : View.VISIBLE);
        khungDanhSach.removeAllViews();

        if (xemTuan) {
            veTuan(homNayIdx);
        } else {
            int[] dem = new int[7];
            for (int t = 0; t < 7; t++) dem[t] = hoatDongNgay(t).size();

            veHangNgay(this, khungNgay, dem, viTriChon, vt -> {
                viTriChon = vt;
                capNhat();
            });

            ArrayList<HoatDong> ds = hoatDongNgay(viTriChon);
            veDanhSachNgay(ds);
            veThoiGianTrong(ds);
        }
    }

    private void datCheDo(TextView tv, boolean bat) {
        tv.setTextColor(bat ? TIM : CHU_XAM);
        tv.setBackground(bat ? nen(this, THE, 16) : null);
    }

    // ====================== Chuyển tab ======================

    private void chonTab(int tab) {
        tabHienTai = tab;
        datTab(tab);

        if (tab == 0) {
            thanhTieuDe.setVisibility(View.VISIBLE);
            khungTimKiem.setVisibility(dangTim ? View.VISIBLE : View.GONE);
            khungPhuCuon.setVisibility(View.GONE);
            capNhat();
            return;
        }

        InputMethodManager imm =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edtTimKiem.getWindowToken(), 0);

        thanhTieuDe.setVisibility(View.GONE);
        khungTimKiem.setVisibility(View.GONE);
        khungTrong.setVisibility(View.GONE);
        khungNoiDung.setVisibility(View.GONE);
        khungPhuCuon.setVisibility(View.VISIBLE);
        khungPhu.removeAllViews();

        if (tab == 1) {
            new ThongKe(this, khungPhu).ve();
        } else if (tab == 2) {
            veLichSuNhac();
        } else {
            veTrangTam("Tôi");
        }
    }

    private void datTab(int chon) {
        int[] ids = {R.id.tabThoiKhoaBieu, R.id.tabCongViec,
                R.id.tabNhacNho, R.id.tabHuongDan};
        for (int i = 0; i < ids.length; i++) {
            LinearLayout tab = findViewById(ids[i]);
            int mau = (i == chon) ? TIM : CHU_MO;
            ((ImageView) tab.getChildAt(0))
                    .setImageTintList(ColorStateList.valueOf(mau));
            ((TextView) tab.getChildAt(1)).setTextColor(mau);
        }
    }

    private void veTrangTam(String tieuDe) {
        TextView t = taoText(this, tieuDe, 30, CHU_DAM, true);
        t.setLayoutParams(lp(this, WRAP, WRAP, 4, 16, 0, 16));
        khungPhu.addView(t);

        TextView p = taoText(this, "Chức năng này sẽ được làm tiếp.",
                16, CHU_XAM, false);
        p.setPadding(dp(this, 4), dp(this, 8), 0, 0);
        khungPhu.addView(p);
    }

    /** Tab Nhắc nhở: lịch sử các thông báo đã nổ. */
    private void veLichSuNhac() {
        TextView tieuDe = taoText(this, "Nhắc nhở", 30, CHU_DAM, true);
        tieuDe.setLayoutParams(lp(this, WRAP, WRAP, 4, 16, 0, 4));
        khungPhu.addView(tieuDe);

        ArrayList<LichDatabase.ThongBao> ds = database.layLichSuThongBao();

        TextView phu = taoText(this, ds.isEmpty()
                ? "Chưa có thông báo nào. Thông báo nhắc lịch học sẽ được lưu ở đây."
                : "Lịch sử thông báo nhắc lịch học", 15, CHU_XAM, false);
        phu.setPadding(dp(this, 4), 0, dp(this, 4), dp(this, 12));
        khungPhu.addView(phu);

        if (ds.isEmpty()) return;

        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.US);
        for (LichDatabase.ThongBao t : ds) {
            LinearLayout the = taoThe(this, 18);

            TextView gio = taoText(this, f.format(new Date(t.thoiGian)),
                    13, CHU_XAM, true);
            TextView ten = taoText(this, t.tieuDe, 18, CHU_DAM, true);
            ten.setPadding(0, dp(this, 4), 0, dp(this, 6));
            TextView nd = taoText(this, t.noiDung, 15, CHU_DAM, false);

            the.addView(gio);
            the.addView(ten);
            the.addView(nd);
            khungPhu.addView(the);
        }

        TextView xoa = taoText(this, "Xóa lịch sử", 16, TIM, true);
        xoa.setPadding(dp(this, 4), dp(this, 8), 0, dp(this, 8));
        xoa.setOnClickListener(v -> {
            database.xoaLichSuThongBao();
            chonTab(2);
        });
        khungPhu.addView(xoa);
    }

    // ====================== Tìm kiếm ======================

    private void batTatTimKiem() {
        dangTim = !dangTim;
        khungTimKiem.setVisibility(dangTim ? View.VISIBLE : View.GONE);

        InputMethodManager imm =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (dangTim) {
            edtTimKiem.requestFocus();
            imm.showSoftInput(edtTimKiem, InputMethodManager.SHOW_IMPLICIT);
        } else {
            edtTimKiem.setText("");
            edtTimKiem.clearFocus();
            imm.hideSoftInputFromWindow(edtTimKiem.getWindowToken(), 0);
        }
        capNhat();
    }

    private String boDau(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return t.replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT).trim();
    }

    private void veKetQuaTim(ArrayList<HoatDong> tatCa) {
        String k = boDau(tuKhoa);

        TextView tvDem = taoText(this, "", 15, CHU_XAM, true);
        tvDem.setLayoutParams(lp(this, WRAP, WRAP, 4, 0, 0, 10));
        khungDanhSach.addView(tvDem);

        int dem = 0;
        for (HoatDong h : tatCa) {
            boolean khop = boDau(h.ten).contains(k)
                    || boDau(h.lop).contains(k)
                    || boDau(h.hocKy).contains(k)
                    || boDau(h.diaDiem).contains(k);
            if (!khop) continue;
            dem++;
            khungDanhSach.addView(taoKetQua(h));
        }

        tvDem.setText(dem == 0
                ? "Không tìm thấy kết quả cho \"" + tuKhoa + "\""
                : "Tìm thấy " + dem + " hoạt động");
    }

    private String nhanNgay(HoatDong h) {
        if (h.laLapTuan()) return TEN_THU_DAY[Math.max(0, Math.min(h.thu, 6))];
        String s = h.ngay;
        return s.length() == 10
                ? s.substring(8, 10) + "/" + s.substring(5, 7) + "/" + s.substring(0, 4)
                : s;
    }

    /** "Lớp 24DTH1D · L.403 · HK1" (chỉ ghép phần có dữ liệu). */
    private String dongPhu(HoatDong h) {
        StringBuilder sb = new StringBuilder();
        if (!h.lop.isEmpty()) sb.append("Lớp ").append(h.lop);
        if (!h.diaDiem.isEmpty()) {
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append(h.diaDiem);
        }
        if (!h.hocKy.isEmpty()) {
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append(h.hocKy);
        }
        return sb.toString();
    }

    private View taoKetQua(HoatDong h) {
        LinearLayout the = taoThe(this, 16);
        the.setLayoutParams(lp(this, MATCH, WRAP, 0, 0, 0, 12));

        TextView ten = taoText(this, h.ten, 18, CHU_DAM, true);
        ten.setSingleLine(true);
        ten.setEllipsize(TextUtils.TruncateAt.END);

        TextView gio = taoText(this, nhanNgay(h) + "  ·  " + h.khoangGio()
                + "  ·  " + h.tenLoai(), 14, TIM, true);
        gio.setPadding(0, dp(this, 4), 0, 0);

        the.addView(ten);
        the.addView(gio);

        String phu = dongPhu(h);
        if (!phu.isEmpty()) {
            TextView t = taoText(this, phu, 14, CHU_XAM, true);
            t.setPadding(0, dp(this, 6), 0, 0);
            the.addView(t);
        }
        the.setOnClickListener(v -> moSuaHoatDong(h.id));
        return the;
    }

    // ====================== Chế độ THEO NGÀY ======================

    private void veDanhSachNgay(ArrayList<HoatDong> ds) {
        LinearLayout the = taoThe(this, 18);

        TextView tvTieuDe = taoText(this, "📅  " + TEN_THU_DAY[viTriChon],
                19, CHU_DAM, true);
        tvTieuDe.setLayoutParams(lp(this, WRAP, WRAP, 0, 0, 0, 8));
        the.addView(tvTieuDe);

        if (ds.isEmpty()) {
            TextView o = taoText(this, "Chưa có hoạt động", 17, CHU_MO, false);
            o.setGravity(Gravity.CENTER_VERTICAL);
            o.setPadding(dp(this, 18), 0, dp(this, 12), 0);
            GradientDrawable g = nen(this, O_TRONG, 16);
            g.setStroke(dp(this, 1), VIEN, dp(this, 5), dp(this, 4));
            o.setBackground(g);
            o.setLayoutParams(lp(this, MATCH, dp(this, 56), 0, 4, 0, 4));
            o.setOnClickListener(v -> moHoatDong(viTriChon));
            the.addView(o);
        }

        for (HoatDong h : ds) {
            boolean trung = false;
            for (HoatDong o : ds) {
                if (o != h && o.giaoNhau(h)) trung = true;
            }
            the.addView(taoMucHoatDong(h, trung));
        }

        if (!ds.isEmpty()) {
            TextView them = taoText(this, "＋  Thêm hoạt động", 15, TIM, true);
            them.setPadding(dp(this, 4), dp(this, 10), 0, 0);
            them.setOnClickListener(v -> moHoatDong(viTriChon));
            the.addView(them);
        }
        khungDanhSach.addView(the);
    }

    /** Một hoạt động trong ngày; đỏ nếu giao giờ với hoạt động khác. */
    private View taoMucHoatDong(HoatDong h, boolean trung) {
        int mau = trung ? DO_TRUNG : XANH_DAM;
        int nenMuc = trung ? Color.rgb(58, 26, 32) : XANH_NHAT;

        LinearLayout muc = new LinearLayout(this);
        muc.setOrientation(LinearLayout.VERTICAL);
        muc.setLayoutParams(lp(this, MATCH, WRAP, 0, 4, 0, 6));

        LayerDrawable ld = new LayerDrawable(new GradientDrawable[]{
                nen(this, mau, 16), nen(this, nenMuc, 16)});
        ld.setLayerInset(1, dp(this, 4), 0, 0, 0);
        muc.setBackground(ld);
        muc.setPadding(dp(this, 22), dp(this, 14), dp(this, 16), dp(this, 14));

        TextView gio = taoText(this, h.khoangGio() + "  ·  " + h.tenLoai()
                + (trung ? "  ·  Trùng giờ" : ""), 14, mau, true);

        TextView ten = taoText(this, h.ten, 18, CHU_DAM, true);
        ten.setPadding(0, dp(this, 4), 0, 0);

        muc.addView(gio);
        muc.addView(ten);

        String phu = dongPhu(h);
        if (!phu.isEmpty()) {
            TextView t = taoText(this, phu, 14, CHU_XAM, true);
            t.setPadding(0, dp(this, 4), 0, 0);
            muc.addView(t);
        }
        if (!h.ghiChu.isEmpty()) {
            TextView t = taoText(this, h.ghiChu, 13, CHU_MO, false);
            t.setPadding(0, dp(this, 4), 0, 0);
            muc.addView(t);
        }

        muc.setOnClickListener(v -> moSuaHoatDong(h.id));
        return muc;
    }

    private View dongChiTiet(String nhan, String giaTri) {
        boolean trong = giaTri == null || giaTri.trim().isEmpty();

        LinearLayout hang = new LinearLayout(this);
        hang.setOrientation(LinearLayout.HORIZONTAL);
        hang.setLayoutParams(lp(this, MATCH, WRAP, 0, 4, 0, 0));

        TextView tvNhan = taoText(this, nhan, 14, CHU_XAM, true);
        tvNhan.setLayoutParams(new LinearLayout.LayoutParams(dp(this, 110), WRAP));

        TextView tvGiaTri = taoText(this, trong ? "Chưa nhập" : giaTri, 15,
                trong ? CHU_MO : CHU_DAM, !trong);
        tvGiaTri.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));

        hang.addView(tvNhan);
        hang.addView(tvGiaTri);
        return hang;
    }

    private String thoiLuong(int phut) {
        int h = phut / 60, m = phut % 60;
        if (h == 0) return m + " phút";
        if (m == 0) return h + " giờ";
        return h + " giờ " + m + " phút";
    }

    /** Khoảng trống giữa các hoạt động trong khung 06:00 - 22:00. */
    private void veThoiGianTrong(ArrayList<HoatDong> ds) {
        LinearLayout the = taoThe(this, 18);

        TextView tvTieuDe = taoText(this, "🕓  Thời gian trống", 19, CHU_DAM, true);
        tvTieuDe.setLayoutParams(lp(this, WRAP, WRAP, 0, 0, 0, 8));
        the.addView(tvTieuDe);

        ArrayList<int[]> khoang = new ArrayList<>();
        int cur = GIO_MO;
        for (HoatDong h : ds) { // đã xếp theo giờ bắt đầu
            int bd = Math.min(h.batDau, GIO_DONG);
            if (bd > cur) khoang.add(new int[]{cur, bd});
            cur = Math.max(cur, h.ketThuc);
        }
        if (cur < GIO_DONG) khoang.add(new int[]{cur, GIO_DONG});

        if (khoang.isEmpty()) {
            the.addView(dongChiTiet("Cả ngày", "Kín lịch"));
        } else {
            int tong = 0;
            for (int[] k : khoang) {
                tong += k[1] - k[0];
                the.addView(dongChiTiet(HoatDong.gio(k[0]) + " → " + HoatDong.gio(k[1]),
                        thoiLuong(k[1] - k[0])));
            }
            TextView t = taoText(this, "Tổng trống: " + thoiLuong(tong),
                    15, TIM, true);
            t.setPadding(0, dp(this, 12), 0, 0);
            the.addView(t);
        }

        TextView ghi = taoText(this, "Tính trong khung 06:00 - 22:00", 12,
                CHU_MO, false);
        ghi.setPadding(0, dp(this, 6), 0, 0);
        the.addView(ghi);
        khungDanhSach.addView(the);
    }

    // ====================== Chế độ CẢ TUẦN ======================

    private void veTuan(int homNayIdx) {
        for (int d = 0; d < 7; d++) {
            final int thuIdx = d;
            boolean laHomNay = (d == homNayIdx);
            ArrayList<HoatDong> ds = hoatDongNgay(d);

            LinearLayout the = taoThe(this, 16);
            the.setLayoutParams(lp(this, MATCH, WRAP, 0, 0, 0, 12));
            if (laHomNay) {
                the.setBackground(nen(this, Color.rgb(20, 50, 52), 22));
            }

            TextView tieuDe = taoText(this, TEN_THU_DAY[d]
                            + (laHomNay ? "  ·  Hôm nay" : ""), 18,
                    laHomNay ? TIM : CHU_DAM, true);
            tieuDe.setPadding(0, 0, 0, dp(this, 6));
            the.addView(tieuDe);

            if (ds.isEmpty()) {
                the.addView(taoText(this, "Trống", 14, CHU_MO, false));
            }
            for (HoatDong h : ds) {
                boolean trung = false;
                for (HoatDong o : ds) {
                    if (o != h && o.giaoNhau(h)) trung = true;
                }
                LinearLayout hang = new LinearLayout(this);
                hang.setOrientation(LinearLayout.HORIZONTAL);
                hang.setLayoutParams(lp(this, MATCH, WRAP, 0, 3, 0, 3));

                TextView gio = taoText(this, h.khoangGio(), 14,
                        trung ? DO_TRUNG : XANH_DAM, true);
                gio.setLayoutParams(new LinearLayout.LayoutParams(dp(this, 118), WRAP));

                TextView ten = taoText(this, h.ten, 14, CHU_DAM, true);
                ten.setSingleLine(true);
                ten.setEllipsize(TextUtils.TruncateAt.END);
                ten.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));

                hang.addView(gio);
                hang.addView(ten);
                the.addView(hang);
            }

            the.setOnClickListener(v -> {
                viTriChon = thuIdx;
                xemTuan = false;
                capNhat();
            });
            khungDanhSach.addView(the);
        }
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}