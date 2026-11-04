package com.newtube.mobile.ui.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/** Renders a page's {@link SettingsRow}s. Clicks go back to the page, which applies them. */
final class SettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    /** Re-read the values (summaries, switch states) without rebuilding the views. */
    static final Object PAYLOAD_VALUES = new Object();

    interface Listener {
        void onRowClicked(@NonNull SettingsRow row);

        /** MONOGRAMA(visual): a segment or an icon card of an inline choice was tapped. */
        void onOptionPicked(@NonNull SettingsRow row, int index);
    }

    /** MONOGRAMA(visual): view types of the inline choices (the row kinds are 0..5). */
    private static final int VIEW_SEGMENTED = 100;
    private static final int VIEW_ICONS = 101;

    private final Listener mListener;
    private List<SettingsRow> mRows = new ArrayList<>();

    SettingsAdapter(Listener listener) {
        mListener = listener;
    }

    void submit(List<SettingsRow> rows) {
        if (rows.size() == mRows.size() && sameShape(mRows, rows)) {
            mRows = rows;
            notifyItemRangeChanged(0, rows.size(), PAYLOAD_VALUES);
        } else {
            mRows = rows;
            notifyDataSetChanged();
        }
    }

    /** Same kinds and titles in the same places: a value changed, not the page. */
    private static boolean sameShape(List<SettingsRow> a, List<SettingsRow> b) {
        for (int i = 0; i < a.size(); i++) {
            SettingsRow x = a.get(i);
            SettingsRow y = b.get(i);
            if (x.kind != y.kind || !String.valueOf(x.title).equals(String.valueOf(y.title))) {
                return false;
            }
        }
        return true;
    }

    /** Where the row with this title is (a search result pointing at it), or -1. */
    int indexOf(@NonNull String title) {
        for (int i = 0; i < mRows.size(); i++) {
            SettingsRow row = mRows.get(i);
            if (row.kind != SettingsRow.KIND_HEADER && row.kind != SettingsRow.KIND_NOTE
                    && title.equals(String.valueOf(row.title))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        SettingsRow row = mRows.get(position);
        if (row.kind == SettingsRow.KIND_CHOICE && row.iconPicker) {
            return VIEW_ICONS;
        }
        if (row.kind == SettingsRow.KIND_CHOICE && row.inline) {
            return VIEW_SEGMENTED;
        }
        return row.kind;
    }

    /**
     * MONOGRAMA(visual): a row's background by its place in its group - the run of card rows
     * between two headers, notes, dividers or pickers - so the run reads as one rounded card.
     * Rows after the first get a hairline on top.
     */
    private void bindGroup(@NonNull View itemView, int position) {
        boolean first = position == 0 || !mRows.get(position - 1).isInCard();
        boolean last = position == mRows.size() - 1 || !mRows.get(position + 1).isInCard();
        itemView.setBackgroundResource(first && last ? R.drawable.bg_settings_group_single
                : first ? R.drawable.bg_settings_group_top
                : last ? R.drawable.bg_settings_group_bottom
                : R.drawable.bg_settings_group_middle);

        View separator = itemView.findViewById(R.id.settings_row_separator);
        if (separator != null) {
            separator.setVisibility(first ? View.GONE : View.VISIBLE);
            // Under the text when the row (and so the card) has icons, edge to edge otherwise.
            boolean icons = mRows.get(position).icon != 0 && mRows.get(position - (first ? 0 : 1)).icon != 0;
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) separator.getLayoutParams();
            int start = itemView.getResources().getDimensionPixelSize(icons
                    ? R.dimen.mobile_settings_separator_with_icon : R.dimen.mobile_settings_row_inset);
            if (lp.getMarginStart() != start) {
                lp.setMarginStart(start);
                separator.setLayoutParams(lp);
            }
        }
        // The outer gap that keeps two cards apart: a header or a divider supplies it.
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
        int bottom = last && position < mRows.size() - 1 && mRows.get(position + 1).kind == SettingsRow.KIND_NOTE
                ? Math.round(4 * itemView.getResources().getDisplayMetrics().density) : 0;
        if (lp.bottomMargin != bottom) {
            lp.bottomMargin = bottom;
            itemView.setLayoutParams(lp);
        }
    }

    @Override
    public int getItemCount() {
        return mRows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case SettingsRow.KIND_HEADER:
                return new TextHolder(inflater.inflate(R.layout.item_mobile_settings_header, parent, false));
            case SettingsRow.KIND_NOTE:
                return new TextHolder(inflater.inflate(R.layout.item_mobile_settings_note, parent, false));
            case SettingsRow.KIND_DIVIDER:
                return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_mobile_settings_divider, parent, false)) {};
            case VIEW_SEGMENTED:
                return new SegmentedHolder(inflater.inflate(R.layout.item_mobile_settings_segmented, parent, false));
            case VIEW_ICONS:
                return new IconsHolder(inflater.inflate(R.layout.item_mobile_settings_icons, parent, false));
            default:
                return new RowHolder(inflater.inflate(R.layout.item_mobile_settings_row, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SettingsRow row = mRows.get(position);
        if (holder instanceof TextHolder) {
            ((TextHolder) holder).text.setText(row.title);
        } else if (holder instanceof RowHolder) {
            ((RowHolder) holder).bind(row, mListener);
            bindGroup(holder.itemView, position);
        } else if (holder instanceof SegmentedHolder) {
            ((SegmentedHolder) holder).bind(row, mListener);
            bindGroup(holder.itemView, position);
        } else if (holder instanceof IconsHolder) {
            ((IconsHolder) holder).bind(row, mListener);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        holder.itemView.setForeground(null); // a search result's highlight, if it was still glowing
    }


    private static final class TextHolder extends RecyclerView.ViewHolder {
        final TextView text;

        TextHolder(@NonNull View itemView) {
            super(itemView);
            text = (TextView) itemView;
        }
    }

    private static final class RowHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;
        private final TextView summary;
        private final SwitchMaterial toggle;
        private final TextView tag;
        private final TextView badge;
        private final ImageView chevron;

        RowHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.settings_row_icon);
            title = itemView.findViewById(R.id.settings_row_title);
            summary = itemView.findViewById(R.id.settings_row_summary);
            toggle = itemView.findViewById(R.id.settings_row_switch);
            tag = itemView.findViewById(R.id.settings_row_tag);
            badge = itemView.findViewById(R.id.settings_row_badge);
            chevron = itemView.findViewById(R.id.settings_row_chevron);
        }

        void bind(SettingsRow row, Listener listener) {
            if (row.icon != 0) {
                icon.setImageResource(row.icon);
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setVisibility(View.GONE);
            }

            title.setText(row.title);
            title.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    row.destructive ? R.color.mobile_color_primary : R.color.mobile_color_on_surface));

            CharSequence text = row.summaryText();
            if (text != null && text.length() > 0) {
                summary.setText(text);
                summary.setVisibility(View.VISIBLE);
            } else {
                summary.setVisibility(View.GONE);
            }

            if (row.tag != null && row.tag.length() > 0) {
                tag.setText(row.tag);
                tag.setVisibility(View.VISIBLE);
            } else {
                tag.setVisibility(View.GONE);
            }

            // MONOGRAMA(visual): the On/Off badge, and a chevron on rows that go somewhere (a
            // page, or a choice's dialog) - not on actions, switches or badged rows.
            if (row.status != null) {
                boolean on = row.status.getAsBoolean();
                badge.setText(on ? R.string.mobile_settings_status_on : R.string.mobile_settings_off);
                badge.setBackgroundResource(on ? R.drawable.bg_settings_badge_on : R.drawable.bg_settings_badge_off);
                badge.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        on ? R.color.mobile_color_on_accent_container : R.color.mobile_color_on_surface_secondary));
                badge.setVisibility(View.VISIBLE);
            } else {
                badge.setVisibility(View.GONE);
            }
            boolean goesSomewhere = row.status == null && (row.page != null || row.kind == SettingsRow.KIND_CHOICE);
            chevron.setVisibility(goesSomewhere ? View.VISIBLE : View.GONE);

            boolean isSwitch = row.kind == SettingsRow.KIND_SWITCH;
            toggle.setVisibility(isSwitch ? View.VISIBLE : View.GONE);
            if (isSwitch && row.checked != null) {
                boolean on = row.checked.getAsBoolean();
                if (toggle.isChecked() != on) {
                    toggle.setChecked(on);
                }
                itemView.setContentDescription(row.title + (text != null ? ", " + text : ""));
            } else if (row.status != null) {
                itemView.setContentDescription(row.title + (text != null ? ", " + text : "") + ", " + badge.getText());
            } else {
                itemView.setContentDescription(null);
            }
            itemView.setAccessibilityDelegate(isSwitch ? SwitchRole.INSTANCE : null);

            boolean enabled = row.isEnabled();
            itemView.setEnabled(enabled);
            itemView.setAlpha(enabled ? 1f : 0.38f);
            toggle.setEnabled(enabled);
            itemView.setOnClickListener(enabled ? v -> listener.onRowClicked(row) : null);
            itemView.setClickable(enabled);
        }
    }

    /** MONOGRAMA(visual): an inline choice as a segmented control; a tap picks that option. */
    private static final class SegmentedHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView summary;
        private final LinearLayout segments;

        SegmentedHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.settings_row_title);
            summary = itemView.findViewById(R.id.settings_row_summary);
            segments = itemView.findViewById(R.id.settings_segments);
        }

        void bind(SettingsRow row, Listener listener) {
            title.setText(row.title);
            title.setVisibility(row.hideTitle ? View.GONE : View.VISIBLE);
            // A choice's summary defaults to its current option, which the segments already show.
            CharSequence text = row.summary != null ? row.summary.get() : null;
            summary.setText(text);
            summary.setVisibility(!row.hideTitle && text != null && text.length() > 0 ? View.VISIBLE : View.GONE);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) segments.getLayoutParams();
            int top = Math.round((row.hideTitle ? 12 : 10) * itemView.getResources().getDisplayMetrics().density);
            if (lp.topMargin != top) {
                lp.topMargin = top;
                segments.setLayoutParams(lp);
            }

            int count = row.options != null ? row.options.size() : 0;
            while (segments.getChildCount() > count) {
                segments.removeViewAt(segments.getChildCount() - 1);
            }
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            while (segments.getChildCount() < count) {
                inflater.inflate(R.layout.item_mobile_settings_segment, segments, true);
            }

            int current = row.currentIndex();
            boolean enabled = row.isEnabled();
            itemView.setAlpha(enabled ? 1f : 0.38f);
            for (int i = 0; i < count; i++) {
                TextView segment = (TextView) segments.getChildAt(i);
                SettingsRow.Option option = row.options.get(i);
                boolean selected = i == current;
                segment.setText(option.label);
                segment.setSelected(selected);
                segment.setBackgroundResource(selected ? R.drawable.bg_settings_segment_selected : 0);
                segment.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        selected ? R.color.mobile_color_on_accent : R.color.mobile_color_on_surface_secondary));
                segment.setContentDescription(row.title + ", " + option.label);
                int index = i;
                segment.setOnClickListener(enabled ? v -> listener.onOptionPicked(row, index) : null);
                segment.setClickable(enabled);
                segment.setAccessibilityDelegate(SegmentRole.INSTANCE);
            }
        }
    }

    /** MONOGRAMA(visual): the app-icon picker, one card per option. */
    private static final class IconsHolder extends RecyclerView.ViewHolder {
        private final LinearLayout cards;

        IconsHolder(@NonNull View itemView) {
            super(itemView);
            cards = (LinearLayout) itemView;
        }

        void bind(SettingsRow row, Listener listener) {
            int count = row.options != null ? row.options.size() : 0;
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            int gap = Math.round(12 * itemView.getResources().getDisplayMetrics().density);
            while (cards.getChildCount() > count) {
                cards.removeViewAt(cards.getChildCount() - 1);
            }
            while (cards.getChildCount() < count) {
                View card = inflater.inflate(R.layout.item_mobile_settings_icon_card, cards, false);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) card.getLayoutParams();
                lp.setMarginStart(cards.getChildCount() > 0 ? gap : 0);
                cards.addView(card, lp);
            }
            int current = row.currentIndex();
            for (int i = 0; i < count; i++) {
                View card = cards.getChildAt(i);
                SettingsRow.Option option = row.options.get(i);
                boolean selected = i == current;
                ((ImageView) card.findViewById(R.id.settings_icon_image)).setImageResource(option.image);
                ((TextView) card.findViewById(R.id.settings_icon_label)).setText(option.label);
                TextView description = card.findViewById(R.id.settings_icon_description);
                description.setText(option.description);
                description.setVisibility(option.description != null ? View.VISIBLE : View.GONE);
                card.findViewById(R.id.settings_icon_check).setVisibility(selected ? View.VISIBLE : View.GONE);
                // The selected card's outline; the ripple card otherwise.
                card.setBackgroundResource(selected ? R.drawable.bg_monograma_icon_card_selected
                        : R.drawable.bg_monograma_card);
                card.setSelected(selected);
                card.setContentDescription(option.label + (option.description != null ? ", " + option.description : ""));
                int index = i;
                card.setOnClickListener(v -> listener.onOptionPicked(row, index));
                card.setAccessibilityDelegate(SegmentRole.INSTANCE);
            }
        }
    }

    /** TalkBack reads a segment or an icon card as a radio button, selected or not. */
    private static final class SegmentRole extends View.AccessibilityDelegate {
        static final SegmentRole INSTANCE = new SegmentRole();

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(android.widget.RadioButton.class.getName());
            info.setCheckable(true);
            info.setChecked(host.isSelected());
        }
    }

    /** TalkBack reads a switch row as a switch with its state, like a platform preference. */
    private static final class SwitchRole extends View.AccessibilityDelegate {
        static final SwitchRole INSTANCE = new SwitchRole();

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            SwitchMaterial toggle = host.findViewById(R.id.settings_row_switch);
            info.setClassName(android.widget.Switch.class.getName());
            info.setCheckable(true);
            info.setChecked(toggle != null && toggle.isChecked());
        }
    }
}
