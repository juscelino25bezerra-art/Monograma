package com.newtube.mobile.ui.browse;

import android.annotation.SuppressLint;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.newtube.mobile.ui.common.ChannelAvatars;
import com.newtube.mobile.ui.playback.PlayerTransitionBridge;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Touch video card adapter (port of the gist of
 * {@code com.liskovsoft.smartyoutubetv2.tv.presenter.VideoCardPresenter}, minus
 * focus-scale/d-pad concerns; ripple/elevation instead via the card's own foreground
 * and {@code MaterialCardView} elevation).
 */
public class VideoCardAdapter extends ListAdapter<Video, RecyclerView.ViewHolder> {
    /** Regular video card (full-width thumbnail + title/meta). */
    private static final int VIEW_TYPE_VIDEO = 0;
    /** Channel result row (round avatar + name + subs) — e.g. channels in search results. */
    private static final int VIEW_TYPE_CHANNEL = 1;
    /** YEWPLAY(visual): the compact video item (item_video_card_row) - see {@link #setRowLayout}. */
    private static final int VIEW_TYPE_VIDEO_ROW = 2;
    /** YEWPLAY(home): the first video of Início's "Tudo", large and with its text over the image. */
    private static final int VIEW_TYPE_VIDEO_HERO = 3;
    /** YEWPLAY(lists): a section header pseudo-item ({@link Video#yewHeader}). */
    private static final int VIEW_TYPE_HEADER = 4;
    /** YEWPLAY(downloads): the storage pseudo-item at the top of Downloads. */
    private static final int VIEW_TYPE_STORAGE = 5;
    /** YEWPLAY(subs): the channel strip at the top of Inscrições. */
    private static final int VIEW_TYPE_STRIP = 6;

    public interface OnVideoClickListener {
        void onVideoClick(Video video);
    }

    /** Long-press = the touch equivalent of the TV D-pad OK-long-press context-menu shortcut
     *  (ARCHITECTURE.md section 3's input contract: {@code onVideoItemLongClicked}). Routes to
     *  {@code BrowsePresenter.onVideoItemLongClicked()}, which builds an OptionCategory menu via
     *  {@code VideoMenuPresenter}/{@code AppDialogPresenter} and shows it through the Wave 3
     *  {@code MobileAppDialogActivity} renderer. */
    public interface OnVideoLongClickListener {
        boolean onVideoLongClick(Video video);
    }

    private final OnVideoClickListener mClickListener;
    private final OnVideoLongClickListener mLongClickListener;
    /** NEWTUBE(feed-avatars): set = cards show their channel's avatar, and tapping it calls this. */
    private OnVideoClickListener mChannelClickListener;
    /** Held here; ChannelAvatars holds it weakly, so it goes away with this adapter. */
    private final ChannelAvatars.Listener mAvatarListener = this::onAvatarsChanged;
    private boolean mRowLayout;
    private boolean mHeroFirst;

    public VideoCardAdapter(OnVideoClickListener clickListener) {
        this(clickListener, null);
    }

    public VideoCardAdapter(OnVideoClickListener clickListener, OnVideoLongClickListener longClickListener) {
        super(DIFF_CALLBACK);
        mClickListener = clickListener;
        mLongClickListener = longClickListener;
    }

    private static final DiffUtil.ItemCallback<Video> DIFF_CALLBACK = new DiffUtil.ItemCallback<Video>() {
        @Override
        public boolean areItemsTheSame(@NonNull Video oldItem, @NonNull Video newItem) {
            // Video#equals/hashCode are content-based (videoId/playlistId/channelId/...).
            return oldItem.equals(newItem);
        }

        @Override
        @SuppressLint("DiffUtilEquals")
        public boolean areContentsTheSame(@NonNull Video oldItem, @NonNull Video newItem) {
            // Video.equals() is identity-like (IDs only). A new instance with the same ID can
            // carry refreshed metadata and must be rebound; the same instance needs no rebind.
            return oldItem == newItem;
        }
    };

    /** YEWPLAY(lists): adds header pseudo-items to a submitted list (Downloads, Inscrições). */
    public interface ListDecorator {
        java.util.List<Video> decorate(java.util.List<Video> list);
    }

    private ListDecorator mDecorator;
    private OnVideoClickListener mStripChannelListener;
    private boolean mNumbered;

    /** YEWPLAY(playlist): number the rows 1, 2, 3... (playlists, proposta p. 15). */
    public void setNumbered(boolean numbered) {
        if (mNumbered != numbered) {
            mNumbered = numbered;
            if (getItemCount() > 0) {
                notifyDataSetChanged();
            }
        }
    }

    /** YEWPLAY(subs): what a tap on a channel of the Inscrições strip does. */
    public void setStripChannelListener(OnVideoClickListener listener) {
        mStripChannelListener = listener;
    }

    public void setListDecorator(ListDecorator decorator) {
        mDecorator = decorator;
    }

    private java.util.List<Video> decorated(java.util.List<Video> list) {
        return mDecorator != null && list != null ? mDecorator.decorate(list) : list;
    }

    @Override
    public void submitList(java.util.List<Video> list) {
        super.submitList(decorated(list));
    }

    @Override
    public void submitList(java.util.List<Video> list, Runnable commitCallback) {
        super.submitList(decorated(list), commitCallback);
    }

    @Override
    public int getItemViewType(int position) {
        // isChannel() alone also matches playlists delivered in the playlist-as-channel shape
        // (videoId/playlistId null + a VL... channelId — the standard shape of the user
        // Playlists section): those must stay thumbnail cards, not round-avatar channel rows.
        Video item = getItem(position);
        if (item.yewStorageUsed >= 0) {
            return VIEW_TYPE_STORAGE;
        }
        if (item.yewStrip != null) {
            return VIEW_TYPE_STRIP;
        }
        if (item.yewHeader != null) {
            return VIEW_TYPE_HEADER;
        }
        if (item.isChannel() && !item.isPlaylistAsChannel()) {
            return VIEW_TYPE_CHANNEL;
        }
        if (mHeroFirst && !mRowLayout && position == 0 && item.videoId != null) {
            return VIEW_TYPE_VIDEO_HERO;
        }
        return mRowLayout ? VIEW_TYPE_VIDEO_ROW : VIEW_TYPE_VIDEO;
    }

    /**
     * NEWTUBE(feed-avatars): show each video card's channel avatar (Home, Subscriptions), opening the
     * channel through {@code onChannelClick} when tapped; null hides them. The browse screen calls
     * it once a section's new list is committed, before that list's cards are laid out; switching
     * on or off also refreshes cards kept from the previous list.
     */
    public void setChannelAvatars(OnVideoClickListener onChannelClick) {
        boolean changed = (mChannelClickListener == null) != (onChannelClick == null);
        mChannelClickListener = onChannelClick;
        if (onChannelClick != null) {
            ChannelAvatars.addListener(mAvatarListener);
        }
        if (changed && getItemCount() > 0) {
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_AVATAR);
        }
    }

    private void onAvatarsChanged(java.util.Set<String> ids) {
        if (mChannelClickListener == null) {
            return;
        }
        boolean all = ids.contains(ChannelAvatars.ALL);
        java.util.List<Video> current = getCurrentList();
        for (int i = 0; i < current.size(); i++) {
            Video video = current.get(i);
            if (!VideoViewHolder.hasChannelAvatar(video)) {
                continue;
            }
            String channelId = ChannelAvatars.channelIdOf(video);
            if (all || ids.contains(video.videoId) || channelId != null && ids.contains(channelId)) {
                notifyItemChanged(i, PAYLOAD_AVATAR);
            }
        }
    }

    /** Full-span rows (channel results) for the grid's SpanSizeLookup in multi-column layouts. */
    public boolean isFullSpan(int position) {
        return position >= 0 && position < getItemCount()
                && (mRowLayout || getItemViewType(position) == VIEW_TYPE_CHANNEL
                        || getItemViewType(position) == VIEW_TYPE_HEADER
                        || getItemViewType(position) == VIEW_TYPE_STORAGE
                        || getItemViewType(position) == VIEW_TYPE_STRIP
                        || getItemViewType(position) == VIEW_TYPE_VIDEO_HERO);
    }

    /**
     * YEWPLAY(visual): compact items (thumbnail beside the text, proposta p. 21) - every list but
     * Início, which keeps the large edge-to-edge item. Same view holder, same bindings.
     */
    public void setRowLayout(boolean rows) {
        if (mRowLayout == rows) {
            return;
        }
        mRowLayout = rows;
        if (getItemCount() > 0) {
            notifyDataSetChanged();
        }
    }

    /**
     * YEWPLAY(home): the editorial item (proposta, p. 12) - Início's first video spans the width,
     * taller, with its title and meta over the image. Only on Início's "Tudo", never in the lists.
     */
    public void setHeroFirst(boolean hero) {
        if (mHeroFirst == hero) {
            return;
        }
        mHeroFirst = hero;
        if (getItemCount() > 0) {
            notifyItemChanged(0);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_yew_list_header, parent, false), mClickListener);
        }
        if (viewType == VIEW_TYPE_STRIP) {
            return new StripViewHolder(inflater.inflate(R.layout.item_yew_channel_strip, parent, false));
        }
        if (viewType == VIEW_TYPE_STORAGE) {
            return new StorageViewHolder(inflater.inflate(R.layout.item_yew_storage, parent, false));
        }
        if (viewType == VIEW_TYPE_CHANNEL) {
            return new ChannelViewHolder(inflater.inflate(R.layout.item_mobile_channel_card, parent, false), mClickListener);
        }
        int layout = viewType == VIEW_TYPE_VIDEO_ROW ? R.layout.item_video_card_row
                : viewType == VIEW_TYPE_VIDEO_HERO ? R.layout.item_video_card_hero : R.layout.item_video_card;
        return new VideoViewHolder(inflater.inflate(layout, parent, false), mClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(getItem(position));
        } else if (holder instanceof StripViewHolder) {
            ((StripViewHolder) holder).bind(getItem(position), mStripChannelListener);
        } else if (holder instanceof StorageViewHolder) {
            ((StorageViewHolder) holder).bind(getItem(position));
        } else if (holder instanceof ChannelViewHolder) {
            ((ChannelViewHolder) holder).bind(getItem(position), mLongClickListener);
        } else {
            ((VideoViewHolder) holder).bind(getItem(position), mLongClickListener);
            ((VideoViewHolder) holder).bindChannelAvatar(mChannelClickListener);
            ((VideoViewHolder) holder).bindNumber(mNumbered ? position + 1 : 0);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof VideoViewHolder) {
            ((VideoViewHolder) holder).unbind();
        } else if (holder instanceof ChannelViewHolder) {
            ((ChannelViewHolder) holder).unbind();
        }
    }

    /**
     * Force a re-bind of the given items in place.
     *
     * Needed for {@code VideoGroup.ACTION_SYNC} (e.g. DeArrow / unlocalized-title overrides):
     * those processors MUTATE the existing {@link Video} instances in place
     * ({@code video.deArrowTitle} / {@code video.altCardImageUrl}) rather than emitting new
     * ones, so {@code submitList()} can't detect the change - {@link #DIFF_CALLBACK}'s
     * {@code areContentsTheSame} sees the same object reference on both sides and skips the
     * bind. We locate each item by content identity ({@link Video#equals}) in the currently
     * displayed list and rebind that row so the crowd-sourced title/thumbnail actually shows.
     */
    public void refreshItems(java.util.List<Video> items) {
        java.util.List<Video> current = getCurrentList();
        for (Video item : items) {
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).equals(item)) {
                    // Payloaded change: RecyclerView reuses the bound holder (no change-animation
                    // cross-fade of the whole card) and the partial bind below skips the Glide
                    // reload unless the thumbnail URL actually changed. A bare notifyItemChanged
                    // here made the just-watched card visibly blink on every Browse resume (the
                    // resume sync only carries a percentWatched update).
                    notifyItemChanged(i, PAYLOAD_SYNC);
                }
            }
        }
    }

    /** Marker payload for in-place item mutations (watch progress, DeArrow overrides). */
    private static final Object PAYLOAD_SYNC = new Object();
    /** NEWTUBE(feed-avatars): the card's channel avatar arrived or changed (ChannelAvatars). */
    private static final Object PAYLOAD_AVATAR = new Object();

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
            @NonNull java.util.List<Object> payloads) {
        if (!payloads.isEmpty() && holder instanceof VideoViewHolder) {
            VideoViewHolder card = (VideoViewHolder) holder;
            if (payloads.contains(PAYLOAD_SYNC)) {
                card.bindPartial(getItem(position));
            }
            // A sync can also bring the card its channel (Video.sync after playback).
            card.bindChannelAvatar(mChannelClickListener);
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    /**
     * Public for reuse by other screens that render the standard video card (kept public
     * after the channel page's old heterogeneous adapter was replaced by tabs, 2026-07-09).
     */
    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mThumbnail;
        private final View mThumbnailFrame;
        private final TextView mBadge;
        private final ProgressBar mWatchProgress;
        private final TextView mTitle;
        private final TextView mMeta;
        private final View mOverflow;
        private final ImageView mAvatar; // null on layouts without one
        // YEWPLAY(downloads): only on the compact row layout.
        private final View mDownloadRow;
        private final ProgressBar mDownloadProgress;
        private final TextView mDownloadPercent;
        private final TextView mMeta2;
        private OnVideoLongClickListener mLongClick;
        private Video mVideo;
        private String mBoundThumbUrl;
        private String mBoundAvatarUrl;

        public VideoViewHolder(@NonNull View itemView, OnVideoClickListener clickListener) {
            super(itemView);

            mThumbnail = itemView.findViewById(R.id.video_thumbnail);
            mThumbnailFrame = itemView.findViewById(R.id.video_thumbnail_frame);
            mBadge = itemView.findViewById(R.id.video_badge);
            mWatchProgress = itemView.findViewById(R.id.video_watch_progress);
            mTitle = itemView.findViewById(R.id.video_title);
            mMeta = itemView.findViewById(R.id.video_meta);
            mOverflow = itemView.findViewById(R.id.video_overflow);
            mAvatar = itemView.findViewById(R.id.video_channel_avatar);
            mDownloadRow = itemView.findViewById(R.id.yew_download_progress_row);
            mDownloadProgress = itemView.findViewById(R.id.yew_download_progress);
            mDownloadPercent = itemView.findViewById(R.id.yew_download_percent);
            mMeta2 = itemView.findViewById(R.id.video_meta2);

            itemView.setOnClickListener(v -> {
                if (mVideo != null && clickListener != null) {
                    if (mVideo.videoId != null) {
                        PlayerTransitionBridge.prepare(mThumbnailFrame, mBadge, mWatchProgress);
                    } else {
                        PlayerTransitionBridge.clear();
                    }
                    clickListener.onVideoClick(mVideo);
                }
            });
        }

        public void bind(Video video, OnVideoLongClickListener longClickListener) {
            mVideo = video;
            Context context = itemView.getContext();

            itemView.setOnLongClickListener(v ->
                    mVideo != null && longClickListener != null && longClickListener.onVideoLongClick(mVideo));

            // ⋮ = the exact long-press context menu, just discoverable (Not interested, Open
            // channel, playlists...). Hidden when the host screen has no menu wired.
            if (mOverflow != null) {
                mOverflow.setVisibility(longClickListener == null ? View.GONE : View.VISIBLE);
                mOverflow.setOnClickListener(v -> {
                    if (mVideo != null && longClickListener != null) {
                        longClickListener.onVideoLongClick(mVideo);
                    }
                });
                // NEWTUBE(a11y): one of 40 identical "More options" otherwise.
                mOverflow.setContentDescription(video.getTitle() != null
                        ? context.getString(R.string.mobile_more_options_for, video.getTitle())
                        : context.getString(R.string.mobile_player_more));
            }

            mTitle.setText(video.getTitle());

            // "Channel · 1.2M views · 3 weeks ago" like YouTube; author-only when the
            // service didn't build the info line (e.g. some playlist rows).
            CharSequence meta = video.getSecondTitle();
            mMeta.setText(yewMeta(context, meta == null || meta.length() == 0 ? video.getAuthor() : meta, video.getAuthor()));

            bindBadge(context, video);
            bindProgress(video);
            bindReadiness(video);
            bindThumbnail(context, video);
            bindDownload(context, video, longClickListener);
        }

        /**
         * YEWPLAY(downloads): a Downloads row as in the proposal (p. 16). Unfinished: title, the
         * rubi bar with its percentage, "82 de 180 MB, 2,1 MB/s" and a pause/resume button where the
         * ⋮ was. Finished: the format ("Vídeo MP4 1080p, 175,7 MB") and the date, with the ⋮.
         * {@code longClickListener} is null on a partial (sync) rebind: the ⋮ wiring is kept.
         */
        private void bindDownload(Context context, Video video, @androidx.annotation.Nullable OnVideoLongClickListener longClickListener) {
            if (longClickListener != null) {
                mLongClick = longClickListener;
            }
            if (mDownloadRow == null || mMeta2 == null) {
                return;
            }
            com.newtube.mobile.downloads.DownloadItem item = video.downloadId != null
                    ? com.newtube.mobile.downloads.DownloadsBridge.instance(context).itemFor(video) : null;
            boolean pending = item != null && !item.isDone();
            mDownloadRow.setVisibility(pending && !item.isFailed() ? View.VISIBLE : View.GONE);
            mMeta2.setVisibility(item != null ? View.VISIBLE : View.GONE);
            if (item == null) {
                mMeta.setVisibility(View.VISIBLE);
                if (mOverflow instanceof ImageView && longClickListener != null) {
                    ((ImageView) mOverflow).setImageResource(R.drawable.ic_mobile_more_vert);
                }
                return;
            }
            if (pending) {
                int percent = Math.max(0, item.progressPercent());
                mDownloadProgress.setIndeterminate(item.progressPercent() < 0 && !item.isPaused());
                mDownloadProgress.setProgress(percent);
                mDownloadPercent.setText(item.progressPercent() >= 0 ? percent + "%" : "");
                mMeta2.setText(com.newtube.mobile.downloads.DownloadRowText.progress(context, item));
                // The channel line is the meta; the progress text is the second line.
                mMeta.setText(item.author != null ? item.author : "");
                mMeta.setVisibility(item.author != null && !item.author.isEmpty() ? View.VISIBLE : View.GONE);
                boolean canToggle = item.state == com.newtube.mobile.downloads.DownloadItem.STATE_DOWNLOADING
                        || item.state == com.newtube.mobile.downloads.DownloadItem.STATE_QUEUED || item.isPaused();
                if (mOverflow instanceof ImageView) {
                    ImageView button = (ImageView) mOverflow;
                    button.setVisibility(canToggle || item.isFailed() ? View.VISIBLE : View.GONE);
                    button.setImageResource(item.isPaused() ? R.drawable.ic_yew_play
                            : item.isFailed() ? R.drawable.ic_mobile_more_vert : R.drawable.ic_yew_pause);
                    button.setContentDescription(context.getString(item.isPaused()
                            ? R.string.yew_download_resume : R.string.yew_download_pause));
                    if (!canToggle) {
                        button.setOnClickListener(v -> openMenu());
                    } else {
                        button.setOnClickListener(v -> {
                            com.newtube.mobile.ui.common.Haptics.tick(v);
                            if (item.isPaused()) {
                                com.newtube.mobile.downloads.MobileDownloadService.retry(context, item);
                            } else {
                                com.newtube.mobile.downloads.MobileDownloadService.pause(context, item);
                            }
                        });
                    }
                }
            } else {
                mMeta.setVisibility(View.VISIBLE);
                mMeta.setText(com.newtube.mobile.downloads.DownloadRowText.format(context, item));
                mMeta2.setText(com.newtube.mobile.downloads.DownloadRowText.date(context, item));
                if (mOverflow instanceof ImageView) {
                    mOverflow.setVisibility(View.VISIBLE);
                    ((ImageView) mOverflow).setImageResource(R.drawable.ic_mobile_more_vert);
                    mOverflow.setContentDescription(context.getString(R.string.mobile_player_more));
                    mOverflow.setOnClickListener(v -> openMenu());
                }
            }
        }

        void bindNumber(int number) {
            View view = itemView.findViewById(R.id.yew_row_number);
            if (view instanceof TextView) {
                ((TextView) view).setText(number > 0 ? String.valueOf(number) : null);
                view.setVisibility(number > 0 ? View.VISIBLE : View.GONE);
            }
        }

        private void openMenu() {
            if (mVideo != null && mLongClick != null) {
                mLongClick.onVideoLongClick(mVideo);
            }
        }

        /**
         * Sync rebind for an item mutated in place (ACTION_SYNC): refresh the text/progress
         * views and only touch the thumbnail if its resolved URL changed (DeArrow override
         * arriving). Keeping the ImageView untouched for a same-URL sync is what stops the
         * just-watched card from blinking when Browse resumes after playback.
         */
        void bindPartial(Video video) {
            mVideo = video;
            Context context = itemView.getContext();

            mTitle.setText(video.getTitle());
            CharSequence meta = video.getSecondTitle();
            mMeta.setText(yewMeta(context, meta == null || meta.length() == 0 ? video.getAuthor() : meta, video.getAuthor()));

            bindBadge(context, video);
            bindProgress(video);
            bindReadiness(video);
            bindDownload(context, video, null);

            String thumbnailUrl = thumbnailUrl(context, video);
            if (thumbnailUrl == null ? mBoundThumbUrl != null : !thumbnailUrl.equals(mBoundThumbUrl)) {
                bindThumbnail(context, video);
            }
        }

        /**
         * YEWPLAY(visual): the channel in SemiBold at the start of the meta line ("Tech Direto
         * 112 mil visualizações, há 2 dias"), like the proposal's items.
         */
        private CharSequence yewMeta(Context context, CharSequence meta, @androidx.annotation.Nullable String author) {
            if (meta == null || author == null || author.isEmpty() || !meta.toString().startsWith(author)) {
                return meta;
            }
            android.text.SpannableString styled = new android.text.SpannableString(meta);
            styled.setSpan(com.newtube.mobile.ui.common.AppFonts.semiboldSpan(context), 0, author.length(),
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // YEWPLAY(home): over the hero's image the channel stays white in both themes.
            int color = "hero".equals(itemView.getTag()) ? android.graphics.Color.WHITE
                    : ContextCompat.getColor(context, R.color.mobile_color_on_surface);
            styled.setSpan(new android.text.style.ForegroundColorSpan(color), 0, author.length(),
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return styled;
        }

        private void bindBadge(Context context, Video video) {
            String badgeText;
            if (video.hasNewContent) {
                badgeText = context.getString(R.string.badge_new_content);
            } else if (video.isLive) {
                badgeText = context.getString(R.string.badge_live);
            } else if (video.isShorts) {
                badgeText = context.getString(R.string.header_shorts).toUpperCase();
            } else {
                badgeText = video.badge;
            }

            if (badgeText == null || badgeText.isEmpty()) {
                mBadge.setVisibility(View.GONE);
            } else {
                // YEWPLAY(visual): "Ao vivo" in rubi at the bottom-left, with a dot; the duration
                // stays a dark pill at the bottom-right. A tint keeps the badge's rounded shape
                // (setBackgroundColor used to replace it with a square).
                boolean live = video.isLive || video.isUpcoming;
                mBadge.setText(video.isLive ? "\u25CF " + badgeText : badgeText);
                mBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context,
                        live ? R.color.mobile_color_badge_live_bg : R.color.mobile_color_badge_bg)));
                android.view.ViewGroup.LayoutParams lp = mBadge.getLayoutParams();
                if (lp instanceof android.widget.FrameLayout.LayoutParams) {
                    // YEWPLAY(home): on the hero the text sits at the bottom of the image and the ⋮ at the
                    // top right, so its badge goes to the top left.
                    boolean hero = "hero".equals(itemView.getTag());
                    int gravity = hero ? android.view.Gravity.TOP | android.view.Gravity.START
                            : android.view.Gravity.BOTTOM | (live ? android.view.Gravity.START : android.view.Gravity.END);
                    if (((android.widget.FrameLayout.LayoutParams) lp).gravity != gravity) {
                        ((android.widget.FrameLayout.LayoutParams) lp).gravity = gravity;
                        mBadge.setLayoutParams(lp);
                    }
                }
                mBadge.setVisibility(View.VISIBLE);
            }
        }

        /**
         * NEWTUBE(downloads): a download that is not playable yet (queued, fetching, finishing,
         * failed) keeps its thumbnail dimmed so it never reads as a ready video - the badge and
         * the meta line say what it is doing.
         */
        private void bindReadiness(Video video) {
            mThumbnail.setAlpha(video.isPendingDownload() ? 0.4f : 1f);
        }

        private void bindProgress(Video video) {
            // Count progress that's very close to zero (e.g. user closed the video immediately).
            int progress = video.percentWatched > 0 && video.percentWatched < 1 ? 1 : Math.round(video.percentWatched);

            if (progress > 0 && progress <= 100) {
                mWatchProgress.setProgress(progress);
                mWatchProgress.setVisibility(View.VISIBLE);
            } else {
                mWatchProgress.setVisibility(View.GONE);
            }
        }

        /**
         * SCROLL-JANK FIX: decode thumbnails downsampled to the card's real display size (not the
         * layout-determined size Glide would otherwise wait a measure pass for) and in RGB_565.
         * Thumbnails are opaque JPEGs, so 565 halves both decode work and the GPU texture upload
         * per card - the biggest per-frame cost while fling-scrolling the grid. Sized for the
         * portrait single-column feed (the largest card = full screen width); landscape splits
         * the wider dimension across 2+ columns, so this stays a correct downsample bound there.
         */
        private static int sThumbW;
        private static int sThumbH;

        private static int thumbWidth(Context context) {
            if (sThumbW == 0) {
                android.util.DisplayMetrics dm = context.getResources().getDisplayMetrics();
                sThumbW = Math.min(dm.widthPixels, dm.heightPixels);
                sThumbH = sThumbW * 9 / 16;
            }
            return sThumbW;
        }

        /**
         * The card's primary thumbnail request, without its view-only options (transition, error
         * fallback). NEWTUBE(scroll): shared with {@link com.newtube.mobile.ui.common.FeedThumbnailPreloader}
         * so a preloaded image has exactly the memory-cache key the bind will ask for (same URL,
         * size, transformation and decode format) and the card binds it with no network wait and
         * no fade.
         */
        public static com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> thumbnailRequest(
                com.bumptech.glide.RequestManager glide, Context context, Video video) {
            int w = thumbWidth(context);
            return glide
                    .load(thumbnailUrl(context, video))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .override(w, sThumbH)
                    .centerCrop();
        }

        public static String thumbnailUrl(Context context, Video video) {
            // NEWTUBE(ux): entries saved without an image (local history, link opens) get the
            // i.ytimg.com fallback here, so the preloader and the bind ask for the same URL.
            com.newtube.mobile.ui.common.LocalThumbnails.fill(video);
            return ClickbaitRemover.updateThumbnail(video, MainUIData.instance(context).getThumbQuality());
        }

        private void bindThumbnail(Context context, Video video) {
            String thumbnailUrl = thumbnailUrl(context, video);
            mBoundThumbUrl = thumbnailUrl;

            int w = thumbWidth(context);
            com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> request =
                    thumbnailRequest(Glide.with(context), context, video)
                    // Network loads fade in over the placeholder-colored card instead of popping;
                    // memory-cache hits skip the transition entirely, so scrolling stays crisp.
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(150));

            // NEWTUBE(startup): release-visible "first thumbnail on screen" milestone. The listener
            // is attached only until it has fired once per process.
            if (com.newtube.mobile.LaunchMilestones.wantsFirstThumb()) {
                request = request.addListener(FIRST_THUMB_LISTENER);
            }

            // At the default thumb quality the primary URL IS the card URL, so a fallback request
            // would be identical - only pay the second RequestBuilder when it can actually differ.
            String fallbackUrl = video.getCardImageUrl();
            if (fallbackUrl != null && !fallbackUrl.equals(thumbnailUrl)) {
                request = request.error(Glide.with(context)
                        .load(fallbackUrl)
                        .format(DecodeFormat.PREFER_RGB_565)
                        .override(w, sThumbH)
                        .centerCrop());
            }

            request.into(mThumbnail);
        }

        private static final com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> FIRST_THUMB_LISTENER =
                new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                            boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                            com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        com.newtube.mobile.LaunchMilestones.onFirstThumb(dataSource);
                        return false;
                    }
                };

        /**
         * NEWTUBE(feed-avatars): the channel avatar left of the title, on a video card of a screen
         * that wants them ({@code onChannelClick} set). Its URL comes from {@link ChannelAvatars};
         * an unknown one shows the placeholder circle and is asked for, and the adapter rebinds
         * this when it arrives.
         */
        public void bindChannelAvatar(OnVideoClickListener onChannelClick) {
            if (mAvatar == null) {
                return;
            }
            Video video = mVideo;
            if (onChannelClick == null || video == null || !hasChannelAvatar(video)) {
                if (mAvatar.getVisibility() != View.GONE) {
                    mAvatar.setVisibility(View.GONE);
                    Glide.with(itemView.getContext().getApplicationContext()).clear(mAvatar);
                    mBoundAvatarUrl = null;
                }
                return;
            }

            Context context = itemView.getContext();
            mAvatar.setVisibility(View.VISIBLE);
            mAvatar.setOnClickListener(v -> {
                if (mVideo != null) {
                    PlayerTransitionBridge.clear();
                    onChannelClick.onVideoClick(mVideo);
                }
            });
            String author = video.getAuthor();
            mAvatar.setContentDescription(author != null && !author.isEmpty()
                    ? context.getString(R.string.mobile_card_go_to_channel, author)
                    : context.getString(R.string.open_channel));

            int size = context.getResources().getDimensionPixelSize(R.dimen.mobile_card_avatar_size);
            ChannelAvatars.request(video); // no-op when known and fresh
            String url = ChannelAvatars.sized(ChannelAvatars.get(video), size);
            if (url == null) {
                mBoundAvatarUrl = null;
                Glide.with(context.getApplicationContext()).clear(mAvatar);
                mAvatar.setImageResource(R.drawable.bg_card_avatar_placeholder);
                return;
            }
            if (url.equals(mBoundAvatarUrl)) {
                return; // already showing (or loading) it: no flash on a rebind
            }
            mBoundAvatarUrl = url;
            Glide.with(context)
                    .load(url)
                    .override(size)
                    .transform(com.newtube.mobile.ui.common.YewAvatar.get()) // YEWPLAY: rounded square
                    .placeholder(R.drawable.bg_card_avatar_placeholder)
                    .error(R.drawable.bg_card_avatar_placeholder)
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(150))
                    .into(mAvatar);
        }

        /**
         * A single video. A mix or a playlist has no one channel to show (YouTube shows none there
         * either; a mix's menu names no channel), nor has a channel row.
         */
        static boolean hasChannelAvatar(Video video) {
            return video.videoId != null && !video.hasPlaylist();
        }

        public void unbind() {
            mVideo = null;
            mBoundThumbUrl = null;
            Glide.with(itemView.getContext().getApplicationContext()).clear(mThumbnail);
            if (mAvatar != null && mBoundAvatarUrl != null) {
                Glide.with(itemView.getContext().getApplicationContext()).clear(mAvatar);
                mAvatar.setImageResource(R.drawable.bg_card_avatar_placeholder);
            }
            mBoundAvatarUrl = null;
        }
    }

    /**
     * Channel result row: round avatar (the channel item's cardImageUrl IS the avatar) +
     * name + "subscribers · N videos". Tap routes through the same click listener — the
     * presenters' onVideoItemClicked already opens the mobile channel screen for
     * {@code Video.isChannel()} items; long-press/⋮-less row keeps the context menu too.
     */
    static class ChannelViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mAvatar;
        private final TextView mName;
        private final TextView mMeta;
        private Video mVideo;

        ChannelViewHolder(@NonNull View itemView, OnVideoClickListener clickListener) {
            super(itemView);

            mAvatar = itemView.findViewById(R.id.channel_avatar);
            mName = itemView.findViewById(R.id.channel_name);
            mMeta = itemView.findViewById(R.id.channel_meta);

            itemView.setOnClickListener(v -> {
                if (mVideo != null && clickListener != null) {
                    PlayerTransitionBridge.clear();
                    clickListener.onVideoClick(mVideo);
                }
            });
        }

        void bind(Video video, OnVideoLongClickListener longClickListener) {
            mVideo = video;

            itemView.setOnLongClickListener(v ->
                    mVideo != null && longClickListener != null && longClickListener.onVideoLongClick(mVideo));

            mName.setText(video.getTitle());

            // secondTitle = subscriber count, badge = "N videos" (see MediaItemImpl mapping).
            CharSequence subs = video.getSecondTitle();
            String videosCount = video.badge;
            StringBuilder meta = new StringBuilder();
            if (subs != null && subs.length() > 0) {
                meta.append(subs);
            }
            if (videosCount != null && !videosCount.isEmpty()) {
                if (meta.length() > 0) {
                    meta.append(com.newtube.mobile.ui.common.MetaSeparator.DOT);
                }
                meta.append(videosCount);
            }
            mMeta.setText(meta);
            mMeta.setVisibility(meta.length() == 0 ? View.GONE : View.VISIBLE);

            Glide.with(itemView.getContext())
                    .load(video.getCardImageUrl())
                    .transform(com.newtube.mobile.ui.common.YewAvatar.get()) // YEWPLAY: rounded square
                    .placeholder(R.drawable.ic_watch_channel_placeholder)
                    .error(R.drawable.ic_watch_channel_placeholder)
                    .into(mAvatar);
        }

        void unbind() {
            mVideo = null;
            Glide.with(itemView.getContext().getApplicationContext()).clear(mAvatar);
        }
    }

    /** YEWPLAY(lists): "Baixando" / "Pausar tudo", "Concluídos" / "10 itens", "Hoje" / "8 novos". */
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTitle;
        private final TextView mEnd;
        private final OnVideoClickListener mClick;

        HeaderViewHolder(@NonNull View itemView, OnVideoClickListener click) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.yew_header_title);
            mEnd = itemView.findViewById(R.id.yew_header_end);
            mClick = click;
        }

        void bind(Video header) {
            mTitle.setText(header.yewHeader);
            mEnd.setText(header.yewHeaderEnd);
            mEnd.setVisibility(header.yewHeaderEnd != null && !header.yewHeaderEnd.isEmpty() ? View.VISIBLE : View.GONE);
            mEnd.setClickable(header.yewHeaderAction);
            mEnd.setFocusable(header.yewHeaderAction);
            mEnd.setOnClickListener(header.yewHeaderAction && mClick != null ? v -> {
                com.newtube.mobile.ui.common.Haptics.tick(v);
                mClick.onVideoClick(header);
            } : null);
        }
    }

    /** YEWPLAY(downloads): "1,4 GB usados pelo app ... 38 GB livres" and the thin bar. */
    static class StorageViewHolder extends RecyclerView.ViewHolder {
        private final TextView mUsed;
        private final TextView mFree;
        private final ProgressBar mBar;

        StorageViewHolder(@NonNull View itemView) {
            super(itemView);
            mUsed = itemView.findViewById(R.id.yew_storage_used);
            mFree = itemView.findViewById(R.id.yew_storage_free);
            mBar = itemView.findViewById(R.id.yew_storage_bar);
        }

        void bind(Video storage) {
            Context context = itemView.getContext();
            mUsed.setText(context.getString(R.string.yew_storage_used,
                    com.newtube.mobile.downloads.DownloadOptions.formatBytes(storage.yewStorageUsed)));
            mFree.setText(storage.yewStorageFree >= 0 ? context.getString(R.string.yew_storage_free,
                    com.newtube.mobile.downloads.DownloadOptions.formatBytes(storage.yewStorageFree)) : "");
            long total = storage.yewStorageUsed + Math.max(0, storage.yewStorageFree);
            mBar.setProgress(total > 0 ? (int) Math.max(storage.yewStorageUsed > 0 ? 8 : 0,
                    storage.yewStorageUsed * 1000 / total) : 0);
        }
    }

    /**
     * YEWPLAY(subs): the avatars row of Inscrições (proposta p. 15) - rounded-square avatars with the
     * channel's name under them and a rubi dot on channels with something new today. A tap opens
     * the channel, like a card's avatar.
     */
    static class StripViewHolder extends RecyclerView.ViewHolder {
        private final android.widget.LinearLayout mRow;

        StripViewHolder(@NonNull View itemView) {
            super(itemView);
            mRow = itemView.findViewById(R.id.yew_strip_row);
        }

        void bind(Video strip, OnVideoClickListener onChannel) {
            Context context = itemView.getContext();
            mRow.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(context);
            int size = context.getResources().getDimensionPixelSize(R.dimen.yew_strip_avatar);
            for (Video video : strip.yewStrip) {
                View cell = inflater.inflate(R.layout.item_yew_channel_strip_cell, mRow, false);
                ImageView avatar = cell.findViewById(R.id.yew_strip_avatar);
                TextView initials = cell.findViewById(R.id.yew_strip_initials);
                TextView name = cell.findViewById(R.id.yew_strip_name);
                String author = video.getAuthor() != null ? video.getAuthor() : "";
                name.setText(author);
                initials.setText(initialsOf(author));
                String channelId = ChannelAvatars.channelIdOf(video);
                cell.findViewById(R.id.yew_strip_dot).setVisibility(strip.yewStripNew != null && channelId != null
                        && strip.yewStripNew.contains(channelId) ? View.VISIBLE : View.GONE);
                String url = ChannelAvatars.get(video);
                if (url != null) {
                    initials.setVisibility(View.GONE);
                    Glide.with(context)
                            .load(ChannelAvatars.sized(url, size))
                            .override(size)
                            .transform(com.newtube.mobile.ui.common.YewAvatar.get())
                            .placeholder(R.drawable.bg_card_avatar_placeholder)
                            .into(avatar);
                } else {
                    ChannelAvatars.request(video);
                    initials.setVisibility(View.VISIBLE);
                    avatar.setImageResource(R.drawable.bg_card_avatar_placeholder);
                }
                cell.setContentDescription(author);
                cell.setOnClickListener(onChannel == null ? null : v -> onChannel.onVideoClick(video));
                mRow.addView(cell);
            }
        }

        private static String initialsOf(String name) {
            StringBuilder out = new StringBuilder();
            for (String part : name.trim().split("\\s+")) {
                if (!part.isEmpty() && Character.isLetterOrDigit(part.codePointAt(0))) {
                    out.appendCodePoint(Character.toUpperCase(part.codePointAt(0)));
                }
                if (out.length() >= 2) {
                    break;
                }
            }
            return out.toString();
        }
    }
}
