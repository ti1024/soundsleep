package io.github.ti1024.soundsleep;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class RuleActivity extends Activity {

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
                boolean isChecked = enabledCheckBox.isChecked();
                Rule rule = Rule.Load(RuleActivity.this);
                rule.enabled = isChecked;
                rule.Save(RuleActivity.this);
                UpdateRuleStatus.updateRuleStatus(RuleActivity.this, rule);
            }
        });
        final TextView startTimeText = (TextView) findViewById(R.id.start_time);
        findViewById(R.id.start_time_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog.OnTimeSetListener callback = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        Rule rule = Rule.Load(RuleActivity.this);
                        rule.startSerial = hour * 60 + minute;
                        startTimeText.setText(formatTime(hour, minute));
                        rule.Save(RuleActivity.this);
                        UpdateRuleStatus.updateRuleStatus(RuleActivity.this, rule);
                    }
                };
                Rule rule = Rule.Load(RuleActivity.this);
                boolean twentyFourHours = android.text.format.DateFormat.is24HourFormat(RuleActivity.this);
                TimePickerDialog dialog = new TimePickerDialog(RuleActivity.this, callback, rule.startSerial / 60, rule.startSerial % 60, twentyFourHours);
                dialog.setTitle(R.string.set_start_time);
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
                        Rule rule = Rule.Load(RuleActivity.this);
                        rule.endSerial = hour * 60 + minute;
                        endTimeText.setText(formatTime(hour, minute));
                        rule.Save(RuleActivity.this);
                        UpdateRuleStatus.updateRuleStatus(RuleActivity.this, rule);
                    }
                };
                Rule rule = Rule.Load(RuleActivity.this);
                boolean twentyFourHours = android.text.format.DateFormat.is24HourFormat(RuleActivity.this);
                TimePickerDialog dialog = new TimePickerDialog(RuleActivity.this, callback, rule.endSerial / 60, rule.endSerial % 60, twentyFourHours);
                dialog.setTitle(R.string.set_end_time);
                dialog.show();
            }
        });
        final CheckBox vibrateCheckBox = (CheckBox) findViewById(R.id.vibrate);
        vibrateCheckBox.setChecked(rule.vibrate);
        findViewById(R.id.vibrate_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vibrateCheckBox.toggle();
                boolean isChecked = vibrateCheckBox.isChecked();
                Rule rule = Rule.Load(RuleActivity.this);
                AudioManager audioManager = (AudioManager) RuleActivity.this.getSystemService(Context.AUDIO_SERVICE);
                boolean updateRingerMode = false;
                if (rule.active) {
                    if (audioManager.getRingerMode() == rule.getRuleRingerMode())
                        updateRingerMode = true;
                }
                rule.vibrate = isChecked;
                rule.Save(RuleActivity.this);
                if (updateRingerMode)
                    audioManager.setRingerMode(rule.getRuleRingerMode());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set start time and end time fields here instead of onCreate
        // because time format may change while this activity is paused or stopped.
        // It would be better to receive a broadcast for time format changes,
        // but I cannot find a suitable broadcast for this purpose.
        Rule rule = Rule.Load(this);
        TextView startTimeText = (TextView) findViewById(R.id.start_time);
        startTimeText.setText(formatTime(rule.startSerial / 60, rule.startSerial % 60));
        TextView endTimeText = (TextView) findViewById(R.id.end_time);
        endTimeText.setText(formatTime(rule.endSerial / 60, rule.endSerial % 60));
    }
}
