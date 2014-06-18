package io.github.ti1024.soundsleep;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class RuleActivity extends Activity {

    private BroadcastReceiver ruleStatusChangedReceiver = null;

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
        final CheckBox enabledCheckBox = (CheckBox) findViewById(R.id.rule_enabled);
        enabledCheckBox.setChecked(rule.enabled);
        findViewById(R.id.rule_enabled_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enabledCheckBox.toggle();
                RuleManager.setRuleEnabled(RuleActivity.this, enabledCheckBox.isChecked());
            }
        });
        final TextView startTimeText = (TextView) findViewById(R.id.start_time);
        findViewById(R.id.start_time_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog.OnTimeSetListener callback = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        RuleManager.setRuleStartSerial(RuleActivity.this, hour * 60 + minute);
                        startTimeText.setText(formatTime(hour, minute));
                    }
                };
                Rule rule = Rule.Load(RuleActivity.this);
                boolean twentyFourHours = android.text.format.DateFormat.is24HourFormat(RuleActivity.this);
                TimePickerDialog dialog = new TimePickerDialog(RuleActivity.this, callback, rule.startSerial / 60, rule.startSerial % 60, twentyFourHours);
                dialog.setTitle(R.string.start_time);
                dialog.show();
            }
        });
        final TextView endTimeText = (TextView) findViewById(R.id.end_time);
        findViewById(R.id.end_time_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog.OnTimeSetListener callback = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        RuleManager.setRuleEndSerial(RuleActivity.this, hour * 60 + minute);
                        endTimeText.setText(formatTime(hour, minute));
                    }
                };
                Rule rule = Rule.Load(RuleActivity.this);
                boolean twentyFourHours = android.text.format.DateFormat.is24HourFormat(RuleActivity.this);
                TimePickerDialog dialog = new TimePickerDialog(RuleActivity.this, callback, rule.endSerial / 60, rule.endSerial % 60, twentyFourHours);
                dialog.setTitle(R.string.end_time);
                dialog.show();
            }
        });
        final CheckBox vibrateCheckBox = (CheckBox) findViewById(R.id.vibrate);
        vibrateCheckBox.setChecked(rule.vibrate);
        findViewById(R.id.vibrate_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vibrateCheckBox.toggle();
                RuleManager.setRuleVibrate(RuleActivity.this, vibrateCheckBox.isChecked());
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
        TextView startTimeText = (TextView) findViewById(R.id.start_time);
        startTimeText.setText(formatTime(rule.startSerial / 60, rule.startSerial % 60));
        TextView endTimeText = (TextView) findViewById(R.id.end_time);
        endTimeText.setText(formatTime(rule.endSerial / 60, rule.endSerial % 60));
        final TextView statusText = (TextView) findViewById(R.id.status);
        if (rule.active)
            statusText.setText(R.string.rule_status_active);
        else
            statusText.setText(R.string.rule_status_inactive);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        ruleStatusChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Rule rule = Rule.Load(RuleActivity.this);
                if (rule.active)
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
