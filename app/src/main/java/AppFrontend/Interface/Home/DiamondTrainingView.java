package AppFrontend.Interface.Home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class DiamondTrainingView extends View {
    private Paint paint;
    private Path path;

    private final float density = getResources().getDisplayMetrics().density;

    // These are now DP-based
    private final float MIN_STROKE = 5f * density;
    private final float MAX_STROKE = 69f * density;
    private float strokeWidth = MIN_STROKE;

    public DiamondTrainingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#181818"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        path = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;

        float scaleX = w / 100f;
        float scaleY = h / 212f;

        path.reset();
        path.moveTo(27.1319f * scaleX, 16.0127f * scaleY);
        path.lineTo(3.00002f * scaleX, 57.5869f * scaleY);
        path.lineTo(3.00002f * scaleX, 127.467f * scaleY);
        path.lineTo(49.9834f * scaleX, 206.149f * scaleY);
        path.lineTo(97f * scaleX, 127.467f * scaleY);
        path.lineTo(97f * scaleX, 57.5869f * scaleY);
        path.lineTo(72.9072f * scaleX, 16.0781f * scaleY);
        path.lineTo(49.9815f * scaleX, 3.42578f * scaleY);
        path.close();

        canvas.save();
        // This 'clips' the drawing area to the inside of the diamond
        canvas.clipPath(path);

        // We double the stroke width because half of it is hidden by the clip
        paint.setStrokeWidth(strokeWidth * 2);
        canvas.drawPath(path, paint);

        canvas.restore();
    }

    public void setProgress(int percentage) {
        float progress = Math.min(100, Math.max(0, percentage)) / 100f;
        this.strokeWidth = MIN_STROKE + ((MAX_STROKE - MIN_STROKE) * progress);
        invalidate();
    }
}