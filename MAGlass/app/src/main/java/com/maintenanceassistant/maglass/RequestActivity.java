package com.maintenanceassistant.maglass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;
import com.ma.cmms.api.client.BasicCredentials;
import com.ma.cmms.api.client.MaCmmsClient;
import com.ma.cmms.api.client.dto.Asset;
import com.ma.cmms.api.client.dto.WorkOrder;
import com.ma.cmms.api.client.dto.WorkOrderAsset;
import com.ma.cmms.api.client.dto.WorkOrderPart;
import com.ma.cmms.api.client.dto.WorkOrderStatus;
import com.ma.cmms.api.crud.AddRequest;
import com.ma.cmms.api.crud.AddResponse;
import com.ma.cmms.api.crud.ChangeRequest;
import com.ma.cmms.api.crud.ChangeResponse;
import com.ma.cmms.api.crud.FindByIdRequest;
import com.ma.cmms.api.crud.FindByIdResponse;
import com.ma.cmms.api.crud.FindFilter;
import com.ma.cmms.api.crud.FindRequest;
import com.ma.cmms.api.crud.FindResponse;
import com.maintenanceassistant.maglass.adapters.MainAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Jake on 2015-07-23.
 */
public class RequestActivity extends Activity {
    private CardScrollView mCardScroller;
    private List<CardBuilder> mCards;
    private GestureDetector mGestureDetector;
    private Long mID;
    private Long mSysCode;
    private WorkOrder woObj = new WorkOrder();
    private String status = "None.";
    private boolean ready = false;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        mCardScroller = new CardScrollView(this);
        mCards = new ArrayList<>();

        mID = getIntent().getLongExtra(ResultsActivity.ID, ResultsActivity.EMPTY);
        mSysCode = getIntent().getLongExtra(ResultsActivity.CODE, ResultsActivity.EMPTY);

        pickStatus();
    }

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);

        //Create a base listener for generic gestures
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    if (ready) {
                        complete();
                    }
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


    private void displaySpeechRecognizer(String text) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(ResultsActivity.EXTRA_PROMPT, text);
        startActivityForResult(intent, ResultsActivity.SPEECH_REQUEST);
    }

    private void pickStatus() {
        Intent intent = new Intent(this, PickStatusActivity.class);
        intent.putExtra(ResultsActivity.SEARCH, ResultsActivity.ASSET);
        startActivityForResult(intent, ResultsActivity.PICK_STATUS);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ResultsActivity.PICK_STATUS && resultCode == RESULT_OK) {
            long id = data.getLongExtra(ResultsActivity.ID, ResultsActivity.EMPTY);
            status = data.getStringExtra(ResultsActivity.CODE);

            FindRequest<WorkOrderStatus> fReqWoS = MainActivity.client.prepareFind(WorkOrderStatus.class);
            fReqWoS.setFields("id");
            fReqWoS.setMaxObjects(1);
            FindFilter controlFilter = new FindFilter();
            controlFilter.setQl("intControlID = ?");
            List< Object > params = Arrays.asList((Object) 100);
            controlFilter.setParameters(params);
            fReqWoS.setFilters(Arrays.asList(controlFilter));
            FindResponse<WorkOrderStatus> fRespWoS = MainActivity.client.find(fReqWoS);

            woObj.setIntWorkOrderStatusID(fRespWoS.getObjects().get(0).getId());
            woObj.setIntSiteID(419608L);
            woObj.setIntMaintenanceTypeID(id);

            displaySpeechRecognizer("Summarize the work order");
        } else if (requestCode == ResultsActivity.SPEECH_REQUEST && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            woObj.setStrDescription(results.get(0));

            FindByIdRequest<Asset> fReqA = MainActivity.client.prepareFindById(Asset.class);
            fReqA.setFields("strName");
            fReqA.setId(mID);
            FindByIdResponse<Asset> fRespA = MainActivity.client.findById(fReqA);

            String cardText = "<font color=\"yellow\"><b>Maintenance Type:</b></font> " + status + "<br />"
                    + "<font color=\"yellow\"><b>For:</b></font> " + fRespA.getObject().getStrName() + "<br />"
                    + "<font color=\"yellow\"><b>Summary:</b></font> " + woObj.getStrDescription();

            mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                    .setText(Html.fromHtml(cardText))
                    .setAttributionIcon(R.drawable.little_logo));

            mCardScroller.setAdapter(new MainAdapter(mCards));

            ready = true;

            Toast.makeText(getApplicationContext(), "Tap to confirm", Toast.LENGTH_LONG).show();

            // Handle the TAP event.
            mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (ready) {
                        ready = false;
                        complete();
                    }
                }
            });

            mGestureDetector = createGestureDetector(this);
            setContentView(mCardScroller);
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "Cancelled", Toast.LENGTH_SHORT).show();
            finish();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void complete() {
        AddRequest< WorkOrder > aReqWo = MainActivity.client.prepareAdd(WorkOrder.class);
        aReqWo.setFields("id, strDescription, intMaintenanceTypeID");
        aReqWo.setObject(woObj);
        AddResponse< WorkOrder > aRespWo = MainActivity.client.add(aReqWo);

        AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        if (aRespWo.getError() == null) {
            ChangeRequest< WorkOrder > cReqWo = MainActivity.client.prepareChange(WorkOrder.class);
            cReqWo.setChangeFields("intRequestedByUserID");
            aRespWo.getObject().setIntRequestedByUserID(MainActivity.SUPERUSER);
            cReqWo.setObject(aRespWo.getObject());
            ChangeResponse< WorkOrder > cRespWo = MainActivity.client.change(cReqWo);

            if(mSysCode != 4) {
                AddRequest< WorkOrderAsset > aReqWoA = MainActivity.client.prepareAdd(WorkOrderAsset.class);
                WorkOrderAsset WoA = new WorkOrderAsset();
                WoA.setIntAssetID(mID);
                WoA.setIntWorkOrderID(aRespWo.getObject().getId());
                aReqWoA.setObject(WoA);
                AddResponse< WorkOrderAsset > aRespWoA = MainActivity.client.add(aReqWoA);
                if (aRespWoA.getError() == null) {
                    audio.playSoundEffect(Sounds.SUCCESS);
                    Toast.makeText(getApplicationContext(), "Successfully requested", Toast.LENGTH_SHORT).show();
                } else {
                    audio.playSoundEffect(Sounds.ERROR);
                    Toast.makeText(getApplicationContext(), "An error occurred", Toast.LENGTH_SHORT).show();
                }
            } else {
                AddRequest<WorkOrderPart> aReqWoP = MainActivity.client.prepareAdd(WorkOrderPart.class);
                WorkOrderPart WoP = new WorkOrderPart();
                WoP.setIntPartID(mID);
                WoP.setIntWorkOrderID(aRespWo.getObject().getId());
                aReqWoP.setObject(WoP);
                AddResponse< WorkOrderPart > aRespWoA = MainActivity.client.add(aReqWoP);
                if (aRespWoA.getError() == null) {
                    audio.playSoundEffect(Sounds.SUCCESS);
                    Toast.makeText(getApplicationContext(), "Successfully requested", Toast.LENGTH_SHORT).show();
                } else {
                    audio.playSoundEffect(Sounds.ERROR);
                    Toast.makeText(getApplicationContext(), "An error occurred", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            audio.playSoundEffect(Sounds.ERROR);
            Toast.makeText(getApplicationContext(), "An error occurred", Toast.LENGTH_SHORT).show();
            finish();
        }

        finish();
    }
}