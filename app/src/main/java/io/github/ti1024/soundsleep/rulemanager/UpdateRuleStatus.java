package io.github.ti1024.soundsleep.rulemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UpdateRuleStatus extends BroadcastReceiver {

    private static final String TAG = "rulemanager.UpdateRuleStatus";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast " + intent.toString());
        Rule rule = Rule.Load(context);
        RuleManager.updateRuleStatus(context, rule);
    }

}
