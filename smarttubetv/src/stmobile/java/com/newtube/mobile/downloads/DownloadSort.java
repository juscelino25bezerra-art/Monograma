package com.newtube.mobile.downloads;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * MONOGRAMA(visual): the order of the Downloads tab - Recent (newest first, as before), Size
 * (largest first) or Name - picked with the chips above the list. Both the list the presenter
 * shows ({@link DownloadsBridge#listSectionVideos}) and Browse's in-place refresh read it from
 * here, so the two always agree on the order.
 */
public final class DownloadSort {
    public static final int RECENT = 0;
    public static final int SIZE = 1;
    public static final int NAME = 2;

    private static final String PREFS = "monograma_downloads";
    private static final String KEY = "sort";

    private DownloadSort() {
    }

    public static int get(@NonNull Context context) {
        int sort = prefs(context).getInt(KEY, RECENT);
        return sort == SIZE || sort == NAME ? sort : RECENT;
    }

    public static void set(@NonNull Context context, int sort) {
        prefs(context).edit().putInt(KEY, sort).apply();
    }

    /** The registry's items in the chosen order. */
    @NonNull
    public static List<DownloadItem> sorted(@NonNull Context context) {
        List<DownloadItem> items = DownloadRegistry.instance(context).itemsNewestFirst();
        return apply(items, get(context));
    }

    @NonNull
    static List<DownloadItem> apply(@NonNull List<DownloadItem> newestFirst, int sort) {
        List<DownloadItem> result = new ArrayList<>(newestFirst);
        if (sort == SIZE) {
            // Stable: equal sizes (or two items still sizing) keep the newest-first order.
            Collections.sort(result, (a, b) -> Long.compare(size(b), size(a)));
        } else if (sort == NAME) {
            Collator collator = Collator.getInstance();
            collator.setStrength(Collator.PRIMARY); // "Á" with "a", case ignored
            Comparator<DownloadItem> byName = (a, b) -> collator.compare(
                    a.title != null ? a.title : "", b.title != null ? b.title : "");
            Collections.sort(result, byName);
        }
        return result;
    }

    /** What the item takes (or will take) on the device; unknown sorts last. */
    private static long size(DownloadItem item) {
        return item.bytesTotal > 0 ? item.bytesTotal : Math.max(0, item.bytesDone);
    }

    /** The bytes the finished downloads take, for the storage card. */
    public static long totalBytes(@NonNull List<DownloadItem> items) {
        long total = 0;
        for (DownloadItem item : items) {
            if (item.isDone()) {
                total += Math.max(0, item.bytesTotal);
            }
        }
        return total;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
