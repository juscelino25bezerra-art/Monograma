package com.newtube.mobile.downloads;

import android.content.Context;
import android.text.format.DateUtils;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * YEWPLAY(downloads): os textos de uma linha da tela Downloads como na proposta (p. 16) -
 * "82 de 180 MB, 2,1 MB/s" enquanto baixa, "Pausado, 12 de 96 MB" parado, e nos concluídos o
 * formato ("Vídeo MP4 1080p, 175,7 MB") e a data ("27 de setembro").
 */
public final class DownloadRowText {
    private DownloadRowText() {
    }

    /** The line under the progress bar of an unfinished item. */
    public static String progress(Context context, DownloadItem item) {
        switch (item.state) {
            case DownloadItem.STATE_DOWNLOADING: {
                String done = DownloadOptions.formatBytes(item.bytesDone);
                if (item.bytesTotal <= 0) {
                    return done;
                }
                String of = context.getString(R.string.yew_download_of, done, DownloadOptions.formatBytes(item.bytesTotal));
                return item.bytesPerSecond > 0
                        ? context.getString(R.string.yew_download_speed, of, DownloadOptions.formatBytes(item.bytesPerSecond))
                        : of;
            }
            default:
                return DownloadProgressText.of(context, item);
        }
    }

    /** "Vídeo MP4 1080p, 175,7 MB" or "Áudio M4A, 3,9 MB". */
    public static String format(Context context, DownloadItem item) {
        String size = DownloadOptions.formatBytes(item.bytesTotal);
        if (item.isAudioOnly()) {
            return context.getString(R.string.yew_download_format_audio, size);
        }
        String quality = item.qualityLabel != null ? item.qualityLabel : "";
        return context.getString(R.string.yew_download_format_video, quality, size).replace("  ", " ");
    }

    /** "27 de setembro" (the year only when it is not this one). */
    public static String date(Context context, DownloadItem item) {
        return DateUtils.formatDateTime(context, item.createdAt,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR);
    }
}
