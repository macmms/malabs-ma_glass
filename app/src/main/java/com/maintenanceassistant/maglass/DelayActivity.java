package com.maintenanceassistant.maglass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.ma.cmms.api.client.dto.AssetCategory;
import com.ma.cmms.api.client.dto.MaintenanceType;
import com.ma.cmms.api.client.dto.WorkOrderStatus;
import com.ma.cmms.api.crud.FindRequest;
import com.ma.cmms.api.crud.FindResponse;
import com.maintenanceassistant.maglass.adapters.MainAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jake Uskoski on 2015-07-21.
 */
public class DelayActivity extends Activity {
    private CardScrollView mCardScroller;
    private View mView;
    private GestureDetector mGestureDetector;
    private AudioManager audio;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        String text = "Blank Entry";
        String note = "Blank Entry";

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mCardScroller = new CardScrollView(this);

        if (getIntent().hasExtra(ResultsActivity.TEXT)) {
            text = getIntent().getStringExtra(ResultsActivity.TEXT);
        }
        if (getIntent().hasExtra(ResultsActivity.NOTE)) {
            note = getIntent().getStringExtra(ResultsActivity.NOTE);
        }

        mView = buildView(text, note);
        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return 1;
            }
            @Override
            public Object getItem(int position) {
                return mView;
            }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mView;
            }
            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });

        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                audio.playSoundEffect(Sounds.SUCCESS);
                Intent intent = new Intent();
                intent.putExtra(ResultsActivity.CODE, "Success");

                setResult(RESULT_OK, intent);
                finish();
            }
        });

        mGestureDetector = createGestureDetector(this);
        setContentView(mCardScroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);

        //Create a base listener for generic gestures
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    // do something on one finger tap
                    return true;
                } else if (gesture == Gesture.TWO_TAP) {
                    // do something on two finger tap
                    return true;
                } else if (gesture == Gesture.SWIPE_RIGHT) {
                    // do something on right (forward) swipe
                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    // do something on left (backwards) swipe
                    return true;
                } else if (gesture == Gesture.SWIPE_DOWN) {
                    setResult(RESULT_CANCELED);
                    audio.playSoundEffect(Sounds.ERROR);
                    finish();
                }
                return false;
            }
        });

        gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
            @Override
            public void onFingerCountChanged(int previousCount, int currentCount) {
                // do something on finger count changes
            }
        });

        gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
            @Override
            public boolean onScroll(float displacement, float delta, float velocity) {
                // do something on scrolling
                return true;
            }
        });

        return gestureDetector;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }

    private View buildView(String text, String note) {
        return new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText("Tap to Describe Asset")
                .setFootnote("This will begin speech recognition.")
                .getView();
    }
}