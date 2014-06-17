package io.github.ti1024.soundsleep;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.text.format.Time;
import android.widget.Toast;

public class UpdateRuleStatus extends BroadcastReceiver {
    private static final String TAG = "UpdateRuleStatus";

    public static void updateRuleStatus(Context context, Rule rule) {
        if (rule.enabled) {
            Time now = new Time();
            now.setToNow();
            int nowSerial = now.hour * 60 + now.minute;
            boolean active;
            if (rule.startSerial <= rule.endSerial)
                active = nowSerial >= rule.startSerial && nowSerial < rule.endSerial;
            else
                active = nowSerial >= rule.startSerial || nowSerial < rule.endSerial;
            int nextUpdateSerial;
            if (active) {
                activateRule(context, rule);
                nextUpdateSerial = rule.endSerial;
            }
            else {
                deactivateRule(context, rule);
                nextUpdateSerial = rule.startSerial;
            }
            Time nextUpdate = new Time(now);
            if (nowSerial >= nextUpdateSerial)
                nextUpdate.monthDay++;
            nextUpdate.hour = nextUpdateSerial / 60;
            nextUpdate.minute = nextUpdateSerial % 60;
            nextUpdate.second = 0;
            long nextUpdateMillis = nextUpdate.toMillis(true); // ignoreDst=true means "Do not trust isDst field"
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent nextIntent = new Intent(context, UpdateRuleStatus.class);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, nextIntent, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextUpdateMillis, alarmIntent);
            else
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextUpdateMillis, alarmIntent);
        }
        else {
            deactivateRule(context, rule);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Rule rule = Rule.Load(context);
        updateRuleStatus(context, rule);
    }

    private static void activateRule(Context context, Rule rule) {
        if (rule.active)
            return;
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        rule.oldRingerMode = audioManager.getRingerMode();
        audioManager.setRingerMode(rule.getRuleRingerMode());
        rule.active = true;
        rule.Save(context);
        Toast.makeText(context, "Sound Sleep rule has been activated.", Toast.LENGTH_SHORT).show();
    }

    private static void deactivateRule(Context context, Rule rule) {
        if (!rule.active)
            return;
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getRingerMode() == rule.getRuleRingerMode())
            audioManager.setRingerMode(rule.oldRingerMode);
        rule.active = false;
        rule.Save(context);
        Toast.makeText(context, "Sound Sleep rule has been deactivated.", Toast.LENGTH_SHORT).show();
    }
}
