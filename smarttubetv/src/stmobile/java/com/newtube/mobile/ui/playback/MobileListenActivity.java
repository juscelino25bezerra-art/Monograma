package com.newtube.mobile.ui.playback;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.utils.AppDialogUtil;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.ui.common.MobileActivity;

import java.util.Locale;

/**
 * YEWPLAY(listen): a tela Ouvir da proposta (p. 16), "o áudio em segundo plano ganha tela
 * própria". Abre do botão Ouvir da página do vídeo: o player vira uma sessão mini (como ao abrir
 * um canal) e esta tela controla o mesmo ExoPlayer pelo {@link MiniPlayerBridge}. Fechar (a seta
 * ou o voltar) leva à tela de onde o vídeo veio, com a barra flutuante; "Ver o vídeo" no menu
 * expande o player de volta.
 */
public class MobileListenActivity extends MobileActivity {
    private static final long TICK_MS = 500;
    private static final float[] SPEEDS = {1f, 1.25f, 1.5f, 2f, 0.75f};

    private View mTint;
    private ImageView mCover;
    private TextView mTitle;
    private TextView mAuthor;
    private SeekBar mSeek;
    private TextView mPosition;
    private TextView mRemaining;
    private ImageButton mPlay;
    private TextView mSpeed;
    private ImageButton mShuffle;
    private boolean mSeeking;
    @Nullable
    private String mBoundVideoId;
    private boolean mLeaving;

    private final Runnable mTick = new Runnable() {
        @Override
        public void run() {
            sync();
            mSeek.postDelayed(this, TICK_MS);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yew_listen);
        MiniPlayerBridge.completePendingNavigation();
        registerBackHandler(this::leave);

        mTint = findViewById(R.id.yew_listen_tint);
        mCover = findViewById(R.id.yew_listen_cover);
        mTitle = findViewById(R.id.yew_listen_title);
        mAuthor = findViewById(R.id.yew_listen_author);
        mSeek = findViewById(R.id.yew_listen_seek);
        mPosition = findViewById(R.id.yew_listen_position);
        mRemaining = findViewById(R.id.yew_listen_remaining);
        mPlay = findViewById(R.id.yew_listen_play);
        mSpeed = findViewById(R.id.yew_listen_speed);
        mShuffle = findViewById(R.id.yew_listen_shuffle);

        View box = findViewById(R.id.yew_listen_cover_box);
        View card = findViewById(R.id.yew_listen_cover_card);
        box.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int side = Math.min(r - l, b - t);
            ViewGroup.LayoutParams lp = card.getLayoutParams();
            if (side > 0 && (lp.width != side || lp.height != side)) {
                lp.width = side;
                lp.height = side;
                card.post(() -> card.setLayoutParams(lp));
            }
        });

        findViewById(R.id.yew_listen_close).setOnClickListener(v -> leave());
        findViewById(R.id.yew_listen_more).setOnClickListener(this::showMore);
        mPlay.setOnClickListener(v -> {
            ExoPlayer player = MiniPlayerBridge.getPlayer();
            if (player != null) {
                player.setPlayWhenReady(!player.getPlayWhenReady());
                sync();
            }
        });
        findViewById(R.id.yew_listen_prev).setOnClickListener(v -> MiniPlayerBridge.listenAction(MiniPlayerBridge.LISTEN_PREVIOUS));
        findViewById(R.id.yew_listen_next).setOnClickListener(v -> MiniPlayerBridge.listenAction(MiniPlayerBridge.LISTEN_NEXT));
        mShuffle.setOnClickListener(v -> {
            MiniPlayerBridge.listenAction(MiniPlayerBridge.LISTEN_SHUFFLE);
            mShuffle.postDelayed(this::sync, 150);
        });
        findViewById(R.id.yew_listen_repeat).setOnClickListener(v -> MiniPlayerBridge.listenAction(MiniPlayerBridge.LISTEN_REPEAT));
        findViewById(R.id.yew_listen_save).setOnClickListener(v -> MiniPlayerBridge.listenAction(MiniPlayerBridge.LISTEN_SAVE));
        findViewById(R.id.yew_listen_share).setOnClickListener(v -> MiniPlayerBridge.listenAction(MiniPlayerBridge.LISTEN_SHARE));
        findViewById(R.id.yew_listen_queue).setOnClickListener(v -> MiniPlayerBridge.listenAction(MiniPlayerBridge.LISTEN_QUEUE));
        findViewById(R.id.yew_listen_timer).setOnClickListener(v -> openSleepTimer());
        mSpeed.setOnClickListener(v -> cycleSpeed());

        mSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    ExoPlayer player = MiniPlayerBridge.getPlayer();
                    long duration = player != null ? player.getDuration() : 0;
                    if (duration > 0) {
                        long at = duration * progress / seekBar.getMax();
                        showTimes(at, duration);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSeeking = false;
                ExoPlayer player = MiniPlayerBridge.getPlayer();
                if (player != null && player.getDuration() > 0) {
                    player.seekTo(player.getDuration() * seekBar.getProgress() / seekBar.getMax());
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSeek.removeCallbacks(mTick);
        mTick.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSeek.removeCallbacks(mTick);
    }

    private void sync() {
        if (!MiniPlayerBridge.isActive()) {
            // The session ended (closed from the notification, a new video expanded...).
            if (!mLeaving) {
                mLeaving = true;
                finish();
            }
            return;
        }
        ExoPlayer player = MiniPlayerBridge.getPlayer();
        Video video = MiniPlayerBridge.getVideo();
        if (video != null && (mBoundVideoId == null || !mBoundVideoId.equals(video.videoId))) {
            bindVideo(video);
        }
        if (player == null) {
            return;
        }
        boolean playing = player.getPlayWhenReady() && player.getPlaybackState() != Player.STATE_ENDED;
        mPlay.setImageResource(playing ? R.drawable.ic_yew_pause : R.drawable.ic_yew_play);
        long duration = player.getDuration();
        long position = Math.max(0, player.getCurrentPosition());
        if (!mSeeking && duration > 0) {
            mSeek.setProgress((int) (position * mSeek.getMax() / duration));
            mSeek.setSecondaryProgress((int) (player.getBufferedPosition() * mSeek.getMax() / duration));
            showTimes(position, duration);
        }
        float speed = player.getPlaybackParameters().speed;
        mSpeed.setText(formatSpeed(speed));
        boolean shuffling = MiniPlayerBridge.isShuffling();
        mShuffle.setImageTintList(ContextCompat.getColorStateList(this,
                shuffling ? R.color.mobile_color_accent : R.color.mobile_color_on_surface));
    }

    private void bindVideo(Video video) {
        mBoundVideoId = video.videoId;
        mTitle.setText(video.getTitle());
        mAuthor.setText(video.getAuthor());
        String url = video.videoId != null
                ? "https://i.ytimg.com/vi/" + video.videoId + "/hqdefault.jpg"
                : video.cardImageUrl;
        Glide.with(this).asBitmap().load(url).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                mCover.setImageBitmap(bitmap);
                tintFrom(bitmap);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                mCover.setImageDrawable(placeholder);
            }
        });
    }

    /** "Fundo com a cor da capa": the cover's average colour fading into the page colour. */
    private void tintFrom(Bitmap bitmap) {
        Bitmap one = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        int average = one.getPixel(0, 0);
        android.util.TypedValue value = new android.util.TypedValue();
        int page = getTheme().resolveAttribute(R.attr.newtubeColorBackground, value, true)
                ? value.data : ContextCompat.getColor(this, R.color.mobile_color_background);
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(average, hsl);
        hsl[1] = Math.min(1f, hsl[1] * 1.2f);
        int top = ColorUtils.blendARGB(page, ColorUtils.HSLToColor(hsl), 0.55f);
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {top, ColorUtils.blendARGB(page, top, 0.35f), page});
        mTint.setBackground(gradient);
    }

    private void showTimes(long position, long duration) {
        mPosition.setText(formatTime(position));
        mRemaining.setText("-" + formatTime(Math.max(0, duration - position)));
    }

    private static String formatTime(long ms) {
        long total = ms / 1000;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
                : String.format(Locale.ROOT, "%d:%02d", m, s);
    }

    private static String formatSpeed(float speed) {
        String text = Math.abs(speed - Math.round(speed)) < 0.01f
                ? String.valueOf(Math.round(speed))
                : String.format(Locale.getDefault(), "%.2f", speed).replaceAll("0+$", "");
        return text + "×";
    }

    private void cycleSpeed() {
        ExoPlayer player = MiniPlayerBridge.getPlayer();
        if (player == null) {
            return;
        }
        float current = player.getPlaybackParameters().speed;
        int next = 0;
        for (int i = 0; i < SPEEDS.length; i++) {
            if (Math.abs(SPEEDS[i] - current) < 0.01f) {
                next = (i + 1) % SPEEDS.length;
                break;
            }
        }
        MiniPlayerBridge.setSpeed(SPEEDS[next]);
        sync();
    }

    private void openSleepTimer() {
        AppDialogPresenter dialog = AppDialogPresenter.instance(this);
        OptionCategory category = AppDialogUtil.createSleepTimerCategory(this);
        dialog.appendRadioCategory(category.title, category.options);
        dialog.showDialog(getString(R.string.yew_listen_timer));
    }

    private void showMore(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, R.string.yew_listen_show_video);
        menu.getMenu().add(0, 2, 1, R.string.yew_listen_audio_mode);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                mLeaving = true;
                MiniPlayerBridge.expand(this);
                finish();
            } else {
                MiniPlayerBridge.listenAction(MiniPlayerBridge.LISTEN_BACKGROUND_MODE);
            }
            return true;
        });
        menu.show();
    }

    /** Back to the screen the video came from, where the floating bar carries on. */
    private void leave() {
        if (mLeaving) {
            return;
        }
        mLeaving = true;
        MiniPlayerBridge.showHost(this);
        finish();
    }
}
