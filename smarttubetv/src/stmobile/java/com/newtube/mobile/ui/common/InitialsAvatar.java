package com.newtube.mobile.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * MONOGRAMA(visual): an account avatar before (or without) its picture - the name's initials on a
 * coral disc, as on the You tab's profile card ("AB").
 */
public final class InitialsAvatar extends Drawable {
    private final Paint mDisc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String mInitials;
    private final Rect mBounds = new Rect();

    public InitialsAvatar(@NonNull Context context, @Nullable String name) {
        mInitials = initials(name);
        mDisc.setColor(ContextCompat.getColor(context, R.color.mobile_color_accent));
        mText.setColor(ContextCompat.getColor(context, R.color.mobile_color_on_accent));
        mText.setTextAlign(Paint.Align.CENTER);
        mText.setTypeface(AppFonts.semibold(context));
        mText.setFakeBoldText(true);
    }

    /** "Antonio Be" -> "AB"; "ana" -> "A"; nothing -> "". */
    @NonNull
    static String initials(@Nullable String name) {
        if (name == null) {
            return "";
        }
        String[] words = name.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty() && Character.isLetterOrDigit(word.codePointAt(0))) {
                out.appendCodePoint(Character.toUpperCase(word.codePointAt(0)));
                if (out.length() >= 2) {
                    break;
                }
            }
        }
        if (out.length() == 2 && words.length > 2) {
            // First and last name, like "Antonio de Be" -> "AB".
            String last = words[words.length - 1];
            if (!last.isEmpty() && Character.isLetterOrDigit(last.codePointAt(0))) {
                out.setLength(out.offsetByCodePoints(0, 1));
                out.appendCodePoint(Character.toUpperCase(last.codePointAt(0)));
            }
        }
        return out.toString();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        float r = Math.min(b.width(), b.height()) / 2f;
        canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), r, mDisc);
        if (mInitials.isEmpty()) {
            return;
        }
        mText.setTextSize(r * (mInitials.length() > 1 ? 0.78f : 0.9f));
        mText.getTextBounds(mInitials, 0, mInitials.length(), mBounds);
        canvas.drawText(mInitials, b.exactCenterX(), b.exactCenterY() + mBounds.height() / 2f, mText);
    }

    @Override
    public void setAlpha(int alpha) {
        mDisc.setAlpha(alpha);
        mText.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mDisc.setColorFilter(colorFilter);
        mText.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
