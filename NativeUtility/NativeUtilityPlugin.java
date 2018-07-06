package com.nativeutility;

/**
 * Created by Kwon on 2017-11-17.
 */

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import com.unity3d.player.*;
import static com.unity3d.player.UnityPlayer.currentActivity;

public class NativeUtilityPlugin extends UnityPlayerActivity {
    private static final String TAG = RogReceiver.class.getSimpleName();

    public static void TEST_OpenGLES() {
        Log.e(TAG, "TEST_OpenGLES start.");

        String version_gles20 = GLES20.glGetString(GLES20.GL_VERSION);
        String extensions_gles20 = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        String version_gles30 = GLES30.glGetString(GLES30.GL_VERSION);
        String extensions_gles30 = GLES30.glGetString(GLES30.GL_EXTENSIONS);
        Log.w(TAG, "GLES20.GL_VERSION: " + version_gles20 );
        Log.w(TAG, "GLES20.GL_EXTENSIONS: " + extensions_gles20 );
        Log.w(TAG, "GLES30.GL_VERSION: " + version_gles30 );
        Log.w(TAG, "GLES30.GL_EXTENSIONS: " + extensions_gles30 );

        final Activity activity = currentActivity;
        if (activity != null) {
            Context context = activity.getApplicationContext();
            String name = context.getApplicationContext().getPackageName() + ".v2.playerprefs";
            SharedPreferences sharedPref = context.getApplicationContext().getSharedPreferences(name, Context.MODE_PRIVATE);
            if (sharedPref != null) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("version_gles20", version_gles20);
                editor.putString("extensions_gles20", extensions_gles20);
                editor.putString("version_gles30", version_gles30);
                editor.putString("extensions_gles30", extensions_gles30);
                editor.commit();
            } else {
                Log.e(TAG, "TEST_OpenGLES can't find sharedPref.");
            }
        } else {
            Log.e(TAG, "TEST_OpenGLES can't find activity.");
        }

        Log.e(TAG, "TEST_OpenGLES done.");
    }

    public static void CopyClipboard(final String t)
    {
        final Activity activity = currentActivity;
        if (activity == null)
            return;

        activity.runOnUiThread(new Runnable()
        {
            public void run()
            {
                ClipboardManager clipboardManager = (ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("text_data", t);
                clipboardManager.setPrimaryClip(clipData);
            }
        });
    }

    public static String getNetworkState()
    {
        final Activity activity = currentActivity;
        if (activity == null)
            return null;

        ConnectivityManager connectivityManager = (ConnectivityManager)activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        String networkState = "DISCONNECT";
        if (networkInfo != null && networkInfo.isConnected())
        {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
                networkState = "WIFI";
            else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
                networkState = "WWAN";
        }

        return networkState;
    }

    public static String getPackageInfo()
    {
        final Activity activity = currentActivity;
        if (activity == null)
            return null;

        String packageName = activity.getPackageName();

        PackageManager packageManager = activity.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            String apk = applicationInfo.publicSourceDir;
            String apklabel = packageManager.getApplicationLabel(applicationInfo).toString();
            return String.format("%s|%s|%s", packageName, apk, apklabel);
        } catch (Throwable x) {
        }

        return  null;
    }

}
