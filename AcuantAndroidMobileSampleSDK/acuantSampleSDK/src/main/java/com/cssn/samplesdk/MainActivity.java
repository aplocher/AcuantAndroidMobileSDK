/*
 *
 */
package com.cssn.samplesdk;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.acuant.mobilesdk.AcuantAndroidMobileSDKController;
import com.acuant.mobilesdk.AcuantErrorListener;
import com.acuant.mobilesdk.Card;
import com.acuant.mobilesdk.CardCroppingListener;
import com.acuant.mobilesdk.CardType;
import com.acuant.mobilesdk.DriversLicenseCard;
import com.acuant.mobilesdk.ErrorType;
import com.acuant.mobilesdk.FacialData;
import com.acuant.mobilesdk.LicenseActivationDetails;
import com.acuant.mobilesdk.LicenseDetails;
import com.acuant.mobilesdk.MedicalCard;
import com.acuant.mobilesdk.PassportCard;
import com.acuant.mobilesdk.Permission;
import com.acuant.mobilesdk.ProcessImageRequestOptions;
import com.acuant.mobilesdk.Region;
import com.acuant.mobilesdk.WebServiceListener;
import com.acuant.mobilesdk.FacialRecognitionListener;
import com.acuant.mobilesdk.task.CroppingTaskManual;
import com.acuant.mobilesdk.util.Utils;
import com.cssn.mobilesdk.utilities.AcuantUtil;
import com.cssn.samplesdk.model.MainActivityModel;
import com.cssn.samplesdk.model.MainActivityModel.State;
import com.cssn.samplesdk.util.ConfirmationListener;
import com.cssn.samplesdk.util.DataContext;
import com.cssn.samplesdk.util.TempImageStore;
import com.cssn.samplesdk.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 */
public class MainActivity extends Activity implements WebServiceListener, CardCroppingListener, AcuantErrorListener, FacialRecognitionListener {

    private static final String TAG = MainActivity.class.getName();
    private static final String IS_SHOWING_DIALOG_KEY = "isShowingDialog";
    private static final String IS_PROCESSING_DIALOG_KEY = "isProcessing";
    private static final String IS_CROPPING_DIALOG_KEY = "isCropping";
    private static final String IS_VALIDATING_DIALOG_KEY = "isValidating";
    private static final String IS_ACTIVATING_DIALOG_KEY = "isActivating";
    private static final String IS_SHOWDUPLEXDIALOG_DIALOG_KEY = "isShowDuplexDialog";
    private static String sPdf417String = "";
    AcuantAndroidMobileSDKController acuantAndroidMobileSdkControllerInstance = null;
    private ImageView frontImageView;
    private ImageView backImageView;
    private TextView txtTapToCaptureFront;
    private TextView txtTapToCaptureBack;
    private Button processCardButton;
    private RelativeLayout layoutFrontImage;
    private RelativeLayout layoutBackImage;
    private LinearLayout layoutCards;
    private EditText editTextLicense;
    private MainActivityModel mainActivityModel = null;
    private Button activateLicenseButton;
    private static ProgressDialog progressDialog;
    private static AlertDialog showDuplexAlertDialog;
    private static AlertDialog alertDialog;
    private static boolean isShowErrorAlertDialog;
    private static boolean isProcessing;
    private static boolean isValidating;
    private static boolean isActivating;
    private static boolean isCropping;
    private static boolean isBackSide;
    private static boolean isShowDuplexDialog;
    private static boolean isProcessingFacial;
    private static boolean isFacialFlow;
    private MainActivity mainActivity;
    private int cardRegion;
    private Bitmap originalImage;
    private Card processedCardInformation;
    private FacialData processedFacialData;
    /**
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (Util.LOG_ENABLED) {
            Utils.appendLog(TAG, "protected void onCreate(Bundle savedInstanceState)");
        }

        // load the model
        if (savedInstanceState == null) {
            if (Util.LOG_ENABLED) {
                Utils.appendLog(TAG, "if (savedInstanceState == null)");
            }
            mainActivityModel = new MainActivityModel();
        } else {
            if (Util.LOG_ENABLED) {
                Utils.appendLog(TAG, "if (savedInstanceState != null)");
            }
            mainActivityModel = DataContext.getInstance().getMainActivityModel();
            // if coming from background and kill the app, restart the model
            if (mainActivityModel == null) {
                mainActivityModel = new MainActivityModel();
            }
        }
        DataContext.getInstance().setContext(getApplicationContext());

        String licenseKey = DataContext.getInstance().getLicenseKey();

        // load the controller instance
        acuantAndroidMobileSdkControllerInstance = AcuantAndroidMobileSDKController.getInstance(this, licenseKey);
        if (!Util.isTablet(this)) {
            acuantAndroidMobileSdkControllerInstance.setPdf417BarcodeImageDrawable(getResources().getDrawable(R.drawable.barcode));
        }


        acuantAndroidMobileSdkControllerInstance.setWebServiceListener(this);
        acuantAndroidMobileSdkControllerInstance.setCloudUrl("cssnwebservices.com");
        acuantAndroidMobileSdkControllerInstance.setWatermarkText("Powered By Acuant", 0, 0, 30, 0);
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;
        int minLength = (int) (Math.min(width,height)*0.9);
        int maxLength = (int) (minLength*1.5);
        int left = minLength/2-50;
        int top = maxLength/2-100;
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        Typeface currentTypeFace =   textPaint.getTypeface();
        Typeface bold = Typeface.create(currentTypeFace, Typeface.BOLD);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(50);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTypeface(bold);
        Paint.FontMetrics metric = textPaint.getFontMetrics();
        acuantAndroidMobileSdkControllerInstance.setInstructionText("Blink Slowly", left,top,textPaint);
        //acuantAndroidMobileSdkControllerInstance.setShowActionBar(false);
        //acuantAndroidMobileSdkControllerInstance.setShowStatusBar(false);
        acuantAndroidMobileSdkControllerInstance.setFlashlight(false);
        //acuantAndroidMobileSdkControllerInstance.setFlashlight(0,0,50,0);
        //acuantAndroidMobileSdkControllerInstance.setFlashlightImageDrawable(getResources().getDrawable(R.drawable.lighton), getResources().getDrawable(R.drawable.lightoff));
        //acuantAndroidMobileSdkControllerInstance.setShowInitialMessage(true);
        //acuantAndroidMobileSdkControllerInstance.setCropBarcode(true);
        //acuantAndroidMobileSdkControllerInstance.setPdf417BarcodeDialogWaitingBarcode("AcuantAndroidMobileSampleSDK","ALIGN AND TAP", 10, "Try Again", "Yes");
        acuantAndroidMobileSdkControllerInstance.setCanShowBracketsOnTablet(true);
        // load several member variables
        setContentView(R.layout.activity_main);

        layoutCards = (LinearLayout) findViewById(R.id.cardImagesLayout);
        layoutBackImage = (RelativeLayout) findViewById(R.id.relativeLayoutBackImage);
        layoutFrontImage = (RelativeLayout) findViewById(R.id.relativeLayoutFrontImage);

        frontImageView = (ImageView) findViewById(R.id.frontImageView);
        backImageView = (ImageView) findViewById(R.id.backImageView);

        editTextLicense = (EditText) findViewById(R.id.editTextLicenceKey);
        editTextLicense.setText(DataContext.getInstance().getLicenseKey());

        txtTapToCaptureFront = (TextView) findViewById(R.id.txtTapToCaptureFront);
        txtTapToCaptureBack = (TextView) findViewById(R.id.txtTapToCaptureBack);

        activateLicenseButton = (Button) findViewById(R.id.activateLicenseButton);

        processCardButton = (Button) findViewById(R.id.processCardButton);
        processCardButton.setVisibility(View.INVISIBLE);

        editTextLicense.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                validateLicenseKey(editTextLicense.getText().toString());
                DataContext.getInstance().setLicenseKey(editTextLicense.getText().toString());
                return true;
            }
        });

        // it is necessary to use a post UI call, because of the previous set text on 'editTextLicense'
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                editTextLicense.addTextChangedListener(new TextWatcher() {
                    public void afterTextChanged(Editable s) {
                        mainActivityModel.setState(State.NO_VALIDATED);
                        updateActivateLicenseButtonFromModel();
                    }

                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }
                });
            }
        });

        editTextLicense.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideVirtualKeyboard();
                }
            }
        });

        // update the UI from the model
        updateUI();
        acuantAndroidMobileSdkControllerInstance.setCardCroppingListener(this);
        acuantAndroidMobileSdkControllerInstance.setAcuantErrorListener(this);
        if (Utils.LOG_ENABLED) {
            Utils.appendLog(TAG, "getScreenOrientation()=" + Util.getScreenOrientation(this));
        }
    }


    /**
     *
     */
    private void validateLicenseKey(String licenseKey) {
        Util.lockScreen(MainActivity.this);
        DataContext.getInstance().setLicenseKey(editTextLicense.getText().toString());
        if(progressDialog!=null && progressDialog.isShowing()){
            Util.dismissDialog(progressDialog);
        }
        isValidating = true;
        acuantAndroidMobileSdkControllerInstance.setLicensekey(licenseKey);
        hideVirtualKeyboard();
    }

    /**
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (Util.LOG_ENABLED) {
            Utils.appendLog(TAG, "protected void onActivityResult(int requestCode, int resultCode, Intent data)");
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        updateUI();
    }

    @Override
    public void onCardCroppingStart(Activity activity) {
        if (Utils.LOG_ENABLED) {
            Utils.appendLog(TAG, "public void onCardCroppingStart(Activity activity)");
        }
        cardRegion = DataContext.getInstance().getCardRegion();
        if(progressDialog!=null && progressDialog.isShowing()) {
            Util.dismissDialog(progressDialog);
        }
        Util.lockScreen(this);
        progressDialog = Util.showProgessDialog(activity, "Cropping image...");
        isCropping = true;
    }

    /**
     * Result from the CSSNMobileSDKController.showCameraInterface method when
     * popover == true
     */
    @Override
    public void onCardCroppingFinish(final Bitmap bitmap) {
        Util.dismissDialog(progressDialog);
        TempImageStore.setBitmapImage(bitmap);
        TempImageStore.setImageConfirmationListener(new ConfirmationListener() {
            @Override
            public void confimed() {
                if (Util.LOG_ENABLED) {
                    Utils.appendLog("appendLog", "public void onCardCroppedFinish(final Bitmap bitmap) - begin");
                }
                if (bitmap != null) {
                    updateModelAndUIFromCroppedCard(bitmap);
                }else{
                    // set an error to be shown in the onResume method.
                    mainActivityModel.setErrorMessage("Unable to detect the card. Please try again.");
                    updateModelAndUIFromCroppedCard(originalImage);
                }
                Util.unLockScreen(MainActivity.this);

                if (Util.LOG_ENABLED) {
                    Utils.appendLog("appendLog", "public void onCardCroppedFinish(final Bitmap bitmap) - end");
                }
                isCropping = false;
            }

            @Override
            public void retry() {
                showCameraInterface();
            }
        });

        Intent imageConfirmationIntent = new Intent(this, ImageConformationActivity.class);
        if(bitmap==null){
            TempImageStore.setCroppingPassed(false);
        }else{
            TempImageStore.setCroppingPassed(true);
        }
        TempImageStore.setCardType(mainActivityModel.getCurrentOptionType());
        startActivity(imageConfirmationIntent);

    }

    /**
     * Result from the CSSNMobileSDKController.showCameraInterface method when
     * popover == true
     */
    @Override
    public void onCardCroppingFinish(final Bitmap bitmap, final boolean scanBackSide) {
        TempImageStore.setBitmapImage(bitmap);
        TempImageStore.setImageConfirmationListener(new ConfirmationListener() {
            @Override
            public void confimed() {
                presentCameraForBackSide(bitmap,scanBackSide);
            }

            @Override
            public void retry() {
                showCameraInterface();
            }
        });
        Intent imageConfirmationIntent = new Intent(this, ImageConformationActivity.class);
        if(bitmap==null){
            TempImageStore.setCroppingPassed(false);
        }else{
            TempImageStore.setCroppingPassed(true);
        }
        TempImageStore.setCardType(mainActivityModel.getCurrentOptionType());
        startActivity(imageConfirmationIntent);
    }

    public void presentCameraForBackSide(final Bitmap bitmap, boolean scanBackSide) {

        if (Util.LOG_ENABLED) {
            Utils.appendLog("appendLog", "public void onCardCroppedFinish(final Bitmap bitmap) - begin");
        }
        cardRegion = DataContext.getInstance().getCardRegion();
        if (bitmap != null) {
            isBackSide = scanBackSide;
            if (isBackSide) {
                mainActivityModel.setCardSideSelected(MainActivityModel.CardSide.FRONT);
                //saveBitmap(bitmap);
            } else {
                mainActivityModel.setCardSideSelected(MainActivityModel.CardSide.BACK);
                //saveBitmap(bitmap);
            }

            if (mainActivityModel.getCurrentOptionType() == CardType.DRIVERS_LICENSE && isBackSide) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showDuplexDialog();
                    }
                }, 100);
            }
            updateModelAndUIFromCroppedCard(bitmap);
        } else {
            // set an error to be shown in the onResume method.
            mainActivityModel.setErrorMessage("Unable to detect the card. Please try again.");
            updateModelAndUIFromCroppedCard(originalImage);
        }

        Util.unLockScreen(this);

        if (Util.LOG_ENABLED) {
            Utils.appendLog("appendLog", "public void onCardCroppedFinish(final Bitmap bitmap) - end");
        }
        isCropping = false;
    }

    private void showDuplexDialog() {
        mainActivity = this;
        cardRegion = DataContext.getInstance().getCardRegion();
        Util.dismissDialog(showDuplexAlertDialog);
        Util.dismissDialog(alertDialog);
        showDuplexAlertDialog = new AlertDialog.Builder(this).create();
        showDuplexAlertDialog = Util.showDialog(this, getString(R.string.dl_duplex_dialog), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (cardRegion == Region.REGION_UNITED_STATES || cardRegion == Region.REGION_CANADA) {
                    acuantAndroidMobileSdkControllerInstance.setInitialMessageDescriptor(R.layout.tap_to_focus);
                    acuantAndroidMobileSdkControllerInstance.showCameraInterfacePDF417(mainActivity, CardType.DRIVERS_LICENSE, cardRegion);
                } else {
                    acuantAndroidMobileSdkControllerInstance.showManualCameraInterface(mainActivity, CardType.DRIVERS_LICENSE, cardRegion, isBackSide);
                }
                dialog.dismiss();
                isShowDuplexDialog = false;
            }
        });
        isShowDuplexDialog = true;
    }


    private void showFacialDialog() {
        AlertDialog facialInstructionDialog = new AlertDialog.Builder(this).create();
        facialInstructionDialog = Util.showDialog(this, getString(R.string.facial_instruction_dialog), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AcuantAndroidMobileSDKController.getInstance().setFacialListener(MainActivity.this);
                isProcessingFacial = AcuantAndroidMobileSDKController.getInstance().showManualFacialCameraInterface(MainActivity.this);
                dialog.dismiss();
            }
        });
    }


    @Override
    public void onPDF417Finish(String result) {
        sPdf417String = result;
    }

    @Override
    public void onOriginalCapture(Bitmap bitmap) {
        originalImage = bitmap;
    }

    @Override
    public void onCancelCapture() {
        Utils.appendLog("Acuant", "onCancelCapture");
    }

    @Override
    public void onBarcodeTimeOut() {
        acuantAndroidMobileSdkControllerInstance.pauseScanningBarcodeCamera();
        AlertDialog.Builder builder = new AlertDialog.Builder(acuantAndroidMobileSdkControllerInstance.getBarcodeCameraContext());
        // barcode Dialog "ignore" option
        builder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                acuantAndroidMobileSdkControllerInstance.finishScanningBarcodeCamera();
                dialog.dismiss();
            }
        });
        // barcode Dialog "retry" option
        builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                acuantAndroidMobileSdkControllerInstance.resumeScanningBarcodeCamera();
                dialog.dismiss();
            }
        });
        //barcode Dialog title and main message
        builder.setMessage("Unable to scan the barcode?");
        builder.setTitle("AcuantMobileSDK");
        builder.create().show();

    }

    /**
     * Updates the model, and the ui. Called after acquiring a cropped card.
     */
    private void updateModelAndUIFromCroppedCard(final Bitmap bitmap) {
        switch (mainActivityModel.getCardSideSelected()) {
            case FRONT:
                mainActivityModel.setFrontSideCardImage(bitmap);
                break;

            case BACK:
                mainActivityModel.setBackSideCardImage(bitmap);
                break;

            default:
                throw new IllegalStateException("This method is bad implemented, there is not processing for the cardSide '"
                        + mainActivityModel.getCardSideSelected() + "'");
        }
        updateUI();
    }

    /**
     * @param v
     */
    public void frontSideCapturePressed(View v) {
        if (Util.LOG_ENABLED) {
            Utils.appendLog(TAG, "public void frontSideCapturePressed(View v)");
        }
        isBackSide = false;

        mainActivityModel.clearImages();

        mainActivityModel.setCardSideSelected(MainActivityModel.CardSide.FRONT);

        showCameraInterface();
    }

    /**
     * @param v
     */
    public void backSideCapturePressed(View v) {
        if (Util.LOG_ENABLED) {
            Utils.appendLog(TAG, "public void backSideCapturePressed(View v)");
        }
        isBackSide = true;

        //mainActivityModel.clearImages();

        mainActivityModel.setCardSideSelected(MainActivityModel.CardSide.BACK);
        showCameraInterface();
    }

    /**
     *
     */
    private void showCameraInterface() {
        final int currentOptionType = mainActivityModel.getCurrentOptionType();
        cardRegion = DataContext.getInstance().getCardRegion();
        alertDialog = new AlertDialog.Builder(this).create();
        LicenseDetails license_details = DataContext.getInstance().getCssnLicenseDetails();
        if (currentOptionType == CardType.PASSPORT) {
            acuantAndroidMobileSdkControllerInstance.setWidth(AcuantUtil.DEFAULT_CROP_PASSPORT_WIDTH);
        }else if (currentOptionType == CardType.MEDICAL_INSURANCE) {
            acuantAndroidMobileSdkControllerInstance.setWidth(AcuantUtil.DEFAULT_CROP_MEDICAL_INSURANCE);
        } else {
            if(license_details.isAssureIDAllowed()) {
                acuantAndroidMobileSdkControllerInstance.setWidth(AcuantUtil.DEFAULT_CROP_DRIVERS_LICENSE_WIDTH_FOR_AUTHENTICATION);
            }else {
                acuantAndroidMobileSdkControllerInstance.setWidth(AcuantUtil.DEFAULT_CROP_DRIVERS_LICENSE_WIDTH);
            }
        }
        acuantAndroidMobileSdkControllerInstance.setInitialMessageDescriptor(R.layout.align_and_tap);
        acuantAndroidMobileSdkControllerInstance.setFinalMessageDescriptor(R.layout.hold_steady);
        acuantAndroidMobileSdkControllerInstance.showManualCameraInterface(this, currentOptionType, cardRegion, isBackSide);

    }
    /**
     * Called after a tap in the driver's card button.
     *
     * @param v
     */
    public void driverCardButtonPressed(View v) {
        // update the model
        processedCardInformation = null;
        processedFacialData=null;
        mainActivityModel.setCurrentOptionType(CardType.DRIVERS_LICENSE);
        mainActivityModel.clearImages();
        isProcessing=false;
        isProcessingFacial=false;
        isFacialFlow = false;
        Intent regionList = new Intent(this, RegionList.class);
        this.startActivity(regionList);

        updateUI();
    }

    public void driverCardWithFacialButtonPressed(View v){
        // update the model
        processedCardInformation = null;
        processedFacialData=null;
        mainActivityModel.setCurrentOptionType(CardType.DRIVERS_LICENSE);
        mainActivityModel.clearImages();
        isProcessing=false;
        isProcessingFacial=false;
        if(DataContext.getInstance().getCssnLicenseDetails().isFacialAllowed()) {
            isFacialFlow = true;
        }else{
            isFacialFlow = false;
        }
        Intent regionList = new Intent(this, RegionList.class);
        this.startActivity(regionList);
        updateUI();
    }

    /**
     * Called after a tap in the passport card button.
     *
     * @param v
     */

    public void passportCardButtonPressed(View v) {
        processedCardInformation = null;
        processedFacialData=null;
        mainActivityModel.setCurrentOptionType(CardType.PASSPORT);
        isProcessing=false;
        isProcessingFacial=false;
        isFacialFlow=false;
        mainActivityModel.clearImages();

        updateUI();
    }
    public void passportCardWithFacialButtonPressed(View v) {
        processedCardInformation = null;
        processedFacialData=null;
        mainActivityModel.setCurrentOptionType(CardType.PASSPORT);
        isProcessing=false;
        isProcessingFacial=false;
        if(DataContext.getInstance().getCssnLicenseDetails().isFacialAllowed()) {
            isFacialFlow = true;
        }else{
            isFacialFlow = false;
        }
        mainActivityModel.clearImages();

        updateUI();
    }

    /**
     * Called after a tap in the medical card button.
     *
     * @param v
     */
    public void medicalCardButtonPressed(View v) {
        processedCardInformation = null;
        processedFacialData=null;
        mainActivityModel.setCurrentOptionType(CardType.MEDICAL_INSURANCE);
        mainActivityModel.clearImages();

        updateUI();
    }

    /**
     * calculate the width and height of the front side card image and resize them
     */
    private void resizeImageFrames(int cardType) {
        double aspectRatio = AcuantUtil.getAspectRatio(cardType);

        int height = (int) (layoutFrontImage.getLayoutParams().width * aspectRatio);
        int width = layoutFrontImage.getLayoutParams().width;

        layoutFrontImage.getLayoutParams().height = height;
        layoutFrontImage.getLayoutParams().width = width;

        layoutFrontImage.setLayoutParams(layoutFrontImage.getLayoutParams());

        if (cardType == CardType.MEDICAL_INSURANCE) {
            layoutBackImage.getLayoutParams().height = height;
            layoutBackImage.getLayoutParams().width = width;

            layoutBackImage.setLayoutParams(layoutBackImage.getLayoutParams());
        }
    }

    /**
     * Updates the card's frame layout, shows/hides the back side card frame,
     * highlights the selected option, and load the card images in the view.
     */
    public void updateUI() {
        if (Utils.LOG_ENABLED) {
            Utils.appendLog(TAG, "private void updateUI()");
        }

        if (mainActivityModel.getErrorMessage() != null) {
            Util.dismissDialog(alertDialog);

            alertDialog = Util.showDialog(this, mainActivityModel.getErrorMessage(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mainActivityModel.setErrorMessage(null);
                    isShowErrorAlertDialog = false;
                }
            });
            isShowErrorAlertDialog = true;
        }

        // change orientation issues
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutCards.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            layoutCards.setOrientation(LinearLayout.VERTICAL);
        }

        // enable/disable the activate button
        updateActivateLicenseButtonFromModel();

        if (mainActivityModel.getCurrentOptionType() == -1) {
            // do not do any extra processing
            return;
        }

        // calculate the width and height of the front side card image and resize them
        resizeImageFrames(mainActivityModel.getCurrentOptionType());

        // show/hide the front and back views in the layout
        switch (mainActivityModel.getCurrentOptionType()) {
            case CardType.PASSPORT:
                txtTapToCaptureFront.setText(getResources().getString(R.string.tap_to_capture));
                showFrontSideCardImage();
                hideBackSideCardImage();
                break;

            case CardType.DRIVERS_LICENSE:
                showFrontSideCardImage();
                if (mainActivityModel.getBackSideCardImage() != null){
                    showBackSideCardImage();
                }else{
                    hideBackSideCardImage();
                }
                break;

            case CardType.MEDICAL_INSURANCE:
                txtTapToCaptureFront.setText(R.string.tap_to_capture_front_side);
                showFrontSideCardImage();
                showBackSideCardImage();
                break;
            case CardType.FACIAL_RECOGNITION:
                showFrontSideCardImage();
                if (mainActivityModel.getBackSideCardImage() != null){
                    showBackSideCardImage();
                }else{
                    hideBackSideCardImage();
                }
                break;

            default:
                throw new IllegalArgumentException(
                        "This method is wrong implemented, there is not processing for the card type '" + mainActivityModel.getCurrentOptionType() + "'");

        }

        // update card in front image view
        frontImageView.setImageBitmap( Util.getRoundedCornerBitmap(mainActivityModel.getFrontSideCardImage(), this.getApplicationContext()));

        if (mainActivityModel.getFrontSideCardImage() != null) {
            hideFrontImageText();
        } else {
            showFrontImageText();
        }

        // update card in back image view
        backImageView.setImageBitmap(Util.getRoundedCornerBitmap(mainActivityModel.getBackSideCardImage(), this.getApplicationContext()));

        if (mainActivityModel.getBackSideCardImage() != null) {
            hideBackImageText();
        } else {
            showBackImageText();
        }

        // update the process button
        if (mainActivityModel.getFrontSideCardImage() != null) {
            processCardButton.setVisibility(View.VISIBLE);
        } else {
            processCardButton.setVisibility(View.GONE);
        }

        highlightCurrentCardOption();

    }

    /**
     * Highlights the current option: drivers card, medical or passport.
     */
    private void highlightCurrentCardOption() {
        int buttonId;

        switch (mainActivityModel.getCurrentOptionType()) {

            case CardType.DRIVERS_LICENSE:
                buttonId = R.id.buttonDriver;
                if(isFacialFlow){
                    buttonId = R.id.buttonDriverFacial;
                }else {
                    buttonId = R.id.buttonDriver;
                }

                break;

            case CardType.PASSPORT:
                buttonId = R.id.buttonPassport;
                if(isFacialFlow){
                    buttonId = R.id.buttonPassportFacial;
                }else {
                    buttonId = R.id.buttonPassport;
                }

                break;

            case CardType.MEDICAL_INSURANCE:

                buttonId = R.id.buttonMedical;

                break;
            case CardType.FACIAL_RECOGNITION:
                buttonId = R.id.buttonDriverFacial;
                if(processedCardInformation instanceof DriversLicenseCard) {
                    buttonId = R.id.buttonDriverFacial;
                }else if(processedCardInformation instanceof PassportCard){
                    buttonId = R.id.buttonPassportFacial;
                }

                break;

            default:
                throw new IllegalArgumentException(
                        "This method is wrong implemented, there is not processing for the card type '"
                                + mainActivityModel.getCurrentOptionType() + "'");

        }

        ((Button) findViewById(R.id.buttonDriver)).setTypeface(null, Typeface.NORMAL);
        ((Button) findViewById(R.id.buttonPassport)).setTypeface(null, Typeface.NORMAL);
        ((Button) findViewById(R.id.buttonMedical)).setTypeface(null, Typeface.NORMAL);
        ((Button) findViewById(R.id.buttonPassportFacial)).setTypeface(null, Typeface.NORMAL);
        ((Button) findViewById(R.id.buttonPassportFacial)).setTypeface(null, Typeface.NORMAL);

        ((Button) findViewById(buttonId)).setTypeface(null, Typeface.BOLD);
    }

    /**
     * Called by the process Button
     *
     * @param v
     */
    public void processCard(View v) {
        if(isFacialFlow) {
            if (mainActivityModel.getCurrentOptionType() == CardType.FACIAL_RECOGNITION || mainActivityModel.getCurrentOptionType() == CardType.PASSPORT || mainActivityModel.getCurrentOptionType() == CardType.DRIVERS_LICENSE) {
                isProcessingFacial = true;
                showFacialDialog();
            }
        }
        if(!isProcessingFacial) {
            if(progressDialog!=null && progressDialog.isShowing()){
                Util.dismissDialog(progressDialog);
            }
            progressDialog = Util.showProgessDialog(MainActivity.this, "Capturing data ...");
            Util.lockScreen(this);
        }
        if (!isProcessing && processedCardInformation==null) {
            isProcessing = true;
            // check for the internet connection
            if (!Utils.isNetworkAvailable(this)) {
                String msg = getString(R.string.no_internet_message);
                Utils.appendLog(TAG, msg);
                Util.dismissDialog(alertDialog);
                alertDialog = Util.showDialog(this, msg,new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isShowErrorAlertDialog = false;
                    }
                });
                isShowErrorAlertDialog = true;
                return;
            }

            // process the card
            //progressDialog = Util.showProgessDialog(MainActivity.this, "Capturing data ...");

            //Util.lockScreen(this);

            ProcessImageRequestOptions options = ProcessImageRequestOptions.getInstance();
            options.autoDetectState = true;
            options.stateID = -1;
            options.reformatImage = true;
            options.reformatImageColor = 0;
            options.DPI = 150;
            options.cropImage = false;
            options.faceDetec = true;
            options.signDetec = true;
            options.imageSource = 101;
            options.iRegion = DataContext.getInstance().getCardRegion();
            options.acuantCardType = mainActivityModel.getCurrentOptionType();

            acuantAndroidMobileSdkControllerInstance.callProcessImageServices(mainActivityModel.getFrontSideCardImage(), mainActivityModel.getBackSideCardImage(), sPdf417String, this, options);

            resetPdf417String();
        }
    }


    /**
     * Called after card processing is over.
     *
     * @param
     */
    public void processImageValidation(Bitmap faceImage,Bitmap idCropedFaceImage) {
        if(processedCardInformation!=null){
            isProcessingFacial=false;
        }
        mainActivityModel.setCurrentOptionType(CardType.FACIAL_RECOGNITION);
        if (!Utils.isNetworkAvailable(this)) {
            String msg = getString(R.string.no_internet_message);
            Utils.appendLog(TAG, msg);
            Util.dismissDialog(alertDialog);
            alertDialog = Util.showDialog(this, msg,new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isShowErrorAlertDialog = false;
                }
            });
            isShowErrorAlertDialog = true;
            return;
        }

        //Util.lockScreen(this);

        ProcessImageRequestOptions options = ProcessImageRequestOptions.getInstance();
        options.acuantCardType = CardType.FACIAL_RECOGNITION;
        acuantAndroidMobileSdkControllerInstance.callProcessImageServices(faceImage, idCropedFaceImage, null, this, options);
    }

    private void resetPdf417String() {
        sPdf417String = "";
    }

    /**
     * @param v
     */
    public void activateLicenseKey(View v) {
        hideVirtualKeyboard();

        String key = editTextLicense.getText().toString().trim();
        if (!key.equals("")) {
            Util.lockScreen(MainActivity.this);
            if(progressDialog!=null && progressDialog.isShowing()){
                Util.dismissDialog(progressDialog);
            }
            progressDialog = Util.showProgessDialog(MainActivity.this, "Activating License ..");
            isActivating = true;
            acuantAndroidMobileSdkControllerInstance.callActivateLicenseKeyService(key);

        } else {
            Util.dismissDialog(alertDialog);
            alertDialog = Util.showDialog(this, "The license key cannot be empty.",new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isShowErrorAlertDialog = false;
                }
            });
            isShowErrorAlertDialog = true;
        }
    }

    /**
     *
     */
    @Override
    public void processImageServiceCompleted(Card card) {
        if (Util.LOG_ENABLED) {
            Utils.appendLog(TAG, "public void processImageServiceCompleted(CSSNCard card, int status, String errorMessage)");
        }

        if(mainActivityModel.getCurrentOptionType()!=CardType.FACIAL_RECOGNITION) {
            isProcessing = false;
            processedCardInformation = card;
        }else{
            isProcessingFacial=false;
            processedFacialData = (FacialData) card;
        }

        presentResults(processedCardInformation,processedFacialData);

    }


    public void presentResults(Card card,FacialData facialData){
        if(!isProcessing && !isProcessingFacial) {
            Util.dismissDialog(progressDialog);
            String dialogMessage = null;
            try {
                DataContext.getInstance().setCardType(mainActivityModel.getCurrentOptionType());

                if (card == null || card.isEmpty()) {
                    dialogMessage = "No data found for this license card!";
                } else {

                    switch (mainActivityModel.getCurrentOptionType()) {
                        case CardType.DRIVERS_LICENSE:
                            DataContext.getInstance().setProcessedLicenseCard((DriversLicenseCard) card);
                            break;

                        case CardType.MEDICAL_INSURANCE:
                            DataContext.getInstance().setProcessedMedicalCard((MedicalCard) card);
                            break;

                        case CardType.PASSPORT:
                            DataContext.getInstance().setProcessedPassportCard((PassportCard) card);
                            break;
                        case CardType.FACIAL_RECOGNITION:
                            if( processedCardInformation instanceof DriversLicenseCard) {
                                DriversLicenseCard dlCard = (DriversLicenseCard)processedCardInformation;
                                DataContext.getInstance().setProcessedLicenseCard(dlCard);
                                DataContext.getInstance().setCardType(CardType.DRIVERS_LICENSE);
                            }else if(processedCardInformation instanceof PassportCard) {
                                PassportCard passportCard = (PassportCard) processedCardInformation;
                                DataContext.getInstance().setProcessedPassportCard(passportCard);
                                DataContext.getInstance().setCardType(CardType.PASSPORT);
                            }
                            DataContext.getInstance().setProcessedFacialData(processedFacialData);
                            break;
                        default:
                            throw new IllegalStateException("There is not implementation for processing the card type '"
                                    + mainActivityModel.getCurrentOptionType() + "'");
                    }

                    Util.unLockScreen(MainActivity.this);

                    Intent showDataActivityIntent = new Intent(this, ShowDataActivity.class);
                    showDataActivityIntent.putExtra("FACIAL",isFacialFlow);
                    this.startActivity(showDataActivityIntent);
                }


            } catch (Exception e) {
                Utils.appendLog(TAG, e.getMessage());
                dialogMessage = "Sorry! Internal error has occurred, please contact us!";

            }

            if (dialogMessage != null) {
                Util.dismissDialog(alertDialog);
                alertDialog = Util.showDialog(this, dialogMessage, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isShowErrorAlertDialog = false;
                    }
                });
                isShowErrorAlertDialog = true;
            }
        }

    }

    /**
     *
     */
    @Override
    public void activateLicenseKeyCompleted(LicenseActivationDetails cssnLicenseActivationDetails) {
        Util.dismissDialog(progressDialog);
        Util.unLockScreen(MainActivity.this);
        isActivating = false;

        String msg="";

        if (cssnLicenseActivationDetails != null) {
            msg = cssnLicenseActivationDetails.getIsLicenseKeyActivatedDescscription();
        }

        Util.lockScreen(this);
        alertDialog =  Util.showDialog(this, msg, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Util.dismissDialog((Dialog) dialog);
                Util.unLockScreen(MainActivity.this);
                // validation if there was not error in the activation, because the
                // validation is done before, every time the license key text
                // changes.
                validateLicenseKey(editTextLicense.getText().toString());
                isShowErrorAlertDialog = false;
            }
        });
        isShowErrorAlertDialog = true;

    }

    /**
     *
     */
    @Override
    public void validateLicenseKeyCompleted(LicenseDetails details) {

        Util.dismissDialog(progressDialog);
        Util.unLockScreen(MainActivity.this);

        LicenseDetails cssnLicenseDetails = DataContext.getInstance().getCssnLicenseDetails();
        DataContext.getInstance().setCssnLicenseDetails(details);

        // update model
        mainActivityModel.setState(State.VALIDATED);
        if (cssnLicenseDetails != null && cssnLicenseDetails.isLicenseKeyActivated()) {
            mainActivityModel.setValidatedStateActivation(State.ValidatedStateActivation.ACTIVATED);
        } else {
            mainActivityModel.setValidatedStateActivation(State.ValidatedStateActivation.NO_ACTIVATED);
        }
        updateActivateLicenseButtonFromModel();
        // message dialogs
        isValidating = false;
    }

    /**
     */
    private void showFrontSideCardImage() {
        layoutFrontImage.setClickable(true);
        layoutFrontImage.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void hideFrontSideCardImage() {
        layoutFrontImage.setClickable(false);
        layoutFrontImage.setVisibility(View.GONE);
    }

    /**
     *
     */
    private void showBackSideCardImage() {
        layoutBackImage.setClickable(true);
        layoutBackImage.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void hideBackSideCardImage() {
        layoutBackImage.setClickable(false);
        layoutBackImage.setVisibility(View.GONE);
    }

    /**
     *
     */
    private void showFrontImageText() {
        txtTapToCaptureFront.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void hideFrontImageText() {
        txtTapToCaptureFront.setVisibility(View.GONE);
    }

    /**
     *
     */
    private void showBackImageText() {
        txtTapToCaptureBack.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void hideBackImageText() {
        txtTapToCaptureBack.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Util.LOG_ENABLED) {
            Utils.appendLog(TAG, "protected void onResume()");
        }
        editTextLicense.clearFocus();
        frontImageView.requestFocus();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //spdf417 = savedInstanceState.get(PDF417_STRING_KEY) != null ? (String) savedInstanceState.get(PDF417_STRING_KEY) : "";
        isShowErrorAlertDialog = savedInstanceState.getBoolean(IS_SHOWING_DIALOG_KEY, false);
        isProcessing = savedInstanceState.getBoolean(IS_PROCESSING_DIALOG_KEY, false);
        isCropping = savedInstanceState.getBoolean(IS_CROPPING_DIALOG_KEY, false);
        isValidating = savedInstanceState.getBoolean(IS_VALIDATING_DIALOG_KEY, false);
        isActivating = savedInstanceState.getBoolean(IS_ACTIVATING_DIALOG_KEY, false);
        isShowDuplexDialog = savedInstanceState.getBoolean(IS_SHOWDUPLEXDIALOG_DIALOG_KEY, false);
        if(progressDialog!=null && progressDialog.isShowing()){
            Util.dismissDialog(progressDialog);
        }
        if (isShowDuplexDialog) {
            showDuplexDialog();
        }
        if (isProcessing) {
            progressDialog = Util.showProgessDialog(MainActivity.this, "Capturing data ...");
        }
        if (isCropping){
            progressDialog = Util.showProgessDialog(MainActivity.this, "Cropping image...");
        }
        if (isValidating){
            progressDialog = Util.showProgessDialog(MainActivity.this, "Validating License ..");
        }
        if (isActivating){
            progressDialog = Util.showProgessDialog(MainActivity.this, "Activating License ..");
        }
        if (isShowErrorAlertDialog){
            alertDialog.show();
        }
        updateUI();
    }

    /**
     *
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (Util.LOG_ENABLED) {
            Utils.appendLog(TAG, "protected void onSaveInstanceState(Bundle outState)");
        }

        DataContext.getInstance().setMainActivityModel(mainActivityModel);
        //outState.putString(PDF417_STRING_KEY, this.pdf417);
        outState.putBoolean(IS_SHOWING_DIALOG_KEY, isShowErrorAlertDialog);
        outState.putBoolean(IS_PROCESSING_DIALOG_KEY, isProcessing);
        outState.putBoolean(IS_CROPPING_DIALOG_KEY, isCropping);
        outState.putBoolean(IS_ACTIVATING_DIALOG_KEY, isActivating);
        outState.putBoolean(IS_VALIDATING_DIALOG_KEY, isValidating);
        outState.putBoolean(IS_SHOWDUPLEXDIALOG_DIALOG_KEY, isShowDuplexDialog);
    }

    /**
     *
     */
    @Override
    protected void onPause() {
        super.onPause();

        if (Utils.LOG_ENABLED) {
            Utils.appendLog(TAG, "protected void onPause()");
        }
    }

    /**
     *
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (Util.LOG_ENABLED) {
            Utils.appendLog(TAG, "protected void onDestroy()");
        }
    }

    /**
     * @param bitmap
     * @return
     */
    private boolean saveBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd-mm-yyyy");
            String formattedDate = df.format(c.getTime());

            File file = new File("sdcard/CSSNCardCropped" + formattedDate + ".png");
            FileOutputStream fOutputStream = null;

            try {
                fOutputStream = new FileOutputStream(file);

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOutputStream);

                fOutputStream.flush();
                fOutputStream.close();

                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
            } catch (FileNotFoundException e) {
                if (Util.LOG_ENABLED) {
                    Utils.appendLog(TAG, e.getMessage());
                }
                Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show();
                return false;
            } catch (IOException e) {
                if (Util.LOG_ENABLED) {
                    Utils.appendLog(TAG, e.getMessage());
                }
                Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     */
    private void hideVirtualKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editTextLicense.getWindowToken(), 0);
    }

    /**
     *
     */
    private void updateActivateLicenseButtonFromModel() {
        activateLicenseButton.setEnabled(
                mainActivityModel.getState() == State.NO_VALIDATED || (mainActivityModel.getState() == State.VALIDATED && mainActivityModel.getValidatedStateActivation() == State.ValidatedStateActivation.NO_ACTIVATED));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Override
    public void didFailWithError(int code, String message) {
        Utils.appendLog("didFailWithError", "didFailWithError:" + code + "message" + message);
        Util.dismissDialog(progressDialog);
        Util.unLockScreen(MainActivity.this);
        String msg = message;
        if (code == ErrorType.AcuantErrorCouldNotReachServer) {
            msg = getString(R.string.no_internet_message);
        }else if (code == ErrorType.AcuantErrorUnableToCrop){
            updateModelAndUIFromCroppedCard(originalImage);
        }
        alertDialog = Util.showDialog(this, msg, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isShowErrorAlertDialog = false;
            }
        });
        isShowErrorAlertDialog = true;
        if (Util.LOG_ENABLED) {
            Utils.appendLog(TAG, "didFailWithError:" + message);
        }
        // message dialogs
        isValidating = false;
        isProcessing = false;
        isActivating = false;
    }

    @Override
    public void onFacialRecognitionCompleted(final Bitmap bitmap) {
        Util.lockScreen(this);
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                if(progressDialog!=null && progressDialog.isShowing()){
                    Util.dismissDialog(progressDialog);
                }
                progressDialog = Util.showProgessDialog(MainActivity.this, "Capturing data ...");
                new Thread(new Runnable()
                {
                    @Override
                    public void run() {
                        while(isProcessing){
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        // process the card
                        if(mainActivityModel.getCurrentOptionType()==CardType.DRIVERS_LICENSE) {
                            DriversLicenseCard dlCard = (DriversLicenseCard)processedCardInformation;
                            processImageValidation(bitmap,dlCard.getFaceImage());
                        }else if(mainActivityModel.getCurrentOptionType()==CardType.PASSPORT) {
                            PassportCard passportCard = (PassportCard) processedCardInformation;
                            processImageValidation(bitmap,passportCard.getFaceImage());
                        }
                    }
                }).start();

            }
        });


    }

    @Override
    public void onFacialRecognitionCanceled(){
        isProcessingFacial=false;
    }


   //Override this only for API 23 and Above
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Permission.PERMISSION_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showCameraInterface();

                } else {
                    // permission denied
                    Util.showDialog(this,"Denied permission.Please give camera permission to proceed.");
                }
                return;
            }
        }
    }
}