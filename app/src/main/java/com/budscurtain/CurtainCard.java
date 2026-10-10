package com.budscurtain;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

final class CurtainCard extends LinearLayout {
    private final Metric left;
    private final Metric cradle;
    private final Metric right;
    private final TextView status;
    private final TextView detail;
    private final TextView title;
    private final TextView foot;
    private final View divider;
    private final List<View> separators = new ArrayList<>();
    private final GradientDrawable background;
    private final GradientDrawable handleBackground;

    CurtainCard(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(dp(20), dp(24), dp(20), dp(20));
        background = new GradientDrawable();
        background.setCornerRadius(dp(28));
        setBackground(background);
        setElevation(dp(18));

        View handle = new View(context);
        handleBackground = new GradientDrawable();
        handleBackground.setCornerRadius(dp(3));
        handle.setBackground(handleBackground);
        LayoutParams handleParams = new LayoutParams(dp(34), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(17);
        addView(handle, handleParams);

        LinearLayout heading = new LinearLayout(context);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        title = text("Xiaomi Buds 5", 17, 0xff17232d, true);
        heading.addView(title, new LayoutParams(0, -2, 1));
        status = text("● Рядом", 11, 0xff45977a, false);
        status.setGravity(Gravity.END);
        heading.addView(status);
        addView(heading);

        detail = text("Кейс открыт · рядом", 12, 0xff71808a, false);
        LayoutParams detailParams = new LayoutParams(-1, -2);
        detailParams.topMargin = dp(5);
        addView(detail, detailParams);

        ImageView art = new ImageView(context);
        art.setImageResource(R.drawable.buds5_graphite);
        art.setScaleType(ImageView.ScaleType.FIT_CENTER);
        art.setContentDescription("Чёрные Xiaomi Buds 5: левый наушник, кейс, правый наушник");
        LayoutParams artParams = new LayoutParams(-1, dp(130));
        artParams.topMargin = dp(5);
        artParams.bottomMargin = dp(5);
        addView(art, artParams);

        divider = new View(context);
        addView(divider, new LayoutParams(-1, dp(1)));

        LinearLayout metrics = new LinearLayout(context);
        LayoutParams metricsParams = new LayoutParams(-1, -2);
        metricsParams.topMargin = dp(14);
        addView(metrics, metricsParams);
        left = addMetric(metrics, "Левый", false);
        cradle = addMetric(metrics, "Кейс", true);
        right = addMetric(metrics, "Правый", false);

        foot = text("Смахни вниз, чтобы скрыть", 11, 0xff71808a, false);
        foot.setGravity(Gravity.CENTER);
        LayoutParams footParams = new LayoutParams(-1, -2);
        footParams.topMargin = dp(16);
        addView(foot, footParams);
        applyTheme(AppTheme.isDark(context));
    }

    void applyTheme(boolean dark) {
        int ink = dark ? 0xfff4f5f2 : 0xff17232d;
        int muted = dark ? 0xffacb8bc : 0xff71808a;
        int accent = dark ? 0xff8bd5ad : 0xff45977a;
        int line = dark ? 0xff444b4f : 0xffe2e3e0;
        background.setColor(dark ? 0xff252a2e : 0xfff8f7f4);
        handleBackground.setColor(dark ? 0xff687176 : 0xffc9cac7);
        title.setTextColor(ink);
        status.setTextColor(accent);
        detail.setTextColor(muted);
        foot.setTextColor(muted);
        divider.setBackgroundColor(line);
        for (View separator : separators) separator.setBackgroundColor(line);
        left.applyTheme(ink, muted, accent);
        cradle.applyTheme(ink, muted, accent);
        right.applyTheme(ink, muted, accent);
    }

    void setLevels(Integer l, Integer r, Integer c, boolean lc, boolean rc, boolean cc, String message) {
        left.set(l, lc);
        right.set(r, rc);
        cradle.set(c, cc);
        String nextStatus = lc || rc || cc ? "● Заряжаются" : "● Рядом";
        if (!nextStatus.contentEquals(status.getText())) status.setText(nextStatus);
        if (!message.contentEquals(detail.getText())) detail.setText(message);
    }

    private Metric addMetric(LinearLayout row, String label, boolean separator) {
        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(VERTICAL);
        cell.setGravity(Gravity.CENTER);
        LinearLayout valueRow = new LinearLayout(getContext());
        valueRow.setGravity(Gravity.CENTER);
        TextView value = text("—", 23, 0xff17232d, true);
        valueRow.addView(value);
        Bolt charging = new Bolt(getContext());
        LayoutParams chargingParams = new LayoutParams(dp(11), dp(16));
        chargingParams.leftMargin = dp(2);
        valueRow.addView(charging, chargingParams);
        charging.setVisibility(GONE);
        cell.addView(valueRow);
        TextView caption = text(label, 11, 0xff71808a, false);
        LayoutParams captionParams = new LayoutParams(-2, -2);
        captionParams.topMargin = dp(3);
        cell.addView(caption, captionParams);

        if (separator) addSeparator(row);
        row.addView(cell, new LayoutParams(0, dp(58), 1));
        if (separator) addSeparator(row);
        return new Metric(value, caption, charging);
    }

    private void addSeparator(LinearLayout row) {
        View line = new View(getContext());
        line.setBackgroundColor(0xffe2e3e0);
        separators.add(line);
        LayoutParams params = new LayoutParams(dp(1), dp(50));
        params.gravity = Gravity.CENTER_VERTICAL;
        row.addView(line, params);
    }

    private TextView text(String content, int sp, int color, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(content);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(null, 1);
        view.setIncludeFontPadding(false);
        return view;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private static final class Metric {
        final TextView value;
        final TextView caption;
        final Bolt charging;

        Metric(TextView value, TextView caption, Bolt charging) {
            this.value = value;
            this.caption = caption;
            this.charging = charging;
        }

        void applyTheme(int ink, int muted, int accent) {
            value.setTextColor(ink);
            caption.setTextColor(muted);
            charging.setColor(accent);
        }

        void set(Integer level, boolean isCharging) {
            String next = level == null ? "—" : level + "%";
            if (!next.contentEquals(value.getText())) value.setText(next);
            int nextVisibility = isCharging ? View.VISIBLE : View.GONE;
            if (charging.getVisibility() != nextVisibility) charging.setVisibility(nextVisibility);
        }
    }

    private static final class Bolt extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path shape = new Path();

        Bolt(Context context) {
            super(context);
            paint.setColor(0xff45977a);
            paint.setStyle(Paint.Style.FILL);
            shape.moveTo(6.5f, 0);
            shape.lineTo(1.3f, 8.9f);
            shape.lineTo(5.0f, 8.9f);
            shape.lineTo(3.6f, 16.0f);
            shape.lineTo(10.2f, 6.0f);
            shape.lineTo(6.6f, 6.0f);
            shape.lineTo(9.2f, 0);
            shape.close();
            setContentDescription("Заряжается");
        }

        void setColor(int color) { paint.setColor(color); invalidate(); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save();
            canvas.scale(getWidth() / 11f, getHeight() / 16f);
            canvas.drawPath(shape, paint);
            canvas.restore();
        }
    }
}
