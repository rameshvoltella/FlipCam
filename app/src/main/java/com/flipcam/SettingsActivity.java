package com.flipcam;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderResult;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.flipcam.constants.Constants;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.DriveStatusCodes;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity{

    public static final String TAG = "SettingsActivity";
    LinearLayout phoneMemParentVert;
    TextView thresholdText;
    TextView phoneMemTextMsg;
    ImageView greenArrow;
    SharedPreferences settingsPref;
    SharedPreferences.Editor settingsEditor;
    RadioButton phoneMemBtn;
    RadioButton sdCardBtn;
    Dialog sdCardDialog;
    LinearLayout sdcardlayout;
    TextView sdCardPathMsg;
    ImageView editSdCardPath;
    LayoutInflater layoutInflater;
    CheckBox switchOnDrive;
    CheckBox switchOnDropbox;
    Dialog saveToCloud;
    TextView savetocloudtitle;
    TextView savetocloudmsg;
    DriveClient mDriveClient;
    DriveResourceClient mDriveResourceClient;
    static final int REQUEST_CODE_SIGN_IN = 0;
    GoogleSignInOptions signInOptions;
    GoogleSignInClient googleSignInClient = null;
    boolean signedInDrive = false;
    boolean signInDropbox = false;
    Dialog cloudUpload;
    View cloudUploadRoot;
    View signInProgressRoot;
    View sdCardRoot;
    View saveToCloudRoot;
    View autoUploadEnabledWithFolderRoot;
    View autoUploadEnabledRoot;
    View autoUploadDisabledRoot;
    View uploadFolderCheckRoot;
    View accessGrantedDropboxRoot;
    TextView uploadFolderTitle;
    TextView uploadFolderMsg;
    int cloud = 0; //Default to Google Drive. 1 for Dropbox.
    AccountManager accountManager;
    final int GET_ACCOUNTS_PERM = 100;
    Dialog permissionAccount;
    Dialog signInProgressDialog;
    boolean signInProgress = false;
    ImageView uploadDestIcon;
    Dialog autoUploadEnabledWithFolder;
    Dialog autoUploadEnabled;
    Dialog autoUploadDisabled;
    Dialog uploadFolderCheck;
    Dialog accesGrantedDropbox;
    DbxClientV2 dbxClientV2;
    DbxRequestConfig dbxRequestConfig;
    boolean goToDropbox = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_settings);
        phoneMemParentVert = (LinearLayout)findViewById(R.id.phoneMemParentVert);
        phoneMemTextMsg = (TextView)findViewById(R.id.phoneMemTextMsg);
        thresholdText = (TextView)findViewById(R.id.thresholdText);
        greenArrow = (ImageView)findViewById(R.id.greenArrow);
        phoneMemBtn = (RadioButton)findViewById(R.id.phoneMemButton);
        sdCardBtn = (RadioButton)findViewById(R.id.sdCardbutton);
        sdCardPathMsg = (TextView)findViewById(R.id.sdcardpathmsg);
        editSdCardPath = (ImageView)findViewById(R.id.editSdCardPath);
        switchOnDropbox = (CheckBox) findViewById(R.id.switchOnDropbox);
        switchOnDrive = (CheckBox) findViewById(R.id.switchOnDrive);
        sdcardlayout = (LinearLayout)findViewById(R.id.sdcardlayout);
        thresholdText.setText(getResources().getString(R.string.memoryThresholdLimit, getResources().getInteger(R.integer.minimumMemoryWarning) + "MB"));
        getSupportActionBar().setTitle(getResources().getString(R.string.settingTitle));
        settingsPref = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        settingsEditor = settingsPref.edit();
        if(settingsPref.contains(Constants.SAVE_MEDIA_PHONE_MEM)){
            Log.d(TAG,"Phone memory exists");
            if(settingsPref.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM,true)){
                Log.d(TAG,"Phone memory is true");
                phoneMemBtn.setChecked(true);
                sdCardBtn.setChecked(false);
                editSdCardPath.setClickable(false);
            }
            else{
                Log.d(TAG,"Phone memory is false");
                phoneMemBtn.setChecked(false);
                sdCardBtn.setChecked(true);
                editSdCardPath.setClickable(true);
            }
        }
        else{
            Log.d(TAG,"Phone memory NOT exists");
            phoneMemBtn.setChecked(true);
            sdCardBtn.setChecked(false);
        }
        Log.d(TAG,"SD Card Path onCreate = "+settingsPref.getString(Constants.SD_CARD_PATH,""));
        if(settingsPref.contains(Constants.SD_CARD_PATH) && !settingsPref.getString(Constants.SD_CARD_PATH,"").equals("")) {
            String sdcardpath = settingsPref.getString(Constants.SD_CARD_PATH, "");
            showSDCardPath(sdcardpath);
        }
        else{
            hideSDCardPath();
        }
        layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        sdCardRoot = layoutInflater.inflate(R.layout.sd_card_location,null);
        saveToCloudRoot = layoutInflater.inflate(R.layout.save_to_cloud,null);
        cloudUploadRoot = layoutInflater.inflate(R.layout.cloud_upload_folder,null);
        signInProgressRoot = layoutInflater.inflate(R.layout.sign_in_progress,null);
        autoUploadEnabledWithFolderRoot = layoutInflater.inflate(R.layout.auto_upload_enabled_with_folder, null);
        autoUploadEnabledRoot = layoutInflater.inflate(R.layout.auto_upload_enabled, null);
        autoUploadDisabledRoot = layoutInflater.inflate(R.layout.auto_upload_disabled, null);
        uploadFolderCheckRoot = layoutInflater.inflate(R.layout.upload_folder_check, null);
        accessGrantedDropboxRoot = layoutInflater.inflate(R.layout.access_granted_dropbox, null);
        sdCardDialog = new Dialog(this);
        saveToCloud = new Dialog(this);
        cloudUpload = new Dialog(this);
        permissionAccount = new Dialog(this);
        signInProgressDialog = new Dialog(this);
        autoUploadEnabledWithFolder = new Dialog(this);
        autoUploadEnabled = new Dialog(this);
        autoUploadDisabled = new Dialog(this);
        uploadFolderCheck = new Dialog(this);
        accesGrantedDropbox = new Dialog(this);
        accountManager = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
    }

    public void updateSettingsValues(){
        //Update Phone memory
        if(settingsPref.contains(Constants.PHONE_MEMORY_DISABLE)){
            if(!settingsPref.getBoolean(Constants.PHONE_MEMORY_DISABLE, false)){
                String memoryLimit = settingsPref.getString(Constants.PHONE_MEMORY_LIMIT, getResources().getInteger(R.integer.minimumMemoryWarning) + "");
                String memoryMetric = settingsPref.getString(Constants.PHONE_MEMORY_METRIC, "MB");
                thresholdText.setText(getResources().getString(R.string.memoryThresholdLimit, Integer.parseInt(memoryLimit) + " " + memoryMetric));
            }
            else{
                thresholdText.setText(getResources().getString(R.string.memoryThresholdLimit, getResources().getString(R.string.phoneMemoryLimitDisabled)));
            }
        }
        else{
            String memoryLimit = settingsPref.getString(Constants.PHONE_MEMORY_LIMIT, getResources().getInteger(R.integer.minimumMemoryWarning) + "");
            String memoryMetric = settingsPref.getString(Constants.PHONE_MEMORY_METRIC, "MB");
            thresholdText.setText(getResources().getString(R.string.memoryThresholdLimit, Integer.parseInt(memoryLimit) + " " + memoryMetric));
        }
        //Update Auto upload
        if(settingsPref.contains(Constants.SAVE_TO_GOOGLE_DRIVE)){
            if(settingsPref.getBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, false)){
                switchOnDrive.setChecked(true);
            }
            else{
                switchOnDrive.setChecked(false);
            }
        }
        else{
            switchOnDrive.setChecked(false);
        }
    }

    public void openSdCardPath(View view){
        if(settingsPref.contains(Constants.SD_CARD_PATH)) {
            ((EditText) sdCardRoot.findViewById(R.id.sdCardPathText)).setText(settingsPref.getString(Constants.SD_CARD_PATH,""));
        }
        Configuration config = getResources().getConfiguration();
        TextView sdcardText;
        if(config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            sdcardText = (TextView)sdCardRoot.findViewById(R.id.sdCardMsg);
            sdcardText.setText(getResources().getString(R.string.sdCardPathPortrait));
        }
        else{
            sdcardText = (TextView)sdCardRoot.findViewById(R.id.sdCardMsg);
            sdcardText.setText(getResources().getString(R.string.sdCardPathLandscape));
        }
        sdCardDialog.setContentView(sdCardRoot);
        sdCardDialog.setCancelable(false);
        sdCardDialog.show();
        TextView sdCardPathText = (TextView)sdCardRoot.findViewById(R.id.sdCardPathText);
        sdCardPathText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                Log.d(TAG,"hasFocus = "+hasFocus);
                if(hasFocus){
                    sdCardDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    public void selectSaveMedia(View view){
        switch (view.getId()){
            case R.id.phoneMemButton:
                Log.d(TAG,"Save in phone memory");
                settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,true);
                settingsEditor.commit();
                phoneMemBtn.setChecked(true);
                sdCardBtn.setChecked(false);
                editSdCardPath.setClickable(false);
                break;
            case R.id.sdCardbutton:
                Log.d(TAG,"Save in sd card");
                phoneMemBtn.setChecked(false);
                sdCardBtn.setChecked(true);
                editSdCardPath.setClickable(true);
                if(!settingsPref.contains(Constants.SD_CARD_PATH)) {
                    openSdCardPath(view);
                }
                else{
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                    settingsEditor.commit();
                }
                break;
        }
    }

    public void saveSdCardPath(View view){
        switch (view.getId()){
            case R.id.okSdCard:
                Log.d(TAG,"Checking if path is valid");
                String path;
                path = ((EditText) sdCardRoot.findViewById(R.id.sdCardPathText)).getText().toString();
                Log.d(TAG,"Path = "+path);
                File sdCard = new File(path);
                if(!sdCard.exists() || !sdCard.isDirectory()){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.sdCardPathNotExist),Toast.LENGTH_SHORT).show();
                }
                else{
                    String fullPath;
                    Log.d(TAG,"Existing path = "+sdCard.getPath());
                    if(!sdCard.getPath().contains(getResources().getString(R.string.app_name))) {
                        if (sdCard.getPath().endsWith("/")) {
                            String pathExcludeFrontSlash = sdCard.getPath().substring(0, sdCard.getPath().length() - 1);
                            fullPath = pathExcludeFrontSlash + getResources().getString(R.string.FC_ROOT);
                        } else {
                            fullPath = sdCard.getPath() + getResources().getString(R.string.FC_ROOT);
                        }
                        Log.d(TAG, "Full path = " + fullPath);
                        File fc = new File(fullPath);
                        if (!fc.exists()) {
                            fc.mkdir();
                            Log.d(TAG, "Able to create FC");
                        }
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.sdCardPathSaved),Toast.LENGTH_SHORT).show();
                        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                        settingsEditor.putString(Constants.SD_CARD_PATH,fc.getPath());
                        settingsEditor.commit();
                        showSDCardPath(fc.getPath());
                    }
                    else{
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.sdCardPathSaved),Toast.LENGTH_SHORT).show();
                        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                        settingsEditor.putString(Constants.SD_CARD_PATH,sdCard.getPath());
                        settingsEditor.commit();
                        showSDCardPath(sdCard.getPath());
                    }
                    sdCardDialog.dismiss();
                }
                break;
            case R.id.cancelSdCard:
                Log.d(TAG,"SD Card Path = "+settingsPref.getString(Constants.SD_CARD_PATH,""));
                if(settingsPref.contains(Constants.SD_CARD_PATH) && !settingsPref.getString(Constants.SD_CARD_PATH,"").equalsIgnoreCase("")){
                    phoneMemBtn.setChecked(false);
                    sdCardBtn.setChecked(true);
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                    settingsEditor.commit();
                }
                else{
                    phoneMemBtn.setChecked(true);
                    sdCardBtn.setChecked(false);
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,true);
                    settingsEditor.commit();
                }
                sdCardDialog.dismiss();
                Log.d(TAG,"cancel sd card");
                break;
        }
    }

    public void showSDCardPath(String path){
        sdCardPathMsg.setText(path);
        sdcardlayout.setVisibility(View.VISIBLE);
    }

    public void hideSDCardPath(){
        sdcardlayout.setVisibility(View.GONE);
    }

    public void reDrawSDCardScreen(){
        Configuration config = getResources().getConfiguration();
        if(config.orientation == Configuration.ORIENTATION_PORTRAIT){
            TextView sdcardText = (TextView)sdCardRoot.findViewById(R.id.sdCardMsg);
            sdcardText.setText(getResources().getString(R.string.sdCardPathPortrait));
        }
        else{
            TextView sdcardText = (TextView)sdCardRoot.findViewById(R.id.sdCardMsg);
            sdcardText.setText(getResources().getString(R.string.sdCardPathLandscape));
        }
    }

    public void showMemoryConsumed(View view){
        Intent memoryAct = new Intent(SettingsActivity.this, MemoryLimitActivity.class);
        startActivity(memoryAct);
        overridePendingTransition(R.anim.slide_from_right,R.anim.slide_to_left);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
    }

    public void saveToCloudDrive(View view) {
        cloud = Constants.GOOGLE_DRIVE_CLOUD;
        if (switchOnDrive.isChecked()) {
            savetocloudtitle = (TextView)saveToCloudRoot.findViewById(R.id.savetocloudtitle);
            savetocloudtitle.setText(getResources().getString(R.string.saveToCloudTitle, getResources().getString(R.string.googleDrive)));
            ImageView placeHolderIcon = (ImageView)saveToCloudRoot.findViewById(R.id.placeHolderIconSavetoCloud);
            placeHolderIcon.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            imageParams.width = (int)getResources().getDimension(R.dimen.googleDriveIconWidth);
            imageParams.height = (int)getResources().getDimension(R.dimen.googleDriveIconHeight);
            placeHolderIcon.setLayoutParams(imageParams);
            saveToCloud.setContentView(saveToCloudRoot);
            saveToCloud.setCancelable(false);
            saveToCloud.show();
        }
        else{
            boolean saveToDrive = settingsPref.getBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, false);
            if(saveToDrive) {
                if(googleSignInClient == null){
                    initializeGoogleSignIn();
                }
                googleSignInClient.signOut();
                signedInDrive = false;
                showUploadDisabled();
                settingsEditor.putBoolean(Constants.SAVE_TO_GOOGLE_DRIVE , false);
                settingsEditor.commit();
            }
        }
    }

    public void saveToDropBox(View view){
        cloud = Constants.DROPBOX_CLOUD;
        if(switchOnDropbox.isChecked()){
            savetocloudtitle = (TextView)saveToCloudRoot.findViewById(R.id.savetocloudtitle);
            savetocloudtitle.setText(getResources().getString(R.string.saveToCloudTitle, getResources().getString(R.string.dropbox)));
            ImageView placeHolderIcon = (ImageView)saveToCloudRoot.findViewById(R.id.placeHolderIconSavetoCloud);
            placeHolderIcon.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            imageParams.width = (int)getResources().getDimension(R.dimen.dropBoxIconWidth);
            imageParams.height = (int)getResources().getDimension(R.dimen.dropBoxIconHeight);
            placeHolderIcon.setLayoutParams(imageParams);
            saveToCloud.setContentView(saveToCloudRoot);
            saveToCloud.setCancelable(false);
            saveToCloud.show();
        }
        else{
            revokeAccessFromDropbox();
            disableDropboxInSetting();
            showUploadDisabled();
        }
    }

    public void createDropboxFolder(View view){
        accesGrantedDropbox.dismiss();
        createUploadFolder();
    }

    public void signInToCloud(View view){
        switch (view.getId()){
            case R.id.continueSignIn:
                saveToCloud.dismiss();
                if(cloud == Constants.GOOGLE_DRIVE_CLOUD){
                    //Sign in to Google Drive
                    int permissionCheck = ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS);
                    if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        continueToGoogleDrive();
                    }
                    else{
                        permissionAccount.setContentView(R.layout.permission_account);
                        permissionAccount.setCancelable(false);
                        permissionAccount.show();
                    }
                }
                else{
                    //Sign in to Dropbox
                    goToDropbox = true;
                    Auth.startOAuth2Authentication(getApplicationContext(), getString(R.string.dropBoxAppKey));
                }
                break;
            case R.id.cancelSignIn:
                saveToCloud.dismiss();
                switch(cloud) {
                    case Constants.GOOGLE_DRIVE_CLOUD:
                        settingsEditor.putBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, false);
                        settingsEditor.commit();
                        switchOnDrive.setChecked(false);
                        break;
                    case Constants.DROPBOX_CLOUD:
                        settingsEditor.putBoolean(Constants.SAVE_TO_DROPBOX, false);
                        settingsEditor.commit();
                        switchOnDropbox.setChecked(false);
                        break;
                    }
                break;
            }
    }

    public void accountsPermission(View view){
        switch (view.getId()){
            case R.id.yesPermission:
                Log.d(TAG,"yesPermission");
                permissionAccount.dismiss();
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.GET_ACCOUNTS}, GET_ACCOUNTS_PERM);
                break;
            case R.id.noPermission:
                Log.d(TAG,"noPermission");
                if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                    switchOnDrive.setChecked(false);
                }
                permissionAccount.dismiss();
                break;
        }
    }

    public void initializeGoogleSignIn(){
        signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .requestScopes(Drive.SCOPE_APPFOLDER)
                        .build();
        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume goToDropbox = "+goToDropbox);
        updateSettingsValues();
        if (signInProgress) {
            signInProgressDialog.dismiss();
            Log.d(TAG, "Reset signinprogess");
            signInProgress = false;
        }
        if(goToDropbox) {
            goToDropbox = false;
            Log.d(TAG, "Access token = " + Auth.getOAuth2Token());
            if(Auth.getOAuth2Token() == null){
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.signInDropboxFail),Toast.LENGTH_LONG).show();
                switchOnDropbox.setChecked(false);
                disableDropboxInSetting();
            }
            else{
                settingsEditor.putString(Constants.DROPBOX_ACCESS_TOKEN, Auth.getOAuth2Token());
                settingsEditor.commit();
                dbxRequestConfig = new DbxRequestConfig("dropbox/flipCam");
                dbxClientV2 = new DbxClientV2(dbxRequestConfig, Auth.getOAuth2Token());
                if(settingsPref.contains(Constants.DROPBOX_FOLDER) && (!settingsPref.getString(Constants.DROPBOX_FOLDER,"").equals("")
                        && !settingsPref.getString(Constants.DROPBOX_FOLDER,"").equalsIgnoreCase(getResources().getString(R.string.app_name)))){
                    checkIfFolderCreatedInDropbox();
                }
                else {
                    //Folder name is same as app name
                    updateDropboxInSetting(getResources().getString(R.string.app_name), true);
                    TextView dropBoxfolderCreated = (TextView) accessGrantedDropboxRoot.findViewById(R.id.dropBoxFolderCreated);
                    dropBoxfolderCreated.setText(getResources().getString(R.string.autouploadFolderUpdated, getResources().getString(R.string.flipCamAppFolder),
                            getResources().getString(R.string.dropbox)));
                    accesGrantedDropbox.setContentView(accessGrantedDropboxRoot);
                    accesGrantedDropbox.setCancelable(false);
                    accesGrantedDropbox.show();
                }
            }
        }
    }

    public void accessGrantedDropbox(View view){
        accesGrantedDropbox.dismiss();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
        if(signInProgress){
            signInProgressDialog.dismiss();
            Log.d(TAG,"Reset signinprogess");
            signInProgress = false;
        }
    }

    String accName;
    public void continueToGoogleDrive(){
        if(!isConnectedToInternet()){
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
            switchOnDrive.setChecked(false);
            return;
        }
        initializeGoogleSignIn();
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(Drive.SCOPE_FILE);
        requiredScopes.add(Drive.SCOPE_APPFOLDER);
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        Account[] googleAccount = accountManager.getAccountsByType("com.google");
        if (googleAccount != null && googleAccount.length > 0) {
            if(googleAccount.length > 0){
                Log.d(TAG, "Acc name = " + googleAccount[0].name);
                accName = googleAccount[0].name;
            }
        } else {
            Log.d(TAG, "No google account");
        }
        if ((googleAccount != null && googleAccount.length > 0) && signInAccount != null && signInAccount.getGrantedScopes().containsAll(requiredScopes)) {
            getDriveClient(signInAccount);
            signedInDrive = true;
            checkIfFolderCreatedInDrive();
        } else {
            Log.d(TAG,"startActivity");
            signInProgress = true;
            TextView signInText = (TextView)signInProgressRoot.findViewById(R.id.signInText);
            TextView signInprogressTitle = (TextView)signInProgressRoot.findViewById(R.id.savetocloudtitle);
            if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                signInprogressTitle.setText(getResources().getString(R.string.signInProgressTitle, getResources().getString(R.string.googleDrive)));
                signInText.setText(getResources().getString(R.string.signInProgress, getResources().getString(R.string.googleDrive)));
                ImageView signInImage = (ImageView) signInProgressRoot.findViewById(R.id.signInImage);
                signInImage.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
            }
            signInProgressDialog.setContentView(signInProgressRoot);
            signInProgressDialog.setCancelable(false);
            signInProgressDialog.show();
            startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        }
    }

    public boolean isConnectedToInternet(){
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    public void checkIfFolderCreatedInDropbox(){
        TextView uploadFolderMsg = (TextView)uploadFolderCheckRoot.findViewById(R.id.uploadFolderMsg);
        uploadFolderMsg.setText(getResources().getString(R.string.uploadCheckDropboxMsg, getResources().getString(R.string.dropbox)));
        ImageView signinImage = (ImageView)uploadFolderCheckRoot.findViewById(R.id.signInImage);
        signinImage.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
        uploadFolderCheck.setContentView(uploadFolderCheckRoot);
        uploadFolderCheck.setCancelable(false);
        uploadFolderCheck.show();
        final String folderName = settingsPref.getString(Constants.DROPBOX_FOLDER, "");
        Log.d(TAG, "saved folderName = "+folderName);
        if (folderName != null && !folderName.equals("")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        com.dropbox.core.v2.files.Metadata metadata = dbxClientV2.files().getMetadata(folderName);
                        Log.d(TAG, "dropbox name = "+metadata.getName());
                        Log.d(TAG, "multiline = "+metadata.toStringMultiline());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uploadFolderCheck.dismiss();
                            }
                        });
                        if(!metadata.getName().equals("")) {
                            Log.d(TAG, "Save folder name in setting");
                            ImageView placeholdericon = (ImageView) autoUploadEnabledRoot.findViewById(R.id.placeHolderIconAutoUpload);
                            placeholdericon.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
                            TextView autoUploadMsg = (TextView) autoUploadEnabledRoot.findViewById(R.id.autoUploadMsg);
                            autoUploadMsg.setText(getResources().getString(R.string.autouploadFolderUpdated, metadata.getName(), getResources().getString(R.string.dropbox)));
                            TextView folderNameTxt = (TextView) autoUploadEnabledRoot.findViewById(R.id.folderName);
                            folderNameTxt.setText(metadata.getName());
                            autoUploadEnabled.setContentView(autoUploadEnabledRoot);
                            autoUploadEnabled.setCancelable(false);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    autoUploadEnabled.show();
                                }
                            });
                            switchOnDrive.setChecked(true);
                            updateDropboxInSetting(metadata.getName(), true);
                        }
                        else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    createUploadFolder();
                                }
                            });
                        }
                    }
                    catch(GetMetadataErrorException metadataerror){
                        Log.d(TAG, "Folder not present = "+metadataerror.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uploadFolderCheck.dismiss();
                                createUploadFolder();
                            }
                        });
                    }
                    catch (DbxException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        else{
            uploadFolderCheck.dismiss();
            createUploadFolder();
        }
    }

    public void checkIfFolderCreatedInDrive(){
        TextView uploadFolderMsg = (TextView)uploadFolderCheckRoot.findViewById(R.id.uploadFolderMsg);
        uploadFolderMsg.setText(getResources().getString(R.string.uploadCheckMessage, getResources().getString(R.string.googleDrive)));
        ImageView signinImage = (ImageView)uploadFolderCheckRoot.findViewById(R.id.signInImage);
        signinImage.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
        uploadFolderCheck.setContentView(uploadFolderCheckRoot);
        uploadFolderCheck.setCancelable(false);
        uploadFolderCheck.show();
        final String folderName = settingsPref.getString(Constants.GOOGLE_DRIVE_FOLDER, "");
        Log.d(TAG, "saved folderName = "+folderName);
        if (folderName != null && !folderName.equals("")) {
            mDriveClient.requestSync()
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "sync success");
                            queryForFolder(folderName);
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if(!isConnectedToInternet()) {
                                Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
                                uploadFolderCheck.dismiss();
                                switchOnDrive.setChecked(false);
                                disableGoogleDriveInSetting();
                                googleSignInClient.signOut();
                            }
                            else if(e.getMessage().contains(String.valueOf(DriveStatusCodes.DRIVE_RATE_LIMIT_EXCEEDED))){
                                Log.d(TAG, "sync already done");
                                //Continue as is, since already synced.
                                queryForFolder(folderName);
                            }
                            else if(e.getMessage().contains(String.valueOf(CommonStatusCodes.TIMEOUT))){
                                Toast.makeText(getApplicationContext(),getResources().getString(R.string.timeoutErrorSync),Toast.LENGTH_SHORT).show();
                                uploadFolderCheck.dismiss();
                                switchOnDrive.setChecked(false);
                                disableGoogleDriveInSetting();
                                googleSignInClient.signOut();
                            }
                        }
                    });
        }
        else{
            uploadFolderCheck.dismiss();
            createUploadFolder();
        }
    }

    CustomPropertyKey ownerKey = new CustomPropertyKey("owner", CustomPropertyKey.PUBLIC);
    Query query = null;
    public void queryForFolder(final String folder){
        if(query == null){
            query = new Query.Builder()
                    .addFilter(Filters.eq(SearchableField.TITLE, folder))
                    .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"))
                    .addFilter(Filters.eq(SearchableField.TRASHED, false))
                    .addFilter(Filters.eq(ownerKey, accName))
                    .build();
        }
        mDriveResourceClient.query(query)
                .addOnSuccessListener(new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadatas) {
                        Log.d(TAG, "result metadata = " + metadatas);
                        Iterator<Metadata> iterator = metadatas.iterator();
                        if (metadatas.getCount() > 0 && iterator.hasNext()) {
                            Metadata metadata = iterator.next();
                            final String driveFolderName = metadata.getTitle();
                            Log.d(TAG, "MD title = " + metadata.getTitle());
                            Log.d(TAG, "MD created date = " + metadata.getCreatedDate());
                            Log.d(TAG, "MD drive id = " + metadata.getDriveId());
                            Log.d(TAG, "MD resource id = " + metadata.getDriveId().getResourceId());
                            mDriveClient.getDriveId(metadata.getDriveId().getResourceId())
                                    .addOnSuccessListener(new OnSuccessListener<DriveId>() {
                                        @Override
                                        public void onSuccess(DriveId driveId) {
                                            uploadFolderCheck.dismiss();
                                            ImageView placeholdericon = (ImageView) autoUploadEnabledRoot.findViewById(R.id.placeHolderIconAutoUpload);
                                            placeholdericon.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
                                            TextView autoUploadMsg = (TextView) autoUploadEnabledRoot.findViewById(R.id.autoUploadMsg);
                                            autoUploadMsg.setText(getResources().getString(R.string.autouploadFolderUpdated, driveFolderName, getResources().getString(R.string.googleDrive)));
                                            TextView folderName = (TextView) autoUploadEnabledRoot.findViewById(R.id.folderName);
                                            folderName.setText(driveFolderName);
                                            autoUploadEnabled.setContentView(autoUploadEnabledRoot);
                                            autoUploadEnabled.setCancelable(false);
                                            autoUploadEnabled.show();
                                            switchOnDrive.setChecked(true);
                                            updateGoogleDriveInSetting(driveFolderName,true,accName);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            if(!isConnectedToInternet()) {
                                                Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
                                            }
                                            else if(e.getMessage().contains(String.valueOf(CommonStatusCodes.TIMEOUT))){
                                                Toast.makeText(getApplicationContext(),getResources().getString(R.string.timeoutErrorSync),Toast.LENGTH_SHORT).show();
                                            }
                                            uploadFolderCheck.dismiss();
                                            switchOnDrive.setChecked(false);
                                            disableGoogleDriveInSetting();
                                            googleSignInClient.signOut();
                                        }
                                    });
                        } else {
                            Log.d(TAG, "No folder exists with name = " + folder);
                            uploadFolderCheck.dismiss();
                            createUploadFolder();
                        }
                        metadatas.release();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Failure = " + e.getMessage());
                        if(!isConnectedToInternet()) {
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
                        }
                        else if(e.getMessage().contains(String.valueOf(CommonStatusCodes.TIMEOUT))){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.timeoutErrorSync),Toast.LENGTH_SHORT).show();
                        }
                        uploadFolderCheck.dismiss();
                        switchOnDrive.setChecked(false);
                        disableGoogleDriveInSetting();
                        googleSignInClient.signOut();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case GET_ACCOUNTS_PERM:
                if(permissions != null && permissions.length > 0) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG,"permission given");
                        if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                            continueToGoogleDrive();
                        }
                    } else {
                        Log.d(TAG,"permission rational");
                        saveToCloud.dismiss();
                        if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                            switchOnDrive.setChecked(false);
                        }
                    }
                }
                else{
                    super.onRequestPermissionsResult(requestCode,permissions,grantResults);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode != RESULT_OK) {
                    //Sign in failed due to connection problem or user cancelled it.
                    Log.d(TAG, "Sign-in failed.");
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.signinfail),Toast.LENGTH_LONG).show();
                    if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                        switchOnDrive.setChecked(false);
                        signedInDrive = false;
                        disableGoogleDriveInSetting();
                    }
                    return;
                }
                Task<GoogleSignInAccount> getAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                if (getAccountTask.isSuccessful()) {
                    Log.d(TAG,"isSuccessful");
                    getDriveClient(getAccountTask.getResult());
                    signedInDrive = true;
                    //Check For Connectivity again.
                    if(!isConnectedToInternet()){
                        Log.d(TAG,"NO Internet");
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
                        switchOnDrive.setChecked(false);
                        disableGoogleDriveInSetting();
                    }
                    else {
                        checkIfFolderCreatedInDrive();
                    }
                } else {
                    Log.e(TAG, "Sign-in failed 222.");
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.signinfail),Toast.LENGTH_LONG).show();
                    if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                        switchOnDrive.setChecked(false);
                        signedInDrive = false;
                        disableGoogleDriveInSetting();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void disableGoogleDriveInSetting(){
        settingsEditor.putBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, false);
        settingsEditor.commit();
    }

    public void disableDropboxInSetting(){
        settingsEditor.putBoolean(Constants.SAVE_TO_DROPBOX, false);
        settingsEditor.commit();
    }

    private void getDriveClient(GoogleSignInAccount signInAccount) {
        Log.d(TAG,"getDriveClient");
        mDriveClient = Drive.getDriveClient(getApplicationContext(), signInAccount);
        mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), signInAccount);
        Log.d(TAG, "Sign-in SUCCESS.");
    }

    EditText folderNameText;
    public void createUploadFolder(){
        uploadFolderMsg = (TextView)cloudUploadRoot.findViewById(R.id.uploadFolderMsg);
        uploadFolderTitle = (TextView)cloudUploadRoot.findViewById(R.id.uploadFolderTitle);
        uploadDestIcon = (ImageView) cloudUploadRoot.findViewById(R.id.uploadDestIcon);
        if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
            uploadFolderMsg.setText(getResources().getString(R.string.uploadFolder, getResources().getString(R.string.googleDrive)));
            uploadFolderTitle.setText(getResources().getString(R.string.uploadFolderTitle, getResources().getString(R.string.googleDrive)));
            uploadDestIcon.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
        }
        else if(cloud == Constants.DROPBOX_CLOUD){
            uploadFolderMsg.setText(getResources().getString(R.string.uploadFolder, getResources().getString(R.string.dropbox)));
            uploadFolderTitle.setText(getResources().getString(R.string.uploadFolderTitle, getResources().getString(R.string.dropbox)));
            uploadDestIcon.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
        }
        Log.d(TAG,"Open cloud upload dialog");
        cloudUpload.setContentView(cloudUploadRoot);
        cloudUpload.setCancelable(false);
        cloudUpload.show();
        folderNameText = (EditText)cloudUploadRoot.findViewById(R.id.folderNameText);
        folderNameText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                Log.d(TAG,"hasFocus = "+hasFocus);
                if(hasFocus){
                    cloudUpload.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    private boolean validateFolderNameDropBox(){
        String folderName = ((EditText)cloudUploadRoot.findViewById(R.id.folderNameText)).getText().toString();
        String[] invalidChars = new String[]{"\\","/","?",":","*","\"","|"};
        for(int i=0;i<invalidChars.length;i++){
            if(folderName.contains(invalidChars[i])){
                return false;
            }
        }
        return true;
    }

    private boolean validateFolderNameIsNotEmpty(){
        String folderName = ((EditText)cloudUploadRoot.findViewById(R.id.folderNameText)).getText().toString();
        return !folderName.trim().equals("");
    }

    public void uploadFolder(View view) {
        switch (view.getId()) {
            case R.id.createFolder:
                if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                    if (validateFolderNameIsNotEmpty()) {
                        cloudUpload.dismiss();
                        mDriveResourceClient
                                .getRootFolder()
                                .continueWithTask(new Continuation<DriveFolder, Task<DriveFolder>>() {
                                    @Override
                                    public Task<DriveFolder> then(@NonNull Task<DriveFolder> task)
                                            throws Exception {
                                        DriveFolder parentFolder = task.getResult();
                                        CustomPropertyKey ownerKey = new CustomPropertyKey("owner", CustomPropertyKey.PUBLIC);
                                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                                .setTitle(folderNameText.getText().toString())
                                                .setMimeType(DriveFolder.MIME_TYPE)
                                                .setCustomProperty(ownerKey, accName)
                                                .build();
                                        Log.d(TAG,"Creating folder in Drive");
                                        return mDriveResourceClient.createFolder(parentFolder, changeSet);
                                    }
                                })
                                .addOnSuccessListener(this,
                                        new OnSuccessListener<DriveFolder>() {
                                            @Override
                                            public void onSuccess(DriveFolder driveFolder) {
                                                ImageView placeholdericon = (ImageView) autoUploadEnabledWithFolderRoot.findViewById(R.id.placeHolderIconAutoUpload);
                                                placeholdericon.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
                                                TextView folderCreated = (TextView) autoUploadEnabledWithFolderRoot.findViewById(R.id.folderCreatedMsg);
                                                folderCreated.setText(getResources().getString(R.string.folderCreatedSuccess, folderNameText.getText().toString()));
                                                TextView autoUploadMsg = (TextView) autoUploadEnabledWithFolderRoot.findViewById(R.id.autoUploadMsg);
                                                autoUploadMsg.setText(getResources().getString(R.string.autouploadFolderCreated, getResources().getString(R.string.googleDrive)));
                                                autoUploadEnabledWithFolder.setContentView(autoUploadEnabledWithFolderRoot);
                                                autoUploadEnabledWithFolder.setCancelable(false);
                                                autoUploadEnabledWithFolder.show();
                                                switchOnDrive.setChecked(true);
                                                updateGoogleDriveInSetting(folderNameText.getText().toString(), true, accName);
                                            }
                                        })
                                .addOnFailureListener(this, new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "Unable to create folder", e);
                                        Toast.makeText(getApplicationContext(),
                                                getResources().getString(R.string.foldercreateErrorGoogleDrive, folderNameText.getText().toString()),
                                                Toast.LENGTH_SHORT).show();
                                        switchOnDrive.setChecked(false);
                                        signedInDrive = false;
                                        updateGoogleDriveInSetting("", false, "");
                                        googleSignInClient.signOut();
                                    }
                                });
                    } else {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.uploadFolderEmpty), Toast.LENGTH_SHORT).show();
                    }
                }
                else if(cloud == Constants.DROPBOX_CLOUD){
                    if(validateFolderNameDropBox()) {
                        cloudUpload.dismiss();
                        final DbxUserFilesRequests dbxUserFilesRequests = dbxClientV2.files();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        CreateFolderResult createFolderResult = dbxUserFilesRequests.createFolderV2("/" + folderNameText.getText().toString());
                                        String folderId = createFolderResult.getMetadata().getId();
                                        if (folderId != null && !folderId.equals("")) {
                                            ImageView placeholdericon = (ImageView) autoUploadEnabledWithFolderRoot.findViewById(R.id.placeHolderIconAutoUpload);
                                            placeholdericon.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
                                            TextView folderCreated = (TextView) autoUploadEnabledWithFolderRoot.findViewById(R.id.folderCreatedMsg);
                                            folderCreated.setText(getResources().getString(R.string.folderCreatedSuccess, folderNameText.getText().toString()));
                                            TextView autoUploadMsg = (TextView) autoUploadEnabledWithFolderRoot.findViewById(R.id.autoUploadMsg);
                                            autoUploadMsg.setText(getResources().getString(R.string.autouploadFolderCreated, getResources().getString(R.string.dropbox)));
                                            autoUploadEnabledWithFolder.setContentView(autoUploadEnabledWithFolderRoot);
                                            autoUploadEnabledWithFolder.setCancelable(false);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    autoUploadEnabledWithFolder.show();
                                                }
                                            });
                                            switchOnDropbox.setChecked(true);
                                            Log.d(TAG, "getPathDisplay = " + createFolderResult.getMetadata().getPathDisplay());
                                            updateDropboxInSetting(createFolderResult.getMetadata().getName(), true);
                                        } else {
                                            Log.d(TAG, "Unable to create folder");
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(getApplicationContext(),
                                                            getResources().getString(R.string.foldercreateErrorGoogleDrive, folderNameText.getText().toString()),
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                            switchOnDropbox.setChecked(false);
                                            revokeAccessFromDropbox();
                                            updateDropboxInSetting("", false);
                                        }
                                    } catch (DbxException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.uploadFolderDropbox), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.cancelFolder:
                cloudUpload.dismiss();
                if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                    switchOnDrive.setChecked(false);
                    signedInDrive = false;
                    //updateGoogleDriveInSetting("",false,"");
                    disableGoogleDriveInSetting();
                    googleSignInClient.signOut();
                    showUploadDisabled();
                }
                else if(cloud == Constants.DROPBOX_CLOUD){
                    switchOnDropbox.setChecked(false);
                    disableDropboxInSetting();
                    revokeAccessFromDropbox();
                    showUploadDisabled();
                }
        }
    }

    public void revokeAccessFromDropbox(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dbxClientV2.auth().tokenRevoke();
                    Log.d(TAG, "Token revoked");
                } catch (DbxException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        settingsEditor.remove(Constants.DROPBOX_ACCESS_TOKEN);
        settingsEditor.commit();
    }

    public void showUploadDisabled(){
        ImageView placeholderIcon = (ImageView) autoUploadDisabledRoot.findViewById(R.id.placeHolderIconAutoUploadDisabled);
        TextView disabledMsg = (TextView) autoUploadDisabledRoot.findViewById(R.id.autoUploadDisabledMsg);
        switch (cloud){
            case Constants.GOOGLE_DRIVE_CLOUD:
                placeholderIcon.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
                disabledMsg.setText(getResources().getString(R.string.signoutcloud, getResources().getString(R.string.googleDrive)));
                break;
            case Constants.DROPBOX_CLOUD:
                placeholderIcon.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
                String savedFolder = settingsPref.getString(Constants.DROPBOX_FOLDER, "");
                disabledMsg.setText(getResources().getString(R.string.üploadDisabledDropbox, savedFolder));
                break;
        }
        autoUploadDisabled.setContentView(autoUploadDisabledRoot);
        autoUploadDisabled.setCancelable(false);
        autoUploadDisabled.show();
    }

    public void closeAutoUploadWithFolder(View view){
        autoUploadEnabledWithFolder.dismiss();
    }

    public void closeAutoUpload(View view){
        autoUploadEnabled.dismiss();
    }

    public void closeAutoUploadDisabled(View view){
        autoUploadDisabled.dismiss();
    }

    public void updateGoogleDriveInSetting(String folderName, boolean saveTo, String accname){
        Log.d(TAG, "Saving folder = "+folderName);
        settingsEditor.putString(Constants.GOOGLE_DRIVE_FOLDER,folderName);
        settingsEditor.putBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, saveTo);
        settingsEditor.putString(Constants.GOOGLE_DRIVE_ACC_NAME, accname);
        settingsEditor.commit();
    }

    public void updateDropboxInSetting(String folderName, boolean saveTo){
        Log.d(TAG, "Saving DB folder = "+folderName);
        settingsEditor.putString(Constants.DROPBOX_FOLDER,folderName);
        settingsEditor.putBoolean(Constants.SAVE_TO_DROPBOX, saveTo);
        settingsEditor.commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reDrawSDCardScreen();
    }
}
