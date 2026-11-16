package com.example.lichhoccanhan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Nhận báo thức (hiện thông báo, đặt lại cho ngày mai) và sự kiện khởi động máy. */
public class NhacReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NhacNho.datLich(context); // khởi động lại máy làm mất báo thức cũ
            return;
        }
        NhacNho.taoKenh(context);
        NhacNho.hienThongBao(context);
        NhacNho.datLich(context); // đặt tiếp cho ngày mai
    }
}