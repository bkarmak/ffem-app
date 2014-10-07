package org.akvo.caddisfly.ui.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.akvo.caddisfly.Config;
import org.akvo.caddisfly.R;
import org.akvo.caddisfly.app.MainApp;
import org.akvo.caddisfly.ui.fragment.CameraFragment;
import org.akvo.caddisfly.ui.fragment.ResultFragment;
import org.akvo.caddisfly.util.AlertUtils;
import org.akvo.caddisfly.util.DataHelper;
import org.akvo.caddisfly.util.FileUtils;
import org.akvo.caddisfly.util.ImageUtils;
import org.akvo.caddisfly.util.PhotoHandler;
import org.akvo.caddisfly.util.PreferencesHelper;
import org.akvo.caddisfly.util.PreferencesUtils;
import org.akvo.caddisfly.util.ShakeDetector;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProgressActivity extends Activity implements ResultFragment.ResultDialogListener {

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            InitializeTest();
        }
    };
    private final Handler delayHandler = new Handler();
    private final PhotoTakenHandler mPhotoTakenHandler = new PhotoTakenHandler((ProgressActivity) this);
    private PowerManager.WakeLock wakeLock;
    private int mInterval = 4000;
    private int mTestType;
    // The folder path where the photos will be stored
    private String mFolderName;
    private CameraFragment mCameraFragment;
    //private ProgressBar mSingleProgress;
    private Timer timer;
    private Runnable delayRunnable;
    private Animation mSlideInRight;
    private Animation mSlideOutLeft;
    private TextView mTitleText;
    private TextView mRemainingText;
    private ProgressBar mProgressBar;
    private TextView mTimeText;
    private long mNextAlarmTime;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    //private MediaPlayer mMediaPlayer;
    private long mId = -1;
    private int mIndex = -1;
    private int mTestTotal;
    private boolean mShakeDevice;
    private boolean mTestCompleted;
    // Track if the test was just started in which case it can be cancelled by back button
    private boolean mWaitingForFirstShake;
    private boolean mWaitingForShake = true;
    private boolean mWaitingForStillness = false;
    private long mLocationId;
    private TextView mRemainingValueText;
    private ViewAnimator mViewAnimator;

    private boolean isCalibration;

    @Override
    public void onAttachedToWindow() {

        // disable the key guard when device wakes up and shake alert is displayed
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_progress);

        this.setTitle(R.string.appName);

        // Gradient shading for title
        mTitleText = (TextView) findViewById(R.id.titleText);
        Shader textShader = new LinearGradient(0, 0, 0, mTitleText.getPaint().getTextSize(),
                new int[]{Color.rgb(28, 53, 63), Color.rgb(44, 85, 103)},
                new float[]{0, 1}, Shader.TileMode.CLAMP);
        mTitleText.getPaint().setShader(textShader);
        //mProgressLayout = (LinearLayout) findViewById(R.id.progressLayout);
        //mStillnessLayout = (LinearLayout) findViewById(R.id.stillnessLayout);
        //mShakeLayout = (LinearLayout) findViewById(R.id.shakeLayout);
        mRemainingValueText = (TextView) findViewById(R.id.remainingValueText);
        mRemainingText = (TextView) findViewById(R.id.remainingText);
        mProgressBar = (ProgressBar) findViewById(R.id.testProgressBar);
        mTimeText = (TextView) findViewById(R.id.timeText);
        //mPlaceInStandText = (TextView) findViewById(R.id.placeInStandText);
        Button mNextButton = (Button) findViewById(R.id.nextButton);

        Button shakeButton = (Button) findViewById(R.id.shakeButton);
        shakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shakeDone();
            }
        });

        mViewAnimator = (ViewAnimator) findViewById(R.id.viewAnimator);
        mSlideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        mSlideOutLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);


        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mViewAnimator.showNext();

                if (!mShakeDevice) {
                    mViewAnimator.showNext();
                }

                mSensorManager.registerListener(mShakeDetector, mAccelerometer,
                        SensorManager.SENSOR_DELAY_UI);

            }
        });

        mViewAnimator.setInAnimation(mSlideInRight);
        mViewAnimator.setOutAnimation(mSlideOutLeft);

        //mSingleProgress = (ProgressBar) findViewById(R.id.singleProgress);

        //Set up the shake detector
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mShakeDetector = new ShakeDetector(new ShakeDetector.OnShakeListener() {
            @Override
            public void onShake() {
                if (mWaitingForShake) {

                    shakeDone();
                } else {
                    if (!mWaitingForStillness && mCameraFragment != null) {
                        mWaitingForStillness = true;
                        mViewAnimator.showNext();
/*
                        mStillnessLayout.setVisibility(View.VISIBLE);
                        mShakeLayout.setVisibility(View.GONE);
                        mProgressLayout.setVisibility(View.GONE);
*/
                        if (mCameraFragment != null) {
                            try {
                                mCameraFragment.stopCamera();
                                mCameraFragment.dismiss();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            //mViewAnimator.setVisibility(View.GONE);
                            //mStillnessLayout.setVisibility(View.GONE);
                            //mProgressLayout.setVisibility(View.GONE);
                            //mShakeLayout.setVisibility(View.GONE);

                            showError(getString(R.string.testInterrupted), null);
                        }

                    }
                }
            }
        }, new ShakeDetector.OnNoShakeListener() {
            @Override
            public void onNoShake() {

                if (!mWaitingForShake && mWaitingForStillness) {
                    mWaitingForStillness = false;
                    dismissShakeAndStartTest();
                }
            }
        });
        mShakeDetector.minShakeAcceleration = 5;
        mShakeDetector.maxShakeDuration = 2000;
    }

    private void shakeDone() {
        mWaitingForShake = false;
        mWaitingForStillness = true;

        mWaitingForFirstShake = false;
       /* if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }*/
        mViewAnimator.showNext();

    }

    private void showError(String message, Bitmap bitmap) {
        cancelService();
        AlertUtils.showError(this, R.string.error, message, bitmap, R.string.retry,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FileUtils.deleteFolder(new File(mFolderName));
                        startNewTest(mTestType);
                        InitializeTest();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(getIntent());
                        setResult(Activity.RESULT_CANCELED, intent);
                        cancelService();
                        finish();
                    }
                }
        );
    }

    /**
     * Start the test by displaying the progress bar
     */
    private void dismissShakeAndStartTest() {
        /*if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }*/
        mSensorManager.unregisterListener(mShakeDetector);
        startTest(mFolderName);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        // Acquire a wake lock while waiting for user action
/*
        if (wakeLock == null || !wakeLock.isHeld()) {
            PowerManager pm = (PowerManager) getApplicationContext()
                    .getSystemService(Context.POWER_SERVICE);
            wakeLock = pm
                    .newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                            | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
            wakeLock.acquire();
        }
*/

        MainApp mainApp = (MainApp) getApplicationContext();
        // Get intent, action and MIME type
        Intent intent = getIntent();
        //String action = intent.getAction();
        //String type = intent.getType();

        mTestType = mainApp.currentTestType;

        getSharedPreferences();

        isCalibration = intent.getBooleanExtra("isCalibration", false);

        mIndex = intent.getIntExtra("position", -1);
        if (isCalibration) {
            mLocationId = -1;
            mFolderName = "";
            mId = -1;
        } else {
            mLocationId = PreferencesHelper.getCurrentLocationId(this, intent);
        }

        // If test is already completed go back to home page
        if (mTestCompleted) {
            startHomeActivity(getApplicationContext());
            return;
        }

        if (mFolderName != null && !mFolderName.isEmpty()) {

            if (!hasTestCompleted(mFolderName)) {
                // If test was automatically started then initialize
                if (getIntent().getBooleanExtra("alarm", false)) {
                    getIntent().removeExtra("alarm");
                    InitializeTest();
                }
                return;
            }

            // Test is complete so cancel
            cancelService();

        } else if (mTestType > -1) {

            mRemainingText.setVisibility(View.GONE);
            mRemainingValueText.setVisibility(View.GONE);
            mProgressBar.setMax(mTestTotal);
            mProgressBar.setProgress(0);
            startNewTest(mTestType);

            if (mShakeDevice) {
                mViewAnimator.setInAnimation(null);
                mViewAnimator.setOutAnimation(null);
                mViewAnimator.setDisplayedChild(0);
                mViewAnimator.setInAnimation(mSlideInRight);
                mViewAnimator.setOutAnimation(mSlideOutLeft);

/*
                mShakeLayout.setVisibility(View.VISIBLE);
                mStillnessLayout.setVisibility(View.GONE);
                mProgressLayout.setVisibility(View.GONE);
*/
                mWaitingForFirstShake = true;

                mSensorManager.unregisterListener(mShakeDetector);

                mWaitingForShake = true;
                mWaitingForStillness = false;

                mViewAnimator.showNext();
                mSensorManager.registerListener(mShakeDetector, mAccelerometer,
                        SensorManager.SENSOR_DELAY_UI);

            } else {

                mWaitingForStillness = true;
                mWaitingForShake = false;
                mWaitingForFirstShake = false;

                mViewAnimator.showNext();
                mViewAnimator.showNext();
                mSensorManager.registerListener(mShakeDetector, mAccelerometer,
                        SensorManager.SENSOR_DELAY_UI);

/*
                mStillnessLayout.setVisibility(View.VISIBLE);
                mShakeLayout.setVisibility(View.GONE);
                mProgressLayout.setVisibility(View.GONE);
*/
                //displayInfo();
                //startTest(getApplicationContext(), mFolderName);
            }
        } else {
            startHomeActivity(getApplicationContext());
        }
    }

    private void InitializeTest() {

        if (wakeLock == null || !wakeLock.isHeld()) {
            PowerManager pm = (PowerManager) getApplicationContext()
                    .getSystemService(Context.POWER_SERVICE);
            //noinspection deprecation
            wakeLock = pm
                    .newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                            | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
            wakeLock.acquire();
        }

        if (mShakeDevice) {

            mWaitingForShake = true;
            mWaitingForStillness = false;
            mSensorManager.unregisterListener(mShakeDetector);
            mShakeDetector.minShakeAcceleration = 5;
            mShakeDetector.maxShakeDuration = 2000;
        } else {
            mWaitingForShake = false;
            mWaitingForStillness = true;
        }

        mViewAnimator.setInAnimation(null);
        mViewAnimator.setOutAnimation(null);
        mViewAnimator.setDisplayedChild(0);
        mViewAnimator.setInAnimation(mSlideInRight);
        mViewAnimator.setOutAnimation(mSlideOutLeft);

/*
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,
                SensorManager.SENSOR_DELAY_UI);
*/

    }

    private void getSharedPreferences() {

        MainApp mainContext = (MainApp) getApplicationContext();

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        switch (mTestType) {
            case Config.FLUORIDE_INDEX:
                mTestTotal = 1;
                mTitleText.setText(R.string.fluoride);
                mainContext.setFluorideSwatches();
                break;
            case Config.FLUORIDE_2_INDEX:
                mTestTotal = 1;
                mTitleText.setText(R.string.fluoride2);
                mainContext.setFluoride2Swatches();
                break;
        }

        mTitleText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissShakeAndStartTest();
            }
        });

        mShakeDevice = sharedPreferences.getBoolean("requireShakePref", Config.REQUIRE_SHAKE_DEFAULT);

        mFolderName = PreferencesUtils.getString(this, R.string.runningTestFolder, "");

        mTestCompleted = sharedPreferences.getBoolean("testCompleted", false);

        if (mId == -1) {
            mId = PreferencesHelper.getCurrentTestId(this, getIntent(), null);
        }
    }

    private void startNewTest(int testType) {

        if (isCalibration) {
            File calibrateFolder = new File(
                    FileUtils.getStoragePath(this, -1,
                            String.format("%s/%d/%d/small/", Config.CALIBRATE_FOLDER, mTestType,
                                    mIndex),
                            false
                    )
            );

            FileUtils.deleteFolder(calibrateFolder);
        } else {
            mFolderName = getNewFolderName();
            FileUtils.deleteFolder(new File(mFolderName));
        }

        Context context = getApplicationContext();
        PreferencesUtils.setInt(context, R.string.currentSamplingCountKey, 0);

        // store the folder name of the current test to be able to refer to if the app is restarted
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.runningTestFolder), mFolderName);
        editor.putInt(PreferencesHelper.CURRENT_TEST_TYPE_KEY, testType);
        editor.apply();
    }

    private void cancelService() {
        releaseResources();
    }

    private void releaseResources() {

        mSensorManager.unregisterListener(mShakeDetector);
        delayHandler.removeCallbacks(delayRunnable);
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Context context = getApplicationContext();
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(getString(R.string.runningTestFolder)); //NON-NLS
        editor.remove(PreferencesHelper.CURRENT_TEST_ID_KEY);
        editor.apply();

      /*  if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }*/
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    void displayInfo() {

        ArrayList<String> imagePaths = FileUtils
                .getFilePaths(this, mFolderName, "/small/", mLocationId);
        int doneCount = imagePaths.size();

        if (doneCount > 0) {

            //Context context = getApplicationContext();
            // boolean is24HourFormat = android.text.format.DateFormat.is24HourFormat(context);
            //String timePattern = context.getString(is24HourFormat ? R.string.twentyFourHourTime : R.string.twelveHourTime);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MILLISECOND, mInterval);

            startTimeDisplay(cal.getTimeInMillis());

            // Using SimpleDateFormat to display seconds also
            //DateFormat timeFormat = new SimpleDateFormat(timePattern);
            //mTimeText.setText(timeFormat.format(cal.getTimeInMillis()));

            mProgressBar.setMax(mTestTotal);
            mProgressBar.setProgress(doneCount);
        }

        mRemainingValueText.setText(String.valueOf(mTestTotal - doneCount));
    }

    private String formatTime(long time) {
        String res = "";
        res += time / 60 + ":";
        if (time % 60 < 10) {
            res += "0";
        }
        res += (time % 60);
        return res;
    }

    private void displayRemainingTime() {
        long timeLeft = (mNextAlarmTime - System.currentTimeMillis()) / 1000;
        mTimeText.setText(formatTime(timeLeft));

    }

    private void startTimeDisplay(long nextTime) {
        if (timer != null) {
            timer.cancel();
        }
        mNextAlarmTime = nextTime - Config.INITIAL_DELAY - Config.INITIAL_DELAY;
        displayRemainingTime();
        timer = new Timer(5000, 5000);
        timer.start();

    }

    void startTest(final String folderName) {

        mWaitingForShake = false;
        mWaitingForFirstShake = false;
        mWaitingForStillness = false;

        mShakeDetector.minShakeAcceleration = 0.5;
        mShakeDetector.maxShakeDuration = 3000;
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,
                SensorManager.SENSOR_DELAY_UI);

        (new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
/*
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
*/
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);

                MainApp mainContext = (MainApp) getApplicationContext();

                if (!hasTestCompleted(folderName)) {

                    PhotoHandler photoHandler = new PhotoHandler(mainContext, mPhotoTakenHandler,
                            mIndex, folderName, mTestType);

                    final FragmentTransaction ft = getFragmentManager().beginTransaction();

                    Fragment prev = getFragmentManager().findFragmentByTag("cameraDialog");
                    if (prev != null) {
                        ft.remove(prev);
                    }
                    ft.addToBackStack(null);

                    mCameraFragment = CameraFragment.newInstance();
                    mCameraFragment.pictureCallback = photoHandler;

                    if (wakeLock == null || !wakeLock.isHeld()) {
                        PowerManager pm = (PowerManager) getApplicationContext()
                                .getSystemService(Context.POWER_SERVICE);
                        //noinspection deprecation
                        wakeLock = pm
                                .newWakeLock(PowerManager.FULL_WAKE_LOCK
                                        | PowerManager.ACQUIRE_CAUSES_WAKEUP
                                        | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
                        wakeLock.acquire();
                    }

                    delayRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mCameraFragment.show(ft, "cameraDialog");

                        }
                    };

                    delayHandler.postDelayed(delayRunnable, Config.INITIAL_DELAY);
                }
            }
        }).execute();
    }

    private boolean hasSamplingCompleted() {

        Context context = getApplicationContext();

        int samplingCount = PreferencesUtils.getInt(context, R.string.currentSamplingCountKey, 0);
        return samplingCount >= PreferencesUtils
                .getInt(context, R.string.samplingCountKey, Config.SAMPLING_COUNT_DEFAULT);
    }

    boolean hasTestCompleted(String folderName) {

        if (!hasSamplingCompleted()) {
            return false;
        }
        Context context = getApplicationContext();
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        mLocationId = PreferencesHelper.getCurrentLocationId(this, null);

        if (sharedPreferences.getString("runningTestFolder", "").isEmpty()) {
            return true;
        } else {
            ArrayList<String> imagePaths = FileUtils
                    .getFilePaths(this, folderName, "/small/", mLocationId);
            mIndex = imagePaths.size();
            return imagePaths.size() >= mTestTotal;
        }
    }

    private String getNewFolderName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Config.FOLDER_NAME_DATE_FORMAT,
                Locale.US);
        return dateFormat.format(new Date()).trim() + "-" + mTestType;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (mWaitingForFirstShake) {
            cancelService();
        }
    }

    @Override
    public void onBackPressed() {
        if (mWaitingForFirstShake) {
            cancelService();
            Intent intent = new Intent(getIntent());
            this.setResult(Activity.RESULT_CANCELED, intent);
            finish();
        } else {
            //Clear the activity back stack
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_cancel:
                cancelService();
                Intent intent = new Intent(getIntent());
                this.setResult(Activity.RESULT_CANCELED, intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.progress, menu);
        return true;
    }

   /* public void dialogCancelled() {
        cancelService();
        Intent intent = new Intent(getIntent());
        this.setResult(Activity.RESULT_CANCELED, intent);

        finish();
    }*/

    void unregisterShakeSensor() {
        mSensorManager.unregisterListener(mShakeDetector);
    }

    void startHomeActivity(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);

        finish();
    }

    void sendResult(final Message msg) {
        if (mFolderName != null && !mFolderName.isEmpty()) {
            if (msg != null && msg.getData() != null) {

                final double result = msg.getData().getDouble(Config.RESULT_VALUE_KEY, -1);
                final int quality = msg.getData().getInt(Config.QUALITY_KEY, 0);

                int minAccuracy = PreferencesUtils
                        .getInt(this, R.string.minPhotoQualityKey, Config.MINIMUM_PHOTO_QUALITY);

                String title = DataHelper.getTestTitle(this, mTestType);

                if (result >= 0 && quality >= minAccuracy) {
                    ResultFragment mResultFragment = ResultFragment.newInstance(title, result, msg);
                    final FragmentTransaction ft = getFragmentManager().beginTransaction();

                    Fragment prev = getFragmentManager().findFragmentByTag("resultDialog");
                    if (prev != null) {
                        ft.remove(prev);
                    }
                    mResultFragment.setCancelable(false);
                    mResultFragment.show(ft, "resultDialog");
                } else {
                    sendResult2(msg);
                }
            } else {
                sendResult2(msg);
            }
        } else {
            sendResult2(msg);
        }
    }

    void sendResult2(Message msg) {
        mSensorManager.unregisterListener(mShakeDetector);

        mId = PreferencesHelper.getCurrentTestId(this, null, msg.getData());

        final double result = msg.getData().getDouble(Config.RESULT_VALUE_KEY, -1);
        final int quality = msg.getData().getInt(Config.QUALITY_KEY, 0);
        final int resultColor = msg.getData().getInt(Config.RESULT_COLOR_KEY, 0);
        String message = getString(R.string.testFailedMessage);

        int minAccuracy = PreferencesUtils
                .getInt(this, R.string.minPhotoQualityKey, Config.MINIMUM_PHOTO_QUALITY);

        if (quality < minAccuracy) {
            message = String.format(getString(R.string.testFailedQualityMessage), minAccuracy);
        }

        if (result < 0 || quality < minAccuracy) {
            showError(message, ImageUtils.getAnalysedBitmap(msg.getData().getString("file")));
        } else {

            releaseResources();

            Intent intent = new Intent(getIntent());
            intent.putExtra(Config.RESULT_COLOR_KEY, resultColor);
            intent.putExtra(Config.QUALITY_KEY, quality);

            if (mFolderName != null && !mFolderName.isEmpty()) {
                if (msg.getData() != null) {
                    intent.putExtra(PreferencesHelper.FOLDER_NAME_KEY, mFolderName);
                    intent.putExtra(PreferencesHelper.CURRENT_TEST_ID_KEY, mId);

                    intent.putExtra("result", result);
                    //intent.putExtra("questionId", mQuestionId);
                    intent.putExtra("response", String.valueOf(result));
                }
            }
            this.setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    public void onFinishDialog(Bundle bundle) {
        Message msg = new Message();
        msg.setData(bundle);
        sendResult2(msg);
    }

    private static class PhotoTakenHandler extends Handler {

        private final WeakReference<ProgressActivity> mService;

        public PhotoTakenHandler(ProgressActivity service) {
            mService = new WeakReference<ProgressActivity>(service);
        }

        @Override
        public void handleMessage(Message msg) {

            ProgressActivity service = mService.get();

            service.mCameraFragment.dismiss();

            Bundle bundle = msg.getData();

            if (bundle != null) {
                String folderName = msg.getData()
                        .getString(PreferencesHelper.FOLDER_NAME_KEY); //NON-NLS
                if (service.hasTestCompleted(folderName)) {
                    service.unregisterShakeSensor();
                    service.sendResult(msg);
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.MILLISECOND, (service.mInterval - Config.INITIAL_DELAY
                            - Config.INITIAL_DELAY));


                    service.displayInfo();

                    if (service.wakeLock != null && service.wakeLock.isHeld()) {
                        service.wakeLock.release();
                    }
                }
            }
        }
    }

    public class Timer extends CountDownTimer {

        public Timer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            timer = new Timer(5000, 5000);
            timer.start();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            displayRemainingTime();
        }
    }
}
