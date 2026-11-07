package com.newtube.mobile.ui.common;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * YEWPLAY(visual): a channel avatar is a rounded square with corners at 32% of its side
 * (proposta, p. 8), not a circle - Glide's circleCrop for channel pictures becomes this.
 * Account and comment avatars stay round.
 */
public final class YewAvatar extends BitmapTransformation {
    private static final String ID = "com.newtube.mobile.ui.common.YewAvatar.v1";
    private static final byte[] ID_BYTES = ID.getBytes(StandardCharsets.UTF_8);
    private static final YewAvatar INSTANCE = new YewAvatar();
    public static final float CORNER_FRACTION = 0.32f;

    private YewAvatar() {
    }

    @NonNull
    public static YewAvatar get() {
        return INSTANCE;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        Bitmap square = TransformationUtils.centerCrop(pool, toTransform, outWidth, outHeight);
        int radius = Math.max(1, Math.round(Math.min(square.getWidth(), square.getHeight()) * CORNER_FRACTION));
        return TransformationUtils.roundedCorners(pool, square, radius);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof YewAvatar;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }
}
