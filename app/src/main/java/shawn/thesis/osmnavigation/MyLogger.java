package shawn.thesis.osmnavigation;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Shawn on 7/9/2016.
 *
 */
public class MyLogger {

    private static final MyLogger instance = new MyLogger();

    public static final String filePath = "sdcard/turn_log.file";
    private Calendar calendar;
    private TimeZone tz;
    private SimpleDateFormat sdf;

    private File logFile = null;

    private MyLogger(){

        calendar = Calendar.getInstance();
        tz = TimeZone.getTimeZone("UTC+02:00");
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        createNewFile();
    }

    public static MyLogger getInstance(){
        return instance;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public String getDateCurrentTimeZone() {
        try{
            Long timestamp = System.currentTimeMillis();

//            Calendar calendar = Calendar.getInstance();
//            TimeZone tz = TimeZone.getTimeZone("UTC+02:00");
            calendar.setTimeInMillis(timestamp);
            calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date currenTimeZone = (Date) calendar.getTime();
            return sdf.format(currenTimeZone);
        }catch (Exception e) {
        }
        return "";
    }

    public void appendLog(String text)
    {
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(getDateCurrentTimeZone() + " : " + text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void createNewFile() {
        if (!isExternalStorageWritable()) return;

        logFile = new File(filePath);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
