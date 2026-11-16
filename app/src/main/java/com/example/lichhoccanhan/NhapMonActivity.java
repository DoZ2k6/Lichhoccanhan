package com.example.lichhoccanhan;

import static com.example.lichhoccanhan.TienIch.*;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Trang thêm / sửa một môn học. Kết quả trả về cho ThemLichActivity. */
public class NhapMonActivity extends AppCompatActivity {

    EditText edtTen, edtLop, edtPhong;
    LinearLayout khungTiet;
    TextView[] cacChip = new TextView[SO_TIET];
    boolean[] chon = new boolean[SO_TIET];
    String[] tietBan; // tên môn đang chiếm tiết đó ("" = trống)
    boolean cheDoSua;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nhap_mon);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.ime());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        edtTen = findViewById(R.id.edtTen);
        edtLop = findViewById(R.id.edtLop);
        edtPhong = findViewById(R.id.edtPhong);
        khungTiet = findViewById(R.id.khungTiet);
        TextView tvTieuDe = findViewById(R.id.tvTieuDe);
        TextView btnLuuMon = findViewById(R.id.btnLuuMon);
        TextView btnXoa = findViewById(R.id.btnXoa);

        // ---- Nhận dữ liệu từ màn hình trước ----
        Intent in = getIntent();
        cheDoSua = in.getBooleanExtra("sua", false);
        tietBan = in.getStringArrayExtra("tietBan");
        if (tietBan == null || tietBan.length != SO_TIET) {
            tietBan = new String[SO_TIET];
            for (int i = 0; i < SO_TIET; i++) tietBan[i] = "";
        }

        edtTen.setText(in.getStringExtra("ten"));
        edtLop.setText(in.getStringExtra("lop"));
        edtPhong.setText(in.getStringExtra("phong"));

        int dau = in.getIntExtra("dau", -1);
        int cuoi = in.getIntExtra("cuoi", -1);
        if (dau < 0) {
            // Thêm mới: chọn sẵn tiết trống đầu tiên
            for (int i = 0; i < SO_TIET; i++) {
                if (tietBan[i].isEmpty()) {
                    dau = cuoi = i;
                    break;
                }
            }
        }
        for (int i = dau; i >= 0 && i <= cuoi && i < SO_TIET; i++) chon[i] = true;

        tvTieuDe.setText(cheDoSua ? "Sửa lịch học" : "Thêm lịch học");
        btnLuuMon.setText(cheDoSua ? "Lưu" : "Thêm");
        btnXoa.setVisibility(cheDoSua ? View.VISIBLE : View.GONE);

        veChip();

        findViewById(R.id.btnQuayLai).setOnClickListener(v -> finish());
        btnLuuMon.setOnClickListener(v -> luu());
        btnXoa.setOnClickListener(v -> Xoa());
    }

    private void veChip() {
        khungTiet.removeAllViews();
        for (int i = 0; i < SO_TIET; i++) {
            final int idx = i;
            TextView chip = taoText(this, "T" + (i + 1), 16, CHU_DAM, true);
            chip.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(0, dp(this, 48), 1f);
            p.setMargins(dp(this, 3), 0, dp(this, 3), 0);
            chip.setLayoutParams(p);
            chip.setOnClickListener(v -> {
                chon[idx] = !chon[idx];
                capNhatChip();
            });
            cacChip[i] = chip;
            khungTiet.addView(chip);
        }
        capNhatChip();
    }

    private void capNhatChip() {
        for (int i = 0; i < SO_TIET; i++) {
            boolean b = chon[i];
            cacChip[i].setTextColor(b ? CHU_TREN_NEON : CHU_DAM);
            cacChip[i].setBackground(nen(this, b ? TIM : THE, 24));
        }
    }

    private void luu() {
        String ten = edtTen.getText().toString().trim();
        String lop = edtLop.getText().toString().trim();
        String phong = edtPhong.getText().toString().trim();

        if (ten.isEmpty()) {
            edtTen.setError("Vui lòng nhập tên môn học");
            edtTen.requestFocus();
            return;
        }

        int dau = -1, cuoi = -1;
        for (int i = 0; i < SO_TIET; i++) {
            if (chon[i]) {
                if (dau < 0) dau = i;
                cuoi = i;
            }
        }
        if (dau < 0) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một tiết",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = dau; i <= cuoi; i++) {
            if (!chon[i]) {
                Toast.makeText(this, "Các tiết phải liền nhau",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!tietBan[i].isEmpty()) {
                Toast.makeText(this, "Tiết " + (i + 1) + " đã có môn \""
                        + tietBan[i] + "\"", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Intent kq = new Intent();
        kq.putExtra("ten", ten);
        kq.putExtra("lop", lop);
        kq.putExtra("phong", phong);
        kq.putExtra("dau", dau);
        kq.putExtra("cuoi", cuoi);
        setResult(RESULT_OK, kq);
        finish();
    }

    private void Xoa() {
        Intent kq = new Intent();
        kq.putExtra("xoa", true);
        setResult(RESULT_OK, kq);
        finish();
    }}