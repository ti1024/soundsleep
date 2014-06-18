package io.github.ti1024.soundsleep.rulemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateRuleStatus extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Rule rule = Rule.Load(context);
        RuleManager.updateRuleStatus(context, rule);
    }
}
