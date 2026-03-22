package com.android.launcher2;
import com.launcher3h.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;

final class Utilities {
    private static final Paint sBlurPaint = new Paint();
    private static final Canvas sCanvas = new Canvas();
    static int sColorIndex = 0;
    static int[] sColors = {-65536, -16711936, -16776961};
    private static final Paint sDisabledPaint = new Paint();
    private static final Paint sGlowColorFocusedPaint = new Paint();
    private static final Paint sGlowColorPressedPaint = new Paint();
    private static int sIconContentSize = -1;
    private static int sIconHeight = -1;
    private static int sIconTextureHeight = -1;
    private static int sIconTextureWidth = -1;
    private static int sIconWidth = -1;
    private static final Rect sOldBounds = new Rect();

    Utilities() {
    }

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
    }

    static int getIconContentSize() {
        return sIconContentSize;
    }

    static Bitmap createIconBitmap(Drawable icon, Context context) {
        int width;
        int width2;
        Bitmap bitmap;
        int height;
        int sourceHeight;
        synchronized (sCanvas) {
            if (sIconWidth == -1) {
                initStatics(context);
            }
            int width3 = sIconWidth;
            int height2 = sIconHeight;
            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width3);
                painter.setIntrinsicHeight(height2);
            } else if (icon instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                if (bitmapDrawable.getBitmap().getDensity() == 0) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight2 = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceWidth > 0) {
                if (width3 < sourceWidth || height2 < sourceHeight2) {
                    float ratio = ((float) sourceWidth) / ((float) sourceHeight2);
                    if (sourceWidth > sourceHeight2) {
                        height = (int) (((float) width3) / ratio);
                        sourceHeight = width3;
                    } else if (sourceHeight2 > sourceWidth) {
                        sourceHeight = (int) (ratio * ((float) height2));
                        height = height2;
                    } else {
                        height = height2;
                        sourceHeight = width3;
                    }
                    width = height;
                    width2 = sourceHeight;
                    int textureWidth = sIconTextureWidth;
                    int textureHeight = sIconTextureHeight;
                    bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
                    Canvas canvas = sCanvas;
                    canvas.setBitmap(bitmap);
                    int textureWidth2 = (textureWidth - width2) / 2;
                    int textureHeight2 = (textureHeight - width) / 2;
                    sOldBounds.set(icon.getBounds());
                    icon.setBounds(textureWidth2, textureHeight2, width2 + textureWidth2, width + textureHeight2);
                    icon.draw(canvas);
                    icon.setBounds(sOldBounds);
                } else if (sourceWidth < width3 && sourceHeight2 < height2) {
                    width2 = sourceWidth;
                    width = sourceHeight2;
                    int textureWidth3 = sIconTextureWidth;
                    int textureHeight3 = sIconTextureHeight;
                    bitmap = Bitmap.createBitmap(textureWidth3, textureHeight3, Bitmap.Config.ARGB_8888);
                    Canvas canvas2 = sCanvas;
                    canvas2.setBitmap(bitmap);
                    int textureWidth22 = (textureWidth3 - width2) / 2;
                    int textureHeight22 = (textureHeight3 - width) / 2;
                    sOldBounds.set(icon.getBounds());
                    icon.setBounds(textureWidth22, textureHeight22, width2 + textureWidth22, width + textureHeight22);
                    icon.draw(canvas2);
                    icon.setBounds(sOldBounds);
                }
            }
            width = height2;
            width2 = width3;
            int textureWidth32 = sIconTextureWidth;
            int textureHeight32 = sIconTextureHeight;
            bitmap = Bitmap.createBitmap(textureWidth32, textureHeight32, Bitmap.Config.ARGB_8888);
            Canvas canvas22 = sCanvas;
            canvas22.setBitmap(bitmap);
            int textureWidth222 = (textureWidth32 - width2) / 2;
            int textureHeight222 = (textureHeight32 - width) / 2;
            sOldBounds.set(icon.getBounds());
            icon.setBounds(textureWidth222, textureHeight222, width2 + textureWidth222, width + textureHeight222);
            icon.draw(canvas22);
            icon.setBounds(sOldBounds);
        }
        return bitmap;
    }

    static void drawSelectedAllAppsBitmap(Canvas dest, int destWidth, int destHeight, boolean pressed, Bitmap src) {
        synchronized (sCanvas) {
            if (sIconWidth == -1) {
                throw new RuntimeException("Assertion failed: Utilities not initialized");
            }
            dest.drawColor(0, PorterDuff.Mode.CLEAR);
            int[] xy = new int[2];
            Bitmap mask = src.extractAlpha(sBlurPaint, xy);
            dest.drawBitmap(mask, ((float) xy[0]) + ((float) ((destWidth - src.getWidth()) / 2)), ((float) xy[1]) + ((float) ((destHeight - src.getHeight()) / 2)), pressed ? sGlowColorPressedPaint : sGlowColorFocusedPaint);
            mask.recycle();
        }
    }

    static Bitmap resampleIconBitmap(Bitmap bitmap, Context context) {
        synchronized (sCanvas) {
            if (sIconWidth == -1) {
                initStatics(context);
            }
            if (bitmap.getWidth() == sIconWidth && bitmap.getHeight() == sIconHeight) {
                return bitmap;
            }
            Bitmap createIconBitmap = createIconBitmap(new BitmapDrawable(bitmap), context);
            return createIconBitmap;
        }
    }

    private static void initStatics(Context context) {
        Resources resources = context.getResources();
        float density = resources.getDisplayMetrics().density;
        int dimension = (int) resources.getDimension(R.dimen.app_icon_size);
        sIconHeight = dimension;
        sIconWidth = dimension;
        if (LauncherApplication.isScreenXLarge()) {
            sIconContentSize = (int) resources.getDimension(R.dimen.app_icon_content_size);
        }
        int i = sIconWidth + 2;
        sIconTextureHeight = i;
        sIconTextureWidth = i;
        sBlurPaint.setMaskFilter(new BlurMaskFilter(5.0f * density, BlurMaskFilter.Blur.NORMAL));
        sGlowColorPressedPaint.setColor(-15616);
        // TableMaskFilter is not part of the public SDK; omitting it still works.
        sGlowColorPressedPaint.setMaskFilter(null);
        sGlowColorFocusedPaint.setColor(-29184);
        sGlowColorFocusedPaint.setMaskFilter(null);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.2f);
        sDisabledPaint.setColorFilter(new ColorMatrixColorFilter(cm));
        sDisabledPaint.setAlpha(136);
    }

    static class BubbleText {
        private final int mBitmapHeight;
        private final int mBitmapWidth;
        private final RectF mBubbleRect = new RectF();
        private final int mDensity;
        private final int mFirstLineY;
        private final int mLeading;
        private final int mLineHeight;
        private final TextPaint mTextPaint;
        private final float mTextWidth;

        BubbleText(Context context) {
            Resources resources = context.getResources();
            DisplayMetrics metrics = resources.getDisplayMetrics();
            float scale = metrics.density;
            this.mDensity = metrics.densityDpi;
            float cellWidth = resources.getDimension(R.dimen.title_texture_width);
            RectF bubbleRect = this.mBubbleRect;
            bubbleRect.left = 0.0f;
            bubbleRect.top = 0.0f;
            bubbleRect.right = (float) ((int) cellWidth);
            this.mTextWidth = (cellWidth - (2.0f * scale)) - (2.0f * scale);
            TextPaint textPaint = new TextPaint();
            this.mTextPaint = textPaint;
            textPaint.setTypeface(Typeface.DEFAULT);
            textPaint.setTextSize(13.0f * scale);
            textPaint.setColor(-1);
            textPaint.setAntiAlias(true);
            float ascent = -textPaint.ascent();
            float descent = textPaint.descent();
            this.mLeading = (int) (0.5f + 0.0f);
            this.mFirstLineY = (int) (0.0f + ascent + 0.5f);
            this.mLineHeight = (int) (0.0f + ascent + descent + 0.5f);
            this.mBitmapWidth = (int) (this.mBubbleRect.width() + 0.5f);
            this.mBitmapHeight = Utilities.roundToPow2((int) (((float) (this.mLineHeight * 2)) + 0.0f + 0.5f));
            this.mBubbleRect.offsetTo((((float) this.mBitmapWidth) - this.mBubbleRect.width()) / 2.0f, 0.0f);
        }

        /* access modifiers changed from: package-private */
        public Bitmap createTextBitmap(String text) {
            Bitmap b = Bitmap.createBitmap(this.mBitmapWidth, this.mBitmapHeight, Bitmap.Config.ALPHA_8);
            b.setDensity(this.mDensity);
            Canvas c = new Canvas(b);
            StaticLayout layout = new StaticLayout(text, this.mTextPaint, (int) this.mTextWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, true);
            int lineCount = layout.getLineCount();
            if (lineCount > 2) {
                lineCount = 2;
            }
            for (int i = 0; i < lineCount; i++) {
                String lineText = text.substring(layout.getLineStart(i), layout.getLineEnd(i));
                c.drawText(lineText, (float) ((int) (this.mBubbleRect.left + ((this.mBubbleRect.width() - this.mTextPaint.measureText(lineText)) * 0.5f))), (float) (this.mFirstLineY + (this.mLineHeight * i)), this.mTextPaint);
            }
            return b;
        }
    }

    static int roundToPow2(int n) {
        int orig = n;
        int n2 = n >> 1;
        int mask = 134217728;
        while (mask != 0 && (n2 & mask) == 0) {
            mask >>= 1;
        }
        while (mask != 0) {
            n2 |= mask;
            mask >>= 1;
        }
        int n3 = n2 + 1;
        if (n3 != orig) {
            return n3 << 1;
        }
        return n3;
    }
}
