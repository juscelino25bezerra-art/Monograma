package com.newtube.mobile.ui.browse;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.ChipGroup;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.downloads.DownloadItem;
import com.newtube.mobile.downloads.DownloadOptions;
import com.newtube.mobile.downloads.DownloadSort;

import java.util.List;

/**
 * MONOGRAMA(visual): the Downloads tab's header (activity_mobile_browse: mobile_downloads_header) -
 * a storage card (how many videos and how much space they take, against what is free on the
 * device) and the Recent / Size / Name chips that order the list (DownloadSort). Also the tab's
 * empty state: "Watch without internet" under a coral download icon.
 */
final class DownloadsHeader {
    private final Activity mActivity;
    private final View mHeader;
    private final TextView mUsed;
    private final TextView mFree;
    private final ProgressBar mBar;
    private final View mStorage;
    private final View mSortScroll;
    private final ChipGroup mSort;
    private boolean mBinding;

    /** @param onSortChanged rebuilds the list in the new order */
    DownloadsHeader(@NonNull Activity activity, @NonNull Runnable onSortChanged) {
        mActivity = activity;
        mHeader = activity.findViewById(R.id.mobile_downloads_header);
        mUsed = activity.findViewById(R.id.mobile_downloads_storage_used);
        mFree = activity.findViewById(R.id.mobile_downloads_storage_free);
        mBar = activity.findViewById(R.id.mobile_downloads_storage_bar);
        mStorage = activity.findViewById(R.id.mobile_downloads_storage);
        mSortScroll = activity.findViewById(R.id.mobile_downloads_sort_scroll);
        mSort = activity.findViewById(R.id.mobile_downloads_sort);
        mSort.check(chipFor(DownloadSort.get(activity)));
        mSort.setOnCheckedStateChangeListener((group, checked) -> {
            if (mBinding || checked.isEmpty()) {
                return;
            }
            int sort = sortFor(checked.get(0));
            if (sort != DownloadSort.get(mActivity)) {
                DownloadSort.set(mActivity, sort);
                onSortChanged.run();
            }
        });
    }

    /** Shown on the Downloads tab only; refreshed each time it is shown or a download changes. */
    void show(boolean visible) {
        mHeader.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            bind();
        }
    }

    private void bind() {
        List<DownloadItem> items = DownloadSort.sorted(mActivity);
        int done = 0;
        for (DownloadItem item : items) {
            if (item.isDone()) {
                done++;
            }
        }
        long used = DownloadSort.totalBytes(items);
        long free = freeBytes();

        String usedText = mActivity.getResources().getQuantityString(R.plurals.mobile_downloads_storage_used,
                done, done, used > 0 ? DownloadOptions.formatBytes(used) : DownloadOptions.formatBytes(0));
        mUsed.setText(usedText);
        String freeText = free >= 0
                ? mActivity.getString(R.string.mobile_downloads_storage_free, DownloadOptions.formatBytes(free)) : "";
        mFree.setText(freeText);
        mFree.setVisibility(free >= 0 ? View.VISIBLE : View.GONE);
        mStorage.setContentDescription(mActivity.getString(R.string.mobile_downloads_storage_description,
                usedText, freeText));

        // The downloads' share of what they could take; a sliver stays visible once there is one.
        int progress = 0;
        if (used > 0) {
            long room = used + Math.max(0, free);
            progress = (int) Math.max(15, Math.min(1000, used * 1000 / Math.max(1, room)));
        }
        mBar.setProgress(progress);

        // Ordering one video is moot.
        mSortScroll.setVisibility(items.size() > 1 ? View.VISIBLE : View.GONE);
        mBinding = true;
        int chip = chipFor(DownloadSort.get(mActivity));
        if (mSort.getCheckedChipId() != chip) {
            mSort.check(chip);
        }
        mBinding = false;
    }

    /** Free space where the downloads go (the shared storage's Movies/Music); -1 if unreadable. */
    private static long freeBytes() {
        try {
            @SuppressWarnings("deprecation")
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            return stat.getAvailableBytes();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /**
     * The empty-state look: Downloads gets its coral icon on a round backdrop and a bold title;
     * every other tab the neutral empty picture and no title.
     */
    static void applyEmptyLook(@NonNull ImageView icon, @NonNull TextView title, @NonNull TextView message,
                               boolean downloads) {
        if (downloads) {
            int pad = Math.round(26 * icon.getResources().getDisplayMetrics().density);
            icon.setImageResource(R.drawable.ic_nav_downloads_outline);
            icon.setBackgroundResource(R.drawable.bg_monograma_empty_circle);
            icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(icon.getContext(),
                    R.color.mobile_color_accent)));
            icon.setPadding(pad, pad, pad, pad);
            title.setText(R.string.mobile_downloads_empty_title);
            title.setVisibility(View.VISIBLE);
            message.setText(R.string.mobile_downloads_empty_body);
            message.setTextColor(ContextCompat.getColor(icon.getContext(), R.color.mobile_color_on_surface_secondary));
        } else {
            icon.setImageResource(R.drawable.ic_mobile_empty);
            icon.setBackground(null);
            icon.setImageTintList(null);
            icon.setPadding(0, 0, 0, 0);
            title.setVisibility(View.GONE);
            message.setTextColor(ContextCompat.getColor(icon.getContext(), R.color.mobile_color_on_surface));
        }
    }

    private static int chipFor(int sort) {
        return sort == DownloadSort.SIZE ? R.id.mobile_downloads_sort_size
                : sort == DownloadSort.NAME ? R.id.mobile_downloads_sort_name : R.id.mobile_downloads_sort_recent;
    }

    private static int sortFor(int chipId) {
        return chipId == R.id.mobile_downloads_sort_size ? DownloadSort.SIZE
                : chipId == R.id.mobile_downloads_sort_name ? DownloadSort.NAME : DownloadSort.RECENT;
    }
}
