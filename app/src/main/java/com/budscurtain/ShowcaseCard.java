package com.budscurtain;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class ShowcaseCard extends OverlayCard {
    private static final String TITLE = "Кейс открыт._";
    private static final String STATUS_PREFIX = "Xiaomi Buds 5 · ";
    private static final int SEGMENTS = 9;

    private final GradientDrawable panel = new GradientDrawable();
    private final GradientDrawable grip = new GradientDrawable();
    private final TextView title;
    private final TextView status;
    private final TextView example;
    private final ImageView art;
    private final View[] separators = new View[2];
    private final Metric left;
    private final Metric cradle;
    private final Metric right;
    private int ink;
    private int muted;
    private int online;
    private int artwork;
    private boolean charging;

    ShowcaseCard(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(dp(18), dp(10), dp(18), dp(20));
        panel.setCornerRadius(dp(22));
        setBackground(panel);

        View handle = new View(context);
        grip.setCornerRadius(dp(2));
        handle.setBackground(grip);
        LayoutParams handleParams = new LayoutParams(dp(34), dp(3));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(15);
        addView(handle, handleParams);

        title = text("", 30);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, 900, false));
        title.setLetterSpacing(-0.035f);
        title.setContentDescription("Кейс открыт");
        addView(title);
        status = text("", 12);
        addView(status, spaced(MATCH_PARENT, WRAP_CONTENT, 6));

        art = new ImageView(context);
        art.setScaleType(ImageView.ScaleType.FIT_CENTER);
        art.setContentDescription("Чёрные Xiaomi Buds 5: левый наушник, кейс, правый наушник");
        addView(art, spaced(MATCH_PARENT, artHeight(getResources().getConfiguration()), 12));

        LinearLayout levels = new LinearLayout(context);
        levels.setBaselineAligned(false);
        addView(levels, spaced(MATCH_PARENT, WRAP_CONTENT, 8));
        left = addMetric(levels, "Левый");
        addSeparator(levels, 0);
        cradle = addMetric(levels, "Кейс");
        addSeparator(levels, 1);
        right = addMetric(levels, "Правый");

        example = text(PREVIEW_MESSAGE, 11);
        example.setGravity(Gravity.CENTER);
        example.setVisibility(GONE);
        addView(example, spaced(MATCH_PARENT, WRAP_CONTENT, 10));
        applyTheme(AppTheme.isDark(context));
    }

    @Override protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        LayoutParams params = (LayoutParams) art.getLayoutParams();
        int height = artHeight(configuration);
        if (params.height != height) {
            params.height = height;
            art.setLayoutParams(params);
        }
    }

    private int artHeight(Configuration configuration) {
        if (configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) return dp(151);
        // Leave room for the heading, charge values and preview label on short screens.
        return dp(Math.max(64, Math.min(151, configuration.screenHeightDp - 255)));
    }

    @Override void applyTheme(boolean dark) {
        ink = dark ? 0xffe8e4d9 : 0xff191918;
        muted = dark ? 0xffaaa79f : 0xff68645d;
        online = dark ? 0xff8cdbad : 0xff296744;
        int accent = dark ? 0xfff0443e : 0xffcf332d;
        int line = dark ? 0xff353535 : 0xffd5d0c7;
        panel.setColor(dark ? 0xff181818 : 0xffeeebe4);
        panel.setStroke(dp(1), line);
        grip.setColor(muted);
        title.setTextColor(ink);
        SpannableString heading = new SpannableString(TITLE);
        heading.setSpan(new ForegroundColorSpan(accent), TITLE.length() - 1, TITLE.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(heading);
        updateStatus();
        example.setTextColor(muted);
        int image = dark ? R.drawable.buds5_showcase_dark : R.drawable.buds5_showcase_light;
        if (artwork != image) {
            art.setImageResource(image);
            artwork = image;
        }
        for (View separator : separators) separator.setBackgroundColor(line);
        left.applyTheme(accent, line);
        cradle.applyTheme(accent, line);
        right.applyTheme(accent, line);
    }

    @Override void setLevels(Integer l, Integer r, Integer c, boolean lc, boolean rc, boolean cc, String message) {
        left.setLevel(l, lc);
        cradle.setLevel(c, cc);
        right.setLevel(r, rc);
        boolean nextCharging = lc || rc || cc;
        if (charging != nextCharging) {
            charging = nextCharging;
            updateStatus();
        }
        int visibility = PREVIEW_MESSAGE.equals(message) ? VISIBLE : GONE;
        if (example.getVisibility() != visibility) example.setVisibility(visibility);
    }

    private void updateStatus() {
        SpannableString text = new SpannableString(STATUS_PREFIX + (charging ? "● Заряжаются" : "● Рядом"));
        text.setSpan(new ForegroundColorSpan(muted), 0, STATUS_PREFIX.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (charging) {
            text.setSpan(new ForegroundColorSpan(online), STATUS_PREFIX.length(), STATUS_PREFIX.length() + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        status.setTextColor(ink);
        status.setText(text);
    }

    private Metric addMetric(LinearLayout levels, String label) {
        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        levels.addView(cell, new LayoutParams(0, WRAP_CONTENT, 1));
        return new Metric(cell, label);
    }

    private void addSeparator(LinearLayout levels, int index) {
        View separator = new View(getContext());
        levels.addView(separator, spaced(dp(1), dp(37), 6));
        separators[index] = separator;
    }

    private LayoutParams spaced(int width, int height, int top) {
        LayoutParams params = new LayoutParams(width, height);
        params.topMargin = dp(top);
        return params;
    }

    private TextView text(String content, int sp) {
        TextView view = new TextView(getContext());
        view.setIncludeFontPadding(false);
        view.setText(content);
        view.setTextSize(sp);
        view.setTypeface(Typeface.MONOSPACE);
        return view;
    }

    private final class Metric {
        private final TextView value;
        private final TextView caption;
        private final Meter meter;

        Metric(LinearLayout cell, String label) {
            value = text("—", 22);
            cell.addView(value, new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
            caption = text(label, 12);
            cell.addView(caption, spaced(WRAP_CONTENT, WRAP_CONTENT, 4));
            meter = new Meter(getContext());
            cell.addView(meter, spaced(MATCH_PARENT, dp(8), 10));
        }

        void applyTheme(int accent, int line) {
            value.setTextColor(ink);
            caption.setTextColor(muted);
            meter.applyTheme(accent, line);
        }

        void setLevel(Integer level, boolean isCharging) {
            String next = level == null ? "—" : level + "%";
            if (!TextUtils.equals(value.getText(), next)) value.setText(next);
            String description = level == null ? "Заряд неизвестен" : next;
            if (isCharging) description += ", заряжается";
            if (!TextUtils.equals(value.getContentDescription(), description)) value.setContentDescription(description);
            meter.setLevel(level);
        }
    }

    private final class Meter extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int accent;
        private int track;
        private int filled;

        Meter(Context context) { super(context); }

        void applyTheme(int active, int inactive) {
            accent = active;
            track = inactive;
            invalidate();
        }

        void setLevel(Integer percent) {
            int next = percent == null ? 0 : Math.round(Math.max(0, Math.min(100, percent)) * SEGMENTS / 100f);
            if (filled != next) {
                filled = next;
                invalidate();
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = Math.min(getWidth() * 0.82f, dp(68));
            float gap = Math.min(dp(3), width / (SEGMENTS * 2f));
            float bar = (width - gap * (SEGMENTS - 1)) / SEGMENTS;
            float radius = Math.min(dp(2), bar / 2f);
            float start = (getWidth() - width) / 2f;
            for (int i = 0; i < SEGMENTS; i++) {
                paint.setColor(i < filled ? accent : track);
                float x = start + i * (bar + gap);
                canvas.drawRoundRect(x, 0, x + bar, getHeight(), radius, radius, paint);
            }
        }
    }
}
