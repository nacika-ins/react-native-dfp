package com.rndfp;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.doubleclick.AppEventListener;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.doubleclick.PublisherAdView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RNDfpBannerViewManager extends SimpleViewManager<RNDfpReactViewGroup> implements AppEventListener {
  private static final String TAG = RNDfpBannerViewManager.class.getSimpleName();

  public static final String REACT_CLASS = "RNDFPBanner";

  public static final String PROP_CUSTOM_TARGETING = "customTargeting";
  public static final String PROP_AD_SIZES = "adSizes";
  public static final String PROP_DIMENSIONS = "dimensions";
  public static final String PROP_BANNER_SIZE = "bannerSize";
  public static final String PROP_AD_UNIT_ID = "adUnitID";
  public static final String PROP_TEST_DEVICE_ID = "testDeviceID";

  private String testDeviceID = null;

  public enum Events {
    EVENT_SIZE_CHANGE("onSizeChange"),
    EVENT_RECEIVE_AD("onAdViewDidReceiveAd"),
    EVENT_ERROR("onDidFailToReceiveAdWithError"),
    EVENT_WILL_PRESENT("onAdViewWillPresentScreen"),
    EVENT_WILL_DISMISS("onAdViewWillDismissScreen"),
    EVENT_DID_DISMISS("onAdViewDidDismissScreen"),
    EVENT_WILL_LEAVE_APP("onAdViewWillLeaveApplication"),
    EVENT_ADMOB_EVENT_RECEIVED("onAdmobDispatchAppEvent");

    private final String mName;

    Events(final String name) {
      mName = name;
    }

    @Override
    public String toString() {
      return mName;
    }
  }

  private ThemedReactContext mThemedReactContext;
  private RCTEventEmitter mEventEmitter;

  @Override
  public String getName() {
    return REACT_CLASS;
  }


  @Override
  public void onAppEvent(String name, String info) {
    String message = String.format("Received app event (%s, %s)", name, info);
    Log.d("PublisherAdBanner", message);
    WritableMap event = Arguments.createMap();
    event.putString(name, info);
    mEventEmitter.receiveEvent(viewID, Events.EVENT_ADMOB_EVENT_RECEIVED.toString(), event);
  }

  @Override
  protected RNDfpReactViewGroup createViewInstance(ThemedReactContext themedReactContext) {
    mThemedReactContext = themedReactContext;
    mEventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);
    RNDfpReactViewGroup view = new RNDfpReactViewGroup(themedReactContext);
    attachNewAdView(view);
    return view;
   }

  int viewID = -1;
  protected void attachNewAdView(final RNDfpReactViewGroup view) {
    final PublisherAdView adView = new PublisherAdView(mThemedReactContext);
    adView.setAppEventListener(this);
    // destroy old AdView if present
    PublisherAdView oldAdView = (PublisherAdView) view.getChildAt(0);
    view.removeAllViews();
    if (oldAdView != null) oldAdView.destroy();
    view.addView(adView);
    attachEvents(view);
  }

  protected void attachEvents(final RNDfpReactViewGroup view) {
    viewID = view.getId();
    final PublisherAdView adView = (PublisherAdView) view.getChildAt(0);
    adView.setAdListener(new AdListener() {
      @Override
      public void onAdLoaded() {
        int width = adView.getAdSize().getWidthInPixels(mThemedReactContext);
        int height = adView.getAdSize().getHeightInPixels(mThemedReactContext);
        int left = adView.getLeft();
        int top = adView.getTop();
        adView.measure(width, height);
        adView.layout(left, top, left + width, top + height);
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_RECEIVE_AD.toString(), null);
        triggerSizeChange(view, adView);
      }

      @Override
      public void onAdFailedToLoad(int errorCode) {
        WritableMap event = Arguments.createMap();
        switch (errorCode) {
          case PublisherAdRequest.ERROR_CODE_INTERNAL_ERROR:
            event.putString("error", "ERROR_CODE_INTERNAL_ERROR");
            break;
          case PublisherAdRequest.ERROR_CODE_INVALID_REQUEST:
            event.putString("error", "ERROR_CODE_INVALID_REQUEST");
            break;
          case PublisherAdRequest.ERROR_CODE_NETWORK_ERROR:
            event.putString("error", "ERROR_CODE_NETWORK_ERROR");
            break;
          case PublisherAdRequest.ERROR_CODE_NO_FILL:
            event.putString("error", "ERROR_CODE_NO_FILL");
            break;
        }

        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_ERROR.toString(), event);
      }

      @Override
      public void onAdOpened() {
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_WILL_PRESENT.toString(), null);
      }

      @Override
      public void onAdClosed() {
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_WILL_DISMISS.toString(), null);
      }

      @Override
      public void onAdLeftApplication() {
        mEventEmitter.receiveEvent(view.getId(), Events.EVENT_WILL_LEAVE_APP.toString(), null);
      }
    });
  }

  protected void triggerSizeChange(RNDfpReactViewGroup viewGroup, PublisherAdView adView) {
    WritableMap event = Arguments.createMap();
    event.putDouble("width", adView.getAdSize().getWidth());
    event.putDouble("height", adView.getAdSize().getHeight());
    mEventEmitter.receiveEvent(viewGroup.getId(), Events.EVENT_SIZE_CHANGE.toString(), event);
  }

  @Override
  @Nullable
  public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
    MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
    for (Events event : Events.values()) {
      builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
    }
    return builder.build();
  }

  @ReactProp(name = PROP_AD_SIZES)
  public void setAdSizes(final RNDfpReactViewGroup view, final ReadableArray adSizesProp) {
    if (adSizesProp != null) {
      ArrayList<AdSize> adSizesArrayList = new ArrayList<AdSize>();
      for (Object obj : adSizesProp.toArrayList()) {
        if (obj instanceof String) {
          AdSize adSize = getAdSizeFromString((String)obj);
          adSizesArrayList.add(adSize);
        }

        if (obj instanceof HashMap) {
          HashMap dimensions = (HashMap)obj;
          if (dimensions.containsKey("width") && dimensions.containsKey("height")) {

            AdSize adSize = new AdSize(
              (int) Double.parseDouble(dimensions.get("width").toString()),
              (int) Double.parseDouble(dimensions.get("height").toString())
            );
            adSizesArrayList.add(adSize);
          }
        }
      }

      AdSize[] adSizes = adSizesArrayList.toArray(new AdSize[adSizesArrayList.size()]);

      attachNewAdView(view);
      PublisherAdView newAdView = (PublisherAdView) view.getChildAt(0);
      newAdView.setAdSizes(adSizes);

      // send measurements to js to style the AdView in react
      triggerSizeChange(view, newAdView);

      loadAd(view, newAdView);
    }
  }

  @ReactProp(name = PROP_DIMENSIONS)
  public void setDimensions(final RNDfpReactViewGroup view, final ReadableMap dimensions) {
    if (dimensions != null && dimensions.hasKey("width") && !dimensions.isNull("width") && dimensions.hasKey("height") && !dimensions.isNull("height")) {
      AdSize adSize = new AdSize(dimensions.getInt("width"), dimensions.getInt("height"));
      AdSize[] adSizes = new AdSize[1];
      adSizes[0] = adSize;

      attachNewAdView(view);
      PublisherAdView newAdView = (PublisherAdView) view.getChildAt(0);
      newAdView.setAdSizes(adSizes);

      // send measurements to js to style the AdView in react
      WritableMap event = Arguments.createMap();
      event.putDouble("width", adSize.getWidth());
      event.putDouble("height", adSize.getHeight());
      mEventEmitter.receiveEvent(view.getId(), Events.EVENT_SIZE_CHANGE.toString(), event);

      loadAd(view, newAdView);
    }
  }

  @ReactProp(name = PROP_BANNER_SIZE)
  public void setBannerSize(final RNDfpReactViewGroup view, final String sizeString) {
    if (sizeString != null && !sizeString.isEmpty()) {
      AdSize adSize = getAdSizeFromString(sizeString);
      AdSize[] adSizes = new AdSize[1];
      adSizes[0] = adSize;

      attachNewAdView(view);
      PublisherAdView newAdView = (PublisherAdView) view.getChildAt(0);
      newAdView.setAdSizes(adSizes);

      // send measurements to js to style the AdView in react
      int width;
      int height;
      WritableMap event = Arguments.createMap();
      if (adSize == AdSize.SMART_BANNER) {
        width = (int) PixelUtil.toDIPFromPixel(adSize.getWidthInPixels(mThemedReactContext));
        height = (int) PixelUtil.toDIPFromPixel(adSize.getHeightInPixels(mThemedReactContext));
      }
      else {
        width = adSize.getWidth();
        height = adSize.getHeight();
      }
      event.putDouble("width", width);
      event.putDouble("height", height);
      mEventEmitter.receiveEvent(view.getId(), Events.EVENT_SIZE_CHANGE.toString(), event);

      loadAd(view, newAdView);
    }
  }

  @ReactProp(name = PROP_CUSTOM_TARGETING)
  public void setCustomTargeting(final RNDfpReactViewGroup view, final ReadableMap customTargeting) {
    view.setCustomTargeting(customTargeting);
    PublisherAdView adView = (PublisherAdView) view.getChildAt(0);
    loadAd(view, adView);
  }

  @ReactProp(name = PROP_AD_UNIT_ID)
  public void setAdUnitID(final RNDfpReactViewGroup view, final String adUnitID) {
    view.setAdUnitId(adUnitID);
    PublisherAdView adView = (PublisherAdView) view.getChildAt(0);
    loadAd(view, adView);
  }

  @ReactProp(name = PROP_TEST_DEVICE_ID)
  public void setPropTestDeviceID(final RNDfpReactViewGroup view, final String testDeviceID) {
    this.testDeviceID = testDeviceID;
    PublisherAdView adView = (PublisherAdView) view.getChildAt(0);
    loadAd(view, adView);
  }

  private void loadAd(RNDfpReactViewGroup view, final PublisherAdView adView) {
    if (adView.getAdSizes() != null && view.getAdUnitId() != null && view.getCustomTargeting() != null) {
      if (adView.getAdUnitId() == null) {
        adView.setAdUnitId(view.getAdUnitId());
      }

      PublisherAdRequest.Builder adRequestBuilder = new PublisherAdRequest.Builder();
      if (testDeviceID != null){
        if (testDeviceID.equals("EMULATOR")) {
          adRequestBuilder = adRequestBuilder.addTestDevice(PublisherAdRequest.DEVICE_ID_EMULATOR);
        } else {
          adRequestBuilder = adRequestBuilder.addTestDevice(testDeviceID);
        }
      }

      for (Map.Entry<String, Object> target : view.getCustomTargeting().toHashMap().entrySet()) {
        adRequestBuilder.addCustomTargeting(target.getKey(), target.getValue().toString());
      }

      PublisherAdRequest adRequest = adRequestBuilder.build();
      adView.loadAd(adRequest);
    }
  }

  private AdSize getAdSizeFromString(String adSize) {
    switch (adSize) {
      case "banner":
        return AdSize.BANNER;
      case "largeBanner":
        return AdSize.LARGE_BANNER;
      case "mediumRectangle":
        return AdSize.MEDIUM_RECTANGLE;
      case "fullBanner":
        return AdSize.FULL_BANNER;
      case "leaderBoard":
        return AdSize.LEADERBOARD;
      case "smartBannerPortrait":
        return AdSize.SMART_BANNER;
      case "smartBannerLandscape":
        return AdSize.SMART_BANNER;
      case "smartBanner":
        return AdSize.SMART_BANNER;
      default:
        return AdSize.BANNER;
    }
  }
}
