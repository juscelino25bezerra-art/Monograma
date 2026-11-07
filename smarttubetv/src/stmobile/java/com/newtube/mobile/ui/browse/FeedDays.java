package com.newtube.mobile.ui.browse;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YEWPLAY(subs): em que dia um vídeo do feed de Inscrições caiu (proposta p. 15, "Feed por Hoje e
 * Ontem"). O feed não traz a data como número, só o texto relativo do YouTube ("há 5 horas",
 * "há 1 dia", "5 hours ago", "1 day ago"), então o dia sai desse texto. Sem idade reconhecível o
 * vídeo fica no grupo anterior.
 */
final class FeedDays {
    static final int TODAY = 0;
    static final int YESTERDAY = 1;
    static final int THIS_WEEK = 2;
    static final int EARLIER = 3;
    static final int UNKNOWN = -1;

    private static final Pattern NUMBER_UNIT = Pattern.compile(
            "(\\d+)\\s*(segundo|minuto|min|hora|dia|semana|m[eê]s|meses|ano|second|minute|hour|day|week|month|year)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private FeedDays() {
    }

    static int of(Video video) {
        if (video.isLive || video.isUpcoming) {
            return TODAY;
        }
        return parse(video.getSecondTitle() != null ? video.getSecondTitle().toString() : null);
    }

    static int parse(@Nullable String meta) {
        if (meta == null) {
            return UNKNOWN;
        }
        String text = meta.toLowerCase(Locale.ROOT);
        if (text.contains("ontem") || text.contains("yesterday")) {
            return YESTERDAY;
        }
        if (text.contains("hoje") || text.contains("today") || text.contains("agora") || text.contains("just now")) {
            return TODAY;
        }
        Matcher m = NUMBER_UNIT.matcher(text);
        int result = UNKNOWN;
        while (m.find()) {
            // The age is the last "number + unit" on the line ("12 mil visualizações · há 3 dias").
            int n = Integer.parseInt(m.group(1));
            String unit = m.group(2);
            if (unit.startsWith("seg") || unit.startsWith("sec") || unit.startsWith("min") || unit.startsWith("hor")
                    || unit.startsWith("hour")) {
                result = TODAY;
            } else if (unit.startsWith("dia") || unit.startsWith("day")) {
                result = n <= 1 ? YESTERDAY : n < 7 ? THIS_WEEK : EARLIER;
            } else {
                result = EARLIER;
            }
        }
        return result;
    }
}
