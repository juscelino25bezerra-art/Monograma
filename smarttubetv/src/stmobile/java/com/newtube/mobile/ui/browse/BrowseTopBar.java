package com.newtube.mobile.ui.browse;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * NEWTUBE(you-subscreen): Browse's top bar. A section opened from a You row (Sports, Playlists,
 * ...) is a sub-screen of You, so it gets a back arrow and the section's name - like YouTube's You
 * sub-pages - instead of a wordmark that gave no hint where the user was or that back returns to You.
 *
 * <p>MONOGRAMA(visual): three looks, as in the mockups - {@link #HOME}: the logo tile and the
 * "Monograma" wordmark, with Cast and Search; {@link #TAB}: the tab's name as a large title
 * ("Downloads", "Você"), with Search (Settings on the You tab); {@link #SUBSCREEN}: back arrow and
 * the section's name.</p>
 */
final class BrowseTopBar {
    static final int HOME = 0;
    static final int TAB = 1;
    static final int SUBSCREEN = 2;

    private static final float WORDMARK_SP = 24f;
    private static final float TAB_TITLE_SP = 28f;
    private static final float SUBSCREEN_TITLE_SP = 20f;

    private final TextView mTitle;
    private final View mBack;
    private final View mLogo;
    private final View mCast;
    private final View mSearch;
    private final View mSettings;
    private final int mBesideLogoInset;
    private final int mEdgeInset;
    private final int mBesideBackInset;

    BrowseTopBar(Activity activity, Runnable onBack) {
        mTitle = activity.findViewById(R.id.mobile_title_bar);
        mBack = activity.findViewById(R.id.mobile_title_back);
        mLogo = activity.findViewById(R.id.mobile_title_logo);
        mCast = activity.findViewById(R.id.mobile_cast_button);
        mSearch = activity.findViewById(R.id.mobile_search_button);
        mSettings = activity.findViewById(R.id.mobile_settings_button);
        mBack.setOnClickListener(v -> onBack.run());
        float density = activity.getResources().getDisplayMetrics().density;
        mBesideLogoInset = mTitle.getPaddingStart();
        mEdgeInset = Math.round(16 * density);
        mBesideBackInset = activity.getResources()
                .getDimensionPixelSize(R.dimen.mobile_dialog_title_inset_with_back);
    }

    /** The Settings button of the You tab's bar. */
    View settingsButton() {
        return mSettings;
    }

    /**
     * @param look  {@link #HOME}, {@link #TAB} or {@link #SUBSCREEN}
     * @param title the tab's or section's name (the wordmark if there is none)
     * @param you   the You tab: Settings instead of Search
     */
    void show(int look, @Nullable CharSequence title, boolean you) {
        boolean home = look == HOME;
        boolean sub = look == SUBSCREEN;
        mBack.setVisibility(sub ? View.VISIBLE : View.GONE);
        // GONE collapses the logo to a point, so the title then starts where the logo did.
        mLogo.setVisibility(home ? View.VISIBLE : View.GONE);
        mTitle.setText(!home && title != null && title.length() > 0
                ? title : mTitle.getContext().getString(R.string.app_name));
        mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, home ? WORDMARK_SP : sub ? SUBSCREEN_TITLE_SP : TAB_TITLE_SP);
        // ConstraintLayout drops a GONE view's margins: without the logo the title starts at the
        // screen edge (or right after the back arrow), so its own padding sets the inset.
        int start = home ? mBesideLogoInset : sub ? mBesideBackInset : mEdgeInset;
        mTitle.setPaddingRelative(start, mTitle.getPaddingTop(), mTitle.getPaddingEnd(), mTitle.getPaddingBottom());

        mCast.setVisibility(home ? View.VISIBLE : View.GONE);
        mSearch.setVisibility(you ? View.GONE : View.VISIBLE);
        mSettings.setVisibility(you ? View.VISIBLE : View.GONE);
    }
}
