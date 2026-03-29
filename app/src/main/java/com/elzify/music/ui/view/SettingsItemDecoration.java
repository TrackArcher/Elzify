package com.elzify.music.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elzify.music.R;
import com.elzify.music.util.UIUtil;

public class SettingsItemDecoration extends RecyclerView.ItemDecoration {

    private final float cornerRadius;
    private final int margin;
    private final int contentInset;
    private final Paint paint;

    public SettingsItemDecoration(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        cornerRadius = 24 * density;
        margin = (int) (16 * density);
        contentInset = (int) (8 * density);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(UIUtil.getThemeColor(context, com.google.android.material.R.attr.colorSurfaceContainerLow));
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDraw(c, parent, state);

        int childCount = parent.getChildCount();
        if (childCount == 0) return;

        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(child);
            if (position == RecyclerView.NO_POSITION) continue;

            if (isCategoryItem(child)) continue;

            boolean isFirst = isFirstInCategory(parent, position);
            boolean isLast = isLastInCategory(parent, position, state.getItemCount());

            float left = margin;
            float right = parent.getWidth() - margin;
            float top = child.getTop();
            float bottom = child.getBottom();

            // Add slight vertical margin between items if they are not part of the same card
            // But here we want items in same card to be tight.
            
            Path path = new Path();
            float[] radii = new float[8];

            if (isFirst && isLast) {
                for (int j = 0; j < 8; j++) radii[j] = cornerRadius;
            } else if (isFirst) {
                radii[0] = cornerRadius; radii[1] = cornerRadius;
                radii[2] = cornerRadius; radii[3] = cornerRadius;
            } else if (isLast) {
                radii[4] = cornerRadius; radii[5] = cornerRadius;
                radii[6] = cornerRadius; radii[7] = cornerRadius;
            }

            path.addRoundRect(new RectF(left, top, right, bottom), radii, Path.Direction.CW);
            c.drawPath(path, paint);
        }
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (!isCategoryItem(view)) {
            outRect.left = contentInset;
            outRect.right = contentInset;
        }
    }

    private boolean isCategoryItem(View view) {
        // Our custom category layout has a title with android.R.id.title
        // and no icon. Regular preferences have icons now.
        return view.findViewById(android.R.id.title) != null && view.findViewById(android.R.id.icon) == null;
    }

    private boolean isFirstInCategory(RecyclerView parent, int position) {
        if (position == 0) return true;
        View prevView = parent.getLayoutManager().findViewByPosition(position - 1);
        if (prevView != null) {
            return isCategoryItem(prevView);
        }
        return false;
    }

    private boolean isLastInCategory(RecyclerView parent, int position, int itemCount) {
        if (position == itemCount - 1) return true;
        View nextView = parent.getLayoutManager().findViewByPosition(position + 1);
        if (nextView != null) {
            return isCategoryItem(nextView);
        }
        return false;
    }
}
