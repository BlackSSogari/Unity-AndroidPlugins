package com.nativekeyboard;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.unity3d.player.UnityPlayer;

/**
 * Created by Kwon on 2017-11-08.
 */

public class NativeKeyboardPlugin
{
    private Activity activity;
    private View unityPlayerActivityView;
    private View decorView;

    private  int uiOption;

    private InputMethodManager inputMethodManager;
    private ViewTreeObserver.OnGlobalLayoutListener listener;
    private TextWatcher textWatcher;
    private View.OnFocusChangeListener focusChangeListener;

    private boolean multiLinesMode = false;
    private boolean checkHeight = true;

    public NativeKeyboardPlugin()
    {
        activity = UnityPlayer.currentActivity;
        unityPlayerActivityView = activity.getWindow().getCurrentFocus();
        inputMethodManager = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        listener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect rect = new Rect();
                unityPlayerActivityView.getWindowVisibleDisplayFrame(rect);
                int screenHeight = unityPlayerActivityView.getRootView().getHeight();
                int keypadHeight = screenHeight - rect.bottom;

                if (keypadHeight > 0) {
                    //SendData(1, keypadHeight);
                    OnKeyboardHeight(keypadHeight);
                }

                if (checkHeight) {
                    checkHeight = false;
                    return;
                }
                if (keypadHeight == 0) {
                    //OnKeyboardHide();
                    OnKeyboardHeight(keypadHeight);
                }
            }
        };

        textWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!multiLinesMode) {
                    if (s.length() == 0) {
                        //SendData(2, "");
                        OnKeyboardInputChanged("");
                        return;
                    }

                    char lastchar = s.toString().charAt(s.length()-1);

                    if (lastchar == '\n') {
                        close();
                    }
                    else {
                        //SendData(2, s.toString());
                        OnKeyboardInputChanged(s.toString());
                    }
                } else {
                    //SendData(2, s.toString());
                    OnKeyboardInputChanged(s.toString());
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                decorView = activity.getWindow().getDecorView();
                uiOption = decorView.getSystemUiVisibility();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                    uiOption |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    uiOption |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    uiOption |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                decorView.setSystemUiVisibility(uiOption);
            }
        });

        focusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    decorView.setSystemUiVisibility(uiOption);
            }
        };
    }

    public int isSoftKeyboardShown(){
        Rect rect = new Rect();
        unityPlayerActivityView.getWindowVisibleDisplayFrame(rect);
        int screenHeight = unityPlayerActivityView.getRootView().getHeight();
        int keypadHeight = screenHeight - rect.bottom;


        return keypadHeight;
    }

    public int GetKeyboardHeight()
    {
        Rect rect = new Rect();
        unityPlayerActivityView.getWindowVisibleDisplayFrame(rect);
        return rect.height();
    }

    public void show (final String text, final boolean mode) {
        checkHeight = true;
        multiLinesMode = mode;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutInflater inflater = activity.getLayoutInflater();
                Resources resources = activity.getResources();
                String packageName = activity.getPackageName();
                int id = resources.getIdentifier("input", "layout", packageName);
                View view = inflater.inflate(id, null);
                FrameLayout.LayoutParams param = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                param.setMargins(2000, 0, 0, 0); // move from screen
                activity.addContentView(view, param);
                initText(text);
            }
        });
    }

    private void initText (final String text) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Resources resources = activity.getResources();
                String packageName = activity.getPackageName();
                final EditText textArea = (EditText)activity.findViewById(resources.getIdentifier("textArea", "id", packageName));

                //textArea.setText(text);
                //textArea.setSelection(textArea.getText().length());

                textArea.setText("");
                textArea.append(text);

                textArea.setBackgroundColor(0x00000000);
                textArea.setTextColor(0x00000000);
                textArea.addTextChangedListener(textWatcher);
                textArea.setOnFocusChangeListener(focusChangeListener);
                textArea.setFocusableInTouchMode(true);
                textArea.requestFocus();
                textArea.setCursorVisible(false);

                if (!multiLinesMode)
                    textArea.setMaxLines(1);

                inputMethodManager.showSoftInput(textArea, InputMethodManager.SHOW_IMPLICIT);
                unityPlayerActivityView.getViewTreeObserver().addOnGlobalLayoutListener(listener);

                //SendData(3, textArea.getText().toString());
                OnKeyboardShow(textArea.getText().toString());
            }
        });
    }

    private void close() {
        inputMethodManager.hideSoftInputFromWindow(unityPlayerActivityView.getWindowToken(), 0);
        //SendData(0, null);
        OnKeyboardHide();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            unityPlayerActivityView.getViewTreeObserver().removeOnGlobalLayoutListener(listener);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Resources resources = activity.getResources();
                String packageName = activity.getPackageName();
                EditText textArea = (EditText) activity.findViewById(resources.getIdentifier("textArea", "id", packageName));
                if (textArea != null) {
                    ViewGroup viewGroup = (ViewGroup) textArea.getParent();
                    viewGroup.removeView(textArea);
                }
            }
        });
    }

//    void SendData (int code, Object info) {
//        String data;
//        if (info == null)
//            info = "";
//
//        data = String.format("{\"code\":%s,\"data\":\"%s\"}", code, info);
//
//        UnityPlayer.UnitySendMessage("NativeKeyboardManager", "OnCustomInputAction", data.toString());
//    }

    private void OnKeyboardHeight(int keyboradHeight)
    {
        UnityPlayer.UnitySendMessage("NativeKeyboardManager", "OnKeyboardHeight", Integer.toString(keyboradHeight));
    }

    private void OnKeyboardHide()
    {
        UnityPlayer.UnitySendMessage("NativeKeyboardManager", "OnKeyboardHide", "");
    }

    private void OnKeyboardInputChanged(String str)
    {
        UnityPlayer.UnitySendMessage("NativeKeyboardManager", "OnKeyboardInputChanged", str);
    }

    private void OnKeyboardShow(String str)
    {
        UnityPlayer.UnitySendMessage("NativeKeyboardManager", "OnKeyboardShow", str);
    }

    public boolean hasSoftMenu() {
        //메뉴버튼 유무
        boolean hasMenuKey = ViewConfiguration.get(activity.getApplicationContext()).hasPermanentMenuKey();

        //뒤로가기 버튼 유무
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

        if (!hasMenuKey && !hasBackKey) { // lg폰 소프트키일 경우
            return true;
        } else { // 삼성폰 등.. 메뉴 버튼, 뒤로가기 버튼 존재
            return false;
        }
    }

    public void getSoftMenuHeight() {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Resources resources = activity.getResources();
                int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
                int deviceHeight = 0;

                if (resourceId > 0) {
                    deviceHeight = resources.getDimensionPixelSize(resourceId);
                }

                OnSoftKeyHeight(Integer.toString(deviceHeight));
            }
        });
    }

    private void OnSoftKeyHeight(String str)
    {
        UnityPlayer.UnitySendMessage("NativeKeyboardManager", "OnSoftkeyHeight", str);
    }
}

