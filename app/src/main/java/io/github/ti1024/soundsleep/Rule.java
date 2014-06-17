package io.github.ti1024.soundsleep;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;

public class Rule {
    public boolean enabled;
    public int startSerial;
    public int endSerial;
    public boolean vibrate;
    public boolean active;
    public int oldRingerMode;

    private static final String PREFERENCES_FILE_NAME = "Rule";
    private static final String ENABLED_PREFERENCES_KEY = "Enabled";
    private static final String START_SERIAL_PREFERENCES_KEY = "StartSerial";
    private static final String END_SERIAL_PREFERENCES_KEY = "EndSerial";
    private static final String VIBRATE_PREFERENCES_KEY = "Vibrate";
    private static final String ACTIVE_PREFERENCES_KEY = "Active";
    private static final String OLD_RINGER_MODE_PREFERENCES_KEY = "OldRingerMode";

    /**
     * <p>Saves this rule into a preferences file.</p>
     * <p>The context argument is used only to call Context.getSharedPreferences.
     * This means that the exact choice of the context does not matter
     * as long as it belongs to the current application.
     * See http://stackoverflow.com/a/11567825.</p>
    */
    public void Save(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(ENABLED_PREFERENCES_KEY, enabled);
        editor.putInt(START_SERIAL_PREFERENCES_KEY, startSerial);
        editor.putInt(END_SERIAL_PREFERENCES_KEY, endSerial);
        editor.putBoolean(VIBRATE_PREFERENCES_KEY, vibrate);
        editor.putBoolean(ACTIVE_PREFERENCES_KEY, active);
        if (active)
            editor.putInt(OLD_RINGER_MODE_PREFERENCES_KEY, oldRingerMode);
        else
            editor.remove(OLD_RINGER_MODE_PREFERENCES_KEY);
        editor.remove("RingerMode");
        editor.commit();
    }

    public static Rule Load(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        Rule rule = new Rule();
        rule.enabled = pref.getBoolean(ENABLED_PREFERENCES_KEY, false);
        rule.startSerial = pref.getInt(START_SERIAL_PREFERENCES_KEY, 22 * 60);
        rule.endSerial = pref.getInt(END_SERIAL_PREFERENCES_KEY, 6 * 60);
        rule.vibrate = pref.getBoolean(VIBRATE_PREFERENCES_KEY, false);
        rule.active = pref.getBoolean(ACTIVE_PREFERENCES_KEY, false);
        if (rule.active)
            rule.oldRingerMode = pref.getInt(OLD_RINGER_MODE_PREFERENCES_KEY, AudioManager.RINGER_MODE_NORMAL);
        return rule;
    }

    public int getRuleRingerMode() {
        return vibrate ? AudioManager.RINGER_MODE_VIBRATE : AudioManager.RINGER_MODE_SILENT;
    }
}
