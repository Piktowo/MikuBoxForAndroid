package com.neko.blur;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import io.nekohasekai.sagernet.R;

public class BlurImageViewGlide extends AppCompatImageView {

    private float defaultBitmapScale = 0.5f;
    private static final int MAX_RADIUS = 25;
    private int mBlurRadius = 0;
    private boolean isBlurring = false;

    public BlurImageViewGlide(Context context) {
        super(context);
        init(null);
    }

    public BlurImageViewGlide(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public BlurImageViewGlide(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.BlurImageView, 0, 0);
            mBlurRadius = typedArray.getInteger(R.styleable.BlurImageView_blur_radius, 0);
            typedArray.recycle();
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (isBlurring) {
            super.setImageDrawable(drawable);
            return;
        }

        if (mBlurRadius == 0 || drawable == null) {
            super.setImageDrawable(drawable);
            return;
        }

        try {
            Bitmap originalBitmap = getBitmapFromDrawable(drawable);
            if (originalBitmap != null) {
                Bitmap blurred = blurRenderScript(originalBitmap, mBlurRadius);
                isBlurring = true;
                super.setImageBitmap(blurred);
                isBlurring = false;
            } else {
                super.setImageDrawable(drawable);
            }
        } catch (Exception e) {
            Log.e("BlurImageViewGlide", "Failed to blur", e);
            super.setImageDrawable(drawable);
        }
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        try {
            Bitmap bitmap;
            if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            }
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    public void setBlur(int radius) {
        this.mBlurRadius = radius;
        if (getDrawable() != null) {
            setImageDrawable(getDrawable()); 
        }
    }

    private Bitmap blurRenderScript(Bitmap smallBitmap, int radius) {
        if (radius > MAX_RADIUS) radius = MAX_RADIUS;
        if (radius < 1) radius = 1;

        try {
            int width = Math.round(smallBitmap.getWidth() * defaultBitmapScale);
            int height = Math.round(smallBitmap.getHeight() * defaultBitmapScale);

            if (width <= 0) width = 1;
            if (height <= 0) height = 1;

            Bitmap inputBitmap = Bitmap.createScaledBitmap(smallBitmap, width, height, false);
            Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

            RenderScript renderScript = RenderScript.create(getContext());
            ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            Allocation tmpIn = Allocation.createFromBitmap(renderScript, inputBitmap);
            Allocation tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap);
            
            theIntrinsic.setRadius(radius);
            theIntrinsic.setInput(tmpIn);
            theIntrinsic.forEach(tmpOut);
            
            tmpOut.copyTo(outputBitmap);
            
            renderScript.destroy(); 
            
            return outputBitmap;
        } catch (Exception e) {
            Log.e("BlurImageViewGlide", "RenderScript Error", e);
            return smallBitmap;
        }
    }
}
