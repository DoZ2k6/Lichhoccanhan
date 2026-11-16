package com.example.lichhoccanhan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * Các bảng:
 *  - tiet_hoc          : thời khóa biểu kiểu tiết (cũ, sẽ thay bằng hoat_dong)
 *  - hoat_dong         : hoạt động có giờ bắt đầu/kết thúc (mới)
 *  - hoc_ky            : danh sách học kỳ
 *  - cai_dat           : cài đặt dạng khóa - giá trị
 *  - lich_su_nhap      : lịch sử các lần nhập file
 *  - lich_su_thong_bao : lịch sử thông báo nhắc lịch
 */
public class LichDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "lich_hoc.db";
    private static final int DB_VERSION = 6;

    public static final String CD_GIO_NHAC = "gio_nhac";        // "06:00"
    public static final String CD_AM_THANH = "bat_am_thanh";    // "1" hoặc "0"
    public static final String CD_NHAC_BAT = "nhac_bat";        // "1" hoặc "0"

    public LichDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE tiet_hoc (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "thu INTEGER NOT NULL, " +
                        "buoi INTEGER NOT NULL, " +
                        "tiet INTEGER NOT NULL, " +
                        "ten_mon TEXT NOT NULL, " +
                        "ten_lop TEXT, " +
                        "phong TEXT, " +
                        "UNIQUE(thu, buoi, tiet))");
        taoBangPhu(db);
        taoBangLichSu(db);
        taoBangHoatDong(db);
    }

    private void taoBangPhu(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS hoc_ky (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "ten TEXT NOT NULL UNIQUE, " +
                        "ngay_bat_dau TEXT, " +
                        "ngay_ket_thuc TEXT)");

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS cai_dat (" +
                        "khoa TEXT PRIMARY KEY, " +
                        "gia_tri TEXT)");

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS lich_su_nhap (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "ten_file TEXT, " +
                        "thoi_gian INTEGER, " +
                        "so_mon INTEGER, " +
                        "so_trung INTEGER)");

        db.execSQL("INSERT OR IGNORE INTO cai_dat(khoa, gia_tri) VALUES('"
                + CD_GIO_NHAC + "', '06:00')");
        db.execSQL("INSERT OR IGNORE INTO cai_dat(khoa, gia_tri) VALUES('"
                + CD_AM_THANH + "', '1')");
    }

    private void taoBangLichSu(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS lich_su_thong_bao (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "thoi_gian INTEGER, " +
                        "tieu_de TEXT, " +
                        "noi_dung TEXT)");
    }

    /** Bảng hoạt động, thêm từ version 6. Giờ lưu bằng số phút trong ngày. */
    private void taoBangHoatDong(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS hoat_dong (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "ten TEXT NOT NULL, " +
                        "loai INTEGER NOT NULL DEFAULT 0, " +
                        "thu INTEGER NOT NULL DEFAULT -1, " +
                        "ngay TEXT, " +
                        "bat_dau INTEGER NOT NULL, " +
                        "ket_thuc INTEGER NOT NULL, " +
                        "lop TEXT, " +
                        "dia_diem TEXT, " +
                        "hoc_ky TEXT, " +
                        "ghi_chu TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS lich_hoc");
            db.execSQL("DROP TABLE IF EXISTS tiet_hoc");
            onCreate(db);
            return;
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE tiet_hoc ADD COLUMN ten_lop TEXT");
            db.execSQL("ALTER TABLE tiet_hoc ADD COLUMN phong TEXT");
        }
        if (oldVersion < 4) {
            taoBangPhu(db);
        }
        if (oldVersion < 5) {
            taoBangLichSu(db);
        }
        if (oldVersion < 6) {
            taoBangHoatDong(db);
            chuyenTuTietHoc(db); // đổi lịch cũ (tiết) sang hoạt động (giờ)
        }
    }

    // ====================== Thời khóa biểu kiểu tiết (cũ) ======================

    /** Đọc bảng tiet_hoc: bang[thu][buoi][tiet] (null nếu trống). */
    private TienIch.Tiet[][][] docBang(SQLiteDatabase db) {
        TienIch.Tiet[][][] bang = new TienIch.Tiet[7][2][TienIch.SO_TIET];

        try (Cursor c = db.query(
                "tiet_hoc", null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                int thu = c.getInt(c.getColumnIndexOrThrow("thu"));
                int buoi = c.getInt(c.getColumnIndexOrThrow("buoi"));
                int tiet = c.getInt(c.getColumnIndexOrThrow("tiet"));

                if (thu < 0 || thu > 6 || buoi < 0 || buoi > 1
                        || tiet < 0 || tiet >= TienIch.SO_TIET) continue;

                TienIch.Tiet t = new TienIch.Tiet();
                t.tenMon = c.getString(c.getColumnIndexOrThrow("ten_mon"));
                t.tenLop = c.getString(c.getColumnIndexOrThrow("ten_lop"));
                t.phong = c.getString(c.getColumnIndexOrThrow("phong"));
                bang[thu][buoi][tiet] = t;
            }
        }
        return bang;
    }

    public TienIch.Tiet[][][] layBang() {
        return docBang(getReadableDatabase());
    }

    public boolean luuBang(TienIch.Tiet[][][] bang) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("tiet_hoc", null, null);

            for (int thu = 0; thu < 7; thu++) {
                for (int buoi = 0; buoi < 2; buoi++) {
                    for (int tiet = 0; tiet < TienIch.SO_TIET; tiet++) {
                        TienIch.Tiet t = bang[thu][buoi][tiet];
                        if (t == null || t.tenMon == null
                                || t.tenMon.trim().isEmpty()) continue;

                        ContentValues v = new ContentValues();
                        v.put("thu", thu);
                        v.put("buoi", buoi);
                        v.put("tiet", tiet);
                        v.put("ten_mon", t.tenMon.trim());
                        v.put("ten_lop", t.tenLop == null ? "" : t.tenLop.trim());
                        v.put("phong", t.phong == null ? "" : t.phong.trim());
                        if (db.insert("tiet_hoc", null, v) == -1) {
                            return false;
                        }
                    }
                }
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Dựng lại các hoạt động lặp hằng tuần từ bảng tiet_hoc:
     * mỗi môn (các tiết liền nhau trong một buổi) thành một hoạt động có giờ.
     * Hoạt động một ngày (có ngày cụ thể) không bị đụng tới.
     */
    private void chuyenTuTietHoc(SQLiteDatabase db) {
        TienIch.Tiet[][][] bang = docBang(db);
        db.delete("hoat_dong", "ngay IS NULL OR ngay = ''", null);

        for (int thu = 0; thu < 7; thu++) {
            for (int buoi = 0; buoi < 2; buoi++) {
                for (TienIch.NhomMon n : TienIch.gomMon(bang[thu][buoi])) {
                    HoatDong h = new HoatDong();
                    h.ten = n.ten;
                    h.loai = HoatDong.HOC;
                    h.thu = thu;
                    h.batDau = HoatDong.batDauTiet(buoi, n.dau);
                    h.ketThuc = HoatDong.ketThucTiet(buoi, n.cuoi);
                    h.lop = n.lop;
                    h.diaDiem = n.phong;
                    db.insert("hoat_dong", null, taoGiaTri(h));
                }
            }
        }
    }

    // ====================== Hoạt động (mới) ======================

    private ContentValues taoGiaTri(HoatDong h) {
        ContentValues v = new ContentValues();
        v.put("ten", h.ten.trim());
        v.put("loai", h.loai);
        v.put("thu", h.laLapTuan() ? h.thu : -1);
        v.put("ngay", h.laLapTuan() ? "" : h.ngay);
        v.put("bat_dau", h.batDau);
        v.put("ket_thuc", h.ketThuc);
        v.put("lop", h.lop == null ? "" : h.lop.trim());
        v.put("dia_diem", h.diaDiem == null ? "" : h.diaDiem.trim());
        v.put("hoc_ky", h.hocKy == null ? "" : h.hocKy.trim());
        v.put("ghi_chu", h.ghiChu == null ? "" : h.ghiChu.trim());
        return v;
    }

    private HoatDong docHoatDong(Cursor c) {
        HoatDong h = new HoatDong();
        h.id = c.getLong(c.getColumnIndexOrThrow("id"));
        h.ten = c.getString(c.getColumnIndexOrThrow("ten"));
        h.loai = c.getInt(c.getColumnIndexOrThrow("loai"));
        h.thu = c.getInt(c.getColumnIndexOrThrow("thu"));
        h.ngay = c.getString(c.getColumnIndexOrThrow("ngay"));
        if (h.ngay == null) h.ngay = "";
        h.batDau = c.getInt(c.getColumnIndexOrThrow("bat_dau"));
        h.ketThuc = c.getInt(c.getColumnIndexOrThrow("ket_thuc"));
        h.lop = nonNull(c.getString(c.getColumnIndexOrThrow("lop")));
        h.diaDiem = nonNull(c.getString(c.getColumnIndexOrThrow("dia_diem")));
        h.hocKy = nonNull(c.getString(c.getColumnIndexOrThrow("hoc_ky")));
        h.ghiChu = nonNull(c.getString(c.getColumnIndexOrThrow("ghi_chu")));
        return h;
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

    /** Thêm hoạt động, trả về id (hoặc -1 nếu lỗi). */
    public long themHoatDong(HoatDong h) {
        return getWritableDatabase().insert("hoat_dong", null, taoGiaTri(h));
    }

    public boolean suaHoatDong(HoatDong h) {
        return getWritableDatabase().update("hoat_dong", taoGiaTri(h),
                "id = ?", new String[]{String.valueOf(h.id)}) > 0;
    }

    public boolean xoaHoatDong(long id) {
        return getWritableDatabase().delete("hoat_dong",
                "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    /** Toàn bộ hoạt động, xếp theo thứ rồi giờ bắt đầu. */
    public ArrayList<HoatDong> layTatCaHoatDong() {
        ArrayList<HoatDong> ds = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("hoat_dong", null, null,
                null, null, null, "thu, ngay, bat_dau")) {
            while (c.moveToNext()) ds.add(docHoatDong(c));
        }
        return ds;
    }

    /** Một hoạt động theo id, null nếu không có. */
    public HoatDong layHoatDong(long id) {
        try (Cursor c = getReadableDatabase().query("hoat_dong", null,
                "id = ?", new String[]{String.valueOf(id)},
                null, null, null)) {
            if (c.moveToFirst()) return docHoatDong(c);
        }
        return null;
    }

    /**
     * Hoạt động của một ngày: các hoạt động lặp đúng thứ đó
     * cộng các hoạt động đặt riêng cho ngày đó. Xếp theo giờ bắt đầu.
     * @param thu  0 = T2 ... 6 = CN
     * @param ngay "yyyy-MM-dd"
     */
    public ArrayList<HoatDong> layHoatDongTheoNgay(int thu, String ngay) {
        ArrayList<HoatDong> ds = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("hoat_dong", null,
                "((ngay IS NULL OR ngay = '') AND thu = ?) OR ngay = ?",
                new String[]{String.valueOf(thu), ngay},
                null, null, "bat_dau")) {
            while (c.moveToNext()) ds.add(docHoatDong(c));
        }
        return ds;
    }

    // ====================== Cài đặt ======================

    public String layCaiDat(String khoa, String macDinh) {
        try (Cursor c = getReadableDatabase().query(
                "cai_dat", new String[]{"gia_tri"}, "khoa = ?",
                new String[]{khoa}, null, null, null)) {
            if (c.moveToFirst()) {
                String v = c.getString(0);
                return v == null ? macDinh : v;
            }
        }
        return macDinh;
    }

    public boolean datCaiDat(String khoa, String giaTri) {
        ContentValues v = new ContentValues();
        v.put("khoa", khoa);
        v.put("gia_tri", giaTri);
        return getWritableDatabase().insertWithOnConflict(
                "cai_dat", null, v, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    // ====================== Lịch sử nhập file ======================

    public boolean themLichSuNhap(String tenFile, int soMon, int soTrung) {
        ContentValues v = new ContentValues();
        v.put("ten_file", tenFile);
        v.put("thoi_gian", System.currentTimeMillis());
        v.put("so_mon", soMon);
        v.put("so_trung", soTrung);
        return getWritableDatabase().insert("lich_su_nhap", null, v) != -1;
    }

    // ====================== Lịch sử thông báo ======================

    public static class ThongBao {
        public long thoiGian;
        public String tieuDe;
        public String noiDung;
    }

    public void themThongBao(String tieuDe, String noiDung) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("thoi_gian", System.currentTimeMillis());
        v.put("tieu_de", tieuDe);
        v.put("noi_dung", noiDung);
        db.insert("lich_su_thong_bao", null, v);
        db.execSQL("DELETE FROM lich_su_thong_bao WHERE id NOT IN "
                + "(SELECT id FROM lich_su_thong_bao ORDER BY id DESC LIMIT 100)");
    }

    public ArrayList<ThongBao> layLichSuThongBao() {
        ArrayList<ThongBao> ds = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("lich_su_thong_bao",
                null, null, null, null, null, "id DESC")) {
            while (c.moveToNext()) {
                ThongBao t = new ThongBao();
                t.thoiGian = c.getLong(c.getColumnIndexOrThrow("thoi_gian"));
                t.tieuDe = c.getString(c.getColumnIndexOrThrow("tieu_de"));
                t.noiDung = c.getString(c.getColumnIndexOrThrow("noi_dung"));
                ds.add(t);
            }
        }
        return ds;
    }

    public void xoaLichSuThongBao() {
        getWritableDatabase().delete("lich_su_thong_bao", null, null);
    }
}