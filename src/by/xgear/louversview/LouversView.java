package by.xgear.louversview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
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

    private Rect mHitRectStartV, mHitRectStartH, mHitRectEndV, mHitRectEndH;

    //Constants
    private float MAX_ANGLE = 40;
    private static final int DEF_PANEL_COUNT = -1;
    private static final int DEF_ANGLE = 0;

    private static final int AXIS_X = 1;
    private static final int AXIS_Y = 2;
    private static final int AXIS_BOTH = 3;

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
    private MovementDirection mCurrentMovementDirection = MovementDirection.RIGHT;
    private boolean mIsScrolling;



    //Measuring related fields
    private int angle;
    private int mLouversCountV;
    private int mLouversCountH;
    private int mOffset = 0;
    private float mPanelWidthV;
    private float mPanelWidthH;
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
        int dataHeight = frontBitmap.getHeight();
        int dataWidth = frontBitmap.getWidth();

        bArrH = new Bitmap[mLouversCountH];
        bArrV = new Bitmap[mLouversCountV];

        switch (mAccessibleDirections) {
            case AXIS_X:

                mPanelWidthH = dataWidth /mLouversCountH;
                mPanelCenterXH = mPanelWidthH / 2;
                mPanelCenterYH = dataHeight / 2;
                for(int i = 0; i < mLouversCountH; i++) {
                    bArrH[i] = Bitmap.createBitmap(frontBitmap, (int) (mPanelWidthH*i), 0, (int) mPanelWidthH, dataHeight);
                }

                break;
            case AXIS_Y:

                mPanelWidthV = dataHeight /mLouversCountV;
                mPanelCenterXV = dataWidth / 2;
                mPanelCenterYV = mPanelWidthV / 2;
                for(int i = 0; i < mLouversCountV; i++) {
                    bArrV[i] = Bitmap.createBitmap(frontBitmap, 0, (int) (mPanelWidthV*i), dataWidth, (int) mPanelWidthV);
                }

                break;
            case AXIS_BOTH:

                mPanelWidthH = dataWidth /mLouversCountH;
                mPanelCenterXH = mPanelWidthH / 2;
                mPanelCenterYH = dataHeight / 2;
                for(int i = 0; i < mLouversCountH; i++) {
                    bArrH[i] = Bitmap.createBitmap(frontBitmap, (int) (mPanelWidthH*i), 0, (int) mPanelWidthH, dataHeight);
                }

                mPanelWidthV = dataHeight /mLouversCountV;
                mPanelCenterXV = dataWidth / 2;
                mPanelCenterYV = mPanelWidthV / 2;
                for(int i = 0; i < mLouversCountV; i++) {
                    bArrV[i] = Bitmap.createBitmap(frontBitmap, 0, (int) (mPanelWidthV*i), dataWidth, (int) mPanelWidthV);
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

        mHitRectStartV = new Rect(0 + (int)mPanelWidthH, 0, dataWidth - (int)mPanelWidthH, (int)mPanelWidthV);
        mHitRectEndV = new Rect(0 + (int)mPanelWidthH, dataHeight - (int)mPanelWidthV, dataWidth - (int)mPanelWidthH, dataHeight);

        mHitRectStartH = new Rect(0 , 0 - (int)mPanelWidthV, (int)mPanelWidthH, dataHeight - (int)mPanelWidthV);
        mHitRectEndH = new Rect(dataWidth - (int)mPanelWidthH, 0 - (int)mPanelWidthV, dataWidth, dataHeight - (int)mPanelWidthV);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        mBackView.draw(canvas);

        if (mCurrentMovementDirection == MovementDirection.DOWN) {
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
        } else if (mCurrentMovementDirection == MovementDirection.UP) {
            isLocked = mOffset == 0;
            for (int i = 0; i <= mLouversCountV - 1; i++) {
                Matrix m = getRotationMatrix(getAngleByOffset(mOffset, i));
                if (!isLocked)
                    m.postTranslate(0, /*(mLouversCount - 1 - i)*/i * mPanelWidthV - getMarginByOffset(mOffset, i));
                else {
                    mOffset = 0;
                    m.postTranslate(0, i * mPanelWidthV );
                }
                canvas.drawBitmap(bArrV[i], m, null);
            }
        } else if (mCurrentMovementDirection == MovementDirection.RIGHT) {
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
        } else if (mCurrentMovementDirection == MovementDirection.LEFT) {
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

            if (mCurrentMovementDirection == MovementDirection.DOWN) {
                mHitRectStartV.offsetTo(0, (int) (mOffset));
            } else if (mCurrentMovementDirection == MovementDirection.UP) {
                mHitRectEndV.offsetTo(0, (int) (getHeight() - mOffset - mPanelWidthV));
            } else if (mCurrentMovementDirection == MovementDirection.RIGHT) {
                mHitRectEndV.offsetTo((int) (mOffset), 0);
            } else if (mCurrentMovementDirection == MovementDirection.LEFT) {
                mHitRectEndV.offsetTo((int) (getWidth() - mOffset - mPanelWidthH), 0);
            }
//			}
            Log.d(TAG, "mOffset=" + mOffset + "\tdistanceY = " + distanceY + "\tdistanceX = " + distanceX);
//			Log.d(TAG, "x="+e1.getX()+" y="+e1.getY()+" onScroll distanceX = "+distanceX+"\tdistanceY = "+distanceY);
            return super.onScroll(e1, e2, distanceX, distanceY);
		}
		
	};

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
        if(mCurrentDirection == AXIS_Y)
            mCamera.rotateX(angle);
        if(mCurrentDirection == AXIS_X)
            mCamera.rotateY(-angle);
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
                } else if (containsEnd) {
                    mCurrentMovementDirection = MovementDirection.LEFT;
                }
                if(containsStart || containsEnd) {
                    mOffset = 0;
                }
                return containsStart || containsEnd;
            }
            case AXIS_Y:{
                boolean containsStart = mHitRectStartV.contains((int) x, (int) y);
                boolean containsEnd = mHitRectEndV.contains((int) x, (int) y);
                if (containsStart) {
                    mCurrentMovementDirection = MovementDirection.DOWN;
                } else if (containsEnd) {
                    mCurrentMovementDirection = MovementDirection.UP;
                }
                if(containsStart || containsEnd) {
                    mOffset = 0;
                }
                return containsStart || containsEnd;
/*                if(y < mPanelWidthV * 1.5 && (mOffset >= 0 && mOffset <= mPanelWidthV)) {
                    Log.d(TAG, "isBorderHit DOWN");
                    mOffset = 0;
                    return true;
                }else if(y > mPanelWidthV * (mLouversCountV - 1) && ((mOffset >= mLockEdgeV - mPanelWidthV && mOffset <= mLockEdgeV) || mOffset == 0)) {
                    mCurrentMovementDirection = MovementDirection.UP;
                    Log.d(TAG, "isBorderHit UP");
                    mOffset = 0;
                    return true;
                } else if(y > mOffset - mPanelWidthV && y < mOffset + mPanelWidthV){
                    return true;
                } else {
                    return false;
                }*/
//				return y < mOffset + mPanelWidth*1.5 && y > mOffset-mPanelWidth*0.5;
            }
            case AXIS_BOTH:{

                boolean containsStartH = mHitRectStartH.contains((int) x, (int) y);
                boolean containsEndH = mHitRectEndH.contains((int) x, (int) y);
                boolean containsStartV = mHitRectStartV.contains((int) x, (int) y);
                boolean containsEndV = mHitRectEndV.contains((int) x, (int) y);

                if (containsStartH) {
                    mCurrentMovementDirection = MovementDirection.RIGHT;
                } else if (containsEndH) {
                    mCurrentMovementDirection = MovementDirection.LEFT;
                } else if (containsStartV) {
                    mCurrentMovementDirection = MovementDirection.DOWN;
                } else if (containsEndV) {
                    mCurrentMovementDirection = MovementDirection.UP;
                }
                if(containsStartH || containsEndH || containsEndV || containsEndV) {
                    mOffset = 0;
                }

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
}