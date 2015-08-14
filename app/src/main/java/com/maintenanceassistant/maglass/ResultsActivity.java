package com.maintenanceassistant.maglass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.text.Html;
import android.util.DisplayMetrics;
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
import com.ma.cmms.api.SimpleRequest;
import com.ma.cmms.api.batch.BatchRequest;
import com.ma.cmms.api.batch.BatchResponse;
import com.ma.cmms.api.batch.TxMode;
import com.ma.cmms.api.client.dto.Asset;
import com.ma.cmms.api.client.dto.MaintenanceType;
import com.ma.cmms.api.client.dto.Priority;
import com.ma.cmms.api.client.dto.ScheduledMaintenance;
import com.ma.cmms.api.client.dto.ScheduledMaintenanceAsset;
import com.ma.cmms.api.client.dto.ScheduledMaintenancePart;
import com.ma.cmms.api.client.dto.Stock;
import com.ma.cmms.api.client.dto.WorkOrderAsset;
import com.ma.cmms.api.client.dto.WorkOrderPart;
import com.ma.cmms.api.crud.ChangeRequest;
import com.ma.cmms.api.crud.ChangeResponse;
import com.ma.cmms.api.crud.FindFilter;
import com.maintenanceassistant.maglass.adapters.MainAdapter;

import com.ma.cmms.api.client.BasicCredentials;
import com.ma.cmms.api.client.MaCmmsClient;
import com.ma.cmms.api.client.dto.WorkOrder;
import com.ma.cmms.api.client.dto.WorkOrderStatus;
import com.ma.cmms.api.crud.FindRequest;
import com.ma.cmms.api.crud.FindResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by Jake Uskoski on 2015-07-10.
 */
public class ResultsActivity extends Activity{
    public static final String EXTRA_PROMPT = "android.speech.extra.PROMPT";
    public static final int SPEECH_REQUEST = 0;
    public static final int PICK_STATUS = 1;
    public static final int SCAN_QR = 2;
    public static final int EMPTY = -1;
    public static final String SEARCH = "search";
    public static final String ID = "id";
    public static final String CODE = "code";
    public static final String IMAGE = "image";
    public static final String BOOL = "bool";
    public static final String WORK_ORDER = "Work Orders";
    public static final String SCH_MAINTENANCE = "Scheduled Maintenance";
    public static final String ASSET = "Asset";
    public static final String GENERATE = "generate";
    private String mActivityController;
    private CardScrollView mCardScroller;
    private List< CardBuilder > mCards;
    private GestureDetector mGestureDetector;
    private int mPosition = 0;
    private long mID;
    private long mSysCode;
    private List< WorkOrder > woObj = new ArrayList<>();
    private List< ScheduledMaintenance > smObj = new ArrayList<>();
    private List< Integer > imageList = new ArrayList<>();
    private List< Integer > boolList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        mCardScroller = new CardScrollView(this);
        mCards = new ArrayList<>();

        if (getIntent().hasExtra(SEARCH)) {
            mActivityController = getIntent().getStringExtra(SEARCH);
        }

        mSysCode = getIntent().getLongExtra(CODE, EMPTY);
        mID = getIntent().getLongExtra(ID, EMPTY);

        if(WORK_ORDER.equals(mActivityController)) {
            getWorkOrders();
        } else if (SCH_MAINTENANCE.equals(mActivityController)) {
            getScheduledMaintenance();
        }

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
            if (WORK_ORDER.equals(mActivityController)) {
                getMenuInflater().inflate(R.menu.find_results_work_order, menu);
            } else if (SCH_MAINTENANCE.equals(mActivityController)) {
                getMenuInflater().inflate(R.menu.find_results_sch_maintenance, menu);
            }
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId ==  Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.view_more:
                    viewMore(mActivityController);
                    break;
                case R.id.see_tasks:
                    seeTasks(mActivityController);
                    break;
                case R.id.change_work_order_status:
                    mPosition = mCardScroller.getSelectedItemPosition();
                    pickStatus();
                    break;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
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
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
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
                } else if (gesture == Gesture.SWIPE_DOWN){
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

    public void viewMore(String activityController){
        Intent resultsIntent = new Intent(this, ViewMoreActivity.class);
        resultsIntent.putExtra(SEARCH, activityController);
        resultsIntent.putExtra(ID, getObjId(mActivityController));
        resultsIntent.putExtra(IMAGE, imageList.get(mCardScroller.getSelectedItemPosition()));
        resultsIntent.putExtra(BOOL, boolList.get(mCardScroller.getSelectedItemPosition()));
        startActivity(resultsIntent);
    }

    public void seeTasks(String activityController){
        Intent resultsIntent = new Intent(this, SeeTasksActivity.class);
        resultsIntent.putExtra(SEARCH, activityController);
        resultsIntent.putExtra(ID, getObjId(mActivityController));
        startActivity(resultsIntent);
    }

    public Long getObjId(String activityController){
        if (activityController.equals(WORK_ORDER)){
            return woObj.get(mCardScroller.getSelectedItemPosition()).getId();
        } else {
            return smObj.get(mCardScroller.getSelectedItemPosition()).getId();
        }
    }

    private void displaySpeechRecognizer(String text) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(EXTRA_PROMPT, text);
        startActivityForResult(intent, SPEECH_REQUEST);
    }

    private void pickStatus() {
        Intent intent = new Intent(this, PickStatusActivity.class);
        intent.putExtra(SEARCH, WORK_ORDER);
        startActivityForResult(intent, PICK_STATUS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        WorkOrder obj;
        if (requestCode == PICK_STATUS && resultCode == RESULT_OK) {
            long id = data.getLongExtra(ID, EMPTY);
            long code = data.getLongExtra(CODE, EMPTY);

            obj = woObj.get(mPosition);
            obj.setIntWorkOrderStatusID(id);

            if (code == 102) {
                displaySpeechRecognizer("Completion Notes (Mandatory)");
            } else {
                ChangeRequest<WorkOrder> cReqWo = MainActivity.client.prepareChange(WorkOrder.class);
                cReqWo.setObject(obj);
                cReqWo.setChangeFields("intWorkOrderStatusID");
                ChangeResponse<WorkOrder> cRespWo = MainActivity.client.change(cReqWo);

                AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

                if (cRespWo.getError() == null) {
                    mCardScroller = new CardScrollView(this);
                    mCards = new ArrayList<>();

                    audio.playSoundEffect(Sounds.SUCCESS);
                    Toast.makeText(getApplicationContext(), "Change was successful", Toast.LENGTH_LONG).show();

                    getWorkOrders();
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
                } else {
                    audio.playSoundEffect(Sounds.ERROR);
                    Toast.makeText(getApplicationContext(), "An error occurred", Toast.LENGTH_LONG).show();
                }
            }
        } else if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String tempData = results.get(0);

            obj = woObj.get(mPosition);
            obj.setDtmDateCompleted(new Date());
            obj.setStrCompletionNotes(tempData);

            ChangeRequest<WorkOrder> cReqWo = MainActivity.client.prepareChange(WorkOrder.class);
            cReqWo.setObject(obj);
            cReqWo.setChangeFields("dtmDateCompleted, strCompletionNotes, intWorkOrderStatusID");

            ChangeResponse<WorkOrder> cRespWoT = MainActivity.client.change(cReqWo);

            AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

            if (cRespWoT.getError() == null) {
                mCardScroller = new CardScrollView(this);
                mCards = new ArrayList<>();

                getWorkOrders();
                if (mCards.size() == 0) {
                    finish();
                }

                mCardScroller.setAdapter(new MainAdapter(mCards));

                audio.playSoundEffect(Sounds.SUCCESS);
                Toast.makeText(getApplicationContext(), "Work Order Closed", Toast.LENGTH_LONG).show();

                // Handle the TAP event.
                mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        openOptionsMenu();
                    }
                });

                mGestureDetector = createGestureDetector(this);
                setContentView(mCardScroller);

            } else {
                audio.playSoundEffect(Sounds.ERROR);
                Toast.makeText(getApplicationContext(), "An error occurred", Toast.LENGTH_LONG).show();
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "Cancelled", Toast.LENGTH_LONG).show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getWorkOrders(){
        boolean flag;
        woObj = new ArrayList<>();
        imageList = new ArrayList<>();
        boolList = new ArrayList<>();
        FindRequest< WorkOrderStatus > fReqWoS;
        FindResponse< WorkOrderStatus > WOStatus;
        FindRequest< WorkOrder > fReqWo;
        FindResponse< WorkOrder > fRespWo;
        FindRequest< MaintenanceType > fReqMT;
        FindResponse< MaintenanceType > fRespMT;
        FindRequest< WorkOrderAsset > fReqWoA;
        FindResponse< WorkOrderAsset > fRespWoA;
        FindRequest< WorkOrderPart > fReqWoP;
        FindResponse< WorkOrderPart > fRespWoP;
        FindRequest< Stock > fReqS;
        FindResponse< Stock > fRespS;
        FindRequest< Asset > fReqA;
        FindResponse< Asset > fRespA;
        FindRequest< Priority > fReqP;
        FindResponse< Priority > fRespP;

        // 0
        fReqWoS = MainActivity.client.prepareFind(WorkOrderStatus.class);
        fReqWoS.setFields("id, intControlID");
        FindFilter pendingActiveQlFilter = new FindFilter();
        pendingActiveQlFilter.setQl("intControlID = ?");
        List< Object > params = Arrays.asList((Object) 101);
        pendingActiveQlFilter.setParameters(params);
        fReqWoS.setFilters(Arrays.asList(pendingActiveQlFilter));
        // 1
        fReqWo = MainActivity.client.prepareFind(WorkOrder.class);
        fReqWo.setFields("id, intWorkOrderStatusID, dtmSuggestedCompletionDate, intMaintenanceTypeID, strDescription, intPriorityID, dv_intPriorityID, dv_intWorkOrderStatusID");
        // 2
        fReqMT = MainActivity.client.prepareFind(MaintenanceType.class);
        fReqMT.setFields("id, strColor");
        // 3
        fReqWoA = MainActivity.client.prepareFind(WorkOrderAsset.class);
        fReqWoA.setFields("id, intAssetID, intWorkOrderID");
        // 4
        fReqWoP = MainActivity.client.prepareFind(WorkOrderPart.class);
        fReqWoP.setFields("intStockID, intPartID, intWorkOrderID");
        // 5
        fReqS = MainActivity.client.prepareFind(Stock.class);
        fReqS.setFields("id, intAssetID");
        // 6
        fReqA = MainActivity.client.prepareFind(Asset.class);
        fReqA.setFields("id, intSuperCategorySysCode, cf_intDefaultImageFileID");
        // 7
        fReqP = MainActivity.client.prepareFind(Priority.class);
        fReqP.setFields("id, intOrder");

        BatchRequest bReq = MainActivity.client.prepareBatch();
        bReq.setTxMode(TxMode.SINGLE_TRANSACTION);
        List< SimpleRequest > batchList = new ArrayList<>();

        batchList.add(fReqWoS);
        batchList.add(fReqWo);
        batchList.add(fReqMT);
        batchList.add(fReqWoA);
        batchList.add(fReqWoP);
        batchList.add(fReqS);
        batchList.add(fReqA);
        batchList.add(fReqP);
        bReq.setRequests(batchList);

        BatchResponse bResp = MainActivity.client.batch(bReq);

        WOStatus = (FindResponse< WorkOrderStatus >) bResp.getResponses().get(0);
        fRespWo = (FindResponse< WorkOrder >) bResp.getResponses().get(1);
        fRespMT = (FindResponse< MaintenanceType >) bResp.getResponses().get(2);
        fRespWoA = (FindResponse< WorkOrderAsset >) bResp.getResponses().get(3);
        fRespWoP = (FindResponse< WorkOrderPart >) bResp.getResponses().get(4);
        fRespS = (FindResponse< Stock >) bResp.getResponses().get(5);
        fRespA = (FindResponse< Asset >) bResp.getResponses().get(6);
        fRespP = (FindResponse< Priority >) bResp.getResponses().get(7);

        if (fRespWo.getError() == null) {
            int listSize = fRespWo.getTotalObjects();
            for (int i = 0; listSize > i; i++) {
                flag = false;

                for (int j = 0; j < WOStatus.getTotalObjects(); j++) {
                    if (fRespWo.getObjects().get(i).getIntWorkOrderStatusID().equals(WOStatus.getObjects().get(j).getId())) {
                        flag = true;
                        break;
                    }
                }

                if (mID != EMPTY) {
                    if (flag) {
                        if (mSysCode != 4) {
                            if (fRespWoA != null) {
                                int count = 0;
                                for (int j = 0; j < fRespWoA.getTotalObjects(); j++) {
                                    if (fRespWoA.getObjects() != null && fRespWoA.getObjects().get(j) != null && fRespWoA.getObjects().get(j).getIntAssetID() != null && fRespWoA.getObjects().get(j).getIntWorkOrderID() != null) {
                                        if (fRespWoA.getObjects().get(j).getIntAssetID().equals(mID) && fRespWoA.getObjects().get(j).getIntWorkOrderID().equals(fRespWo.getObjects().get(i).getId())) {
                                            count += 1;
                                        }
                                    }
                                }
                                if (count == 0) flag = false;
                            } else flag = false;
                        } else {
                            if (fRespWoP != null) {
                                boolean check = false;
                                for (int j = 0; j < fRespWoP.getTotalObjects(); j++) {
                                    if (fRespWoP.getObjects().get(j).getIntWorkOrderID().equals(fRespWo.getObjects().get(i).getId())) {
                                        if (fRespWoP.getObjects().get(j).getIntStockID() != null) {
                                            for (int k = 0; k < fRespS.getTotalObjects(); k++) {
                                                if (fRespS.getObjects().get(k).getId() != null && fRespS.getObjects().get(k).getIntAssetID().equals(mID) && fRespWoP.getObjects().get(j).getIntStockID().equals(fRespS.getObjects().get(k).getId())) {
                                                    check = true;
                                                }
                                            }
                                        } else if (fRespWoP.getObjects().get(j).getIntPartID() != null && fRespWoP.getObjects().get(j).getIntPartID().equals(mID)) {
                                            check = true;
                                        }
                                    }
                                }
                                if (!check) flag = false;
                            } else flag = false;
                        }
                    }
                }

                if (flag) {
                    woObj.add(fRespWo.getObjects().get(i));
                    imageList.add(EMPTY);
                    boolList.add(EMPTY);
                }
            }

            WorkOrder temp;
            for (int i = 1; i < woObj.size(); i++)
            {
                for (int j = i; j > 0; j--)
                {
                    long id1 = EMPTY;
                    long id2 = EMPTY;
                    long comp1 = fRespP.getTotalObjects() + 1;
                    long comp2 = fRespP.getTotalObjects() + 1;

                    if (woObj.get(j).getIntPriorityID() != null) {
                        id1 = woObj.get(j).getIntPriorityID();
                    }
                    if (woObj.get(j - 1).getIntPriorityID() != null) {
                        id2 = woObj.get(j - 1).getIntPriorityID();
                    }

                    for (int k = 0; k < fRespP.getTotalObjects(); k++) {
                        if (fRespP.getObjects().get(k).getId().equals(id1)) comp1 = fRespP.getObjects().get(k).getIntOrder();
                        if (fRespP.getObjects().get(k).getId().equals(id2)) comp2 = fRespP.getObjects().get(k).getIntOrder();
                    }

                    if (comp1 == comp2) {
                        if (woObj.get(j).getDtmSuggestedCompletionDate() != null) {
                            if (woObj.get(j).getDtmSuggestedCompletionDate().before(new Date())) {
                                comp1 = 0;
                            } else {
                                comp1 = 1;
                            }
                        } else {
                            comp1 = 1;
                        }
                        if (woObj.get(j - 1).getDtmSuggestedCompletionDate() != null) {
                            if (woObj.get(j - 1).getDtmSuggestedCompletionDate().before(new Date())) {
                                comp2 = 0;
                            } else {
                                comp2 = 1;
                            }
                        } else {
                            comp2 = 1;
                        }
                    }

                    if (comp1 < comp2) {
                        temp = woObj.get(j);
                        woObj.set(j, woObj.get(j - 1));
                        woObj.set(j - 1, temp);
                    }
                }
            }

            for (int i = 0; i < woObj.size(); i++) {
                String subheading;
                String heading;
                String cardText;
                String cardNote;

                if (woObj.get(i).getStrDescription() != null) {
                    cardText = "<big><b>" + woObj.get(i).getStrDescription() + "</b></big><br />";
                } else {
                    cardText = "<font color=\"gray\"><big><i>This work order has no description.</i></big></font><br />";
                }

                cardNote = "<font color=\"yellow\"><b>Status:</b></font> ";
                if (woObj.get(i).getExtraFields().get("dv_intWorkOrderStatusID") != null) {
                    cardNote += woObj.get(i).getExtraFields().get("dv_intWorkOrderStatusID");
                } else {
                    cardNote += "<font color=\"gray\"><i>Not entered.</i></font>";
                }

                if (woObj.get(i).getDtmSuggestedCompletionDate() != null) {
                    cardText += "<font color=\"yellow\"><b>Complete by:</b></font> " + woObj.get(i).getDtmSuggestedCompletionDate() + "";
                    if (woObj.get(i).getDtmSuggestedCompletionDate().before(new Date())) {
                        subheading = "<font color=\"red\"><b>Overdue.</b></font>";
                    } else {
                        subheading = "";
                    }
                } else {
                    cardText += "<font color=\"yellow\"><b>Complete by:</b></font> <font color=\"gray\"><i>No suggested completion date.</i></font>";
                    subheading = "";
                }

                if (woObj.get(i).getExtraFields().get("dv_intPriorityID") != null) {
                    heading = "<font color=\"yellow\"><b>Priority:</b></font> " +woObj.get(i).getExtraFields().get("dv_intPriorityID");
                } else {
                    heading = "<font color=\"yellow\"><b>Priority:</b></font> <font color=\"gray\"><i>Not entered.</i></font>";
                }

                Drawable image = null;
                if (mID != EMPTY) {
                    for (int j = 0; j < fRespA.getTotalObjects(); j++) {
                        if (fRespA.getObjects().get(j).getId().equals(mID)) {
                            if (notNegOne(fRespA.getObjects().get(j).getExtraFields().get("cf_intDefaultImageFileID").toString())) {
                                image = MainActivity.LoadImageFromWebOperations("https://juskoski.masandbox.com/fileDownload/?f=" + fRespA.getObjects().get(j).getExtraFields().get("cf_intDefaultImageFileID"));
                                imageList.set(i, Integer.parseInt(fRespA.getObjects().get(j).getExtraFields().get("cf_intDefaultImageFileID").toString()));
                                boolList.set(i, 1);
                            } else {
                                if (fRespA.getObjects().get(j).getIntSuperCategorySysCode() == 1) {
                                    image = getResources().getDrawable(R.drawable.default_facility);
                                    imageList.set(i, R.drawable.default_facility);
                                    boolList.set(i, 0);
                                } else if (fRespA.getObjects().get(j).getIntSuperCategorySysCode() == 2) {
                                    image = getResources().getDrawable(R.drawable.default_asset);
                                    imageList.set(i, R.drawable.default_asset);
                                    boolList.set(i, 0);
                                } else if (fRespA.getObjects().get(j).getIntSuperCategorySysCode() == 3) {
                                    image = getResources().getDrawable(R.drawable.default_tool);
                                    imageList.set(i, R.drawable.default_tool);
                                    boolList.set(i, 0);
                                } else {
                                    image = getResources().getDrawable(R.drawable.default_part);
                                    imageList.set(i, R.drawable.default_part);
                                    boolList.set(i, 0);
                                }
                            }
                        }
                    }
                }

                if (nullOrDefault(image) && fRespWoA != null && fRespWoA.getError() == null && fRespWoA.getTotalObjects() != 0) {
                    for (int j = 0; j < fRespWoA.getTotalObjects(); j++) {
                        if (fRespWoA.getObjects().get(j).getIntWorkOrderID().equals(woObj.get(i).getId())) {
                            for (int k = 0; k < fRespA.getTotalObjects(); k++) {
                                if (fRespA.getObjects().get(k).getId().equals(fRespWoA.getObjects().get(j).getIntAssetID())) {
                                    if (notNegOne(fRespA.getObjects().get(k).getExtraFields().get("cf_intDefaultImageFileID").toString())) {
                                        image = MainActivity.LoadImageFromWebOperations("https://juskoski.masandbox.com/fileDownload/?f=" + fRespA.getObjects().get(k).getExtraFields().get("cf_intDefaultImageFileID"));
                                        imageList.set(i, Integer.parseInt(fRespA.getObjects().get(k).getExtraFields().get("cf_intDefaultImageFileID").toString()));
                                        boolList.set(i, 1);
                                    } else if (image == null) {
                                        if (fRespA.getObjects().get(j).getIntSuperCategorySysCode() == 1) {
                                            image = getResources().getDrawable(R.drawable.default_facility);
                                            imageList.set(i, R.drawable.default_facility);
                                            boolList.set(i, 0);
                                        } else if (fRespA.getObjects().get(j).getIntSuperCategorySysCode() == 2) {
                                            image = getResources().getDrawable(R.drawable.default_asset);
                                            imageList.set(i, R.drawable.default_asset);
                                            boolList.set(i, 0);
                                        } else if (fRespA.getObjects().get(j).getIntSuperCategorySysCode() == 3) {
                                            image = getResources().getDrawable(R.drawable.default_tool);
                                            imageList.set(i, R.drawable.default_tool);
                                            boolList.set(i, 0);
                                        }
                                    }
                                }
                            }
                        }
                        if (!nullOrDefault(image)) {
                            break;
                        }
                    }
                }

                if (nullOrDefault(image) && fRespWoP.getTotalObjects() != 0) {
                    for (int j = 0; j < fRespWoP.getTotalObjects(); j++) {
                        if (fRespWoP.getObjects().get(j).getIntWorkOrderID().equals(woObj.get(i).getId())) {
                            for (int k = 0; k < fRespS.getTotalObjects(); k++) {
                                if (fRespS.getObjects().get(k).getId().equals(fRespWoP.getObjects().get(j).getIntStockID())) {
                                    for (int l = 0; l < fRespA.getTotalObjects(); l++) {
                                        if (fRespA.getObjects().get(l).getId().equals(fRespS.getObjects().get(k).getIntAssetID())) {
                                            if (notNegOne(fRespA.getObjects().get(l).getExtraFields().get("cf_intDefaultImageFileID").toString())) {
                                                image = MainActivity.LoadImageFromWebOperations("https://juskoski.masandbox.com/fileDownload/?f=" + fRespA.getObjects().get(l).getExtraFields().get("cf_intDefaultImageFileID"));
                                                imageList.set(i, Integer.parseInt(fRespA.getObjects().get(l).getExtraFields().get("cf_intDefaultImageFileID").toString()));
                                                boolList.set(i, 1);
                                                break;
                                            } else if (image == null) {
                                                image = getResources().getDrawable(R.drawable.default_part);
                                                imageList.set(i, R.drawable.default_part);
                                                boolList.set(i, 0);
                                            }
                                        }
                                    }
                                }
                                if (!nullOrDefault(image)) break;
                            }
                            if (nullOrDefault(image) && fRespWoP.getObjects().get(j).getIntPartID() != null) {
                                for(int k = 0; k < fRespA.getTotalObjects(); k++) {
                                    if (fRespA.getObjects().get(k).getId().equals(fRespWoP.getObjects().get(j).getIntPartID())) {
                                        if (notNegOne(fRespA.getObjects().get(k).getExtraFields().get("cf_intDefaultImageFileID").toString())) {
                                            image = MainActivity.LoadImageFromWebOperations("https://juskoski.masandbox.com/fileDownload/?f=" + fRespA.getObjects().get(k).getExtraFields().get("cf_intDefaultImageFileID"));
                                            imageList.set(i, Integer.parseInt(fRespA.getObjects().get(k).getExtraFields().get("cf_intDefaultImageFileID").toString()));
                                            boolList.set(i, 1);
                                        } else if (image == null) {
                                            image = getResources().getDrawable(R.drawable.default_part);
                                            imageList.set(i, R.drawable.default_part);
                                            boolList.set(i, 0);
                                        }
                                    }
                                }
                            }
                        }
                        if (!nullOrDefault(image)) break;
                    }
                }

                if (image == null) {
                    image = this.getResources().getDrawable(R.drawable.default_null);
                    imageList.set(i, R.drawable.default_null);
                    boolList.set(i, 0);
                }

                if (woObj.get(i).getIntMaintenanceTypeID() != null) {
                    for (int j = 0; j < fRespMT.getTotalObjects(); j++) {
                        if (woObj.get(i).getIntMaintenanceTypeID().equals(fRespMT.getObjects().get(j).getId())) {
                            Bitmap.Config conf = Bitmap.Config.ARGB_8888;
                            DisplayMetrics metrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(metrics);
                            Bitmap bmp = Bitmap.createBitmap(metrics, 640, 320, conf);
                            bmp.eraseColor(hex2Rgb(fRespMT.getObjects().get(j).getStrColor()));

                            mCards.add(new CardBuilder(this, CardBuilder.Layout.AUTHOR)
                                    .setIcon(image)
                                    .setHeading(Html.fromHtml(heading))
                                    .setSubheading(Html.fromHtml(subheading))
                                    .setText(Html.fromHtml(cardText))
                                    .setFootnote(Html.fromHtml(cardNote))
                                    .addImage(bmp));

                            break;
                        }
                    }
                } else {
                    mCards.add(new CardBuilder(this, CardBuilder.Layout.AUTHOR)
                            .setIcon(image)
                            .setHeading(Html.fromHtml(heading))
                            .setSubheading(Html.fromHtml(subheading))
                            .setText(Html.fromHtml(cardText))
                            .setFootnote(Html.fromHtml(cardNote)));
                }
            }
        if (mCards.size() == 0){
                AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                if (mID == EMPTY) {
                    audio.playSoundEffect(Sounds.ERROR);
                    Toast.makeText(getApplicationContext(), "You have no work orders.", Toast.LENGTH_LONG).show();
                } else {
                    audio.playSoundEffect(Sounds.ERROR);
                    Toast.makeText(getApplicationContext(), "No active related work orders", Toast.LENGTH_LONG).show();
                }
            } else {
                mCardScroller.setSelection(0);
            }
        } else {
            AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            audio.playSoundEffect(Sounds.ERROR);
            Toast.makeText(getApplicationContext(), "ERROR: " + fRespWo.getError().getLeg().toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void getScheduledMaintenance(){
        smObj = new ArrayList<>();
        imageList = new ArrayList<>();
        FindRequest< ScheduledMaintenance > fReqSM;
        FindResponse< ScheduledMaintenance > fRespSM;
        FindRequest< MaintenanceType > fReqMT;
        FindResponse< MaintenanceType > fRespMT;
        FindRequest< ScheduledMaintenanceAsset > fReqSMA;
        FindResponse< ScheduledMaintenanceAsset > fRespSMA;
        FindRequest< ScheduledMaintenancePart > fReqSMP;
        FindResponse< ScheduledMaintenancePart > fRespSMP;
        FindRequest< Stock > fReqS;
        FindResponse< Stock > fRespS;
        FindRequest< Asset > fReqA;
        FindResponse< Asset > fRespA;
        FindRequest< Priority > fReqP;
        FindResponse< Priority > fRespP;

        // 0
        fReqSM = MainActivity.client.prepareFind(ScheduledMaintenance.class);
        fReqSM.setFields("id, strCode, strDescription, intMaintenanceTypeID, intScheduledMaintenanceStatusID, strAssignedUserIds, intPriorityID, dv_intPriorityID");
        FindFilter pendingActiveQlFilter = new FindFilter();
        pendingActiveQlFilter.setQl("intScheduledMaintenanceStatusID = ?");
        List< Object > params = Arrays.asList((Object) 1);
        pendingActiveQlFilter.setParameters(params);
        fReqSM.setFilters(Arrays.asList(pendingActiveQlFilter));
        // 1
        fReqMT = MainActivity.client.prepareFind(MaintenanceType.class);
        fReqMT.setFields("id, strColor");
        // 2
        fReqSMA = MainActivity.client.prepareFind(ScheduledMaintenanceAsset.class);
        fReqSMA.setFields("id, intAssetID, intScheduledMaintenanceID");
        // 3
        fReqSMP = MainActivity.client.prepareFind(ScheduledMaintenancePart.class);
        fReqSMP.setFields("intStockID, intPartID, intScheduledMaintenanceID");
        // 4
        fReqS = MainActivity.client.prepareFind(Stock.class);
        fReqS.setFields("id, intAssetID");
        // 5
        fReqA = MainActivity.client.prepareFind(Asset.class);
        fReqA.setFields("id, intSuperCategorySysCode, cf_intDefaultImageFileID");
        // 6
        fReqP = MainActivity.client.prepareFind(Priority.class);
        fReqP.setFields("id, intOrder");

        BatchRequest bReq = MainActivity.client.prepareBatch();
        bReq.setTxMode(TxMode.SINGLE_TRANSACTION);
        List< SimpleRequest > batchList = new ArrayList<>();

        batchList.add(fReqSM);
        batchList.add(fReqMT);
        batchList.add(fReqSMA);
        batchList.add(fReqSMP);
        batchList.add(fReqS);
        batchList.add(fReqA);
        batchList.add(fReqP);
        bReq.setRequests(batchList);

        BatchResponse bResp = MainActivity.client.batch(bReq);

        fRespSM = (FindResponse< ScheduledMaintenance >) bResp.getResponses().get(0);
        fRespMT = (FindResponse< MaintenanceType >) bResp.getResponses().get(1);
        fRespSMA = (FindResponse< ScheduledMaintenanceAsset >) bResp.getResponses().get(2);
        fRespSMP = (FindResponse< ScheduledMaintenancePart >) bResp.getResponses().get(3);
        fRespS = (FindResponse< Stock >) bResp.getResponses().get(4);
        fRespA = (FindResponse< Asset >) bResp.getResponses().get(5);
        fRespP = (FindResponse< Priority >) bResp.getResponses().get(6);

        if (fRespSM.getError() == null) {
            int listSize = fRespSM.getTotalObjects();
            for (int i = 0; listSize > i; i++) {
                boolean flag = true;

                if (fRespSM.getObjects().get(i).getIntScheduledMaintenanceStatusID() == 0L) flag = false;

                if(mID != EMPTY) {
                    if (flag) {
                        if (mSysCode != 4) {
                            if (fRespSMA != null) {
                                int count = 0;
                                for (int j = 0; j < fRespSMA.getTotalObjects(); j++) {
                                    if (fRespSMA.getObjects() != null && fRespSMA.getObjects().get(j) != null && fRespSMA.getObjects().get(j).getIntAssetID() != null && fRespSMA.getObjects().get(j).getIntScheduledMaintenanceID() != null) {
                                        if (fRespSMA.getObjects().get(j).getIntAssetID().equals(mID) && fRespSMA.getObjects().get(j).getIntScheduledMaintenanceID().equals(fRespSM.getObjects().get(i).getId())) {
                                            count += 1;
                                        }
                                    }
                                }
                                if (count == 0) flag = false;
                            } else flag = false;
                        } else {
                            if (fRespSMP.getError() == null) {
                                boolean check = false;
                                for (int j = 0; j < fRespSMP.getTotalObjects(); j++) {
                                    if (fRespSMP.getObjects().get(j).getIntScheduledMaintenanceID().equals(fRespSM.getObjects().get(i).getId())) {
                                        if (fRespSMP.getObjects().get(j).getIntStockID() != null) {
                                            for (int k = 0; k < fRespS.getTotalObjects(); k++) {
                                                if (fRespS.getObjects().get(k).getId() != null && fRespS.getObjects().get(k).getIntAssetID().equals(mID) && fRespSMP.getObjects().get(j).getIntStockID().equals(fRespS.getObjects().get(k).getId())) check = true;
                                            }
                                        } else if (fRespSMP.getObjects().get(j).getIntPartID() != null && fRespSMP.getObjects().get(j).getIntPartID().equals(mID)) check = true;
                                    }
                                }
                                if (!check) flag = false;
                            } else flag = false;
                        }
                    }
                }

                if (flag) {
                    smObj.add(fRespSM.getObjects().get(i));
                    imageList.add(EMPTY);
                    boolList.add(EMPTY);
                }
            }

            ScheduledMaintenance temp;
            for (int i = 1; i < smObj.size(); i++) {
                for (int j = i; j > 0; j--) {
                    long id1 = EMPTY;
                    long id2 = EMPTY;
                    long comp1 = fRespP.getTotalObjects() + 1;
                    long comp2 = fRespP.getTotalObjects() + 1;

                    if (smObj.get(j).getIntPriorityID() != null) {
                        id1 = smObj.get(j).getIntPriorityID();
                    }
                    if (smObj.get(j - 1).getIntPriorityID() != null) {
                        id2 = smObj.get(j - 1).getIntPriorityID();
                    }

                    for (int k = 0; k < fRespP.getTotalObjects(); k++) {
                        if (fRespP.getObjects().get(k).getId().equals(id1)) comp1 = fRespP.getObjects().get(k).getIntOrder();
                        if (fRespP.getObjects().get(k).getId().equals(id2)) comp2 = fRespP.getObjects().get(k).getIntOrder();
                    }

                    if (comp1 < comp2) {
                        temp = smObj.get(j);
                        smObj.set(j, smObj.get(j - 1));
                        smObj.set(j - 1, temp);
                    }
                }
            }

            for (int i = 0; i < smObj.size(); i++) {
                String cardText;
                String heading;

                if (smObj.get(i).getStrDescription() != null) {
                    cardText = "<big><b>" + smObj.get(i).getStrDescription() + "</b></big>";
                } else {
                    cardText = "<font color=\"gray\"><big><i>This scheduled maintenance has no description.</i></big></font>";
                }
                if (smObj.get(i).getExtraFields().get("dv_intPriorityID") != null) {
                    heading = "<font color=\"yellow\"><b>Priority:</b></font> " + smObj.get(i).getExtraFields().get("dv_intPriorityID");
                } else {
                    heading = "<font color=\"gray\"><i>No Priority</i></font>";
                }

                Drawable image = null;
                if (mID != EMPTY) {
                    for (int j = 0; j < fRespA.getTotalObjects(); j++) {
                        if (fRespA.getObjects().get(j).getId().equals(mID)) {
                            if (notNegOne(fRespA.getObjects().get(j).getExtraFields().get("cf_intDefaultImageFileID").toString())) {
                                image = MainActivity.LoadImageFromWebOperations("https://juskoski.masandbox.com/fileDownload/?f=" + fRespA.getObjects().get(j).getExtraFields().get("cf_intDefaultImageFileID"));
                                imageList.set(i, Integer.parseInt(fRespA.getObjects().get(j).getExtraFields().get("cf_intDefaultImageFileID").toString()));
                                boolList.set(i, 1);
                            } else {
                                if (fRespA.getObjects().get(j).getIntSuperCategorySysCode() == 1) {
                                    image = getResources().getDrawable(R.drawable.default_facility);
                                    imageList.set(i, R.drawable.default_facility);
                                    boolList.set(i, 0);
                                } else if (fRespA.getObjects().get(j).getIntSuperCategorySysCode() == 2) {
                                    image = getResources().getDrawable(R.drawable.default_asset);
                                    imageList.set(i, R.drawable.default_asset);
                                    boolList.set(i, 0);
                                } else if (fRespA.getObjects().get(j).getIntSuperCategorySysCode() == 3) {
                                    image = getResources().getDrawable(R.drawable.default_tool);
                                    imageList.set(i, R.drawable.default_tool);
                                    boolList.set(i, 0);
                                } else {
                                    image = getResources().getDrawable(R.drawable.default_part);
                                    imageList.set(i, R.drawable.default_part);
                                    boolList.set(i, 0);
                                }
                            }
                        }
                    }
                }

                if (nullOrDefault(image) && fRespSMA != null && fRespSMA.getError() == null && fRespSMA.getTotalObjects() != 0) {
                    for (int j = 0; j < fRespSMA.getTotalObjects(); j++) {
                        if (fRespSMA.getObjects().get(j).getIntScheduledMaintenanceID().equals(smObj.get(i).getId())) {
                            for (int k = 0; k < fRespA.getTotalObjects(); k++) {
                                if (fRespA.getObjects().get(k).getId().equals(fRespSMA.getObjects().get(j).getIntAssetID())) {
                                    if (notNegOne(fRespA.getObjects().get(k).getExtraFields().get("cf_intDefaultImageFileID").toString())) {
                                        image = MainActivity.LoadImageFromWebOperations("https://juskoski.masandbox.com/fileDownload/?f=" + fRespA.getObjects().get(k).getExtraFields().get("cf_intDefaultImageFileID"));
                                        imageList.set(i, Integer.parseInt(fRespA.getObjects().get(k).getExtraFields().get("cf_intDefaultImageFileID").toString()));
                                        boolList.set(i, 1);
                                    } else if (image == null) {
                                        if (fRespA.getObjects().get(j).getIntSuperCategorySysCode() == 1) {
                                            image = getResources().getDrawable(R.drawable.default_facility);
                                            imageList.set(i, R.drawable.default_facility);
                                            boolList.set(i, 0);
                                        } else if (fRespA.getObjects().get(j).getIntSuperCategorySysCode() == 2) {
                                            image = getResources().getDrawable(R.drawable.default_asset);
                                            imageList.set(i, R.drawable.default_asset);
                                            boolList.set(i, 0);
                                        } else if (fRespA.getObjects().get(j).getIntSuperCategorySysCode() == 3) {
                                            image = getResources().getDrawable(R.drawable.default_tool);
                                            imageList.set(i, R.drawable.default_tool);
                                            boolList.set(i, 0);
                                        }
                                    }
                                }
                            }
                        }
                        if (!nullOrDefault(image)) break;
                    }
                }

                if (nullOrDefault(image) && fRespSMP.getTotalObjects() != 0) {
                    for (int j = 0; j < fRespSMP.getTotalObjects(); j++) {
                        if (fRespSMP.getObjects().get(j).getIntScheduledMaintenanceID().equals(smObj.get(i).getId())) {
                            for (int k = 0; k < fRespS.getTotalObjects(); k++) {
                                if (fRespS.getObjects().get(k).getId().equals(fRespSMP.getObjects().get(j).getIntStockID())) {
                                    for (int l = 0; l < fRespA.getTotalObjects(); l++) {
                                        if (fRespA.getObjects().get(l).getId().equals(fRespS.getObjects().get(k).getIntAssetID())) {
                                            if (notNegOne(fRespA.getObjects().get(l).getExtraFields().get("cf_intDefaultImageFileID").toString())) {
                                                image = MainActivity.LoadImageFromWebOperations("https://juskoski.masandbox.com/fileDownload/?f=" + fRespA.getObjects().get(l).getExtraFields().get("cf_intDefaultImageFileID"));
                                                imageList.set(i, Integer.parseInt(fRespA.getObjects().get(l).getExtraFields().get("cf_intDefaultImageFileID").toString()));
                                                boolList.set(i, 1);
                                            } else if (image == null) {
                                                image = getResources().getDrawable(R.drawable.default_part);
                                                imageList.set(i, R.drawable.default_part);
                                                boolList.set(i, 0);
                                            }
                                        }
                                    }
                                }
                                if (!nullOrDefault(image)) break;
                            }
                            if (nullOrDefault(image) && fRespSMP.getObjects().get(j).getIntPartID() != null) {
                                for(int k = 0; k < fRespA.getTotalObjects(); k++) {
                                    if (fRespA.getObjects().get(k).getId().equals(fRespSMP.getObjects().get(j).getIntPartID())) {
                                        if (notNegOne(fRespA.getObjects().get(k).getExtraFields().get("cf_intDefaultImageFileID").toString())) {
                                            image = MainActivity.LoadImageFromWebOperations("https://juskoski.masandbox.com/fileDownload/?f=" + fRespA.getObjects().get(k).getExtraFields().get("cf_intDefaultImageFileID"));
                                            imageList.set(i, Integer.parseInt(fRespA.getObjects().get(k).getExtraFields().get("cf_intDefaultImageFileID").toString()));
                                            boolList.set(i, 1);
                                        } else if (image == null) {
                                            image = getResources().getDrawable(R.drawable.default_part);
                                            imageList.set(i, R.drawable.default_part);
                                            boolList.set(i, 0);
                                        }
                                    }
                                }
                            }
                        }
                        if (!nullOrDefault(image)) break;
                    }
                }

                if (image == null) {
                    image = this.getResources().getDrawable(R.drawable.default_null);
                    imageList.set(i, R.drawable.default_null);
                    boolList.set(i, 0);
                }

                if (smObj.get(i).getIntMaintenanceTypeID() != null) {
                    for (int j = 0; j < fRespMT.getTotalObjects(); j++) {
                        if (smObj.get(i).getIntMaintenanceTypeID().equals(fRespMT.getObjects().get(j).getId())) {
                            Bitmap.Config conf = Bitmap.Config.ARGB_8888;
                            DisplayMetrics metrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(metrics);
                            Bitmap bmp = Bitmap.createBitmap(metrics, 640, 320, conf);
                            bmp.eraseColor(hex2Rgb(fRespMT.getObjects().get(j).getStrColor()));

                            mCards.add(new CardBuilder(this, CardBuilder.Layout.AUTHOR)
                                    .setIcon(image)
                                    .setHeading(Html.fromHtml(heading))
                                    .setText(Html.fromHtml(cardText))
                                    .addImage(bmp));
                        }
                    }
                } else {
                    mCards.add(new CardBuilder(this, CardBuilder.Layout.AUTHOR)
                            .setIcon(image)
                            .setHeading(Html.fromHtml(heading))
                            .setText(Html.fromHtml(cardText)));
                }
            }
            if (mCards.size() == 0){
                if (mID == EMPTY) {
                    AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                    audio.playSoundEffect(Sounds.ERROR);
                    Toast.makeText(getApplicationContext(), "There is no scheduled maintenance", Toast.LENGTH_LONG).show();
                } else {
                    AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                    audio.playSoundEffect(Sounds.ERROR);
                    Toast.makeText(getApplicationContext(), "No related scheduled maintenance", Toast.LENGTH_LONG).show();
                }
            } else {
                mCardScroller.setSelection(0);
            }
        } else {
            AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            audio.playSoundEffect(Sounds.ERROR);
            Toast.makeText(getApplicationContext(), "ERROR: " + fRespSM.getError().getLeg().toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public boolean nullOrDefault(Drawable image) {

        if (image == null) {
            return true;
        } else if (image.equals(getResources().getDrawable(R.drawable.default_facility))) {
            return true;
        } else if (image.equals(getResources().getDrawable(R.drawable.default_asset))) {
            return true;
        } else if (image.equals(getResources().getDrawable(R.drawable.default_tool))) {
            return true;
        } else if (image.equals(getResources().getDrawable(R.drawable.default_part))) {
            return true;
        }

        return false;
    }

    public boolean notNegOne (String id) {
        if(id == null) return false;
        return !(Integer.parseInt(id) == -1);
    }

    public static int hex2Rgb(String colorStr) {
        return Color.parseColor("#".concat(colorStr.toUpperCase()));
    }
}
