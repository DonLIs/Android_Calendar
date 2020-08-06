package me.donlis.mcalendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MCalendarView extends View {

    //画笔
    private Paint mMonthPaint;//年月
    private Paint mWeekPaint;//周
    private Paint mDayPaint;//日

    private Paint mLinePaint;//分割线
    private Paint mSelecterPaint;//选中背景

    //屏幕宽度
    private float mWidth;

    private float mMonthHeight;
    private float mWeekHeight;
    private float mDayHeight;

    private float mTextSpec;//文字与箭头的距离
    private float mArrowWith;//箭头宽度
    private float mArrowSpec;//箭头间隔

    private Date mCurrentDate;//当前日期
    private int mYear;//当前年
    private int mMonth;//当前月
    private int mDay;//当前日

    private int mTriggerYear;//切换年
    private int mTriggerMonth;//切换月
    private int mTriggerDay;//切换日

    private int mSelectYear;//选中年
    private int mSelectMonth;//选中月
    private int mSelectDay;//选中日

    private int mDayCountOfMonth;
    private int mFirstWeekIndex;//当月第一天，星期的索引
    private int mEndWeekIndex;//当月最后一天,星期的索引

    private int mFirstLine;//日历第一行
    private int mEndLine;//日历最后一行

    private DateSelectListener listener;

    public interface DateSelectListener{
        void onSelect(int year, int month, int day);
    }

    public MCalendarView(Context context) {
        super(context);
        init(context);
    }

    public MCalendarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MCalendarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        mMonthPaint = new Paint();
        mMonthPaint.setAntiAlias(true);
        mMonthPaint.setColor(Color.BLACK);
        mMonthPaint.setTextSize(50);
        mMonthPaint.setStrokeWidth(3);

        mWeekPaint = new Paint();
        mWeekPaint.setAntiAlias(true);
        mWeekPaint.setColor(Color.BLACK);
        mWeekPaint.setTextSize(50);
        mWeekPaint.setStrokeWidth(3);
        mWeekPaint.setTextAlign(Paint.Align.CENTER);

        mDayPaint = new Paint();
        mDayPaint.setAntiAlias(true);
        mDayPaint.setColor(Color.BLACK);
        mDayPaint.setTextSize(50);
        mDayPaint.setStrokeWidth(3);
        mDayPaint.setTextAlign(Paint.Align.CENTER);

        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setColor(Color.GRAY);
        mLinePaint.setStrokeWidth(1);
        mLinePaint.setStyle(Paint.Style.STROKE);

        mSelecterPaint = new Paint();
        mSelecterPaint.setAntiAlias(true);
        mSelecterPaint.setColor(Color.parseColor("#1CD8C6"));
        mSelecterPaint.setStyle(Paint.Style.FILL);

        mMonthHeight = 150;
        mWeekHeight = 140;
        mDayHeight = 130;
        mTextSpec = 150;
        mArrowWith = 25;
        mArrowSpec = 50;

        mWidth = getPhoneWidth(context);
        initDate();
    }

    private void initDate(){
        mCurrentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mCurrentDate);

        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH) + 1;
        mDay = calendar.get(Calendar.DAY_OF_MONTH);

        mDayCountOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);//当月总天数

        //默认一个星期第一天是星球日，索引为0，星期六索引为7，所以要减1
        calendar.set(Calendar.DATE,1);
        mFirstWeekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        calendar.set(Calendar.DATE,mDayCountOfMonth);
        mEndWeekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        mFirstLine = 7 - mFirstWeekIndex;//日历第一行的天数
        mEndLine = mEndWeekIndex + 1;//日历最后一行，第一天是星期日，所以要加1

        mTriggerYear = mSelectYear = mYear;
        mTriggerMonth = mSelectMonth = mMonth;
        mTriggerDay = mSelectDay = mDay;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int width;
        int height = (int) (mMonthHeight + mWeekHeight + mDayHeight * 6);

        switch (widthMode){
            case MeasureSpec.EXACTLY:
                width = widthSize;
                break;
            case MeasureSpec.AT_MOST:
            default:
                width = (int) mWidth;
                break;
        }

        setMeasuredDimension(width,height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //先绘制选中背景，不然遮挡数字
        if(mTriggerYear == mSelectYear
                && mTriggerMonth == mSelectMonth){
            drawSelectBg(canvas);
        }

        drawMonth(canvas);
        drawWeek(canvas);
        drawDay(canvas);
    }

    private void drawSelectBg(Canvas canvas) {
        float startX,startY;
        float width = mWidth / 7;
        if(mSelectDay <= mFirstLine){
            //点击第一行
            int dayIndex = 7 - (mFirstLine - mSelectDay);
            startX = (dayIndex - 1) * width;
            startY = mMonthHeight + mWeekHeight + mDayHeight;
        }else{
            int dayIndex = (mSelectDay - mFirstLine - 1) % 7;
            int dayRowIndex = (mSelectDay - mFirstLine - dayIndex) / 7 + 2;
            startX = dayIndex * width;
            startY = mMonthHeight + mWeekHeight + dayRowIndex * mDayHeight;
        }

        //计算圆心
        float radiuX = startX + width / 2;
        float radiuY = startY - mDayHeight / 2;
        float radius = mDayHeight / 2 - 10;
        canvas.drawCircle(radiuX,radiuY,radius,mSelecterPaint);
    }

    private void drawMonth(Canvas canvas) {
        String date = getFormatDate(mCurrentDate);

        //测量文字的宽高
        Rect rect = new Rect();
        mMonthPaint.getTextBounds(date,0,date.length(),rect);

        //计算文字开启的x,y值
        float textX = (mWidth - rect.width()) / 2;
        float textY = (mMonthHeight - rect.height()) / 2 + rect.height();

        //绘制日期
        canvas.drawText(date,textX,textY,mMonthPaint);

        //绘制分割线
        mLinePaint.setStrokeWidth(1);
        canvas.drawLine(0,mMonthHeight,mWidth,mMonthHeight,mLinePaint);

        mLinePaint.setStrokeWidth(4);

        //绘制左箭头
        Path pathL = new Path();
        pathL.moveTo(textX - mTextSpec, mArrowSpec);
        pathL.lineTo(textX - mTextSpec - mArrowWith,mMonthHeight / 2);
        pathL.lineTo(textX - mTextSpec, mMonthHeight - mArrowSpec);
        canvas.drawPath(pathL,mLinePaint);

        //绘制右箭头
        Path pathR = new Path();
        pathR.moveTo(textX + rect.width() + mTextSpec, mArrowSpec);
        pathR.lineTo(textX + rect.width() + mTextSpec + mArrowWith, mMonthHeight / 2);
        pathR.lineTo(textX + rect.width() + mTextSpec, mMonthHeight - mArrowSpec);
        canvas.drawPath(pathR,mLinePaint);
    }

    private void drawWeek(Canvas canvas) {
        String[] weekStr = new String[]{"日","一","二","三","四","五","六"};
        float weekWidth = mWidth / 7;
        for(int i = 0;i < weekStr.length;i++){
            drawWeekText(canvas,weekStr[i],weekWidth * i + weekWidth / 2);
        }
    }

    private void drawWeekText(Canvas canvas, String text, float width) {
        Rect rect = new Rect();
        mWeekPaint.getTextBounds(text,0,text.length(),rect);
        float textX = width;
        float textY = mMonthHeight + (mWeekHeight - rect.height()) / 2 + rect.height();

        canvas.drawText(text,textX,textY,mWeekPaint);
    }

    private void drawDay(Canvas canvas) {
        Rect rect = new Rect();
        String text = "1";
        mDayPaint.getTextBounds(text,0,text.length(),rect);

        //计算当前绘制坐标，currentWidth为X轴位置，currentHeight为Y轴位置（基线）
        float dayWidth = mWidth / 7;
        float currentWidth = mFirstWeekIndex * dayWidth - dayWidth / 2;
        float currentHeight = mMonthHeight + mWeekHeight + (mDayHeight - rect.height()) / 2 + rect.height();

        int weekIndex = mFirstWeekIndex;
        for(int i = 0;i < mDayCountOfMonth;i++){
            if(mTriggerYear == mSelectYear
                    && mTriggerMonth == mSelectMonth
                    && (i+1) == mSelectDay){
                mDayPaint.setColor(Color.WHITE);
            }else{
                mDayPaint.setColor(Color.BLACK);
            }

            //星期天的索引为7的倍数，需要换行和绘制分割线
            if(weekIndex > 0 && weekIndex % 7 == 0){
                currentWidth = dayWidth / 2;
                currentHeight = currentHeight + mDayHeight;
                canvas.drawText(i+1+"",currentWidth,currentHeight,mDayPaint);

                //绘制分割线，currentHeight为基线的高度
                float lineHeight = currentHeight - (mDayHeight - rect.height()) / 2 - rect.height();
                mLinePaint.setStrokeWidth(0.5F);
                canvas.drawLine(0,lineHeight,mWidth,lineHeight,mLinePaint);
            }else{
                currentWidth += dayWidth;
                canvas.drawText(i+1+"",currentWidth,currentHeight,mDayPaint);
            }
            weekIndex++;
        }
    }

    private float mStartX = 0,mStartY = 0;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mStartX = event.getX();
                mStartY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                performClick(mStartX,mStartY,event.getX(),event.getY());
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void performClick(float startX, float startY, float endX, float endY) {
        String date = getFormatDate(mCurrentDate);

        Rect rect = new Rect();
        mMonthPaint.getTextBounds(date,0,date.length(),rect);

        float phoneWidth = mWidth;
        float textX = (phoneWidth - rect.width()) / 2;
        float arrowStartY = 10;
        float arrowEndY = mMonthHeight - 10;

        float arrowLeftStartX = textX - mTextSpec - (mArrowWith + 60);
        float arrowLeftEndX = textX - mTextSpec + 60;

        float arrowRightStartX = textX + rect.width() + mTextSpec - 60;
        float arrowRightEndX = textX + rect.width() + mTextSpec + mArrowWith + 60;

        if(startX > arrowLeftStartX
                && startX < arrowLeftEndX
                && startY > arrowStartY
                && startY < arrowEndY
                && endX > arrowLeftStartX
                && endX < arrowLeftEndX
                && endY > arrowStartY
                && endY < arrowEndY){
            prevMonth();//上一个月
        }else if(startX > arrowRightStartX
                && startX < arrowRightEndX
                && startY > arrowStartY
                && startY < arrowEndY
                && endX > arrowRightStartX
                && endX < arrowRightEndX
                && endY > arrowStartY
                && endY < arrowEndY){
            nextMonth();//下一个月
        }else{
            clickDay(startX,startY,endX,endY);
        }
    }

    private void prevMonth(){
        setMonth(-1);
    }

    private void nextMonth(){
        setMonth(1);
    }

    private void setMonth(int prevOrNext){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(mCurrentDate);
        calendar.add(Calendar.MONTH,prevOrNext);

        mDayCountOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);//当月总天数

        //默认一个星期第一天是星球日，索引为0，星期六索引为7，所以要减1
        calendar.set(Calendar.DATE,1);
        mFirstWeekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        calendar.set(Calendar.DATE,mDayCountOfMonth);
        mEndWeekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        mFirstLine = 7 - mFirstWeekIndex;//日历第一行的天数
        mEndLine = mEndWeekIndex + 1;//日历最后一行，第一天是星期日，所以要加1

        mCurrentDate = calendar.getTime();

        mTriggerYear = calendar.get(Calendar.YEAR);
        mTriggerMonth = calendar.get(Calendar.MONTH) + 1;
        mTriggerDay = calendar.get(Calendar.DAY_OF_MONTH);

        invalidate();
    }

    private void clickDay(float startX, float startY, float endX, float endY){
        //判断点击日期有效范围
        if(startY < mMonthHeight + mWeekHeight || endY < mMonthHeight + mWeekHeight){
            return;
        }
        int selectStartDay = 0,selectEndDay = 0;
        float dayWidth = mWidth / 7;

        int startLineX = (int) (startX / dayWidth) + 1;
        int startRowY = (int) ((startY - mMonthHeight - mWeekHeight) / mDayHeight) + 1;

        if(startRowY == 1){
            //点击了空白无效区域
            if(startLineX - 1 < mFirstWeekIndex){
                return;
            }

            selectStartDay = startLineX - mFirstWeekIndex;
        }else{
            selectStartDay = (startRowY - 2) * 7 + mFirstLine + startLineX;
            if(selectStartDay > mDayCountOfMonth){
                return;
            }
        }

        int endLineX = (int) (endX / dayWidth) + 1;
        int endRowY = (int) ((endY - mMonthHeight - mWeekHeight) / mDayHeight) + 1;

        if(endRowY == 1){
            //点击了空白无效区域
            if(endLineX - 1 < mFirstWeekIndex){
                return;
            }

            selectEndDay = endLineX - mFirstWeekIndex;
        }else{
            selectEndDay = (endRowY - 2) * 7 + mFirstLine + endLineX;
            if(selectEndDay > mDayCountOfMonth){
                return;
            }
        }

        if(selectStartDay == selectEndDay){
            mSelectYear = mTriggerYear;
            mSelectMonth = mTriggerMonth;
            mSelectDay = selectStartDay;
            invalidate();
            if(listener != null){
                listener.onSelect(mSelectYear,mSelectMonth,mSelectDay);
            }
        }

    }

    private int getPhoneWidth(Context context){
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if(windowManager != null){
            windowManager.getDefaultDisplay().getMetrics(dm);
        }
        return dm.widthPixels;
    }

    private String getFormatDate(Date date){
        SimpleDateFormat df = new SimpleDateFormat("yyyy年MM月");
        return df.format(date);
    }

    public void setDateSelectListener(DateSelectListener listener) {
        this.listener = listener;
    }

}
