package com.budscurtain;

import android.content.Context;
import android.widget.LinearLayout;

abstract class OverlayCard extends LinearLayout {
    static final String PREVIEW_MESSAGE = "Пример отображения";

    OverlayCard(Context context) { super(context); }

    abstract void applyTheme(boolean dark);

    abstract void setLevels(Integer left, Integer right, Integer cradle,
                            boolean leftCharging, boolean rightCharging, boolean cradleCharging,
                            String message);

    final int dp(float size) {
        return Math.round(size * getResources().getDisplayMetrics().density);
    }
}
