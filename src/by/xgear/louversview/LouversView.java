package by.xgear.louversview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class LouversView extends FrameLayout {
    public static final String TAG = LouversView.class.getSimpleName();

    private GestureDetectorCompat mGestureDetector;

    private Bitmap[] bArr;

    private Matrix skew;
    private Camera mCamera;

    private View mBackView;
    private View mFrontView;



    //Constants
    private float MAX_ANGLE = 40;
    private static final int DEF_PANEL_COUNT = -1;
    private static final int DEF_ANGLE = 0;



    //State related fields
    public enum Direction{
        AXIS_X,
        AXIS_Y;
    }

    public enum MovementDirection{
        LEFT,
        RIGHT,
        UP,
        DOWN;
    }

    private boolean isLocked;
    private Direction mCurrentDirection = Direction.AXIS_Y;
    private MovementDirection mCurrentMovementDirection = MovementDirection.DOWN;
    private boolean mIsScrolling;



    //Measuring related fields
    private int angle;
    private int mLouversCount;
    private int mOffset = 0;
    private float mPanelWidth;
    private float mPanelCenterX, mPanelCenterY;
    private int mLockEdge;


	public LouversView(Context context) {
        this(context, null, 0);
	}

	public LouversView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
	}
	
	public LouversView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

        if(getChildCount() > 2)
            throw new RuntimeException();
        setWillNotDraw(false);

		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.LouversView);
        mLouversCount = ta.getInteger(R.styleable.LouversView_panelCount, DEF_PANEL_COUNT);

        if(ta.getBoolean(R.styleable.LouversView_vertical, false))
            mCurrentDirection = Direction.AXIS_Y;

        if(ta.getBoolean(R.styleable.LouversView_horizontal, false))
            mCurrentDirection = Direction.AXIS_X;



//		mAngle = ta.getInteger(R.styleable.LouversView_angle, DEF_ANGLE);
	    ta.recycle();
		
        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);


        skew = new Matrix();
        mCamera = new Camera();

	}

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mBackView = getChildAt(0);
        mFrontView = getChildAt(1);

        Bitmap frontBitmap = loadBitmapFromView(mFrontView);
        int dataHeight = frontBitmap.getHeight();
        int dataWidth = frontBitmap.getWidth();

        bArr = new Bitmap[mLouversCount];

        switch (mCurrentDirection) {
            case AXIS_X:
                mPanelWidth = dataWidth /mLouversCount;
                mPanelCenterX = mPanelWidth / 2;
                mPanelCenterY = dataHeight / 2;
                break;
            case AXIS_Y:
                mPanelWidth = dataHeight /mLouversCount;
                mPanelCenterX = dataWidth / 2;
                mPanelCenterY = mPanelWidth / 2;
                for(int i = 0; i < mLouversCount; i++) {
                    bArr[i] = Bitmap.createBitmap(frontBitmap, 0, (int) (mPanelWidth*i), dataWidth, (int) mPanelWidth);
                }
                break;
        }

        frontBitmap.recycle();
        frontBitmap = null;

        mLockEdge = (int) ((mLouversCount-1) * mPanelWidth - mPanelWidth * 3/4 - (mLouversCount-1) * mPanelWidth/4 + mPanelWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        mBackView.draw(canvas);


        isLocked = mLockEdge < mOffset;
        for(int i = mLouversCount-1; i>=0;i--){
            Matrix m= getRotationMatrix(getAngleByOffset(mOffset, i));
            if(!isLocked)
                m.postTranslate(0, i*mPanelWidth+getMarginByOffset(mOffset, i));
            else{
                mOffset = mLockEdge;
                m.postTranslate(0, i*mPanelWidth+getMarginByOffset(mOffset, i));
            }
            canvas.drawBitmap(bArr[i], m, null);
        }
//        mFrontView.draw(canvas);
        super.onDraw(canvas);
        canvas.restore();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        //View group manages child drawing
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP)
            mIsScrolling = false;
        if(event.getAction() == MotionEvent.ACTION_CANCEL)
            mIsScrolling = false;
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            mIsScrolling = isBorderHit(event.getX(), event.getY());
        }

//        if(event.getAction() == MotionEvent.ACTION_UP) {
//            if(mIsScrolling ) {
//                Log.d("OnTouchListener --> onTouch ACTION_UP");
//                mIsScrolling  = false;
//                handleScrollFinished();
//            };
//        }
        if(mIsScrolling)
            mGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;//TODO resolve situations when click by back view
    }

    private final GestureDetector.SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener(){

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
//			Log.d(TAG, "x1="+e1.getX()+" y1="+e1.getY()+" mOffset = "+mOffset);
//			Log.d(TAG, "x2="+e2.getX()+" y2="+e2.getY());
//			if(isBorderHit(e2.getX(), e2.getY())) {
            mOffset-=distanceY;
            mOffset = mOffset < 0 ? 0 : mOffset;
            invalidate();
//			}
            Log.d(TAG, "mOffset=" + mOffset + "\tdistanceY = " + distanceY);
//			Log.d(TAG, "x="+e1.getX()+" y="+e1.getY()+" onScroll distanceX = "+distanceX+"\tdistanceY = "+distanceY);
            return super.onScroll(e1, e2, distanceX, distanceY);
		}
		
	};

    private float getAngleByOffset(float offset, int i) {
//		return 0;
        int angle = 0;
        offset+=i*30;
        if(offset < (i+1)*mPanelWidth-mPanelWidth*3/4)
            return angle;
        else if(offset > (i+1)*mPanelWidth-mPanelWidth/4)
            return MAX_ANGLE;
        else
            return MAX_ANGLE*Math.abs((offset - ((i)*mPanelWidth+mPanelWidth/4)))/(mPanelWidth/2);
    }

    private int getMarginByOffset(int offset, int i) {
        if(offset < i*mPanelWidth-mPanelWidth*3/4 - i*mPanelWidth/4 + mPanelWidth)
            return 0;
        else
            return (int) (offset - (i*mPanelWidth - mPanelWidth*3/4 - i*mPanelWidth/4 + mPanelWidth));
    }

    public void skewCanvas(Canvas canvas) {
        mCamera.save();
        mCamera.rotateX(angle);
        mCamera.getMatrix(skew);
        mCamera.restore();

        skew.preTranslate(-mPanelCenterX, -mPanelCenterY); //This is the key to getting the correct viewing perspective
        skew.postTranslate(mPanelCenterX, mPanelCenterY);

        canvas.concat(skew);
    }

    public Bitmap applyMatrix(Bitmap bmp, int i) {
        mCamera.save();
        mCamera.rotateX(angle);
        mCamera.rotateY(0);
        mCamera.rotateZ(0);
        mCamera.getMatrix(skew);
        mCamera.restore();

        int CenterX = 200;
        int CenterY = 50+i*100;
        skew.preTranslate(-CenterX, -CenterY); //This is the key to getting the correct viewing perspective
        skew.postTranslate(CenterX, CenterY);

        Bitmap result = Bitmap.createBitmap(bmp, 0, 0, 400, 400, skew, true);
        return result;
    }

    private Matrix getRotationMatrix(float angle) {
        mCamera.save();
        mCamera.rotateX(angle);
        mCamera.rotateY(0);
        mCamera.rotateZ(0);
        mCamera.getMatrix(skew);
        mCamera.restore();

        skew.preTranslate(-mPanelCenterX, -mPanelCenterY); //This is the key to getting the correct viewing perspective
        skew.postTranslate(mPanelCenterX, mPanelCenterY);
        return skew;
    }

    //TODO look if scroll started near border and only then hit!
    private boolean isBorderHit(float x, float y) {
        Log.d(TAG, "isBorderHit");
        switch (mCurrentDirection) {
            case AXIS_X:{//TODO mPanelWidth change to constants multiplexed on dp
                return x < mOffset + mPanelWidth*1.5 && x > mOffset-mPanelWidth*0.5;
            }
            case AXIS_Y:{
                if(y < mPanelWidth * 1.5 && (mOffset >= 0 && mOffset <= mPanelWidth)) {
                    mCurrentMovementDirection = MovementDirection.DOWN;
                    return true;
                }else if(y > mPanelWidth * (mLouversCount - 1) && (mOffset >= mLockEdge - mPanelWidth && mOffset <= mLockEdge)) {
                    mCurrentMovementDirection = MovementDirection.UP;
                    mOffset = mLockEdge;
                    return true;
                } else if(y > mOffset - mPanelWidth && y < mOffset + mPanelWidth){
                    return true;
                } else {
                    return false;
                }
//				return y < mOffset + mPanelWidth*1.5 && y > mOffset-mPanelWidth*0.5;
            }
            default:{
                return false;
            }
        }
    }


    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getOffset() {
        return mOffset;
    }

    public void setOffset(int mOffset) {
        this.mOffset = mOffset;
    }

    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap( v.getLayoutParams().width, v.getLayoutParams().height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }
}
