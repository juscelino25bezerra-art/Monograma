package com.newtube.mobile.ui.common;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * MONOGRAMA(visual): the app's typeface (Manrope, res/font) for text drawn in code - a Paint or a
 * span that no layout attribute reaches. Layouts name the font themselves; this is only for the
 * places that used to call {@code Typeface.create("sans-serif-medium")}.
 */
public final class AppFonts {
    private static Typeface sSemibold;

    private AppFonts() {
    }

    /** Manrope SemiBold, the weight that replaced sans-serif-medium; the system's if it fails. */
    @NonNull
    public static Typeface semibold(@NonNull Context context) {
        Typeface typeface = sSemibold;
        if (typeface == null) {
            try {
                typeface = ResourcesCompat.getFont(context.getApplicationContext(), R.font.manrope_semibold);
            } catch (RuntimeException e) {
                typeface = null; // a missing or unreadable font must never break a screen
            }
            if (typeface == null) {
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);
            }
            sSemibold = typeface;
        }
        return typeface;
    }

    /** A span that sets {@link #semibold}: {@code TypefaceSpan(Typeface)} needs API 28. */
    @NonNull
    public static MetricAffectingSpan semiboldSpan(@NonNull Context context) {
        return new TypefaceOverride(semibold(context));
    }

    private static final class TypefaceOverride extends MetricAffectingSpan {
        private final Typeface mTypeface;

        TypefaceOverride(Typeface typeface) {
            mTypeface = typeface;
        }

        @Override
        public void updateDrawState(TextPaint paint) {
            paint.setTypeface(mTypeface);
        }

        @Override
        public void updateMeasureState(@NonNull TextPaint paint) {
            paint.setTypeface(mTypeface);
        }
    }
}
