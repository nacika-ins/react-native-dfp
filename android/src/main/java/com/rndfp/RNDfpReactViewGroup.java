package com.rndfp;

import android.content.Context;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.views.view.ReactViewGroup;

/**
 * Created by jamesmorton on 2018-01-30.
 */

public class RNDfpReactViewGroup extends ReactViewGroup {
    private String adUnitId = null;
    private ReadableMap customTargeting = null;

    RNDfpReactViewGroup(Context context) {
        super(context);
    }

    public void setAdUnitId(String adUnitId) {
        this.adUnitId = adUnitId;
    }

    public String getAdUnitId() {
        return this.adUnitId;
    }

    public void setCustomTargeting(ReadableMap customTargeting) {
        this.customTargeting = customTargeting;
    }

    public ReadableMap getCustomTargeting() {
        return this.customTargeting;
    }
}
