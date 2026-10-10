package com.budscurtain;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

/** Shows the exact icon returned by PackageManager for device verification. */
public final class DebugIconActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ImageView image = new ImageView(this);
        image.setBackgroundColor(0xffedf3ef);
        image.setScaleType(ImageView.ScaleType.CENTER);
        try {
            image.setImageDrawable(getPackageManager().getApplicationIcon(getPackageName()));
        } catch (Exception ignored) {}
        setContentView(image);
    }
}
