package by.xgear.louversview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
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

    private Bitmap[] bArrV;
    private Bitmap[] bArrH;

    private Matrix skew;
    private Camera mCamera;

    private View mBackView;
    private View mFrontView;

    private Rect mHitRectStartV, mHitRectStartH, mHitRectEndV, mHitRectEndH, startPosition[];

    //Constants
    private float MAX_ANGLE = 40;
    private static final int DEF_PANEL_COUNT = -1;
    private static final int DEF_ANGLE = 0;

    private static final int AXIS_X = 1;
    private static final int AXIS_Y = 2;
    private static final int AXIS_BOTH = 3;
    private int mFrontViewHeight;
    private int mFrontViewWidth;

    //State related fields

    public enum MovementDirection{
        LEFT,
        RIGHT,
        UP,
        DOWN;
    }

    private boolean edgeLock;
    private boolean isLocked;
    private int mAccessibleDirections = 0;
    private int mCurrentDirection = AXIS_BOTH;
    private MovementDirection mCurrentMovementDirection = null;
    private boolean mIsScrollingAcceptd;



    //Measuring related fields
    private int angle;
    private int mLouversCountV;
    private int mLouversCountH;
    private int mOffset = 0;
    private int mPanelWidthV;
    private int mPanelWidthH;
    private float mPanelCenterXV, mPanelCenterYV;
    private float mPanelCenterXH, mPanelCenterYH;
    private int mLockEdgeV;
    private int mLockEdgeH;


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

        mLouversCountV = ta.getInteger(R.styleable.LouversView_panel_count_vertical, 8);
        mLouversCountH = ta.getInteger(R.styleable.LouversView_panel_count_horizontal, 4);

        if(ta.getBoolean(R.styleable.LouversView_vertical, false))
            mAccessibleDirections |= AXIS_Y;

        if(ta.getBoolean(R.styleable.LouversView_horizontal, false))
            mAccessibleDirections |= AXIS_X;

        edgeLock = ta.getBoolean(R.styleable.LouversView_edge_lock, false);

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
        mFrontViewHeight = frontBitmap.getHeight();
        mFrontViewWidth = frontBitmap.getWidth();

        bArrH = new Bitmap[mLouversCountH];
        bArrV = new Bitmap[mLouversCountV];

        switch (mAccessibleDirections) {
            case AXIS_X:

                mPanelWidthH = mFrontViewWidth /mLouversCountH;
                mPanelCenterXH = mPanelWidthH / 2;
                mPanelCenterYH = mFrontViewHeight / 2;
                for(int i = 0; i < mLouversCountH; i++) {
                    bArrH[i] = Bitmap.createBitmap(frontBitmap, (int) (mPanelWidthH*i), 0, (int) mPanelWidthH, mFrontViewHeight);
                }

                break;
            case AXIS_Y:

                mPanelWidthV = mFrontViewHeight /mLouversCountV;
                mPanelCenterXV = mFrontViewWidth / 2;
                mPanelCenterYV = mPanelWidthV / 2;
                for(int i = 0; i < mLouversCountV; i++) {
                    bArrV[i] = Bitmap.createBitmap(frontBitmap, 0, (int) (mPanelWidthV*i), mFrontViewWidth, (int) mPanelWidthV);
                }

                break;
            case AXIS_BOTH:

                mPanelWidthH = mFrontViewWidth /mLouversCountH;
                mPanelCenterXH = mPanelWidthH / 2;
                mPanelCenterYH = mFrontViewHeight / 2;
                for(int i = 0; i < mLouversCountH; i++) {
                    bArrH[i] = Bitmap.createBitmap(frontBitmap, (int) (mPanelWidthH*i), 0, (int) mPanelWidthH, mFrontViewHeight);
                }

                mPanelWidthV = mFrontViewHeight /mLouversCountV;
                mPanelCenterXV = mFrontViewWidth / 2;
                mPanelCenterYV = mPanelWidthV / 2;
                for(int i = 0; i < mLouversCountV; i++) {
                    bArrV[i] = Bitmap.createBitmap(frontBitmap, 0, (int) (mPanelWidthV*i), mFrontViewWidth, (int) mPanelWidthV);
                }

                break;
        }

        frontBitmap.recycle();
        frontBitmap = null;

        mLockEdgeH = edgeLock
                ? (int) ((mLouversCountH-1) * mPanelWidthH - mPanelWidthH * 3/4 - (mLouversCountH-1) * mPanelWidthH/4 + mPanelWidthH)
                : getWidth();

        mLockEdgeV = edgeLock
                ? (int) ((mLouversCountV-1) * mPanelWidthH - mPanelWidthV * 3/4 - (mLouversCountV-1) * mPanelWidthV/4 + mPanelWidthV)
                : getHeight();

        startPosition = new Rect[4];

        mHitRectStartV = new Rect(0 + (int)mPanelWidthH, 0, mFrontViewWidth - (int)mPanelWidthH, (int)mPanelWidthV);
        startPosition[0] = new Rect(0 + (int)mPanelWidthH, 0, mFrontViewWidth - (int)mPanelWidthH, (int)mPanelWidthV);

        mHitRectEndV = new Rect(0 + (int)mPanelWidthH, mFrontViewHeight - (int)mPanelWidthV, mFrontViewWidth - (int)mPanelWidthH, mFrontViewHeight);
        startPosition[1] = new Rect(0 + (int)mPanelWidthH, mFrontViewHeight - (int)mPanelWidthV, mFrontViewWidth - (int)mPanelWidthH, mFrontViewHeight);

        mHitRectStartH = new Rect(0 , 0 + (int)mPanelWidthV, (int)mPanelWidthH, mFrontViewHeight - (int)mPanelWidthV);
        startPosition[2] = new Rect(0 , 0 + (int)mPanelWidthV, (int)mPanelWidthH, mFrontViewHeight - (int)mPanelWidthV);

        mHitRectEndH = new Rect(mFrontViewWidth - (int)mPanelWidthH, 0 + (int)mPanelWidthV, mFrontViewWidth, mFrontViewHeight - (int)mPanelWidthV);
        startPosition[3] = new Rect(mFrontViewWidth - (int)mPanelWidthH, 0 + (int)mPanelWidthV, mFrontViewWidth, mFrontViewHeight - (int)mPanelWidthV);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        mBackView.draw(canvas);
        if(mCurrentMovementDirection != null) {
            switch (mCurrentMovementDirection) {
                case DOWN: {
                    isLocked = mLockEdgeV < mOffset;
                    for (int i = mLouversCountV - 1; i >= 0; i--) {
                        Matrix m = getRotationMatrix(getAngleByOffset(mOffset, i));
                        if (!isLocked)
                            m.postTranslate(0, i * mPanelWidthV + getMarginByOffset(mOffset, i));
                        else {
                            mOffset = mLockEdgeV;
                            m.postTranslate(0, i * mPanelWidthV + getMarginByOffset(mOffset, i));
                        }
                        canvas.drawBitmap(bArrV[i], m, null);
                    }
                    break;
                }
                case UP: {
                    isLocked = mOffset == 0;
                    for (int i = 0; i <= mLouversCountV - 1; i++) {
                        Matrix m = getRotationMatrix(getAngleByOffset(mOffset, i));
                        if (!isLocked)
                            m.postTranslate(0, /*(mLouversCount - 1 - i)*/i * mPanelWidthV - getMarginByOffset(mOffset, i));
                        else {
                            mOffset = 0;
                            m.postTranslate(0, i * mPanelWidthV);
                        }
                        canvas.drawBitmap(bArrV[i], m, null);
                    }
                    break;
                }
                case RIGHT: {
                    isLocked = mLockEdgeH < mOffset;
                    for (int i = mLouversCountH - 1; i >= 0; i--) {
                        Matrix m = getRotationMatrix(getAngleByOffset(mOffset, i));
//                if (!isLocked)
                        m.postTranslate(i * mPanelWidthH + getMarginByOffset(mOffset, i), /*(mLouversCount - 1 - i)*/0);
//                else {
//                    mOffset = mLockEdge;
//                    m.postTranslate(0, i * mPanelWidth + getMarginByOffset(mOffset, i));
//                }
                        canvas.drawBitmap(bArrH[i], m, null);
                    }
                    break;
                }
                case LEFT: {
                    isLocked = mOffset == 0;
                    for (int i = 0; i <= mLouversCountH - 1; i++) {
                        Matrix m = getRotationMatrix(getAngleByOffset(mOffset, i));
//                if (!isLocked)
                        m.postTranslate(i * mPanelWidthH - getMarginByOffset(mOffset, i), /*(mLouversCount - 1 - i)*/0);
//                else {
//                    mOffset = mLockEdge;
//                    m.postTranslate(0, i * mPanelWidth + getMarginByOffset(mOffset, i));
//                }
                        canvas.drawBitmap(bArrH[i], m, null);
                    }
                    break;
                }
            }
        } else {
            mFrontView.draw(canvas);
        }
//        if (mCurrentMovementDirection == MovementDirection.DOWN) {
//        } else if (mCurrentMovementDirection == MovementDirection.UP) {
//        } else if (mCurrentMovementDirection == MovementDirection.RIGHT) {
//        } else if (mCurrentMovementDirection == MovementDirection.LEFT) {
//        }
        super.onDraw(canvas);
        canvas.restore();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        //View group manages child drawing
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                mIsScrollingAcceptd = false;
                moveHitRectArea();
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                mIsScrollingAcceptd = isBorderHit(event.getX(), event.getY());
                break;
            }
        }

        if(mIsScrollingAcceptd)
            mGestureDetector.onTouchEvent(event);

        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return !isBackViewHit((int)event.getX(), (int)event.getY());//TODO resolve situations when click by back view
    }

    private final GestureDetector.SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener(){

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
//			Log.d(TAG, "x1="+e1.getX()+" y1="+e1.getY()+" mOffset = "+mOffset);
//			Log.d(TAG, "x2="+e2.getX()+" y2="+e2.getY());
//			if(isBorderHit(e2.getX(), e2.getY())) {

            if (mCurrentMovementDirection == MovementDirection.DOWN)
                mOffset -= distanceY;
            else if (mCurrentMovementDirection == MovementDirection.UP)
                mOffset += distanceY;
            else if (mCurrentMovementDirection == MovementDirection.RIGHT)
                mOffset -= distanceX;
            else if (mCurrentMovementDirection == MovementDirection.LEFT)
                mOffset += distanceX;

            mOffset = mOffset < 0 ? 0 : mOffset;

            invalidate();

//			}
//            Log.d(TAG, "mOffset=" + mOffset + "\tdistanceY = " + distanceY + "\tdistanceX = " + distanceX);
//			Log.d(TAG, "x="+e1.getX()+" y="+e1.getY()+" onScroll distanceX = "+distanceX+"\tdistanceY = "+distanceY);
            return super.onScroll(e1, e2, distanceX, distanceY);
		}

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            printState();
            return super.onDoubleTap(e);
        }
    };

    private void moveHitRectArea() {
        if(mCurrentMovementDirection != null) {
            switch (mCurrentMovementDirection) {
                case DOWN: {
                    mHitRectStartV.offsetTo(mHitRectStartV.left, mOffset);
                    Log.d(TAG, "onScroll = " + MovementDirection.DOWN);
                    break;
                }
                case UP: {
                    mHitRectEndV.offsetTo(mHitRectEndV.left, getHeight() - mOffset - mPanelWidthV);
                    Log.d(TAG, "onScroll = " + MovementDirection.UP);
                    break;
                }
                case RIGHT: {
                    mHitRectStartH.offsetTo(mOffset, mHitRectStartH.top);
                    Log.d(TAG, "onScroll = " + MovementDirection.RIGHT);
                    break;
                }
                case LEFT: {
                    mHitRectEndH.offsetTo(getWidth() - mOffset - mPanelWidthH, mHitRectEndH.top);
                    Log.d(TAG, "onScroll = " + MovementDirection.LEFT);
                    break;
                }
            }
        }
    }

    private float getAngleByOffset(float offset, int i) {
//		return 0;
        if(mCurrentMovementDirection == MovementDirection.DOWN ) {

            offset += i * 30;
            if (offset < (i + 1) * mPanelWidthV - mPanelWidthV * 3 / 4)
                return 0;
            else if (offset > (i + 1) * mPanelWidthV - mPanelWidthV / 4)
                return MAX_ANGLE;
            else
                return MAX_ANGLE * Math.abs((offset - ((i) * mPanelWidthV + mPanelWidthV / 4))) / (mPanelWidthV / 2);

        } else if(mCurrentMovementDirection == MovementDirection.RIGHT){

            offset += i * 30;
            if (offset < (i + 1) * mPanelWidthH - mPanelWidthH * 3 / 4)
                return 0;
            else if (offset > (i + 1) * mPanelWidthH - mPanelWidthH / 4)
                return MAX_ANGLE;
            else
                return MAX_ANGLE * Math.abs((offset - ((i) * mPanelWidthH + mPanelWidthH / 4))) / (mPanelWidthH / 2);

        } else if (mCurrentMovementDirection == MovementDirection.UP) {

            i = mLouversCountV - 1 - i;
            offset += i * 30;
            if (offset < (i + 1) * mPanelWidthV - mPanelWidthV * 3 / 4)
                return 0;
            else if (offset > (i + 1) * mPanelWidthV - mPanelWidthV / 4)
                return -MAX_ANGLE;
            else
                return -MAX_ANGLE * Math.abs((offset - ((i) * mPanelWidthV + mPanelWidthV / 4))) / (mPanelWidthV / 2);

        } else if (mCurrentMovementDirection == MovementDirection.LEFT) {

            i = mLouversCountH - 1 - i;
            offset += i * 30;
            if (offset < (i + 1) * mPanelWidthH - mPanelWidthH * 3 / 4)
                return 0;
            else if (offset > (i + 1) * mPanelWidthH - mPanelWidthH / 4)
                return -MAX_ANGLE;
            else
                return -MAX_ANGLE * Math.abs((offset - ((i) * mPanelWidthH + mPanelWidthH / 4))) / (mPanelWidthH / 2);

        }
        return 0;
    }

    private int getMarginByOffset(int offset, int i) {
        int marginThreshold = 0;
        if(mCurrentMovementDirection == MovementDirection.DOWN) {
            marginThreshold  = (int) (i * mPanelWidthV * 3 / 4 + mPanelWidthV / 4);
        } else if(mCurrentMovementDirection == MovementDirection.RIGHT) {
            marginThreshold  = (int) (i * mPanelWidthH * 3 / 4 + mPanelWidthH / 4);
        }else if(mCurrentMovementDirection == MovementDirection.UP) {
            marginThreshold = (int) ((mLouversCountV -1 - i) * mPanelWidthV * 3 / 4 + mPanelWidthV / 4);
        } else if(mCurrentMovementDirection == MovementDirection.LEFT) {
            marginThreshold = (int) ((mCurrentMovementDirection == MovementDirection.UP ? mLouversCountH : mLouversCountH -1 - i) * mPanelWidthH * 3 / 4 + mPanelWidthH / 4);
        }
        if (offset < marginThreshold)
            return 0;
        else
            return offset - marginThreshold;
    }

    public void skewCanvas(Canvas canvas) {
        mCamera.save();
        mCamera.rotateX(angle);
        mCamera.getMatrix(skew);
        mCamera.restore();

        if(mCurrentMovementDirection == MovementDirection.DOWN || mCurrentMovementDirection == MovementDirection.UP) {
            skew.preTranslate(-mPanelCenterXV, -mPanelCenterYV); //This is the key to getting the correct viewing perspective
            skew.postTranslate(mPanelCenterXV, mPanelCenterYV);
        } else if (mCurrentMovementDirection == MovementDirection.LEFT || mCurrentMovementDirection == MovementDirection.RIGHT) {
            skew.preTranslate(-mPanelCenterXH, -mPanelCenterYH); //This is the key to getting the correct viewing perspective
            skew.postTranslate(mPanelCenterXH, mPanelCenterYH);
        }

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
        switch (mCurrentDirection) {
            case AXIS_Y : {
                mCamera.rotateX(angle);
                break;
            }
            case AXIS_X : {
                mCamera.rotateY(-angle);
                break;
            }
            case AXIS_BOTH : {
                if(mCurrentMovementDirection == MovementDirection.DOWN || mCurrentMovementDirection == MovementDirection.UP) {
                    mCamera.rotateX(angle);
                } else {
                    mCamera.rotateY(-angle);
                }
                break;
            }
        }
        mCamera.rotateZ(0);
        mCamera.getMatrix(skew);
        mCamera.restore();

        if(mCurrentMovementDirection == MovementDirection.DOWN || mCurrentMovementDirection == MovementDirection.UP) {
            skew.preTranslate(-mPanelCenterXV, -mPanelCenterYV); //This is the key to getting the correct viewing perspective
            skew.postTranslate(mPanelCenterXV, mPanelCenterYV);
        } else if (mCurrentMovementDirection == MovementDirection.LEFT || mCurrentMovementDirection == MovementDirection.RIGHT) {
            skew.preTranslate(-mPanelCenterXH, -mPanelCenterYH); //This is the key to getting the correct viewing perspective
            skew.postTranslate(mPanelCenterXH, mPanelCenterYH);
        }
        return skew;
    }

    //TODO look if scroll started near border and only then hit!
    private boolean isBorderHit(float x, float y) {

        switch (mAccessibleDirections) {
            case AXIS_X:{//TODO mPanelWidth change to constants multiplexed on dp
                boolean containsStart = mHitRectStartH.contains((int) x, (int) y);
                boolean containsEnd = mHitRectEndH.contains((int) x, (int) y);
                if (containsStart) {
                    mCurrentMovementDirection = MovementDirection.RIGHT;
                    mHitRectEndH = new Rect(startPosition[3]);
                } else if (containsEnd) {
                    mCurrentMovementDirection = MovementDirection.LEFT;
                    mHitRectStartH = new Rect(startPosition[2]);
                }
//                if(containsStart || containsEnd) {
//                    mOffset = 0;
//                }
                Log.d(TAG, "isBorderHit = "+mCurrentMovementDirection);
                return containsStart || containsEnd;
            }
            case AXIS_Y:{
                boolean containsStart = mHitRectStartV.contains((int) x, (int) y);
                boolean containsEnd = mHitRectEndV.contains((int) x, (int) y);
                if (containsStart) {
                    mCurrentMovementDirection = MovementDirection.DOWN;
                    mHitRectEndV = new Rect(startPosition[1]);
                } else if (containsEnd) {
                    mCurrentMovementDirection = MovementDirection.UP;
                    mHitRectStartV = new Rect(startPosition[0]);
                }

//                if(containsStart || containsEnd) {
//                    mOffset = 0;
//                }
                Log.d(TAG, "isBorderHit = "+mCurrentMovementDirection);
                return containsStart || containsEnd;
            }
            case AXIS_BOTH:{

                boolean containsStartH = mHitRectStartH.contains((int) x, (int) y);
                containsStartH = (mCurrentMovementDirection != MovementDirection.RIGHT && mOffset > 0) ? false : containsStartH;

                boolean containsEndH = mHitRectEndH.contains((int) x, (int) y);
                containsEndH = (mCurrentMovementDirection != MovementDirection.LEFT && mOffset > 0) ? false : containsEndH;

                boolean containsStartV = mHitRectStartV.contains((int) x, (int) y);
                containsStartV = (mCurrentMovementDirection != MovementDirection.DOWN && mOffset > 0) ? false : containsStartV;

                boolean containsEndV = mHitRectEndV.contains((int) x, (int) y);
                containsEndV = (mCurrentMovementDirection != MovementDirection.UP && mOffset > 0) ? false : containsEndV;

                if (containsStartH) {
                    mCurrentMovementDirection = MovementDirection.RIGHT;
                    mHitRectEndH = new Rect(startPosition[3]);
                } else if (containsEndH) {
                    mCurrentMovementDirection = MovementDirection.LEFT;
                    mHitRectStartH = new Rect(startPosition[2]);
                } else if (containsStartV) {
                    mCurrentMovementDirection = MovementDirection.DOWN;
                    mHitRectEndV = new Rect(startPosition[1]);
                } else if (containsEndV) {
                    mCurrentMovementDirection = MovementDirection.UP;
                    mHitRectStartV = new Rect(startPosition[0]);
                }

                Log.d(TAG, "isBorderHit = "+mCurrentMovementDirection);
                return containsStartH || containsEndH || containsStartV || containsEndV;
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

    public void printState() {
        Log.d(TAG, "offset = " +mOffset);
        Log.d(TAG, "CurrentMovementDirection = " +mCurrentMovementDirection);
        Log.d(TAG, "HitRectEndH = " +mHitRectEndH.toShortString());
        Log.d(TAG, "HitRectStartH = " +mHitRectStartH.toShortString());
        Log.d(TAG, "HitRectEndV = " +mHitRectEndV.toShortString());
        Log.d(TAG, "HitRectStartV = " +mHitRectStartV.toShortString());
//        mHitRectStartV.offsetTo(0, 300);
//        Log.d(TAG, "HitRectStartV = " +mHitRectStartV.toShortString());
        for(int i = 0 ; i < 4; i++) {
            Log.d(TAG, "startPosition[" + i + "] = " + startPosition[i].toShortString());
        }
    }

    private boolean isBackViewHit(int x, int y) {
        if(mCurrentMovementDirection != null) {
            Rect rr = null;
            switch (mCurrentMovementDirection) {
                case DOWN: {
                    rr = new Rect(0, 0, getWidth(), mHitRectStartV.top);
                    break;
                }
                case UP: {
                    rr = new Rect(0, mHitRectEndV.bottom, getWidth(), getHeight());
                    break;
                }
                case RIGHT: {
                    rr = new Rect(0, 0, mHitRectStartH.left, getHeight());
                    break;
                }
                case LEFT: {
                    rr = new Rect(mHitRectEndH.right, 0, getWidth(), getHeight());
                    break;
                }
            }
            if (rr != null)
                return rr.contains(x, y);
            else
                return false;
        } else
            return false;
    }
}