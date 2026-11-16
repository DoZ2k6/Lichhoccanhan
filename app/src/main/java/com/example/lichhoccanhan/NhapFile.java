package com.example.lichhoccanhan;

import static com.example.lichhoccanhan.TienIch.SO_TIET;

import android.content.Context;
import android.net.Uri;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Đọc lịch từ file Excel. Ưu tiên cột Giờ; thiếu thì dùng Tiết (+ Buổi). */
public final class NhapFile {

    private NhapFile() {}

    /** Một dòng hợp lệ: thu 0..6, giờ tính bằng phút trong ngày. */
    public static class Dong {
        public int soDong;
        public int thu;
        public int batDau, ketThuc;
        public String ten = "", lop = "", phong = "", hocKy = "";
    }

    public static class KetQua {
        public ArrayList<Dong> dong = new ArrayList<>();
        public ArrayList<String> loi = new ArrayList<>();
        public String loiChung;
    }

    public static KetQua docExcel(Context c, Uri uri) {
        KetQua kq = new KetQua();
        DataFormatter fmt = new DataFormatter();

        try (InputStream is = c.getContentResolver().openInputStream(uri)) {
            if (is == null) {
                kq.loiChung = "Không mở được file.";
                return kq;
            }
            try (Workbook wb = WorkbookFactory.create(is)) {
                if (wb.getNumberOfSheets() == 0) {
                    kq.loiChung = "File không có trang tính nào.";
                    return kq;
                }
                Sheet sh = wb.getSheetAt(0);

                // ---- Tìm dòng tiêu đề ----
                int hang = -1;
                int cThu = -1, cMon = -1, cGio = -1, cTiet = -1, cBuoi = -1;
                int cPhong = -1, cLop = -1, cHocKy = -1;
                int gioiHan = Math.min(sh.getLastRowNum(), 20);

                for (int r = 0; r <= gioiHan && hang < 0; r++) {
                    Row row = sh.getRow(r);
                    if (row == null) continue;
                    cThu = cMon = cGio = cTiet = cBuoi = -1;
                    cPhong = cLop = cHocKy = -1;
                    for (int k = 0; k < row.getLastCellNum(); k++) {
                        String s = boDau(layO(row.getCell(k), fmt));
                        if (s.isEmpty()) continue;
                        if (s.equals("thu") || s.equals("ngay hoc")) cThu = k;
                        else if (s.startsWith("mon") || s.contains("hoc phan")
                                || s.equals("hoat dong") || s.equals("ten hoat dong")) cMon = k;
                        else if (s.startsWith("gio") || s.equals("thoi gian")) cGio = k;
                        else if (s.equals("tiet")) cTiet = k;
                        else if (s.equals("buoi")) cBuoi = k;
                        else if (s.startsWith("phong") || s.equals("dia diem")) cPhong = k;
                        else if (s.startsWith("lop")) cLop = k;
                        else if (s.startsWith("hoc ky")) cHocKy = k;
                    }
                    if (cThu >= 0 && cMon >= 0 && (cGio >= 0 || cTiet >= 0)) {
                        hang = r;
                    }
                }
                if (hang < 0) {
                    kq.loiChung = "Không tìm thấy dòng tiêu đề. Cần các cột: "
                            + "Thứ, Môn học và Giờ học (hoặc Tiết).";
                    return kq;
                }

                // ---- Đọc từng dòng ----
                for (int r = hang + 1; r <= sh.getLastRowNum(); r++) {
                    Row row = sh.getRow(r);
                    if (row == null) continue;

                    String sThu = layO(row.getCell(cThu), fmt);
                    String sMon = layO(row.getCell(cMon), fmt);
                    String sGio = cGio >= 0 ? layO(row.getCell(cGio), fmt) : "";
                    String sTiet = cTiet >= 0 ? layO(row.getCell(cTiet), fmt) : "";
                    String sBuoi = cBuoi >= 0 ? layO(row.getCell(cBuoi), fmt) : "";

                    if (sThu.isEmpty() && sMon.isEmpty()
                            && sGio.isEmpty() && sTiet.isEmpty()) continue;

                    int soDong = r + 1;

                    int thu = parseThu(sThu);
                    if (thu < 0) {
                        kq.loi.add("Dòng " + soDong + ": Thứ không hợp lệ (\"" + sThu + "\")");
                        continue;
                    }
                    if (sMon.isEmpty()) {
                        kq.loi.add("Dòng " + soDong + ": thiếu tên môn học");
                        continue;
                    }

                    int[] gio = parseGio(sGio);
                    String loiGio = null;
                    if (gio == null) {
                        gio = gioTuTiet(sTiet, sBuoi);
                        if (gio == null) {
                            loiGio = sGio.isEmpty() && sTiet.isEmpty()
                                    ? "thiếu giờ học"
                                    : "không đọc được giờ/tiết (\""
                                    + (sGio.isEmpty() ? sTiet : sGio) + "\")";
                        }
                    }
                    if (loiGio != null) {
                        kq.loi.add("Dòng " + soDong + ": " + loiGio);
                        continue;
                    }

                    // Trùng giờ với dòng khác trong chính file
                    boolean trung = false;
                    for (Dong o : kq.dong) {
                        if (o.thu == thu && gio[0] < o.ketThuc && o.batDau < gio[1]) {
                            trung = true;
                            break;
                        }
                    }
                    if (trung) {
                        kq.loi.add("Dòng " + soDong + ": trùng giờ với dòng khác trong file");
                        continue;
                    }

                    Dong d = new Dong();
                    d.soDong = soDong;
                    d.thu = thu;
                    d.batDau = gio[0];
                    d.ketThuc = gio[1];
                    d.ten = sMon;
                    d.phong = cPhong >= 0 ? layO(row.getCell(cPhong), fmt) : "";
                    d.lop = cLop >= 0 ? layO(row.getCell(cLop), fmt) : "";
                    d.hocKy = cHocKy >= 0 ? layO(row.getCell(cHocKy), fmt) : "";
                    kq.dong.add(d);
                }
            }
        } catch (Throwable e) {
            kq.loiChung = "Không đọc được file Excel: " + e.getMessage();
        }
        return kq;
    }

    // ====================== Phân tích từng ô ======================

    /** 0 = T2 ... 6 = CN, -1 nếu không hợp lệ. */
    private static int parseThu(String s) {
        String t = boDau(s);
        if (t.equals("cn") || t.contains("chu nhat")) return 6;
        if (t.contains("hai")) return 0;
        if (t.contains("ba")) return 1;
        if (t.contains("tu")) return 2;
        if (t.contains("nam")) return 3;
        if (t.contains("sau")) return 4;
        if (t.contains("bay")) return 5;
        String so = t.replaceAll("[^0-9]", "");
        if (so.isEmpty()) return -1;
        try {
            int n = Integer.parseInt(so);
            return (n >= 2 && n <= 7) ? n - 2 : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** "06h45 - 09h00", "6:45-9:00", "18g30 - 20g00" -> {405, 540}. */
    private static int[] parseGio(String s) {
        if (s == null || s.isEmpty()) return null;
        Matcher m = Pattern.compile(
                "(\\d{1,2})\\s*[hHgG:.]\\s*(\\d{0,2})\\s*[-–—~đến]+\\s*"
                        + "(\\d{1,2})\\s*[hHgG:.]\\s*(\\d{0,2})").matcher(s);
        if (!m.find()) return null;
        try {
            int h1 = Integer.parseInt(m.group(1));
            int p1 = m.group(2).isEmpty() ? 0 : Integer.parseInt(m.group(2));
            int h2 = Integer.parseInt(m.group(3));
            int p2 = m.group(4).isEmpty() ? 0 : Integer.parseInt(m.group(4));
            if (h1 > 23 || h2 > 23 || p1 > 59 || p2 > 59) return null;
            int bd = h1 * 60 + p1, kt = h2 * 60 + p2;
            return kt > bd ? new int[]{bd, kt} : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Dự phòng khi không có giờ: dùng tiết. Có cột Buổi thì tiết 1..6 trong buổi đó;
     * không có thì tiết 1..6 là sáng, 7..12 là chiều (tiết 7 = tiết 1 buổi chiều).
     */
    private static int[] gioTuTiet(String sTiet, String sBuoi) {
        Matcher m = Pattern.compile("(\\d{1,2})(?:\\s*[-–—]\\s*(\\d{1,2}))?")
                .matcher(sTiet == null ? "" : sTiet.trim());
        if (!m.find()) return null;
        try {
            int dau = Integer.parseInt(m.group(1));
            int cuoi = m.group(2) == null ? dau : Integer.parseInt(m.group(2));
            if (dau < 1 || dau > cuoi) return null;

            int buoi;
            String b = boDau(sBuoi);
            if (b.contains("sang")) buoi = 0;
            else if (b.contains("chieu")) buoi = 1;
            else if (dau >= SO_TIET + 1) {
                buoi = 1;
                dau -= SO_TIET;
                cuoi -= SO_TIET;
            } else buoi = 0;

            if (dau < 1 || cuoi > SO_TIET) return null;
            return new int[]{HoatDong.batDauTiet(buoi, dau - 1),
                    HoatDong.ketThucTiet(buoi, cuoi - 1)};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String layO(Cell cell, DataFormatter fmt) {
        if (cell == null) return "";
        try {
            String v = fmt.formatCellValue(cell);
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static String boDau(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return t.replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT).trim();
    }
}