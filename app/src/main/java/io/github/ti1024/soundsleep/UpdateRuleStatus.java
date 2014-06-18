package io.github.ti1024.soundsleep;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.widget.Toast;

public class UpdateRuleStatus extends BroadcastReceiver {
    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "UpdateRuleStatus";

    public static final String ACTION_RULE_STATUS_CHANGED = "io.github.ti1024.soundsleep.RULE_STATUS_CHANGED";

    // Keep in mind: Keep the work of this function minimal!
    // This broadcast receiver can be called very often even when the user is not interacting
    // with our app.  Namely, if the device is configured to use the date and time from
    // the network (android.provider.Settings.Global.AUTO_TIME), TIME_SET broadcast is sent
    // approximately once per minute, at least in the case of my Nexus 5.
    // This is not just me; see also http://stackoverflow.com/q/16684132.
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
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        Intent intent = new Intent(ACTION_RULE_STATUS_CHANGED);
        localBroadcastManager.sendBroadcast(intent);
        Toast.makeText(context, R.string.rule_activated, Toast.LENGTH_SHORT).show();
    }

    private static void deactivateRule(Context context, Rule rule) {
        if (!rule.active)
            return;
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getRingerMode() == rule.getRuleRingerMode())
            audioManager.setRingerMode(rule.oldRingerMode);
        rule.active = false;
        rule.Save(context);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        Intent intent = new Intent(ACTION_RULE_STATUS_CHANGED);
        localBroadcastManager.sendBroadcast(intent);
        Toast.makeText(context, R.string.rule_deactivated, Toast.LENGTH_SHORT).show();
    }
}
