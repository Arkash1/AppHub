package com.arkashstudio.apphub.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * TextView с неоновым градиентным текстом (фиолет → бирюза).
 * Используется для логотипа «AppHub» и крупных заголовков.
 */
public class GradientTextView extends AppCompatTextView {

    public GradientTextView(Context context) {
        super(context);
    }

    public GradientTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GradientTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            LinearGradient gradient = new LinearGradient(
                    0, 0, w, getTextSize(),
                    new int[]{
                            0xFF7C4DFF,  // фиолет
                            0xFF00E5FF   // бирюза
                    },
                    null,
                    Shader.TileMode.CLAMP);
            getPaint().setShader(gradient);
        }
    }
}
