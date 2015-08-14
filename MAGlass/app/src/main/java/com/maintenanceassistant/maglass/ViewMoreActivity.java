package com.maintenanceassistant.maglass;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Html;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollView;
import com.ma.cmms.api.client.dto.Asset;
import com.ma.cmms.api.client.dto.MeterReadingUnit;
import com.ma.cmms.api.client.dto.ScheduledMaintenance;
import com.ma.cmms.api.client.dto.Stock;
import com.ma.cmms.api.client.dto.User;
import com.ma.cmms.api.crud.FindByIdRequest;
import com.ma.cmms.api.crud.FindByIdResponse;
import com.ma.cmms.api.crud.FindFilter;
import com.ma.cmms.api.crud.FindRequest;
import com.ma.cmms.api.crud.FindResponse;
import com.maintenanceassistant.maglass.adapters.MainAdapter;

import com.ma.cmms.api.client.BasicCredentials;
import com.ma.cmms.api.client.MaCmmsClient;
import com.ma.cmms.api.client.dto.WorkOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Jake on 2015-07-17.
 */
public class ViewMoreActivity extends Activity{
    private String mActivityController;
    private CardScrollView mCardScroller;
    private List<CardBuilder> mCards;
    private GestureDetector mGestureDetector;
    private Long mID;
    private int imageVal;
    private int boolVal;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        mCardScroller = new CardScrollView(this);
        mCards = new ArrayList<>();

        if (getIntent().hasExtra(ResultsActivity.SEARCH)) {
            mActivityController = getIntent().getStringExtra(ResultsActivity.SEARCH);
        }
        mID = getIntent().getLongExtra(ResultsActivity.ID, ResultsActivity.EMPTY);
        imageVal = getIntent().getIntExtra(ResultsActivity.IMAGE, ResultsActivity.EMPTY);
        boolVal = getIntent().getIntExtra(ResultsActivity.BOOL, ResultsActivity.EMPTY);

        if(ResultsActivity.WORK_ORDER.equals(mActivityController)) {
            getWorkOrderInfo();
        } else if (ResultsActivity.SCH_MAINTENANCE.equals(mActivityController)) {
            getScheduledMaintenanceInfo();
        } else {
            getAssetInfo();
        }

        if (mCards.size() == 0){
            finish();
        }

        mCardScroller.setAdapter(new MainAdapter(mCards));

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
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
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

    private void getWorkOrderInfo(){
        String cardText;
        String cardNote;
        FindByIdRequest<User> fReqU = MainActivity.client.prepareFindById(User.class);

        FindByIdRequest<WorkOrder> fReqWo = MainActivity.client.prepareFindById(WorkOrder.class);
        fReqWo.setId(mID);
        fReqWo.setFields("strAssets, strAssignedUsers, intRequestedByUserID, strEmailUserGuest, strAdminNotes, strNameUserGuest, dtmSuggestedCompletionDate, strPhoneUserGuest, strCode, dtmDateLastModified, dv_intProjectID, dv_intChargeDepartmentID, dv_intAccountID, dv_intSiteID, dv_intWorkOrderStatusID, dv_intMaintenanceTypeID, dv_intScheduledMaintenanceID");

        FindByIdResponse<WorkOrder> fRespWo = MainActivity.client.findById(fReqWo);


        //
        // Card 1
        //
        cardText = "<font color=\"yellow\"><b>Type:</b></font> ";
        if (fRespWo.getObject().getExtraFields().get("dv_intMaintenanceTypeID") != null){
            cardText += fRespWo.getObject().getExtraFields().get("dv_intMaintenanceTypeID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Status:</b></font> ";
        if (fRespWo.getObject().getExtraFields().get("dv_intWorkOrderStatusID") != null){
            cardText += fRespWo.getObject().getExtraFields().get("dv_intWorkOrderStatusID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Code:</b></font> ";
        if (fRespWo.getObject().getStrCode() != null){
            cardText += fRespWo.getObject().getStrCode() + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Site:</b></font> ";
        if (fRespWo.getObject().getExtraFields().get("dv_intSiteID") != null){
            cardText += fRespWo.getObject().getExtraFields().get("dv_intSiteID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Notes:</b></font> ";
        if (fRespWo.getObject().getStrAdminNotes() != null){
            cardText += fRespWo.getObject().getStrAdminNotes() + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardNote = "<font color=\"yellow\"><b>Complete By:</b></font> ";
        if (fRespWo.getObject().getDtmSuggestedCompletionDate() != null){
            cardNote += fRespWo.getObject().getDtmSuggestedCompletionDate();
        } else {
            cardNote += "<font color=\"gray\"><i>Not entered.</i></font>";
        }

        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(Html.fromHtml(cardText))
                .setFootnote(Html.fromHtml(cardNote))
                .setAttributionIcon(R.drawable.little_logo));


        //
        // Card 2
        //
        String heading;
        String subheading;
        Drawable user = null;
        String url = "https://juskoski.masandbox.com/fileDownload/?f=";

        if (fRespWo.getObject().getIntRequestedByUserID() != null) {
            if (fRespWo.getObject().getStrNameUserGuest() == null && fRespWo.getObject().getStrPhoneUserGuest() == null && fRespWo.getObject().getStrEmailUserGuest() == null) {
                fReqU.setId(fRespWo.getObject().getIntRequestedByUserID());
                fReqU.setFields("strFullName, strUserTitle, strTelephone, strTelephone2, strEmailAddress, cf_intDefaultImageFileID");
                FindByIdResponse<User> fRespU = MainActivity.client.findById(fReqU);

                if (fRespU.getObject().getExtraFields().get("cf_intDefaultImageFileID") != null) {
                    url += fRespU.getObject().getExtraFields().get("cf_intDefaultImageFileID");
                    user = MainActivity.LoadImageFromWebOperations(url);
                }

                heading = "<font color=\"yellow\"><b>Requested by ";
                if (fRespU.getObject().getStrFullName() != null) {
                    heading += fRespU.getObject().getStrFullName() + "</b></big></font>";
                } else {
                    heading += "</font><font color=\"gray\">(Missing name)</b></big></font>";
                }

                if(fRespU.getObject().getStrUserTitle() != null) {
                    subheading = fRespU.getObject().getStrUserTitle();
                } else {
                    subheading = "<font color=\"gray\"><i>No title</i></font>";
                }

                cardText = "<font color=\"yellow\"><b>Phone 1:</b></font> ";
                if(fRespU.getObject().getStrTelephone() != null) {
                    cardText += fRespU.getObject().getStrTelephone() + "<br />";
                } else {
                    cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
                }

                cardText += "<font color=\"yellow\"><b>Phone 2:</b></font> ";
                if(fRespU.getObject().getStrTelephone2() != null) {
                    cardText += fRespU.getObject().getStrTelephone2() + "<br />";
                } else {
                    cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
                }

                cardText += "<font color=\"yellow\"><b>Email:</b></font> ";
                if(fRespU.getObject().getStrEmailAddress() != null) {
                    cardText += fRespU.getObject().getStrEmailAddress() + "<br />";
                } else {
                    cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
                }

                if (user != null) {
                    mCards.add(new CardBuilder(this, CardBuilder.Layout.AUTHOR)
                            .setIcon(user)
                            .setHeading(Html.fromHtml(heading))
                            .setSubheading(Html.fromHtml(subheading))
                            .setText(Html.fromHtml(cardText))
                            .setAttributionIcon(R.drawable.little_logo));
                } else {
                    mCards.add(new CardBuilder(this, CardBuilder.Layout.AUTHOR)
                            .setIcon(R.drawable.ic_person_50)
                            .setHeading(Html.fromHtml(heading))
                            .setSubheading(Html.fromHtml(subheading))
                            .setText(Html.fromHtml(cardText))
                            .setAttributionIcon(R.drawable.little_logo));
                }
            } else {
                heading = "<font color=\"yellow\"><b>Requested by ";
                if(fRespWo.getObject().getStrNameUserGuest() != null) {
                    heading += fRespWo.getObject().getStrNameUserGuest() + "</font><font color=\"gray\">(Guest)</font>";
                } else {
                    heading += "</font><font color=\"gray\"><i>Guest</i></font>";
                }

                cardText = "<font color=\"yellow\"><b>Phone:</b></font> ";
                if(fRespWo.getObject().getStrPhoneUserGuest() != null) {
                    cardText += fRespWo.getObject().getStrPhoneUserGuest() + "<br />";
                } else {
                    cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
                }

                cardText += "<font color=\"yellow\"><b>Email:</b></font> ";
                if(fRespWo.getObject().getStrEmailUserGuest() != null) {
                    cardText += fRespWo.getObject().getStrEmailUserGuest() + "<br />";
                } else {
                    cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
                }
                if (user != null) {
                    mCards.add(new CardBuilder(this, CardBuilder.Layout.AUTHOR)
                            .setIcon(user)
                            .setHeading(Html.fromHtml(heading))
                            .setText(Html.fromHtml(cardText))
                            .setAttributionIcon(R.drawable.little_logo));
                } else {
                    mCards.add(new CardBuilder(this, CardBuilder.Layout.AUTHOR)
                            .setIcon(R.drawable.ic_person_50)
                            .setHeading(Html.fromHtml(heading))
                            .setText(Html.fromHtml(cardText))
                            .setAttributionIcon(R.drawable.little_logo));
                }
            }
        } else {
            cardText = "<font color=\"yellow\"><big><b>Requested by</b></font> <font color=\"gray\"><i>no one.</i></big></font><br />";
            mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                    .setText(Html.fromHtml(cardText))
                    .setAttributionIcon(R.drawable.little_logo));
        }


        //
        // Card 3
        //
        if (fRespWo.getObject().getStrAssets() != null) {
            cardText = "<font color=\"yellow\"><big><b>Related Assets:</b></big></font><br />" + fRespWo.getObject().getStrAssets();
        } else {
            cardText = "<font color=\"yellow\"><big><b>Related Assets:</b></big></font><br /><font color=\"gray\"><i>None entered.</i></font>";
        }
        if (boolVal == 0) {
            mCards.add(new CardBuilder(this, CardBuilder.Layout.COLUMNS)
                    .addImage(imageVal)
                    .setText(Html.fromHtml(cardText))
                    .setAttributionIcon(R.drawable.little_logo));
        } else if (boolVal == 1) {
            mCards.add(new CardBuilder(this, CardBuilder.Layout.COLUMNS)
                    .addImage(MainActivity.LoadImageFromWebOperations("https://juskoski.masandbox.com/fileDownload/?f=" + imageVal))
                    .setText(Html.fromHtml(cardText))
                    .setAttributionIcon(R.drawable.little_logo));
        } else {
            mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                    .setText(Html.fromHtml(cardText))
                    .setAttributionIcon(R.drawable.little_logo));
        }


        //
        // Card 4
        //
        if (fRespWo.getObject().getStrAssignedUsers() != null) {
            cardText = "<font color=\"yellow\"><big><b>Assigned Users:</b></big></font><br />" + fRespWo.getObject().getStrAssignedUsers();
        } else {
            cardText = "<font color=\"yellow\"><big><b>Assigned Users:</b></big></font><br /><font color=\"gray\"><i>None entered.</i></font>";
        }
        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(Html.fromHtml(cardText))
                .setAttributionIcon(R.drawable.little_logo));


        //
        // Card 5
        //
        cardText = "<font color=\"yellow\"><b>Project:</b></font> ";
        if (fRespWo.getObject().getExtraFields().get("dv_intProjectID") != null){
            cardText += fRespWo.getObject().getExtraFields().get("dv_intProjectID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Account:</b></font> ";
        if (fRespWo.getObject().getExtraFields().get("dv_intAccountID") != null){
            cardText += fRespWo.getObject().getExtraFields().get("dv_intAccountID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Charge Department:</b></font> ";
        if (fRespWo.getObject().getExtraFields().get("dv_intChargeDepartmentID") != null){
            cardText += fRespWo.getObject().getExtraFields().get("dv_intChargeDepartmentID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Scheduled Maintenance:</b></font> ";
        if (fRespWo.getObject().getExtraFields().get("dv_intScheduledMaintenanceID") != null){
            cardText += fRespWo.getObject().getExtraFields().get("dv_intScheduledMaintenanceID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        if (fRespWo.getObject().getDtmDateLastModified() != null) {
            cardNote = "<font color=\"yellow\"><b>Last Modified:</b></font> " + fRespWo.getObject().getDtmDateLastModified();
        } else {
            cardNote = "<font color=\"yellow\"><b>Last Modified:</b></font> <font color=\"gray\"><i>N/A</i></font>";
        }
        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(Html.fromHtml(cardText))
                .setFootnote(Html.fromHtml(cardNote)));
                //.setAttributionIcon(R.drawable.little_logo));


        //
        // Finished
        //
        mCardScroller.setSelection(0);
    }

    private void getScheduledMaintenanceInfo(){
        String cardText;
        String cardNote;

        FindByIdRequest<ScheduledMaintenance> fReqSM = MainActivity.client.prepareFindById(ScheduledMaintenance.class);
        fReqSM.setId(mID);
        fReqSM.setFields("dv_intMaintenanceTypeID, strCode, strDescription, dblSuggestedTime, cf_dvSMAsset, intRequestorUserID, dv_intProjectID, dv_intAccountID, dv_intChargeDepartmentID, dv_intStartAsWorkOrderStatusID, dtmUpdatedDate");

        FindByIdResponse<ScheduledMaintenance> fRespSM = MainActivity.client.findById(fReqSM);

        FindByIdRequest<User> fReqU = MainActivity.client.prepareFindById(User.class);
        fReqU.setId(fRespSM.getObject().getIntRequestorUserID());
        fReqU.setFields("strFullName, strUserTitle, strTelephone, strTelephone2, strEmailAddress, cf_intDefaultImageFileID");

        FindByIdResponse<User> fRespU = MainActivity.client.findById(fReqU);


        //
        // Card 1
        // dv_intMaintenanceTypeID, dv_intPriorityID, strCode, strDescription, dblSuggestedTime
        //
        cardText = "<font color=\"yellow\"><b>Type:</b></font> ";
        if (fRespSM.getObject().getExtraFields().get("dv_intMaintenanceTypeID") != null){
            cardText += fRespSM.getObject().getExtraFields().get("dv_intMaintenanceTypeID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Opening Status:</b></font> ";
        if (fRespSM.getObject().getExtraFields().get("dv_intStartAsWorkOrderStatusID") != null){
            cardText += fRespSM.getObject().getExtraFields().get("dv_intStartAsWorkOrderStatusID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Code:</b></font> ";
        if (fRespSM.getObject().getStrCode() != null){
            cardText += fRespSM.getObject().getStrCode() + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Description:</b></font> ";
        if (fRespSM.getObject().getStrDescription() != null){
            cardText += fRespSM.getObject().getStrDescription() + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardNote = "<font color=\"yellow\"><b>Estimated Time Needed:</b></font> ";
        if (fRespSM.getObject().getDblSuggestedTime() != null){
            cardNote += fRespSM.getObject().getDblSuggestedTime();
            cardNote += (fRespSM.getObject().getDblSuggestedTime() == 1.0) ? " hour" : " hours";
        } else {
            cardNote += "<font color=\"gray\"><i>Not entered.</i></font>";
        }

        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(Html.fromHtml(cardText))
                .setFootnote(Html.fromHtml(cardNote))
                .setAttributionIcon(R.drawable.little_logo));


        //
        // Card 2
        // cf_dvSMAsset
        //
        if (fRespSM.getObject().getExtraFields().get("cf_dvSMAsset") != null) {
            cardText = "<font color=\"yellow\"><big><b>Related Assets:</b></big></font><br />" + fRespSM.getObject().getExtraFields().get("cf_dvSMAsset");
        } else {
            cardText = "<font color=\"yellow\"><big><b>Related Assets:</b></big></font><br /><font color=\"gray\"><i>None entered.</i></font>";
        }

        if (boolVal == 0) {
            mCards.add(new CardBuilder(this, CardBuilder.Layout.COLUMNS)
                    .addImage(imageVal)
                    .setText(Html.fromHtml(cardText))
                    .setAttributionIcon(R.drawable.little_logo));
        } else if (boolVal == 1) {
            mCards.add(new CardBuilder(this, CardBuilder.Layout.COLUMNS)
                    .addImage(MainActivity.LoadImageFromWebOperations("https://juskoski.masandbox.com/fileDownload/?f=" + imageVal))
                    .setText(Html.fromHtml(cardText))
                    .setAttributionIcon(R.drawable.little_logo));
        } else {
            mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                    .setText(Html.fromHtml(cardText))
                    .setAttributionIcon(R.drawable.little_logo));
        }


        //
        // Card 3
        // intRequestorUserID
        //
        String heading;
        String subheading;
        Drawable user = null;
        String url = "https://juskoski.masandbox.com/fileDownload/?f=";

        if (fRespU.getError() == null) {
            heading = "<font color=\"yellow\"><b>Requested by ";
            if (fRespU.getObject().getStrFullName() != null) {
                heading += fRespU.getObject().getStrFullName() + "</b></big></font>";
            } else {
                heading += "</font><font color=\"gray\">(Missing name)</b></big></font>";
            }

            if (fRespU.getObject().getExtraFields() != null && fRespU.getObject().getExtraFields().get("cf_intDefaultImageFileID") != null) {
                url += fRespU.getObject().getExtraFields().get("cf_intDefaultImageFileID").toString();
                user = MainActivity.LoadImageFromWebOperations(url);
            }

            if(fRespU.getObject().getStrUserTitle() != null) {
                subheading = fRespU.getObject().getStrUserTitle();
            } else {
                subheading = "<font color=\"gray\"><i>No title</i></font>";
            }

            cardText = "<font color=\"yellow\"><b>Phone 1:</b></font> ";
            if(fRespU.getObject().getStrTelephone() != null) {
                cardText += fRespU.getObject().getStrTelephone() + "<br />";
            } else {
                cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
            }

            cardText += "<font color=\"yellow\"><b>Phone 2:</b></font> ";
            if(fRespU.getObject().getStrTelephone2() != null) {
                cardText += fRespU.getObject().getStrTelephone2() + "<br />";
            } else {
                cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
            }

            cardText += "<font color=\"yellow\"><b>Email:</b></font> ";
            if(fRespU.getObject().getStrEmailAddress() != null) {
                cardText += fRespU.getObject().getStrEmailAddress() + "<br />";
            } else {
                cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
            }

            if (user != null) {
                mCards.add(new CardBuilder(this, CardBuilder.Layout.AUTHOR)
                        .setIcon(user)
                        .setHeading(Html.fromHtml(heading))
                        .setSubheading(Html.fromHtml(subheading))
                        .setText(Html.fromHtml(cardText))
                        .setAttributionIcon(R.drawable.little_logo));
            } else {
                mCards.add(new CardBuilder(this, CardBuilder.Layout.AUTHOR)
                        .setIcon(R.drawable.ic_person_50)
                        .setHeading(Html.fromHtml(heading))
                        .setSubheading(Html.fromHtml(subheading))
                        .setText(Html.fromHtml(cardText))
                        .setAttributionIcon(R.drawable.little_logo));
            }
        } else {
            cardText = "<font color=\"yellow\"><big><b>Requested by</b></font> <font color=\"gray\"><i>no one.</i></big></font><br />";
            mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                    .setText(Html.fromHtml(cardText))
                    .setAttributionIcon(R.drawable.little_logo));
        }


        //
        // Card 4
        // dv_intProjectID, dv_intAccountID, dv_intChargeDepartmentID, dv_intStartAsWorkOrderStatusID, dtmUpdatedDate
        //
        cardText = "<font color=\"yellow\"><b>Project:</b></font> ";
        if (fRespSM.getObject().getExtraFields().get("dv_intProjectID") != null){
            cardText += fRespSM.getObject().getExtraFields().get("dv_intProjectID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Account:</b></font> ";
        if (fRespSM.getObject().getExtraFields().get("dv_intAccountID") != null){
            cardText += fRespSM.getObject().getExtraFields().get("dv_intAccountID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Charge Department:</b></font> ";
        if (fRespSM.getObject().getExtraFields().get("dv_intChargeDepartmentID") != null){
            cardText += fRespSM.getObject().getExtraFields().get("dv_intChargeDepartmentID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        if (fRespSM.getObject().getDtmUpdatedDate() != null) {
            cardNote = "<font color=\"yellow\"><b>Last Updated:</b></font> " + fRespSM.getObject().getDtmUpdatedDate();
        } else {
            cardNote = "<font color=\"yellow\"><b>Last Updated:</b></font> <font color=\"gray\"><i>N/A</i></font>";
        }
        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(Html.fromHtml(cardText))
                .setFootnote(Html.fromHtml(cardNote)));


        //
        // Finished
        //
        mCardScroller.setSelection(0);
    }

    private void getAssetInfo() {
        String cardText;
        String cardNote;

        FindByIdRequest<Asset> fReqA = MainActivity.client.prepareFindById(Asset.class);
        fReqA.setId(mID);
        fReqA.setFields("strName, strCode, strMake, strModel, qtyMinStockCount, qtyStockCount, strDescription, bolIsOnline, dv_intAssetParentID, dv_intAssetLocationID, strAisle, strRow, strBinNumber, dv_intAccountID, dv_intChargeDepartmentID, strNotes, strShippingTerms, dv_intCategoryID, strSerialNumber, strUnspcCode, dblLastPrice, dv_intLastPriceCurrencyID, cf_getLatestReadingsFor, dv_intLastMeterReadingUnitID, dv_intSiteID, dv_intAssetLocationID, cf_assetAddressString, intSuperCategorySysCode");

        FindByIdResponse<Asset> fRespA = MainActivity.client.findById(fReqA);

        //
        // Card 1
        // strName
        // strCode
        // strMake
        // strModel
        // qtyMinStockCount [x]
        // qtyStockCount    [x]
        // strDescription
        //
        //
        cardText = "<font color=\"yellow\"><b>Name:</b></font> ";
        if (fRespA.getObject().getStrName() != null) {
            cardText += fRespA.getObject().getStrName();
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        if (fRespA.getObject().getStrCode() != null) {
            cardText += " (" + fRespA.getObject().getStrCode() + ")<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Make:</b></font> ";
        if (fRespA.getObject().getStrMake() != null) {
            cardText += fRespA.getObject().getStrMake() + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Model:</b></font> ";
        if (fRespA.getObject().getStrModel() != null) {
            cardText += fRespA.getObject().getStrModel() + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        if (fRespA.getObject().getIntSuperCategorySysCode() == 4) {
            cardText += "<font color=\"yellow\"><b>Current Stock:</b></font> ";
            if (fRespA.getObject().getQtyStockCount() != null) {
                cardText += fRespA.getObject().getQtyStockCount() + "<br />";
            } else {
                cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
            }
        }

        if (fRespA.getObject().getBolIsOnline() != null){
            cardNote = (fRespA.getObject().getBolIsOnline() == 1L) ? "<font color=\"green\">Online</font>" : "<font color=\"red\"><i>Offline</i></font>";
        } else {
            cardNote = "";
        }

        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(Html.fromHtml(cardText))
                .setFootnote(Html.fromHtml(cardNote))
                .setAttributionIcon(R.drawable.little_logo));


        //
        // Card 2
        // strDescription
        //
        cardText = "<font color=\"yellow\"><big><b>Description:</b></big></font><br />";
        if (fRespA.getObject().getStrDescription() != null) {
            cardText += fRespA.getObject().getStrDescription();
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font>";
        }

        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(Html.fromHtml(cardText))
                .setAttributionIcon(R.drawable.little_logo));


        //
        // Card 3
        // strNotes
        //
        cardText = "<font color=\"yellow\"><big><b>Notes:</b></big></font><br />";
        if(fRespA.getObject().getStrNotes() != null) {
            cardText += fRespA.getObject().getStrNotes();
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font>";
        }

        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(Html.fromHtml(cardText))
                .setAttributionIcon(R.drawable.little_logo));


        //
        // Card 4
        // dv_intAssetParentID
        // dv_intAssetLocationID [o]
        // strAisle              [o]
        // strRow                [o]
        // strBinNumber          [o]
        //
        cardText = "";
        if (fRespA.getObject().getExtraFields().get("dv_intAssetParentID") != null) {
            cardText += "<font color=\"yellow\"><b>Parent Asset:</b></font> " + fRespA.getObject().getExtraFields().get("dv_intAssetParentID") + "<br />";
        }

        if (fRespA.getObject().getIntSuperCategorySysCode() != 4) {
            cardText += "<font color=\"yellow\"><b>Location:</b></font> ";
            if (fRespA.getObject().getExtraFields().get("dv_intAssetLocationID") != null) {
                cardText += fRespA.getObject().getExtraFields().get("dv_intAssetLocationID") + "<br />";
            } else {
                cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
            }

            cardText += "<font color=\"yellow\"><b>Aisle:</b></font> ";
            if (fRespA.getObject().getStrAisle() != null) {
                cardText += fRespA.getObject().getStrAisle() + "<br />";
            } else {
                cardText += "<font color=\"gray\"><i>Not entered, </i></font><br />";
            }

            cardText += "<font color=\"yellow\"><b>Row:</b></font> ";
            if (fRespA.getObject().getStrRow() != null) {
                cardText += fRespA.getObject().getStrRow() + "<br />";
            } else {
                cardText += "<font color=\"gray\"><i>Not entered, </i></font><br />";
            }
            cardText += "<font color=\"yellow\"><b>Bin:</b></font> ";
            if (fRespA.getObject().getStrBinNumber() != null) {
                cardText += fRespA.getObject().getStrBinNumber() + "<br />";
            } else {
                cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
            }
        } else {
            cardText += "See final slides for stock locations.";
        }

        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(Html.fromHtml(cardText))
                .setAttributionIcon(R.drawable.little_logo));


        //
        // Card 5
        // dv_intAccountID
        // dv_intChargeDepartmentID
        // strShippingTerms
        // dv_intCategoryID
        //
        cardText = "<font color=\"yellow\"><b>Account:</b></font> ";
        if(fRespA.getObject().getExtraFields().get("dv_intAccountID") != null) {
            cardText += fRespA.getObject().getExtraFields().get("dv_intAccountID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Charge Department:</b></font> ";
        if(fRespA.getObject().getExtraFields().get("dv_intChargeDepartmentID") != null) {
            cardText += fRespA.getObject().getExtraFields().get("dv_intChargeDepartmentID") + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Shipping Terms:</b></font> ";
        if(fRespA.getObject().getStrShippingTerms() != null) {
            cardText += fRespA.getObject().getStrShippingTerms() + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        if(fRespA.getObject().getExtraFields().get("dv_intCategoryID") != null) {
            cardNote = "<font color=\"yellow\"><b>Category:</b></font> " + fRespA.getObject().getExtraFields().get("dv_intCategoryID");
        } else {
            cardNote = "\"<font color=\"yellow\"><b>Category:</b></font> <font color=\"gray\"><i>Not entered.</i></font>";
        }

        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(Html.fromHtml(cardText))
                .setFootnote(Html.fromHtml(cardNote))
                .setAttributionIcon(R.drawable.little_logo));


        //
        // Card 6
        // strSerialNumber
        // strUnspcCode
        // dblLastPrice
        // dv_intLastPriceCurrencyID
        // dv_intSiteID
        //
        cardText = "<font color=\"yellow\"><b>Serial Number:</b></font> ";
        if (fRespA.getObject().getStrSerialNumber() != null) {
            cardText += fRespA.getObject().getStrSerialNumber() + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Unspc Code:</b></font> ";
        if (fRespA.getObject().getStrUnspcCode() != null) {
            cardText += fRespA.getObject().getStrUnspcCode() + "<br />";
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        cardText += "<font color=\"yellow\"><b>Last Price:</b></font> ";
        if (fRespA.getObject().getDblLastPrice() != null) {
            cardText += fRespA.getObject().getDblLastPrice();
            if (fRespA.getObject().getExtraFields().get("dv_intLastPriceCurrencyID") != null) {
                cardText += " " + fRespA.getObject().getExtraFields().get("dv_intLastPriceCurrencyID");
            }
        } else {
            cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
        }

        if (fRespA.getObject().getExtraFields().get("dv_intSiteID") != null) {
            cardNote = "<font color=\"yellow\"><b>Site:</b></font> " + fRespA.getObject().getExtraFields().get("dv_intSiteID").toString();
        } else {
            cardNote = "<font color=\"yellow\"><b>Site:</b></font> <font color=\"gray\"><i>Error</i></font>";
        }

        mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(Html.fromHtml(cardText))
                .setFootnote(Html.fromHtml(cardNote))
                .setAttributionIcon(R.drawable.little_logo));


        //
        // Card 7
        // All latest meter readings
        //
        if (fRespA.getObject().getIntSuperCategorySysCode() != 4) {
            if (fRespA.getObject().getExtraFields().get("cf_getLatestReadingsFor") != null) {
                cardText = "<font color=\"yellow\"><big><b>Last Readings:</b></big></font><br />";
                LinkedHashMap<String, ArrayList> tempMap = (LinkedHashMap<String, ArrayList>) fRespA.getObject().getExtraFields().get("cf_getLatestReadingsFor");
                ArrayList<LinkedHashMap> readings = tempMap.get("returnObjects");

                FindRequest<MeterReadingUnit> fReqMRU = MainActivity.client.prepareFind(MeterReadingUnit.class);
                fReqMRU.setFields("id, strName");
                FindResponse<MeterReadingUnit> fRespMRU = MainActivity.client.find(fReqMRU);

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
                        cardText += readings.get(i).get("dblMeterReading") + " " + fRespMRU.getObjects().get(flag).getStrName() + "<br />";
                    }
                }

                mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                        .setText(Html.fromHtml(cardText))
                        .setAttributionIcon(R.drawable.little_logo));
            }

        //
        // Card 8 [o]
        // dv_intAssetLocationID [o]
        // cf_assetAddressString [o]
        //
            cardText = "<font color=\"yellow\"><b>Address:</b></font><br />";
            if (fRespA.getObject().getExtraFields().get("cf_assetAddressString") != null) {
                cardText += fRespA.getObject().getExtraFields().get("cf_assetAddressString");
            } else {
                cardText += "<font color=\"gray\"><i>Not entered.</i></font>";
            }

            mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                    .setText(Html.fromHtml(cardText))
                    .setAttributionIcon(R.drawable.little_logo));
        }


        //
        // Stock Locations [x]
        // fReqS.setFields("dv_intFacilityID, strAisle, strRow, strBin, qtyOnHand, qtyMinQty");
        // dv_intFacilityID
        // strAisle
        // strRow
        // strBin
        // qtyOnHand
        // qtyMinQty
        //
        if (fRespA.getObject().getIntSuperCategorySysCode() == 4) {
            FindRequest<Stock> fReqS = MainActivity.client.prepareFind(Stock.class);
            fReqS.setFields("dv_intFacilityID, strAisle, strRow, strBin, qtyOnHand, qtyMinQty");

            FindFilter assetIDFilter = new FindFilter();
            assetIDFilter.setQl("intAssetID = ?");
            List< Object > AParam = Arrays.asList((Object) mID);
            assetIDFilter.setParameters(AParam);

            FindFilter qtyFilter = new FindFilter();
            qtyFilter.setQl("qtyOnHand > ?");
            List< Object > qtyParam = Arrays.asList((Object) 0.0D);
            qtyFilter.setParameters(qtyParam);

            List< FindFilter > filters = new ArrayList<>();
            filters.add(assetIDFilter);
            filters.add(qtyFilter);
            fReqS.setFilters(filters);

            FindResponse<Stock> fRespS = MainActivity.client.find(fReqS);

            for (int i = 0; i < fRespS.getTotalObjects(); i++) {
                cardText = "<font color=\"yellow\"><b>Stock Location:</b></font> ";
                if (fRespS.getObjects().get(i).getExtraFields().get("dv_intFacilityID") != null) {
                    cardText += fRespS.getObjects().get(i).getExtraFields().get("dv_intFacilityID") + "<br />";
                } else {
                    cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
                }

                cardText += "<font color=\"yellow\"><b>Aisle, Row, Bin Number:</b></font><br />";
                if (fRespS.getObjects().get(i).getStrAisle() != null) {
                    cardText += fRespS.getObjects().get(i).getStrAisle() + ", ";
                } else {
                    cardText += "<font color=\"gray\"><i>Not entered, </i></font><br />";
                }
                if (fRespS.getObjects().get(i).getStrRow() != null) {
                    cardText += fRespS.getObjects().get(i).getStrRow() + ", ";
                } else {
                    cardText += "<font color=\"gray\"><i>Not entered, </i></font><br />";
            }
                if (fRespS.getObjects().get(i).getStrBin() != null) {
                    cardText += fRespS.getObjects().get(i).getStrBin() + "<br />";
                } else {
                    cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
                }

                cardText += "<font color=\"yellow\"><b>Quantity On Hand:</b></font> ";
                if (fRespS.getObjects().get(i).getQtyOnHand() != null) {
                    cardText += fRespS.getObjects().get(i).getQtyOnHand() + "<br />";
                } else {
                    cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
                }

                if (fRespS.getObjects().get(i).getQtyMinQty() != null) {
                    cardNote = "<font color=\"yellow\"><b>Minimum Quantity:</b></font> " + fRespS.getObjects().get(i).getQtyMinQty();
                } else {
                    cardNote = "<font color=\"gray\"></i>No minimum quantity</i></font>";
                }

                mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                        .setText(Html.fromHtml(cardText))
                        .setFootnote(Html.fromHtml(cardNote))
                        .setAttributionIcon(R.drawable.little_logo));
            }
        }


        //
        // Finished
        //
        mCardScroller.setSelection(0);
    }
}
