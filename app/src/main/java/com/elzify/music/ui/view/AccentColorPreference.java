package com.elzify.music.ui.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.elzify.music.R;
import com.elzify.music.util.Preferences;

public class AccentColorPreference extends Preference {

    private static final int[] COLORS = {
            Color.parseColor("#6750A4"), // Default Purple
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#9C27B0")  // Purple
    };

    public AccentColorPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_accent_color);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        LinearLayout container = (LinearLayout) holder.findViewById(R.id.color_options_container);
        container.removeAllViews();

        int selectedColor = Preferences.getAccentColor();

        for (int color : COLORS) {
            View colorView = createColorView(color, color == selectedColor);
            container.addView(colorView);
        }
    }

    private View createColorView(int color, boolean isSelected) {
        int size = (int) (40 * getContext().getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getContext().getResources().getDisplayMetrics().density);

        FrameLayout frame = new FrameLayout(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(0, 0, margin, 0);
        frame.setLayoutParams(params);

        View circle = new View(getContext());
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        circle.setBackground(shape);
        frame.addView(circle);

        if (isSelected) {
            ImageView check = new ImageView(getContext());
            check.setImageResource(R.drawable.ic_check_circle);
            check.setColorFilter(Color.WHITE);
            FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(size / 2, size / 2);
            checkParams.gravity = android.view.Gravity.CENTER;
            check.setLayoutParams(checkParams);
            frame.addView(check);
        }

        frame.setOnClickListener(v -> {
            Preferences.setAccentColor(color);
            notifyChanged();
            if (getContext() instanceof android.app.Activity) {
                ((android.app.Activity) getContext()).recreate();
            }
        });

        return frame;
    }
}
