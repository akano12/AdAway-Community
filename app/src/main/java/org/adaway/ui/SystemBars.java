package org.adaway.ui;

import android.graphics.Color;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * This class draws the screens behind the system bars, as Android 15 and later require, and keeps
 * their content out of the way of those bars.
 * <p>
 * The status bar shows the red app bar or header of every screen, so it uses light icons. The
 * navigation bar follows the theme.
 */
public final class SystemBars {
    private SystemBars() {

    }

    /**
     * Draw the activity behind the system bars. Call it before setting the content view.
     *
     * @param activity The activity to draw behind the system bars.
     */
    public static void enableEdgeToEdge(ComponentActivity activity) {
        EdgeToEdge.enable(activity, SystemBarStyle.dark(Color.TRANSPARENT));
    }

    /**
     * Keep the content of a screen with an action bar out of the way of the system bars and of the
     * display cutouts. The action bar extends behind the status bar, but the content is only placed
     * below its own height: it also needs the status bar height.
     *
     * @param activity The activity whose content to fit.
     */
    public static void fitContentBelowActionBar(ComponentActivity activity) {
        padForInsets(activity.findViewById(android.R.id.content), true, true, true);
    }

    /**
     * Pad a view by the system bars and display cutout insets, on top of its own padding, and consume
     * them for its children.
     *
     * @param view   The view to pad.
     * @param sides  Whether to pad its sides by the system bars and display cutouts.
     * @param top    Whether to pad its top by the status bar.
     * @param bottom Whether to pad its bottom by the navigation bar.
     */
    public static void padForInsets(View view, boolean sides, boolean top, boolean bottom) {
        int paddingLeft = view.getPaddingLeft();
        int paddingTop = view.getPaddingTop();
        int paddingRight = view.getPaddingRight();
        int paddingBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            int left = sides ? insets.left : 0;
            int right = sides ? insets.right : 0;
            int topInset = top ? insets.top : 0;
            int bottomInset = bottom ? insets.bottom : 0;
            v.setPadding(paddingLeft + left, paddingTop + topInset, paddingRight + right, paddingBottom + bottomInset);
            // Consume what is applied here, so the children do not apply it again (as the bottom
            // navigation bar and the floating action buttons do by default)
            return windowInsets.inset(left, topInset, right, bottomInset);
        });
    }
}
