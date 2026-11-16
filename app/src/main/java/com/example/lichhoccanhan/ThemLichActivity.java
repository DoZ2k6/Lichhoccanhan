package com.example.lichhoccanhan;

import static com.example.lichhoccanhan.TienIch.*;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;

public class ThemLichActivity extends AppCompatActivity {

    LichDatabase database;
    LinearLayout khungNgay, khungDanhSach;
    TextView tvTenThu;

    // bang[thu][buoi][tiet] = tiết đang nhập (null = trống)
    Tiet[][][] bang;
    int viTriChon;
    boolean daSua = false; // true khi người dùng đã thay đổi dữ liệu

    // Môn đang được thêm/sửa ở trang NhapMonActivity
    int thuDangNhap, buoiDangNhap;
    NhomMon monDangSua; // null = đang thêm mới

    private final ActivityResultLauncher<Intent> nhapMon =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), kq -> {
                        if (kq.getResultCode() != RESULT_OK || kq.getData() == null) {
                            return;
                        }
                        xuLyKetQua(kq.getData());
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_them_lich);

        // Chừa chỗ cho thanh hệ thống và bàn phím
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.ime());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        database = new LichDatabase(this);
        bang = database.layBang();

        khungNgay = findViewById(R.id.khungNgay);
        khungDanhSach = findViewById(R.id.khungDanhSach);
        tvTenThu = findViewById(R.id.tvTenThu);

        int homNay = (Calendar.getInstance()
                .get(Calendar.DAY_OF_WEEK) + 5) % 7;
        viTriChon = getIntent().getIntExtra("thu", homNay);
        if (viTriChon < 0 || viTriChon > 6) viTriChon = homNay;

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        xacNhanThoat();
                    }
                });
        findViewById(R.id.btnQuayLai).setOnClickListener(
                v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.btnLuu).setOnClickListener(v -> luu());

        veHangNgay();
        veDanhSach();
    }

    private void veHangNgay() {
        TienIch.veHangNgay(this, khungNgay, demTheoThu(bang), viTriChon, vt -> {
            anBanPhim();
            viTriChon = vt;
            veHangNgay();
            veDanhSach();
        });
    }

    private void veDanhSach() {
        tvTenThu.setText(TEN_THU_DAY[viTriChon]);
        khungDanhSach.removeAllViews();
        veBuoi("☀️  Buổi sáng", 0);
        veBuoi("🌤️  Buổi chiều", 1);
    }

    private void veBuoi(String tieuDe, int buoi) {
        final int thu = viTriChon;

        LinearLayout the = taoThe(this, 18);

        TextView tvTieuDe = taoText(this, tieuDe, 19, CHU_DAM, true);
        tvTieuDe.setLayoutParams(lp(this, WRAP, WRAP, 0, 0, 0, 10));
        the.addView(tvTieuDe);

        // Các môn đã thêm
        for (NhomMon n : gomMon(bang[thu][buoi])) {
            the.addView(taoDongMon(thu, buoi, n));
        }

        // Dòng "Thêm lịch học"
        TextView them = taoText(this, "Thêm lịch học", 16, CHU_DAM, false);
        them.setGravity(Gravity.CENTER_VERTICAL);
        them.setPadding(dp(this, 24), 0, dp(this, 16), 0);
        them.setBackground(nen(this, O_TRONG, 16));
        them.setLayoutParams(lp(this, MATCH, dp(this, 64), 0, 6, 0, 6));
        them.setOnClickListener(v -> moTrangNhap(thu, buoi, null));
        the.addView(them);

        khungDanhSach.addView(the);
    }

    /** Một dòng môn đã thêm: tên môn + dòng phụ (tiết, lớp, phòng). */
    private View taoDongMon(int thu, int buoi, NhomMon n) {
        LinearLayout dong = new LinearLayout(this);
        dong.setOrientation(LinearLayout.VERTICAL);
        dong.setGravity(Gravity.CENTER_VERTICAL);
        dong.setPadding(dp(this, 24), dp(this, 12), dp(this, 16), dp(this, 12));
        dong.setBackground(nen(this, O_TRONG, 16));
        dong.setLayoutParams(lp(this, MATCH, WRAP, 0, 6, 0, 6));

        TextView ten = taoText(this, n.ten, 17, CHU_DAM, true);
        ten.setSingleLine(true);
        ten.setEllipsize(TextUtils.TruncateAt.END);

        StringBuilder sb = new StringBuilder("Tiết " + tietChu(n));
        if (!n.lop.isEmpty()) sb.append("  ·  Lớp ").append(n.lop);
        if (!n.phong.isEmpty()) sb.append("  ·  Phòng ").append(n.phong);
        TextView phu = taoText(this, sb.toString(), 13, TIM, true);
        phu.setPadding(0, dp(this, 2), 0, 0);

        dong.addView(ten);
        dong.addView(phu);
        dong.setOnClickListener(v -> moTrangNhap(thu, buoi, n));
        return dong;
    }

    // ====================== Mở trang thêm / sửa môn ======================

    private void moTrangNhap(int thu, int buoi, NhomMon sua) {
        anBanPhim();
        thuDangNhap = thu;
        buoiDangNhap = buoi;
        monDangSua = sua;

        // Tiết nào đang bị môn khác chiếm (bỏ qua các tiết của chính môn đang sửa)
        String[] tietBan = new String[SO_TIET];
        for (int k = 0; k < SO_TIET; k++) {
            boolean cuaChinhNo = sua != null && k >= sua.dau && k <= sua.cuoi;
            Tiet t = bang[thu][buoi][k];
            tietBan[k] = (t == null || cuaChinhNo) ? "" : t.tenMon;
        }

        Intent i = new Intent(this, NhapMonActivity.class);
        i.putExtra("sua", sua != null);
        i.putExtra("tietBan", tietBan);
        if (sua != null) {
            i.putExtra("ten", sua.ten);
            i.putExtra("lop", sua.lop);
            i.putExtra("phong", sua.phong);
            i.putExtra("dau", sua.dau);
            i.putExtra("cuoi", sua.cuoi);
        }
        nhapMon.launch(i);
    }

    /** Nhận kết quả từ NhapMonActivity. */
    private void xuLyKetQua(Intent d) {
        Tiet[] cacTiet = bang[thuDangNhap][buoiDangNhap];

        // Xóa các tiết cũ của môn đang sửa
        if (monDangSua != null) {
            for (int k = monDangSua.dau; k <= monDangSua.cuoi; k++) {
                cacTiet[k] = null;
            }
        }

        // Nếu không phải xóa thì ghi môn mới
        if (!d.getBooleanExtra("xoa", false)) {
            String ten = d.getStringExtra("ten");
            String lop = d.getStringExtra("lop");
            String phong = d.getStringExtra("phong");
            int dau = d.getIntExtra("dau", 0);
            int cuoi = d.getIntExtra("cuoi", 0);
            for (int k = dau; k <= cuoi; k++) {
                Tiet t = new Tiet();
                t.tenMon = ten;
                t.tenLop = lop;
                t.phong = phong;
                cacTiet[k] = t;
            }
        }

        daSua = true;
        monDangSua = null;
        veDanhSach();
        veHangNgay();
    }

    // ====================== Tiện ích ======================

    private void anBanPhim() {
        View f = getCurrentFocus();
        if (f != null) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(f.getWindowToken(), 0);
            f.clearFocus();
        }
    }

    private void xacNhanThoat() {
        if (!daSua) {
            finish();
            return;
        }
        new AlertDialog.Builder(this, R.style.HopThoaiToi)
                .setTitle("Thoát mà chưa lưu?")
                .setMessage("Các thay đổi sẽ bị mất.")
                .setPositiveButton("Thoát", (d, w) -> finish())
                .setNegativeButton("Ở lại", null)
                .show();
    }

    private void luu() {
        anBanPhim();

        if (database.luuBang(bang)) {
            Toast.makeText(this, "Đã lưu thời khóa biểu",
                    Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Lưu thất bại, vui lòng thử lại",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}