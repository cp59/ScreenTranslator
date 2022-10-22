package com.zeroapp.screentranslator;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TranslatorAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override
    public void onInterrupt() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        closeQSAndTakeScreenshot();
        return super.onStartCommand(intent, flags, startId);
    }
    private boolean isAccessibilityServiceEnabled() {
        ComponentName expectedComponentName = new ComponentName(getApplicationContext(), TranslatorAccessibilityService.class);
        String enabledServicesSetting = Settings.Secure.getString(getContentResolver(),  Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null)
            return false;
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
            if (enabledService != null && enabledService.equals(expectedComponentName))
                return true;
        }
        return false;
    }
    private void closeQSAndTakeScreenshot() {
        if (isAccessibilityServiceEnabled()) {
            float midX = getResources().getDisplayMetrics().widthPixels * .5F;
            float bottomY = getResources().getDisplayMetrics().heightPixels * .95F;
            float topY = getResources().getDisplayMetrics().heightPixels * .05F;
            Path path = new Path();
            path.moveTo(midX,bottomY);
            path.lineTo(midX,topY);
            GestureDescription gestureDescription = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path,0,50))
                    .build();
            dispatchGesture(gestureDescription, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> takeScreenshot(Display.DEFAULT_DISPLAY, getApplication().getMainExecutor(), new TakeScreenshotCallback() {
                        @Override
                        public void onSuccess(@NonNull ScreenshotResult screenshot) {
                            Bitmap screenBitmap = Bitmap.wrapHardwareBuffer(screenshot.getHardwareBuffer(),screenshot.getColorSpace());
                            File screenshotFile = new File(getFilesDir(), "screenshot.png");
                            if (!screenshotFile.exists()) {
                                try {
                                    screenshotFile.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                FileOutputStream fileOutputStream = new FileOutputStream(screenshotFile);
                                screenBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                                fileOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Uri fileUri = FileProvider.getUriForFile(getApplicationContext(), "com.zeroapp.screentranslator.fileProvider", screenshotFile);
                            Intent intent = new Intent(Intent.ACTION_SEND)
                                    .addCategory(Intent.CATEGORY_DEFAULT)
                                    .setPackage("com.naver.labs.translator")
                                    .setClassName("com.naver.labs.translator","com.naver.labs.translator.ui.main.DeepLinkActivity")
                                    .setDataAndTypeAndNormalize(fileUri, "image/*")
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        }
                        @Override
                        public void onFailure(int errorCode) {}
                    }),50);
                }
            },null);
        } else {
            startActivity(new Intent(getApplicationContext(),MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

}
