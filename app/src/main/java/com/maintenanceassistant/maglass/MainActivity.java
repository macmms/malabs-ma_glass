package com.maintenanceassistant.maglass;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.ma.cmms.api.client.BasicCredentials;
import com.ma.cmms.api.client.MaCmmsClient;
import com.ma.cmms.api.client.dto.Asset;
import com.ma.cmms.api.client.dto.User;
import com.ma.cmms.api.crud.AddRequest;
import com.ma.cmms.api.crud.AddResponse;
import com.ma.cmms.api.crud.FindFilter;
import com.ma.cmms.api.crud.FindRequest;
import com.ma.cmms.api.crud.FindResponse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Jake Uskoski on 2015-07-10.
 */

public class MainActivity extends Activity {
    private static final int KEY_SWIPE_DOWN = 4;
    private CardScrollView mCardScroller;
    private View mView;
    private GestureDetector mGestureDetector;
    public static Long SUPERUSER = null;
    public static Long SITEID = null;
    public static MaCmmsClient client;
    FindResponse< User > fRespU;
    AddResponse< Asset > aRespA;
    Asset newA;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        client = new MaCmmsClient(new BasicCredentials(getString(R.string.app_key), getString(R.string.acs_key), getString(R.string.sec_key)), getString(R.string.user_URL));

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        if (SUPERUSER == null) {
            FindRequest<User> fReqU = client.prepareFind(User.class);
            fReqU.setFields("id, strFullName");
            fReqU.setOrderBy("id");
            fReqU.setMaxObjects(1);
            fRespU = client.find(fReqU);
            if (fRespU.getError() != null) {
                finish();
            } else {
                SUPERUSER = fRespU.getObjects().get(0).getId();
            }
        }

        if (SITEID == null) {
            FindRequest< Asset > fReqA = client.prepareFind(Asset.class);
            fReqA.setFields("id");
            fReqA.setOrderBy("id");
            fReqA.setMaxObjects(1);

            FindFilter siteFilter = new FindFilter();
            siteFilter.setQl("bolIsSite = ?");
            List< Object > params = Arrays.asList((Object) 1);
            siteFilter.setParameters(params);

            fReqA.setFilters(Arrays.asList(siteFilter));

            FindResponse< Asset > fRespA = client.find(fReqA);
            if (fRespA.getError() != null) {
                finish();
            } else {
                SITEID = fRespA.getObjects().get(0).getId();
            }
        }

        mView = buildView();
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
                openOptionsMenu();
            }
        });

        mGestureDetector = createGestureDetector(this);
        setContentView(mCardScroller);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId ==  Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.work_orders:
                    getMainResults("Work Orders");
                    break;
                case R.id.scheduled_maintenance:
                    getMainResults("Scheduled Maintenance");
                    break;
                case R.id.scan_qr_code:
                    scanAsset();
                    break;
                case R.id.generate_asset:
                    pickStatus();
                    break;
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
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
                    //Toast.makeText(MainActivity.this, "Swipe down with two fingers to exit", Toast.LENGTH_SHORT).show();
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
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if (keyCode == KEY_SWIPE_DOWN)
        {
            Toast.makeText(MainActivity.this, "Swipe down with two fingers to exit", Toast.LENGTH_SHORT).show();
            return false;
        }

        super.onKeyDown(keyCode, event);
        return false;
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

    private View buildView() {
        return new CardBuilder(this, CardBuilder.Layout.MENU)
                .setText(Html.fromHtml("<b>" + getResources().getString(R.string.app_name) + "</b>"))
                .setIcon(R.drawable.logo)
                .getView();
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu){
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS || featureId ==  Window.FEATURE_OPTIONS_PANEL) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    public void getMainResults(String platform){
        Intent resultsIntent = new Intent(this, ResultsActivity.class);
        resultsIntent.putExtra(ResultsActivity.SEARCH, platform);
        startActivity(resultsIntent);
    }

    public void scanAsset() {
        Intent objIntent = new Intent("com.google.zxing.client.android.SCAN");
        objIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        startActivityForResult(objIntent, ResultsActivity.SCAN_QR);
    }

    private void pickStatus() {
        Intent intent = new Intent(this, PickStatusActivity.class);
        intent.putExtra(ResultsActivity.SEARCH, ResultsActivity.GENERATE);
        startActivityForResult(intent, ResultsActivity.PICK_STATUS);
    }

    private void delay(String text, String note) {
        Intent intent = new Intent(this, DelayActivity.class);
        intent.putExtra(ResultsActivity.TEXT, text);
        intent.putExtra(ResultsActivity.NOTE, note);
        startActivityForResult(intent, ResultsActivity.DELAY);
    }

    private void displaySpeechRecognizer(String text) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(ResultsActivity.EXTRA_PROMPT, text);
        startActivityForResult(intent, ResultsActivity.SPEECH_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ResultsActivity.SCAN_QR && resultCode == RESULT_OK) {
            String result = data.getStringExtra("SCAN_RESULT");
            String[] parsedResults = result.split("/?a=");
            String newId = parsedResults[parsedResults.length - 1];
            try {
                Intent resultIntent = new Intent(this, AssetActivity.class);
                resultIntent.putExtra(ResultsActivity.ID, Long.parseLong(newId, 10));
                startActivity(resultIntent);
            } catch (NumberFormatException e) {
                AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                audio.playSoundEffect(Sounds.ERROR);
                Toast.makeText(getApplicationContext(), "Invalid ID from scan", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == ResultsActivity.PICK_STATUS && resultCode == RESULT_OK) {
            long id = data.getLongExtra(ResultsActivity.ID, ResultsActivity.EMPTY);
            newA = new Asset();
            newA.setIntSiteID(SITEID);
            newA.setIntCategoryID(id);

            delay("Tap to Describe Asset", "This will begin speech recognition.");
        } else if (requestCode == ResultsActivity.DELAY && resultCode == RESULT_OK) {
            displaySpeechRecognizer("Add a note to the asset:");
        } else if (requestCode == ResultsActivity.SPEECH_REQUEST && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            AddRequest< Asset > aReqA = client.prepareAdd(Asset.class);
            aReqA.setFields("id");
            newA.setStrName("New Generated Asset");
            newA.setStrDescription("\"" + results.get(0) + "\" - Generated from Google Glass by: " + fRespU.getObjects().get(0).getStrFullName());
            aReqA.setObject(newA);

            aRespA = client.add(aReqA);

            if (aRespA.getError() == null) {
                Toast.makeText(this, "Asset successfully generated", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent(this, AssetActivity.class);
                resultIntent.putExtra(ResultsActivity.ID, aRespA.getObject().getId());
                startActivity(resultIntent);
            } else {
                Toast.makeText(this, "Failed to generate asset", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
