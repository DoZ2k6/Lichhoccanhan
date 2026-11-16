package com.example.lichhoccanhan;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/** Đặt báo thức hằng ngày và hiện thông báo lịch trong ngày (hoặc ngày mai). */
public final class NhacNho {

    private NhacNho() {}

    // ====== Chỉnh ở đây ======
    private static final String GIO_NHAC = "06:00";        // đổi giờ để test
    private static final boolean NHAC_NGAY_MAI = false;    // true = báo lịch ngày mai
    // =========================

    private static final String KENH = "nhac_lich_hoc";
    private static final int MA_BAO = 1001;
    private static final int MA_THONG_BAO = 2001;

    public static void taoKenh(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel k = new NotificationChannel(
                KENH, "Nhắc lịch học", NotificationManager.IMPORTANCE_HIGH);
        k.setDescription("Báo các hoạt động trong ngày");
        c.getSystemService(NotificationManager.class)
                .createNotificationChannel(k);
    }

    /** Mở trang cài đặt kênh để đổi âm thanh. */
    public static void moCaiDatAmThanh(Context c) {
        taoKenh(c);
        Intent i;
        if (Build.VERSION.SDK_INT >= 26) {
            i = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_CHANNEL_ID, KENH);
        } else {
            i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        }
        i.putExtra(Settings.EXTRA_APP_PACKAGE, c.getPackageName());
        c.startActivity(i);
    }

    private static PendingIntent pendingBao(Context c) {
        Intent i = new Intent(c, NhacReceiver.class);
        return PendingIntent.getBroadcast(c, MA_BAO, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Hẹn báo thức lần tới (hôm nay nếu chưa quá giờ, không thì ngày mai). */
    public static void datLich(Context c) {
        int h = 6, m = 0;
        try {
            String[] p = GIO_NHAC.split(":");
            h = Integer.parseInt(p[0].trim());
            m = Integer.parseInt(p[1].trim());
        } catch (Exception ignored) {}

        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, h);
        t.set(Calendar.MINUTE, m);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (t.getTimeInMillis() <= System.currentTimeMillis()) {
            t.add(Calendar.DAY_OF_MONTH, 1);
        }

        AlarmManager am = c.getSystemService(AlarmManager.class);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                t.getTimeInMillis(), pendingBao(c));
    }

    /** Hiện thông báo và ghi lịch sử (không làm gì nếu ngày đó trống). */
    public static void hienThongBao(Context c) {
        Calendar ngay = Calendar.getInstance();
        if (NHAC_NGAY_MAI) ngay.add(Calendar.DAY_OF_MONTH, 1);

        int thu = (ngay.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        String ngayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(ngay.getTime());

        LichDatabase db = new LichDatabase(c);
        ArrayList<HoatDong> ds = db.layHoatDongTheoNgay(thu, ngayStr);

        if (ds.isEmpty()) {
            db.close();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (HoatDong h : ds) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(h.khoangGio()).append("  ").append(h.ten);
            if (!h.diaDiem.isEmpty()) sb.append(" (").append(h.diaDiem).append(")");
        }

        String tenNgay = NHAC_NGAY_MAI ? "Ngày mai · " : "";
        String tieuDe = tenNgay + TienIch.TEN_THU_DAY[thu] + ": "
                + ds.size() + " hoạt động";

        db.themThongBao(tieuDe, sb.toString());
        db.close();

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                c, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent mo = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, 0, mo,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(c, KENH)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle(tieuDe)
                .setContentText(sb.toString().split("\n")[0])
                .setStyle(new NotificationCompat.BigTextStyle().bigText(sb.toString()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pi)
                .setAutoCancel(true);

        NotificationManagerCompat.from(c).notify(MA_THONG_BAO, nb.build());
    }
}