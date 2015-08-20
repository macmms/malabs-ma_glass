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
import android.view.WindowManager;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardBuilder;
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
public class PickStatusActivity extends Activity {
    private String mActivityController = "";
    private CardScrollView mCardScroller;
    private List< CardBuilder > mCards;
    private GestureDetector mGestureDetector;
    private AudioManager audio;
    private List< WorkOrderStatus > wsObj;
    private List< MaintenanceType > mtObj;
    private List< AssetCategory > acObj;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mCardScroller = new CardScrollView(this);
        mCards = new ArrayList<>();

        if (getIntent().hasExtra(ResultsActivity.SEARCH)) {
            mActivityController = getIntent().getStringExtra(ResultsActivity.SEARCH);
        }

        getStatuses();

        if (mCards.size() == 0) {
            setResult(RESULT_CANCELED);
            finish();
        }

        mCardScroller.setAdapter(new MainAdapter(mCards));

        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                audio.playSoundEffect(Sounds.SUCCESS);
                Intent intent = new Intent();
                if (mActivityController.equals(ResultsActivity.WORK_ORDER)) {
                    intent.putExtra(ResultsActivity.ID, wsObj.get(mCardScroller.getSelectedItemPosition()).getId());
                    intent.putExtra(ResultsActivity.CODE, wsObj.get(mCardScroller.getSelectedItemPosition()).getIntControlID());
                } else if (mActivityController.equals(ResultsActivity.ASSET)) {
                    intent.putExtra(ResultsActivity.ID, mtObj.get(mCardScroller.getSelectedItemPosition()).getId());
                    intent.putExtra(ResultsActivity.CODE, mtObj.get(mCardScroller.getSelectedItemPosition()).getStrName());
                } else if (mActivityController.equals(ResultsActivity.GENERATE)) {
                    intent.putExtra(ResultsActivity.ID, acObj.get(mCardScroller.getSelectedItemPosition()).getId());
                } else if (mActivityController.equals(ResultsActivity.CODE)) {
                    intent.putExtra(ResultsActivity.ID, wsObj.get(mCardScroller.getSelectedItemPosition()).getId());
                }

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

    private void getStatuses() {
        String cardText;
        String cardNote;

        if (mActivityController.equals(ResultsActivity.WORK_ORDER)) {
            FindRequest<WorkOrderStatus> fReqWoS = MainActivity.client.prepareFind(WorkOrderStatus.class);
            fReqWoS.setFields("id, strName, intControlID");
            FindResponse<WorkOrderStatus> fRespWoS = MainActivity.client.find(fReqWoS);

            int listSize = fRespWoS.getTotalObjects();
            wsObj = fRespWoS.getObjects();
            for (int i = 0; listSize > i; i++) {
                if (fRespWoS.getObjects().get(i).getStrName() != null) {
                    cardText = fRespWoS.getObjects().get(i).getStrName();
                } else {
                    cardText = "This status has no name.";
                }

                if (fRespWoS.getObjects().get(i).getIntControlID() == 100) {
                    cardNote = "Pending";
                } else if (fRespWoS.getObjects().get(i).getIntControlID() == 101) {
                    cardNote = "Active";
                } else if (fRespWoS.getObjects().get(i).getIntControlID() == 102) {
                    cardNote = "Closed";
                } else {
                    cardNote = "Draft";
                }

                mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                        .setText(cardText)
                        .setFootnote(cardNote));
            }
        } else if (mActivityController.equals(ResultsActivity.ASSET)) {
            FindRequest<MaintenanceType> fReqMT = MainActivity.client.prepareFind(MaintenanceType.class);
            fReqMT.setFields("id, strName, strColor");
            FindResponse<MaintenanceType> fRespMT = MainActivity.client.find(fReqMT);
            cardNote = "Tap to select this maintenance type.";

            int listSize = fRespMT.getTotalObjects();
            mtObj = fRespMT.getObjects();
            for (int i = 0; listSize > i; i++) {
                if (fRespMT.getObjects().get(i).getStrName() != null) {
                    cardText = "\n" + fRespMT.getObjects().get(i).getStrName();
                } else {
                    cardText = "\nThis maintenance type has no name.";
                }

                Bitmap.Config conf = Bitmap.Config.ARGB_8888;
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                Bitmap bmp = Bitmap.createBitmap(metrics, 640, 320, conf);
                bmp.eraseColor(ResultsActivity.hex2Rgb(fRespMT.getObjects().get(i).getStrColor()));
                mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                        .addImage(bmp)
                        .setText(cardText)
                        .setFootnote(cardNote)
                        .setAttributionIcon(R.drawable.little_logo));
            }
        } else if (mActivityController.equals(ResultsActivity.GENERATE)) {
            FindRequest< AssetCategory > fReqAC = MainActivity.client.prepareFind(AssetCategory.class);
            fReqAC.setFields("id, strName, intParentID, intSysCode");
            FindResponse< AssetCategory > fRespAC = MainActivity.client.find(fReqAC);
            acObj = new ArrayList<>();
            cardNote = "Tap to select this asset category.";

            int listSize = fRespAC.getTotalObjects();
            for (int i = 0; listSize > i; i++) {
                long temp = recurseParent(fRespAC.getObjects().get(i), fRespAC.getObjects().get(i), fRespAC);
                if (temp != 0 && temp != 10) {
                    acObj.add(fRespAC.getObjects().get(i));
                }
            }

            for (int i = 0; acObj.size() > i; i++) {
                long checkSysCode = recurseParent(acObj.get(i), acObj.get(i), fRespAC);
                int image;

                if (acObj.get(i).getStrName() != null) {
                    cardText = acObj.get(i).getStrName();
                } else {
                    cardText = "Nameless category";
                }

                if (checkSysCode == 0 || checkSysCode == 2) {
                    image = R.drawable.default_asset;
                } else if (checkSysCode == 1) {
                    image = R.drawable.default_facility;
                } else if (checkSysCode == 3) {
                    image = R.drawable.default_tool;
                } else if (checkSysCode == 4) {
                    image = R.drawable.default_part;
                } else {
                    image = R.drawable.default_null;
                }

                mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                        .setIcon(image)
                        .setText(cardText)
                        .setFootnote(cardNote));
            }
        } else if (mActivityController.equals(ResultsActivity.CODE)) {
            FindRequest<WorkOrderStatus> fReqWoS = MainActivity.client.prepareFind(WorkOrderStatus.class);
            fReqWoS.setFields("id, strName, intControlID");
            FindResponse<WorkOrderStatus> fRespWoS = MainActivity.client.find(fReqWoS);
            wsObj = new ArrayList<>();
            cardNote = "Tap to select this work order status";

            int listSize = fRespWoS.getTotalObjects();
            for (int i = 0; listSize > i; i++) {
                if (fRespWoS.getObjects().get(i).getIntControlID() == 100) {
                    wsObj.add(fRespWoS.getObjects().get(i));

                    if (fRespWoS.getObjects().get(i).getStrName() != null) {
                        cardText = fRespWoS.getObjects().get(i).getStrName();
                    } else {
                        cardText = "This status has no name.";
                    }

                    mCards.add(new CardBuilder(this, CardBuilder.Layout.MENU)
                            .setText(cardText)
                            .setFootnote(cardNote));
                }
            }
        }

        mCardScroller.setSelection(0);
    }

    private long recurseParent(AssetCategory currentObj, AssetCategory previousObj, FindResponse< AssetCategory > fRespAC) {
        long finalReturn = ResultsActivity.EMPTY;

        if (currentObj.getIntParentID() == null) {
            if (currentObj.getIntSysCode() == null) {
                return ResultsActivity.EMPTY;
            } else {
                return previousObj.getIntSysCode();
            }
        } else {
            for (int i = 0; i < fRespAC.getTotalObjects(); i++) {
                if (fRespAC.getObjects().get(i).getId().equals(currentObj.getIntParentID())) {
                    finalReturn = recurseParent(fRespAC.getObjects().get(i), currentObj, fRespAC);
                    break;
                }
            }
        }

        return finalReturn;
    }
}