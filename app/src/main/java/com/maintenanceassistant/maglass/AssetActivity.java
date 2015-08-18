package com.maintenanceassistant.maglass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;
import com.ma.cmms.api.client.dto.Asset;
import com.ma.cmms.api.client.dto.MeterReadingUnit;
import com.ma.cmms.api.crud.FindByIdRequest;
import com.ma.cmms.api.crud.FindByIdResponse;
import com.ma.cmms.api.crud.FindRequest;
import com.ma.cmms.api.crud.FindResponse;
import com.maintenanceassistant.maglass.adapters.MainAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Jake Uskoski on 2015-07-23.
 */
public class AssetActivity extends Activity {
    private CardScrollView mCardScroller;
    private List<CardBuilder> mCards;
    private GestureDetector mGestureDetector;
    private long mID;
    private long sysCode;

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

        getAsset();

        if (mCards.size() == 0){
            finish();
        }

        mCardScroller.setAdapter(new MainAdapter(mCards));

        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openOptionsMenu();
            }
        });

        mGestureDetector = createGestureDetector(this);
        setContentView(mCardScroller);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu){
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId ==  Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.asset_menu, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId ==  Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.view_more:
                    viewMore();
                    break;
                case R.id.related_work_orders:
                    seeWorkOrders();
                    break;
                case R.id.related_scheduled_maintenance:
                    seeScheduledMaintenance();
                    break;
                case R.id.work_order_request:
                    workOrderRequest();
                    break;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);

        //Create a base listener for generic gestures
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    openOptionsMenu();
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

    private void viewMore() {
        Intent viewIntent = new Intent(this, ViewMoreActivity.class);
        viewIntent.putExtra(ResultsActivity.SEARCH, ResultsActivity.ASSET);
        viewIntent.putExtra(ResultsActivity.ID, mID);
        startActivity(viewIntent);
    }

    private void seeWorkOrders() {
        Intent resultsIntent = new Intent(this, ResultsActivity.class);
        resultsIntent.putExtra(ResultsActivity.SEARCH, ResultsActivity.WORK_ORDER);
        resultsIntent.putExtra(ResultsActivity.ID, mID);
        resultsIntent.putExtra(ResultsActivity.CODE, sysCode);
        startActivity(resultsIntent);
    }

    private void seeScheduledMaintenance() {
        Intent resultsIntent = new Intent(this, ResultsActivity.class);
        resultsIntent.putExtra(ResultsActivity.SEARCH, ResultsActivity.SCH_MAINTENANCE);
        resultsIntent.putExtra(ResultsActivity.ID, mID);
        resultsIntent.putExtra(ResultsActivity.CODE, sysCode);
        startActivity(resultsIntent);
    }

    private void workOrderRequest() {
        Intent requestIntent = new Intent(this, RequestActivity.class);
        requestIntent.putExtra(ResultsActivity.ID, mID);
        requestIntent.putExtra(ResultsActivity.CODE, sysCode);
        startActivity(requestIntent);
    }

    private void getAsset(){
        String cardText;
        String cardNote;
        FindByIdRequest<Asset> fReqA = MainActivity.client.prepareFindById(Asset.class);
        fReqA.setId(mID);
        fReqA.setFields("strName, strCode, bolIsOnline, intSuperCategorySysCode, dv_intCategoryID, dv_intLastMeterReadingUnitID, cf_getLatestReadingsFor, cf_intDefaultImageFileID");
        FindByIdResponse<Asset> fRespA = MainActivity.client.findById(fReqA);

        if (fRespA.getError() == null) {

            sysCode = fRespA.getObject().getIntSuperCategorySysCode();


            cardText = "<font color=\"yellow\"><b>Name:</b></font> ";
            if (fRespA.getObject().getStrName() != null) {
                cardText += fRespA.getObject().getStrName() + "<br />";
            } else {
                cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
            }

            cardText += "<font color=\"yellow\"><b>Category:</b></font> ";
            if (fRespA.getObject().getExtraFields().get("dv_intCategoryID") != null) {
                cardText += fRespA.getObject().getExtraFields().get("dv_intCategoryID") + "<br />";
            } else {
                cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
            }

            if (fRespA.getObject().getBolIsOnline() != null) {
                cardNote = (fRespA.getObject().getBolIsOnline() == 1L) ? "<font color=\"green\">Online</font>" : "<font color=\"red\"><i>Offline</i></font>";
            } else {
                cardNote = "";
            }

            int defaultImage;
            if (sysCode == 1) {
                defaultImage = R.drawable.default_facility;
            } else if (sysCode == 2) {
                defaultImage = R.drawable.default_asset;
            } else if (sysCode == 3) {
                defaultImage = R.drawable.default_tool;
            } else if (sysCode == 4) {
                defaultImage = R.drawable.default_part;
            } else {
                defaultImage = R.drawable.default_null;
            }

            mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                    .setText(Html.fromHtml(cardText))
                    .setFootnote(Html.fromHtml(cardNote))
                    .addImage(defaultImage)
                    .setAttributionIcon(R.drawable.little_logo));

            if (sysCode != 4) {
                if (fRespA.getObject().getExtraFields().get("cf_getLatestReadingsFor") != null) {
                    cardText = "<font color=\"yellow\"><b>Last Readings:</b></font><br />";
                    LinkedHashMap<String, ArrayList> tempMap = (LinkedHashMap<String, ArrayList>) fRespA.getObject().getExtraFields().get("cf_getLatestReadingsFor");
                    ArrayList<LinkedHashMap> readings = tempMap.get("returnObjects");

                    FindRequest<MeterReadingUnit> fReqMRU = MainActivity.client.prepareFind(MeterReadingUnit.class);
                    fReqMRU.setFields("id, strName");
                    FindResponse<MeterReadingUnit> fRespMRU = MainActivity.client.find(fReqMRU);

                    if (fRespMRU.getError() != null) {
                        AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                        audio.playSoundEffect(Sounds.ERROR);
                        Toast.makeText(getApplicationContext(), "An error occurred", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    if (readings.size() == 0) {
                        cardText += "<font color=\"gray\"><i>No readings available.</i></font>";
                    } else {
                        for (int i = 0; i < readings.size(); i++) {
                            int flag = 0;
                            for (int j = 0; j < fRespMRU.getTotalObjects(); j++) {
                                int temp = (int) readings.get(i).get("intMeterReadingUnitsID");
                                long otherTemp = fRespMRU.getObjects().get(j).getId();
                                if (temp == otherTemp) {
                                    flag = j;
                                    break;
                                }
                            }
                            cardText += readings.get(i).get("dblMeterReading") + " " + fRespMRU.getObjects().get(flag).getStrName();
                            if (i != readings.size() - 1) cardText += "<br />";
                        }
                    }

                    mCards.add(new CardBuilder(this, CardBuilder.Layout.COLUMNS)
                            .setText(Html.fromHtml(cardText))
                            .addImage(defaultImage)
                            .setAttributionIcon(R.drawable.little_logo));
                }
            }
        } else {
            AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            audio.playSoundEffect(Sounds.ERROR);
            Toast.makeText(getApplicationContext(), "An error occurred", Toast.LENGTH_SHORT).show();
            finish();
        }

        mCardScroller.setSelection(0);
    }
}
