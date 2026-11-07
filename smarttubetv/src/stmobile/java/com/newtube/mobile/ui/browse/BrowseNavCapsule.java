package com.newtube.mobile.ui.browse;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * YEWPLAY(nav): a barra flutuante do Browse (activity_mobile_browse: mobile_nav_capsule).
 * <ul>
 *   <li>o traço rubi acima do destino ativo, posicionado sobre o item escolhido;</li>
 *   <li>o respiro no fim da lista, que passa por baixo da barra, na altura da barra (com ou sem o
 *   mini-player dentro dela);</li>
 *   <li>onde o vídeo do mini-player vai ficar, para a animação de minimizar do player.</li>
 * </ul>
 */
final class BrowseNavCapsule {
    private final View mCapsule;
    private final BottomNavigationView mNav;
    private final View mIndicator;
    private final View mMini;
    private final RecyclerView mList;
    private final int mListBasePadding;
    private final int mGap;
    private int mSelectedId = View.NO_ID;

    BrowseNavCapsule(@NonNull View root, @NonNull RecyclerView list) {
        mCapsule = root.findViewById(R.id.mobile_nav_capsule);
        mNav = root.findViewById(R.id.mobile_bottom_nav);
        mIndicator = root.findViewById(R.id.mobile_nav_indicator);
        mMini = root.findViewById(R.id.mobile_mini_player);
        mList = list;
        mListBasePadding = list.getPaddingBottom();
        mGap = Math.round(12 * root.getResources().getDisplayMetrics().density);
        mCapsule.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            updateListPadding();
            placeIndicator(false);
        });
    }

    /** The destination now lit (a menu item id), or NO_ID for none (a Library sub-screen). */
    void setSelected(int itemId) {
        boolean moved = mSelectedId != View.NO_ID && mSelectedId != itemId;
        mSelectedId = itemId;
        placeIndicator(moved);
    }

    private void placeIndicator(boolean animate) {
        View item = mSelectedId != View.NO_ID ? mNav.findViewById(mSelectedId) : null;
        if (item == null || item.getWidth() == 0) {
            mIndicator.setVisibility(View.INVISIBLE);
            return;
        }
        // The item sits inside the nav's menu view; both start at the capsule's left edge.
        Rect bounds = new Rect(0, 0, item.getWidth(), item.getHeight());
        ((ViewGroup) mIndicator.getParent()).offsetDescendantRectToMyCoords(item, bounds);
        float x = bounds.centerX() - mIndicator.getWidth() / 2f;
        mIndicator.setVisibility(View.VISIBLE);
        mIndicator.animate().cancel();
        if (animate) {
            mIndicator.animate().translationX(x).setDuration(180).start(); // proposta, p. 8: 180 ms
        } else {
            mIndicator.setTranslationX(x);
        }
    }

    private void updateListPadding() {
        View parent = (View) mCapsule.getParent();
        int covered = parent.getHeight() - mCapsule.getTop();
        int bottom = mListBasePadding + Math.max(0, covered) + mGap;
        if (mList.getPaddingBottom() != bottom) {
            mList.setPadding(mList.getPaddingLeft(), mList.getPaddingTop(), mList.getPaddingRight(), bottom);
        }
    }

    /**
     * Where the mini-player's video box is (or will be once shown) on screen: the player's
     * minimize animation lands the video there. Hidden, the row will open above the destinations,
     * growing the capsule upwards by its own height.
     */
    boolean videoTargetOnScreen(@NonNull Rect out) {
        View box = mMini.findViewById(R.id.mobile_mini_video_box);
        if (box == null || mCapsule.getWidth() == 0) {
            return false;
        }
        if (mMini.getVisibility() == View.VISIBLE && box.getWidth() > 0) {
            int[] at = new int[2];
            box.getLocationOnScreen(at);
            out.set(at[0], at[1], at[0] + box.getWidth(), at[1] + box.getHeight());
            return true;
        }
        android.content.res.Resources res = mCapsule.getResources();
        int rowHeight = res.getDimensionPixelSize(R.dimen.yew_mini_row_height);
        int padding = res.getDimensionPixelSize(R.dimen.yew_mini_row_padding);
        int width = res.getDimensionPixelSize(R.dimen.yew_mini_video_width);
        int height = res.getDimensionPixelSize(R.dimen.yew_mini_video_height);
        int[] at = new int[2];
        mCapsule.getLocationOnScreen(at);
        int rowTop = at[1] - rowHeight;
        int top = rowTop + (rowHeight - height) / 2;
        out.set(at[0] + padding, top, at[0] + padding + width, top + height);
        return true;
    }
}
