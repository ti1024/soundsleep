package io.github.ti1024.soundsleep;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import io.github.ti1024.soundsleep.rulemanager.Rule;
import io.github.ti1024.soundsleep.rulemanager.RuleManager;

public class RuleActivity extends Activity {

    private BroadcastReceiver ruleStatusChangedReceiver = null;

    // dateFormat must be in UTC.
    static String formatDay(DateFormat dateFormat, int day) {
        final int MILLIS_PER_DAY = 86400000;
        // Epoch time 3 * MILLIS_PER_DAY is Sunday in UTC.
        return dateFormat.format(new Date((day + 3) * MILLIS_PER_DAY));
    }

    String formatDays(boolean[] days) {
        int count = 0;
        for (int day = 0; day < 7; day++) {
            if (days[day])
                count++;
        }
        if (count == 7)
            return getString(R.string.everyday);
        SimpleDateFormat dayFormat;
        if (count == 1 || count == 6)
            dayFormat = new SimpleDateFormat("cccc");
        else
            dayFormat = new SimpleDateFormat("c");
        dayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (count <= 4) {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (int day = 0; day < 7; day++) {
                if (days[day]) {
                    if (first)
                        first = false;
                    else
                        builder.append(", ");
                    builder.append(formatDay(dayFormat, day));
                }
            }
            return builder.toString();
        }
        else {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (int day = 0; day < 7; day++) {
                if (!days[day]) {
                    if (first)
                        first = false;
                    else
                        builder.append(", ");
                    builder.append(formatDay(dayFormat, day));
                }
            }
            return getString(R.string.days_except, builder.toString());
        }
    }

    String formatTime(int hour, int minute) {
        // We use UTC instead of local timezone because local timezone may have DST
        // and the specified time may not exist in local timezone depending on which day we use.
        DateFormat utcTimeFormat = android.text.format.DateFormat.getTimeFormat(this);
        utcTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcTimeFormat.format(new Date((hour * 60 + minute) * 60000));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule);
        Rule rule = Rule.Load(this);
        CheckBox enabledCheckBox = (CheckBox) findViewById(R.id.rule_enabled);
        enabledCheckBox.setChecked(rule.isEnabled());
        findViewById(R.id.rule_enabled_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox enabledCheckBox = (CheckBox) findViewById(R.id.rule_enabled);
                enabledCheckBox.toggle();
                RuleManager.setRuleEnabled(view.getContext(), enabledCheckBox.isChecked());
            }
        });
        boolean[] days = new boolean[7];
        for (int day = 0; day < 7; day++)
            days[day] = rule.isDayContained(day);
        TextView daysText = (TextView) findViewById(R.id.days);
        daysText.setText(formatDays(days));
        findViewById(R.id.days_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Rule rule = Rule.Load(RuleActivity.this);
                final boolean[] days = new boolean[7];
                for (int day = 0; day < 7; day++)
                    days[day] = rule.isDayContained(day);
                AlertDialog.Builder builder = new AlertDialog.Builder(RuleActivity.this);
                builder.setTitle(R.string.days);
                SimpleDateFormat dayFormat = new SimpleDateFormat("cccc");
                dayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                CharSequence[] dayNames = new CharSequence[7];
                for (int day = 0; day < 7; day++)
                    dayNames[day] = dayFormat.format(new Date((day + 3) * 86400000));
                builder.setMultiChoiceItems(dayNames, days, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        days[which] = isChecked;
                        boolean any = false;
                        for (int day = 0; day < 7; day++) {
                            if (days[day]) {
                                any = true;
                                break;
                            }
                        }
                        ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(any);
                    }
                });
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RuleManager.setRuleDays(RuleActivity.this, days);
                        TextView daysText = (TextView) findViewById(R.id.days);
                        daysText.setText(formatDays(days));
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        findViewById(R.id.start_time_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog.OnTimeSetListener callback = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        RuleManager.setRuleStartSerial(timePicker.getContext(), hour * 60 + minute);
                        TextView startTimeText = (TextView) findViewById(R.id.start_time);
                        startTimeText.setText(formatTime(hour, minute));
                    }
                };
                Context context = view.getContext();
                Rule rule = Rule.Load(context);
                int startSerial = rule.getStartSerial();
                boolean twentyFourHours = android.text.format.DateFormat.is24HourFormat(context);
                TimePickerDialog dialog = new TimePickerDialog(context, callback, startSerial / 60, startSerial % 60, twentyFourHours);
                dialog.setTitle(R.string.start_time);
                dialog.show();
            }
        });
        findViewById(R.id.end_time_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog.OnTimeSetListener callback = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        RuleManager.setRuleEndSerial(timePicker.getContext(), hour * 60 + minute);
                        TextView endTimeText = (TextView) findViewById(R.id.end_time);
                        endTimeText.setText(formatTime(hour, minute));
                    }
                };
                Context context = view.getContext();
                Rule rule = Rule.Load(context);
                int endSerial = rule.getEndSerial();
                boolean twentyFourHours = android.text.format.DateFormat.is24HourFormat(context);
                TimePickerDialog dialog = new TimePickerDialog(context, callback, endSerial / 60, endSerial % 60, twentyFourHours);
                dialog.setTitle(R.string.end_time);
                dialog.show();
            }
        });
        CheckBox vibrateCheckBox = (CheckBox) findViewById(R.id.vibrate);
        vibrateCheckBox.setChecked(rule.getVibrate());
        findViewById(R.id.vibrate_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox vibrateCheckBox = (CheckBox) findViewById(R.id.vibrate);
                vibrateCheckBox.toggle();
                RuleManager.setRuleVibrate(view.getContext(), vibrateCheckBox.isChecked());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set start time and end time fields here instead of onCreate
        // because time format may change while this activity is paused or stopped.
        // It would be better to receive a broadcast for time format changes,
        // but I cannot find a suitable broadcast for this purpose.
        Rule rule = Rule.Load(this);
        int startSerial = rule.getStartSerial();
        TextView startTimeText = (TextView) findViewById(R.id.start_time);
        startTimeText.setText(formatTime(startSerial / 60, startSerial % 60));
        int endSerial = rule.getEndSerial();
        TextView endTimeText = (TextView) findViewById(R.id.end_time);
        endTimeText.setText(formatTime(endSerial / 60, endSerial % 60));
        TextView statusText = (TextView) findViewById(R.id.status);
        if (rule.isActive())
            statusText.setText(R.string.rule_status_active);
        else
            statusText.setText(R.string.rule_status_inactive);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        ruleStatusChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Rule rule = Rule.Load(context);
                TextView statusText = (TextView) findViewById(R.id.status);
                if (rule.isActive())
                    statusText.setText(R.string.rule_status_active);
                else
                    statusText.setText(R.string.rule_status_inactive);
            }
        };
        IntentFilter intentFilter = new IntentFilter(RuleManager.ACTION_RULE_STATUS_CHANGED);
        localBroadcastManager.registerReceiver(ruleStatusChangedReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(ruleStatusChangedReceiver);
        ruleStatusChangedReceiver = null;
        super.onStop();
    }

}
