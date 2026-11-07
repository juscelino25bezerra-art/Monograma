package com.newtube.mobile.ui.browse;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.List;

/**
 * YEWPLAY(nav): as abas de filtro do Início (proposta, p. 6 e 12) - texto, sem chip nem contorno; a
 * ativa em marfim com um ponto rubi embaixo, as outras em cinza quente. Cada aba é uma seção do
 * Browse ("Tudo" é o Início, as outras são Música, Jogos, Ao vivo...), então tocar numa troca de
 * seção sem sair do destino Início.
 */
final class FilterTabs {
    interface Listener {
        void onTabChosen(int sectionId);
    }

    static final class Tab {
        final int sectionId;
        final CharSequence title;

        Tab(int sectionId, CharSequence title) {
            this.sectionId = sectionId;
            this.title = title;
        }
    }

    private final HorizontalScrollView mScroll;
    private final LinearLayout mRow;
    private final Listener mListener;
    private String mSignature = "";
    private int mSelected = Integer.MIN_VALUE;

    FilterTabs(@NonNull HorizontalScrollView scroll, @NonNull LinearLayout row, @NonNull Listener listener) {
        mScroll = scroll;
        mRow = row;
        mListener = listener;
    }

    void show(boolean visible) {
        mScroll.setVisibility(visible && mRow.getChildCount() > 1 ? View.VISIBLE : View.GONE);
    }

    /** Rebuilds the row only when the tabs changed. */
    void setTabs(@NonNull List<Tab> tabs) {
        StringBuilder signature = new StringBuilder();
        for (Tab tab : tabs) {
            signature.append(tab.sectionId).append('\u0001').append(tab.title).append('\u0002');
        }
        if (signature.toString().equals(mSignature)) {
            return;
        }
        mSignature = signature.toString();
        mRow.removeAllViews();
        Context context = mRow.getContext();
        for (Tab tab : tabs) {
            mRow.addView(makeTab(context, tab));
        }
        int selected = mSelected;
        mSelected = Integer.MIN_VALUE;
        select(selected);
    }

    void select(int sectionId) {
        if (mSelected == sectionId) {
            return;
        }
        mSelected = sectionId;
        for (int i = 0; i < mRow.getChildCount(); i++) {
            View tab = mRow.getChildAt(i);
            boolean on = tab.getTag() instanceof Integer && (Integer) tab.getTag() == sectionId;
            TextView label = tab.findViewById(R.id.yew_filter_label);
            label.setTextColor(ContextCompat.getColor(tab.getContext(),
                    on ? R.color.mobile_color_on_surface : R.color.mobile_color_on_surface_secondary));
            label.setTypeface(ResourcesCompat.getFont(tab.getContext(),
                    on ? R.font.instrument_semibold : R.font.instrument_medium));
            tab.findViewById(R.id.yew_filter_dot).setVisibility(on ? View.VISIBLE : View.INVISIBLE);
            tab.setSelected(on);
            if (on) {
                // Keep the chosen tab in view when it is off the end of the row.
                tab.post(() -> mScroll.requestChildRectangleOnScreen(mRow,
                        new android.graphics.Rect(tab.getLeft(), 0, tab.getRight(), tab.getHeight()), false));
            }
        }
    }

    private View makeTab(Context context, Tab tab) {
        float density = context.getResources().getDisplayMetrics().density;
        FrameLayout frame = new FrameLayout(context);
        frame.setTag(tab.sectionId);
        frame.setMinimumHeight(Math.round(44 * density));
        int pad = Math.round(12 * density);
        frame.setPadding(pad, 0, pad, 0);
        TypedValue ripple = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        frame.setBackgroundResource(ripple.resourceId);
        frame.setContentDescription(tab.title);
        frame.setOnClickListener(v -> {
            if (mSelected != tab.sectionId) {
                com.newtube.mobile.ui.common.Haptics.tick(v);
                mListener.onTabChosen(tab.sectionId);
            }
        });
        frame.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.RadioButton.class.getName());
                info.setCheckable(true);
                info.setChecked(host.isSelected());
            }
        });

        TextView label = new TextView(context);
        label.setId(R.id.yew_filter_label);
        label.setText(tab.title);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        label.setMaxLines(1);
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        labelLp.bottomMargin = Math.round(4 * density);
        frame.addView(label, labelLp);

        View dot = new View(context);
        dot.setId(R.id.yew_filter_dot);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(ContextCompat.getColor(context, R.color.mobile_color_accent));
        dot.setBackground(disc);
        dot.setVisibility(View.INVISIBLE);
        int size = Math.round(4 * density);
        FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(size, size, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        dotLp.bottomMargin = Math.round(5 * density);
        frame.addView(dot, dotLp);
        return frame;
    }
}
