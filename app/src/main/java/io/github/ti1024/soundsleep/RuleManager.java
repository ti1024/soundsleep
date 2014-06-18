package io.github.ti1024.soundsleep;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.widget.Toast;

public class RuleManager {
    public static final String ACTION_RULE_STATUS_CHANGED = "io.github.ti1024.soundsleep.RULE_STATUS_CHANGED";

    public static void setRuleEnabled(Context context, boolean enabled) {
        Rule rule = Rule.Load(context);
        if (rule.enabled == enabled)
            return;
        rule.enabled = enabled;
        rule.Save(context);
        context.getPackageManager().setComponentEnabledSetting(
                new ComponentName(context, UpdateRuleStatus.class),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
        );
        updateRuleStatus(context, rule);
    }

    public static void setRuleStartSerial(Context context, int startSerial) {
        Rule rule = Rule.Load(context);
        if (rule.startSerial == startSerial)
            return;
        rule.startSerial = startSerial;
        rule.Save(context);
        updateRuleStatus(context, rule);
    }

    public static void setRuleEndSerial(Context context, int endSerial) {
        Rule rule = Rule.Load(context);
        if (rule.endSerial == endSerial)
            return;
        rule.endSerial = endSerial;
        rule.Save(context);
        updateRuleStatus(context, rule);
    }

    public static void setRuleVibrate(Context context, boolean vibrate) {
        Rule rule = Rule.Load(context);
        if (rule.vibrate == vibrate)
            return;
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        boolean updateRingerMode = false;
        if (rule.active) {
            if (audioManager.getRingerMode() == rule.getRuleRingerMode())
                updateRingerMode = true;
        }
        rule.vibrate = vibrate;
        rule.Save(context);
        if (updateRingerMode)
            audioManager.setRingerMode(rule.getRuleRingerMode());
    }

    // Keep in mind: Keep the work of this function minimal!
    // This broadcast receiver can be called very often even when the user is not interacting
    // with our app.  Namely, if the device is configured to use the date and time from
    // the network (android.provider.Settings.Global.AUTO_TIME), TIME_SET broadcast is sent
    // approximately once per minute, at least in the case of my Nexus 5.
    // This is not just me; see also http://stackoverflow.com/q/16684132.
    private static void updateRuleStatus(Context context, Rule rule) {
        if (rule.enabled) {
            Time now = new Time();
            now.setToNow();
            int nowSerial = now.hour * 60 + now.minute;
            boolean active;
            if (rule.startSerial <= rule.endSerial)
                active = nowSerial >= rule.startSerial && nowSerial < rule.endSerial;
            else
                active = nowSerial >= rule.startSerial || nowSerial < rule.endSerial;
            setRuleActive(context, rule, active);
            int nextUpdateSerial = active ? rule.endSerial : rule.startSerial;
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
            setRuleActive(context, rule, false);
        }
    }

    private static void setRuleActive(Context context, Rule rule, boolean active) {
        if (rule.active == active)
            return;
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int toastResId;
        if (active) {
            rule.oldRingerMode = audioManager.getRingerMode();
            audioManager.setRingerMode(rule.getRuleRingerMode());
            toastResId = R.string.rule_activated;
        }
        else {
            if (audioManager.getRingerMode() == rule.getRuleRingerMode())
                audioManager.setRingerMode(rule.oldRingerMode);
            toastResId = R.string.rule_deactivated;
        }
        rule.active = active;
        rule.Save(context);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        Intent intent = new Intent(ACTION_RULE_STATUS_CHANGED);
        localBroadcastManager.sendBroadcast(intent);
        Toast.makeText(context, toastResId, Toast.LENGTH_SHORT).show();
    }

    public static class UpdateRuleStatus extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Rule rule = Rule.Load(context);
            updateRuleStatus(context, rule);
        }
    }
}
