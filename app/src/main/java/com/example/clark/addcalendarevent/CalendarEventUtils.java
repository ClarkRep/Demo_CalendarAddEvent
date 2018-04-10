package com.example.clark.addcalendarevent;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.TimeZone;

/**
 * @description 添加事件到系统日历的工具类
 * @date createTime: 2018/3/28.
 */
public class CalendarEventUtils {

    private static String CALANDER_URL = "content://com.android.calendar/calendars";
    private static String CALANDER_EVENT_URL = "content://com.android.calendar/events";
    private static String CALANDER_REMIDER_URL = "content://com.android.calendar/reminders";

    private static String CALENDARS_NAME = "alpha_test";
    private static String CALENDARS_ACCOUNT_NAME = "alpha";
    private static String CALENDARS_ACCOUNT_TYPE = "com.alpha.test";
    private static String CALENDARS_DISPLAY_NAME = "alpha任务";

    /**
     * 查询是否存在日历账户的数据信息
     */
    private static final String[] EVENT_PROJECTION = new String[]{
            // 0
            CalendarContract.Calendars._ID,
            // 1
            CalendarContract.Calendars.ACCOUNT_NAME,
            // 2
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
    };

    /**
     * The indices for the projection array above.
     */
    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
    private static final int PROJECTION_DISPLAY_NAME_INDEX = 2;


    /**
     * 检查是否有现有存在的账户。存在则返回账户id，否则返回-1
     *
     * @param context
     * @return
     */
    private static int checkCalendarAccount(Context context) {
        String selection = CalendarContract.Calendars.ACCOUNT_NAME + "=?";

        String[] selectionArgs = new String[]{CALENDARS_ACCOUNT_NAME};

        Cursor userCursor = context.getContentResolver().query(Uri.parse(CALANDER_URL), EVENT_PROJECTION, selection, selectionArgs, null);
        try {
            //查询返回空值
            if (userCursor == null) {
                return -1;
            }
            int count = userCursor.getCount();
            //存在现有账户，取第一个账户的id返回
            if (count > 0) {
                userCursor.moveToFirst();
                int callId = userCursor.getInt(PROJECTION_ID_INDEX);
                String accountName = userCursor.getString(PROJECTION_ACCOUNT_NAME_INDEX);
                String displayName = userCursor.getString(PROJECTION_DISPLAY_NAME_INDEX);
                return callId;
            } else {
                return -1;
            }
        } finally {
            if (userCursor != null) {
                userCursor.close();
            }
        }
    }

    /**
     * 添加账户。账户创建成功则返回账户id，否则返回-1
     *
     * @param context
     * @return
     */
    private static long addCalendarAccount(Context context) {
        TimeZone timeZone = TimeZone.getDefault();
        ContentValues value = new ContentValues();
        value.put(CalendarContract.Calendars.NAME, CALENDARS_NAME);

        value.put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDARS_ACCOUNT_NAME);
        value.put(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDARS_ACCOUNT_TYPE);
        value.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDARS_DISPLAY_NAME);
        value.put(CalendarContract.Calendars.VISIBLE, 1);
        value.put(CalendarContract.Calendars.CALENDAR_COLOR, Color.BLUE);
        value.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        value.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        value.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, timeZone.getID());
        value.put(CalendarContract.Calendars.OWNER_ACCOUNT, CALENDARS_ACCOUNT_NAME);
        value.put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 0);

        Uri calendarUri = Uri.parse(CALANDER_URL);
        calendarUri = calendarUri.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDARS_ACCOUNT_NAME)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDARS_ACCOUNT_TYPE)
                .build();

        Uri result = context.getContentResolver().insert(calendarUri, value);
        long id = result == null ? -1 : ContentUris.parseId(result);
        return id;
    }

    /**
     * 获取账户。如果账户不存在则先创建账户，账户存在获取账户id；获取账户成功返回账户id，否则返回-1
     *
     * @param context
     * @return
     */
    private static int checkAndAddCalendarAccount(Context context) {
        int oldId = checkCalendarAccount(context);
        if (oldId >= 0) {
            return oldId;
        } else {
            long addId = addCalendarAccount(context);
            if (addId >= 0) {
                return checkCalendarAccount(context);
            } else {
                return -1;
            }
        }
    }

    /**
     * 获取账户。如果账户不存在则先创建账户，账户存在获取账户id；获取账户成功返回账户id，否则返回-1
     *
     * @param context
     * @param title
     * @param description
     * @param beginTime
     */
    public static int addCalendarEvent(Context context, int eventId, String title, String description, long beginTime, long endTime) {
        // 获取日历账户的id
        int calId = checkAndAddCalendarAccount(context);
        if (calId < 0) {
            // 获取账户id失败直接返回，添加日历事件失败
            return -1;
        }

        //先判断这个事件是否存在
        //如果存在，则更新这个事件
        if (isExistEvent(context, eventId)) {
            updateCalendarEvent(context, calId, eventId, title, description, beginTime, endTime);
            return -1;
        }

        //添加事件
        Uri eventUri = context.getContentResolver().insert(Uri.parse(CALANDER_EVENT_URL),
                getEventData(calId, title, description, beginTime, endTime));

        if (eventUri == null) {
            // 添加日历事件失败直接返回
            return -1;
        }
        //事件提醒的设定
        ContentValues values = new ContentValues();
        //添加事件提醒的id
        values.put(CalendarContract.Reminders.EVENT_ID, ContentUris.parseId(eventUri));
        //提前10分钟有提醒
        values.put(CalendarContract.Reminders.MINUTES, 10);
        //提醒方式
        values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
        //将事件提醒更新到该事件
        Uri uri = context.getContentResolver().insert(Uri.parse(CALANDER_REMIDER_URL), values);
        if (uri == null) {
            // 添加闹钟提醒失败直接返回
            return -1;
        }

        Cursor query = context.getContentResolver().query(eventUri, null, null, null, null);
        try {
            if (query != null && query.getCount() > 0) {
                query.moveToFirst();
                int id = query.getInt(query.getColumnIndex(CalendarContract.Calendars._ID));
                Log.i("haha", "插入的事件id：" + id);
                return id;
            }
        } finally {
            if (query != null) {
                query.close();
            }
        }
        Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show();
        return -1;

    }

    private static void updateCalendarEvent(Context context, int calId, int eventId, String title, String description, long beginTime, long endTime) {

        // 获取日历账户的id
        if (calId < 0) {
            // 获取账户id失败直接返回，添加日历事件失败
            return;
        }
        Log.i("haha", "更新的事件id：" + eventId);

        //先判断这个事件是否存在
        Uri eventUri = ContentUris.withAppendedId(Uri.parse(CALANDER_EVENT_URL), eventId);
        if (eventUri == null) {
            return;
        }
        //更新事件
        int updateResult = context.getContentResolver().update(eventUri, getEventData(calId, title, description, beginTime, endTime), null, null);
        Log.i("haha", "更新事件的返回值：" + updateResult);
        if (updateResult != -1) {
            //更新成功
            Toast.makeText(context, "更新成功", Toast.LENGTH_SHORT).show();
        }
    }

    private static ContentValues getEventData(int calId, String title, String description, long beginTime, long endTime) {
        ContentValues event = new ContentValues();
        //插入账户的id
        event.put(CalendarContract.Events.CALENDAR_ID, calId);
        //事件的标题
        event.put(CalendarContract.Events.TITLE, title);
        //事件的描述
        event.put(CalendarContract.Events.DESCRIPTION, description);
        //设置任务开始时间
        event.put(CalendarContract.Events.DTSTART, beginTime);
        //甚至任务结束时间
        event.put(CalendarContract.Events.DTEND, endTime);
        //设置有闹钟提醒
        event.put(CalendarContract.Events.HAS_ALARM, 1);
        //这个是时区，必须有
        event.put(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Shanghai");

        return event;
    }

    public static void deleteEventRemind(Context context, int eventId) {
        //事件提醒的设定
        ContentValues values = new ContentValues();
        //添加事件提醒的id
        values.put(CalendarContract.Reminders.EVENT_ID, eventId);
        context.getContentResolver().delete(Uri.parse(CALANDER_REMIDER_URL), CalendarContract.Reminders.EVENT_ID + "=" + eventId, null);
    }

    /**
     * 根据设置的title来查找并删除
     *
     * @param context
     * @param taskId
     */
    public static void deleteCalendarEvent(Context context, String taskId) {
        Cursor eventCursor = context.getContentResolver().query(Uri.parse(CALANDER_EVENT_URL), null, null, null, null);
        try {
            //查询返回空值
            if (eventCursor == null) {
                return;
            }
            if (eventCursor.getCount() > 0) {
                //遍历所有事件，找到title跟需要查询的title一样的项
                for (eventCursor.moveToFirst(); !eventCursor.isAfterLast(); eventCursor.moveToNext()) {
                    String syncData1 = eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Events.SYNC_DATA1));
                    if (!TextUtils.isEmpty(taskId) && taskId.equals(syncData1)) {
                        //取得id
                        int id = eventCursor.getInt(eventCursor.getColumnIndex(CalendarContract.Calendars._ID));
                        Uri deleteUri = ContentUris.withAppendedId(Uri.parse(CALANDER_EVENT_URL), id);
                        int rows = context.getContentResolver().delete(deleteUri, null, null);
                        if (rows == -1) {
                            //事件删除失败
                            return;
                        }
                    }
                }
            }
        } finally {
            if (eventCursor != null) {
                eventCursor.close();
            }
        }
    }

    private static boolean isExistEvent(Context context, int eventId) {

        Cursor eventCursor = context.getContentResolver().query(Uri.parse(CALANDER_EVENT_URL), new String[]{CalendarContract.Events._ID}, CalendarContract.Events._ID + "=" + eventId, null, null);
        try {
            //查询返回空值
            if (eventCursor == null) {
                return false;
            }
            if (eventCursor.moveToFirst()) {
                return true;
            }
//            if (eventCursor.getCount() > 0) {
//                //遍历所有事件，找到title跟需要查询的title一样的项
//                for (eventCursor.moveToFirst(); !eventCursor.isAfterLast(); eventCursor.moveToNext()) {
//                    int id = eventCursor.getInt(eventCursor.getColumnIndex(CalendarContract.Events._ID));
//                    if (eventId == id) {
//                        //取得id
//                        Log.i("haha", "isExistEvent()返回的id：" + id);
//                        return true;
//                    }
//                }
//            }
        } finally {
            if (eventCursor != null) {
                eventCursor.close();
            }
        }
        return false;
    }

}
