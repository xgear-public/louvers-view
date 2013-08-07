package by.xgear.louversview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class LouversView extends FrameLayout {

	private static final int DEF_LCOUNT = 0;
	private static final int DEF_ANGLE = 0; 

	private int mAngle = 0;
	private int mLCount = 0;
    private GestureDetectorCompat mGestureDetector;

	public LouversView(Context context) {
        this(context, null, 0);
	}

	public LouversView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
	}
	
	public LouversView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.LouversView);
		mLCount = ta.getInteger(R.styleable.LouversView_lamellaCount, DEF_LCOUNT);
		mAngle = ta.getInteger(R.styleable.LouversView_angle, DEF_ANGLE);
	    ta.recycle();
		
        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);
        if(getChildCount() > 2)
        	throw new RuntimeException();
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
        if(getChildCount() > 2)
        	throw new RuntimeException();
		mGestureDetector.onTouchEvent(event);
		return true;
	}
	
	private final GestureDetector.SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener(){

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			Log.d("Pelkin", "onFling ACTION_MOVE velocityX = "+velocityX+"\tvelocityY = "+velocityY);
			return super.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			Log.d("Pelkin", "onScroll ACTION_MOVE distanceX = "+distanceX+"\tdistanceY = "+distanceY);
			return super.onScroll(e1, e2, distanceX, distanceY);
		}
		
	};

}
