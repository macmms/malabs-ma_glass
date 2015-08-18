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
import com.ma.cmms.api.client.dto.ScheduledTask;
import com.ma.cmms.api.client.dto.WorkOrderTask;
import com.ma.cmms.api.crud.ChangeRequest;
import com.ma.cmms.api.crud.ChangeResponse;
import com.ma.cmms.api.crud.FindFilter;
import com.ma.cmms.api.crud.FindRequest;
import com.ma.cmms.api.crud.FindResponse;
import com.maintenanceassistant.maglass.adapters.MainAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by Jake Uskoski on 2015-07-17.
 */
public class SeeTasksActivity extends Activity{
    private String mActivityController;
    private CardScrollView mCardScroller;
    private List<CardBuilder> mCards;
    private GestureDetector mGestureDetector;
    private int mPosition = 0;
    private List<WorkOrderTask> wtObj;
    private List<ScheduledTask> stObj;
    private String tempData = null;
    private Long mID;

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

        getTasks(mActivityController, 0);

        if (mCards.size() == 0){
            AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            audio.playSoundEffect(Sounds.ERROR);
            Toast.makeText(this, "No tasks", Toast.LENGTH_SHORT).show();
            finish();
        }

        mCardScroller.setAdapter(new MainAdapter(mCards));

        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (ResultsActivity.WORK_ORDER.equals(mActivityController)) {
                    completeTask(position);
                }
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

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        if (wtObj.get(mPosition).getIntTaskType() == 1) {
            intent.putExtra(ResultsActivity.EXTRA_PROMPT, "What was the result?");
        } else if (wtObj.get(mPosition).getIntTaskType() == 2) {
            intent.putExtra(ResultsActivity.EXTRA_PROMPT, "What was the reading?");
        }
        startActivityForResult(intent, ResultsActivity.SPEECH_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ResultsActivity.SPEECH_REQUEST && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            tempData = results.get(0);

            ChangeRequest<WorkOrderTask> cReqWoT = MainActivity.client.prepareChange(WorkOrderTask.class);
            WorkOrderTask obj = wtObj.get(mPosition);
            obj.setIntCompletedByUserID(MainActivity.SUPERUSER);

            if (wtObj.get(mCardScroller.getSelectedItemPosition()).getIntTaskType() == 1) {

                obj.setDtmDateCompleted(new Date());
                obj.setStrResult(tempData);

                cReqWoT.setObject(obj);
                cReqWoT.setChangeFields("dtmDateCompleted, intCompletedByUserID, strResult");

                ChangeResponse<WorkOrderTask> cRespWoT = MainActivity.client.change(cReqWoT);
                Toast.makeText(getApplicationContext(), "Task Completed", Toast.LENGTH_SHORT).show();

                mCardScroller.setAdapter(new MainAdapter(mCards));

                // Handle the TAP event.
                mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (ResultsActivity.WORK_ORDER.equals(mActivityController)) {
                            completeTask(position);
                        }
                    }
                });

                mGestureDetector = createGestureDetector(this);
                setContentView(mCardScroller);
            } else {
                obj.setDtmDateCompleted(new Date());
                obj.setStrResult(tempData);

                cReqWoT.setObject(obj);
                cReqWoT.setChangeFields("dtmDateCompleted, intCompletedByUserID, strResult");

                ChangeResponse<WorkOrderTask> cRespWoT = MainActivity.client.change(cReqWoT);
                Toast.makeText(getApplicationContext(), "Task Completed", Toast.LENGTH_SHORT).show();

                mCardScroller = new CardScrollView(this);
                mCards = new ArrayList<>();
                getTasks(mActivityController, mPosition);
                mCardScroller.setAdapter(new MainAdapter(mCards));

                // Handle the TAP event.
                mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (ResultsActivity.WORK_ORDER.equals(mActivityController)) {
                            completeTask(position);
                        }
                    }
                });

                mGestureDetector = createGestureDetector(this);
                setContentView(mCardScroller);
            }

        } else if (requestCode == ResultsActivity.SPEECH_REQUEST && resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "Cancelled", Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
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

    private void getTasks(String activityController, int position){
        String cardText;
        String cardNote;
        String cardStamp;

        if (activityController.equals(ResultsActivity.SCH_MAINTENANCE)) {
            FindRequest<ScheduledTask> fReqST = MainActivity.client.prepareFind(ScheduledTask.class);
            fReqST.setFields("id, strDescription, dblTimeEstimatedHours, intOrder, intTaskType, dv_intAssignedToUserID, dv_intAssetID, dv_intMeterReadingUnitID");

            FindFilter pendingActiveQlFilter = new FindFilter();
            pendingActiveQlFilter.setQl("intScheduledMaintenanceID = ?");
            List< Object > params = Arrays.asList((Object) mID);
            pendingActiveQlFilter.setParameters(params);

            fReqST.setFilters(Arrays.asList(pendingActiveQlFilter));

            FindResponse<ScheduledTask> fRespST = MainActivity.client.find(fReqST);

            if (fRespST.getError() != null) {
                return;
            }

            int listSize = fRespST.getTotalObjects();
            stObj = new ArrayList<>();
            for (int i = 0; listSize > i; i++) {
                int flag = 0;
                for (int j = 0; listSize > j; j++) {
                    if (fRespST.getObjects().get(j).getIntOrder() == i + 1) {
                        flag = j;
                        stObj.add(fRespST.getObjects().get(j));
                    }
                }

                if (fRespST.getObjects().get(flag).getStrDescription() != null) {
                    cardText = "<b><font color=\"yellow\">Task:</font> " + fRespST.getObjects().get(flag).getStrDescription() + "</b>";
                } else {
                    cardText = "<b><i>This task has no description.</i></b>";
                }

                if (fRespST.getObjects().get(flag).getExtraFields().get("dv_intAssignedToUserID") != null) {
                    cardText += "<br /><font color=\"yellow\"><b>Assigned to:</b></font> " + fRespST.getObjects().get(flag).getExtraFields().get("dv_intAssignedToUserID");
                } else {
                    cardText += "<br /><font color=\"yellow\"><b>Assigned to:</b></font> <i>No one.</i>";
                }

                if (fRespST.getObjects().get(flag).getIntTaskType() == 0) {
                    cardText += "<br /><font color=\"yellow\"><b>Task Type:</b></font> General";
                } else if (fRespST.getObjects().get(flag).getIntTaskType() == 1) {
                    cardText += "<br /><font color=\"yellow\"><b>Task Type:</b></font> Text Result";
                } else {
                    if (fRespST.getObjects().get(flag).getExtraFields().get("dv_intMeterReadingID") != null) {
                        cardText += "<br /><font color=\"yellow\"><b>Task Type:<b></font> Meter Reading - " + fRespST.getObjects().get(flag).getExtraFields().get("dv_intMeterReadingID");
                    } else {
                        cardText += "<br /><font color=\"yellow\"><b>Task Type:</b></font> Meter Reading - <i>Unit not entered.</i>";
                    }
                }

                if (fRespST.getObjects().get(flag).getDblTimeEstimatedHours() != null) {
                    if (fRespST.getObjects().get(flag).getDblTimeEstimatedHours() != 1.0){
                        cardStamp = fRespST.getObjects().get(flag).getDblTimeEstimatedHours().toString() + " Hours";
                    } else {
                        cardStamp = fRespST.getObjects().get(flag).getDblTimeEstimatedHours().toString() + " Hour";
                    }
                } else {
                    cardStamp = "<font color=\"gray\"><i>No estimated time.</i></font>";
                }

                if (fRespST.getObjects().get(flag).getExtraFields().get("dv_intAssetID") != null) {
                    cardNote = fRespST.getObjects().get(flag).getExtraFields().get("dv_intAssetID").toString();
                } else {
                    cardNote = "<font color=\"gray\"><i>No assigned asset</i></font>";
                }

                mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                        .setText(Html.fromHtml(cardText))
                                .setFootnote(Html.fromHtml(cardNote))
                                .setTimestamp(Html.fromHtml(cardStamp)));
            }
        } else if (activityController.equals(ResultsActivity.WORK_ORDER)) {
            FindRequest<WorkOrderTask> fReqWoT = MainActivity.client.prepareFind(WorkOrderTask.class);
            fReqWoT.setFields("id, strDescription, dtmDateCompleted, dblTimeEstimatedHours, dblTimeSpentHours, intOrder, intTaskType, strResult, dv_intAssignedToUserID, dv_intCompletedByUserID, dv_intAssetID, dv_intMeterReadingUnitID");

            FindFilter pendingActiveQlFilter = new FindFilter();
            pendingActiveQlFilter.setQl("intWorkOrderID = ?");
            List< Object > params = Arrays.asList((Object) mID);
            pendingActiveQlFilter.setParameters(params);

            fReqWoT.setFilters(Arrays.asList(pendingActiveQlFilter));

            FindResponse<WorkOrderTask> fRespWoT = MainActivity.client.find(fReqWoT);

            if (fRespWoT.getError() != null) {
                return;
            }

            int listSize = fRespWoT.getTotalObjects();
            wtObj = new ArrayList<>();
            for (int i = 0; listSize > i; i++) {
                int flag = 0;
                for (int j = 0; listSize > j; j++) {
                    if (fRespWoT.getObjects().get(j).getIntOrder() == i + 1) {
                        flag = j;
                        wtObj.add(fRespWoT.getObjects().get(j));
                        break;
                    }
                }

                if (fRespWoT.getObjects().get(flag).getIntTaskType() != null) {
                    if (fRespWoT.getObjects().get(flag).getStrDescription() != null) {
                        cardText = "<b><font color=\"yellow\">Task:</font> " + fRespWoT.getObjects().get(flag).getStrDescription() + "</b>";
                    } else {
                        cardText = "<font color=\"gray\"><b><i>This task has no description.</i></b></font>";
                    }

                    if (fRespWoT.getObjects().get(flag).getDtmDateCompleted() != null) {
                        if (fRespWoT.getObjects().get(flag).getIntTaskType() == 0) {
                            cardText += "<br /><font color=\"yellow\"><b>Task Type:</b></font> <i>General</i>";
                        } else if (fRespWoT.getObjects().get(flag).getIntTaskType() == 1) {
                            cardText += "<br /><font color=\"yellow\"><b>Task Type:</b></font> Text Result";
                            if (fRespWoT.getObjects().get(flag).getStrResult() != null) {
                                cardText += "<br /><font color=\"yellow\"><b>Result:</b></font> " + fRespWoT.getObjects().get(flag).getStrResult();
                            } else {
                                cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
                            }
                        } else {
                            cardText += "<br /><font color=\"yellow\"><b>Task Type:</b></font> Meter Reading";
                            if (fRespWoT.getObjects().get(flag).getStrResult() != null) {
                                if (fRespWoT.getObjects().get(flag).getExtraFields().get("dv_intMeterReadingUnitID") != null) {
                                    cardText += "<br /><font color=\"yellow\"><b>Result:</b></font> " + fRespWoT.getObjects().get(flag).getStrResult() + " " + fRespWoT.getObjects().get(flag).getExtraFields().get("dv_intMeterReadingUnitID");
                                } else {
                                    cardText += "<br /><font color=\"yellow\"><b>Result:</b></font> " + fRespWoT.getObjects().get(flag).getStrResult() + " <font color=\"gray\"><i>(Unit not entered)</i></font>";
                                }
                            } else {
                                cardText += "<font color=\"gray\"><i>Not entered.</i></font><br />";
                            }
                        }

                        if (fRespWoT.getObjects().get(flag).getExtraFields().get("dv_intAssignedToUserID") != null) {
                            cardText += "<br /><font color=\"yellow\"><b>Completed by:</b></font> " + fRespWoT.getObjects().get(flag).getExtraFields().get("dv_intCompletedByUserID");
                        } else {
                            if (fRespWoT.getObjects().get(flag).getExtraFields().get("dv_intAssignedToUserID") != null) {
                                cardText += "<br /><font color=\"yellow\"><b>Assigned to:</b></font> " + fRespWoT.getObjects().get(flag).getExtraFields().get("dv_intAssignedToUserID");
                            } else {
                                cardText += "<br /><font color=\"yellow\"><b>Assigned to:</b></font> <font color=\"gray\"><i>No one.</i></font>";
                            }
                        }

                        cardText += "<br /><font color=\"yellow\"><b>Completed on:</b></font> " + fRespWoT.getObjects().get(flag).getDtmDateCompleted();

                        if (fRespWoT.getObjects().get(flag).getDblTimeSpentHours() != null) {
                            if (fRespWoT.getObjects().get(flag).getDblTimeSpentHours() != 1.0) {
                                cardStamp = "<font color=\"yellow\"><b>Took:</b></font> " + fRespWoT.getObjects().get(flag).getDblTimeSpentHours().toString() + " Hours";
                            } else {
                                cardStamp = "<font color=\"yellow\"><b>Took:</b></font> " + fRespWoT.getObjects().get(flag).getDblTimeSpentHours().toString() + " Hour";
                            }
                        } else {
                            if (fRespWoT.getObjects().get(flag).getDblTimeEstimatedHours() != null) {
                                if (fRespWoT.getObjects().get(flag).getDblTimeEstimatedHours() != 1.0) {
                                    cardStamp = "<font color=\"yellow\"><b>Estimate:</b></font> " + fRespWoT.getObjects().get(flag).getDblTimeEstimatedHours().toString() + " Hours";
                                } else {
                                    cardStamp = "<font color=\"yellow\"><b>Estimate:</b></font> " + fRespWoT.getObjects().get(flag).getDblTimeEstimatedHours().toString() + " Hour";
                                }
                            } else {
                                cardStamp = "<font color=\"gray\"><i>No time taken.</i></font>";
                            }
                        }
                    } else {
                        if (fRespWoT.getObjects().get(flag).getIntTaskType() == 0) {
                            cardText += "<br /><font color=\"yellow\"><b>Task Type:</b></font> General";
                        } else if (fRespWoT.getObjects().get(flag).getIntTaskType() == 1) {
                            cardText += "<br /><font color=\"yellow\"><b>Task Type:</b></font> Text Result";
                        } else {
                            if (fRespWoT.getObjects().get(flag).getExtraFields().get("dv_intMeterReadingUnitID") != null) {
                                cardText += "<br /><font color=\"yellow\"><b>Task Type:</b></font> Meter Reading - " + fRespWoT.getObjects().get(flag).getExtraFields().get("dv_intMeterReadingUnitID");
                            } else {
                                cardText += "<br /><font color=\"yellow\"><b>Task Type:</b></font> Meter Reading - <color font=\"gray\"><i>Unit not entered.</i></font>";
                            }
                        }

                        if (fRespWoT.getObjects().get(flag).getExtraFields().get("dv_intAssignedToUserID") != null) {
                            cardText += "<br /><font color=\"yellow\"><b>Assigned to:</b></font> " + fRespWoT.getObjects().get(flag).getExtraFields().get("dv_intAssignedToUserID");
                        } else {
                            cardText += "<br /><font color=\"yellow\"><b>Assigned to:</b></font> <font color=\"gray\"><i>No one.</i></font>";
                        }

                        if (fRespWoT.getObjects().get(flag).getDblTimeEstimatedHours() != null) {
                            if (fRespWoT.getObjects().get(flag).getDblTimeEstimatedHours() != 1.0) {
                                cardStamp = "<font color=\"yellow\"><b>Estimate:</b></font> " + fRespWoT.getObjects().get(flag).getDblTimeEstimatedHours().toString() + " Hours";
                            } else {
                                cardStamp = "<font color=\"yellow\"><b>Estimate:</b></font> " + fRespWoT.getObjects().get(flag).getDblTimeEstimatedHours().toString() + " Hour";
                            }
                        } else {
                            cardStamp = "<font color=\"gray\"><i>No estimated time.</i></font>";
                        }
                    }
                    if (fRespWoT.getObjects().get(flag).getExtraFields().get("dv_intAssetID") != null) {
                        cardNote = fRespWoT.getObjects().get(flag).getExtraFields().get("dv_intAssetID").toString();
                    } else {
                        cardNote = cardText += "<font color=\"gray\"><i>No assigned asset</i></font>";
                    }

                    mCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                            .setText(Html.fromHtml(cardText))
                            .setFootnote(Html.fromHtml(cardNote))
                            .setTimestamp(Html.fromHtml(cardStamp)));
                }
            }
        }
        mCardScroller.setSelection(position);
    }

    private void completeTask(int position) {
        WorkOrderTask obj = wtObj.get(position);
        mPosition = position;

        if (obj.getDtmDateCompleted() == null) {
            if (obj.getIntTaskType() != 0) {
                displaySpeechRecognizer();
            } else {
                ChangeRequest< WorkOrderTask > cReqWoT = MainActivity.client.prepareChange(WorkOrderTask.class);

                cReqWoT.setChangeFields("dtmDateCompleted, intCompletedByUserID");

                obj.setDtmDateCompleted(new Date());
                obj.setIntCompletedByUserID(MainActivity.SUPERUSER);
                cReqWoT.setObject(obj);

                ChangeResponse< WorkOrderTask > cRespWoT = MainActivity.client.change(cReqWoT);
                AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

                if(cRespWoT.getError() == null) {
                    audio.playSoundEffect(Sounds.SUCCESS);
                    Toast.makeText(getApplicationContext(), "Task Completed", Toast.LENGTH_SHORT).show();
                } else {
                    audio.playSoundEffect(Sounds.ERROR);
                    Toast.makeText(getApplicationContext(), "An error occurred", Toast.LENGTH_SHORT).show();

                }

                mCardScroller = new CardScrollView(this);
                mCards = new ArrayList<>();
                getTasks(mActivityController, mPosition);
                mCardScroller.setAdapter(new MainAdapter(mCards));
            }
        } else {
            Toast.makeText(getApplicationContext(), "Task Is Already Complete", Toast.LENGTH_SHORT).show();
        }
    }
}
