using Rg;
using Rg.Manager;
using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Android 전용
/// </summary>
public class NativeKeyboardManager : MonoBehaviour {

    public Image m_TextBox;
    public CanvasGroup m_TextBoxCanvasGroup;
    public Text m_TextInputField;

    [HideInInspector]
    public int NativeKeyboardHeight = -1;
    public bool IsShowKeyboard { get; set; }
    public string currentInputString { get; set; }

    private int SoftManuHeight = -1;

    private Action<string> OnTextChangedAction;
    private Action<string> OnTextEndEditAction;

    #region Singleton

    static NativeKeyboardManager _Manager = null;
    public static NativeKeyboardManager GetManager()
    {
        if (_Manager == null)
        {
            _Manager = GameObject.FindObjectOfType<NativeKeyboardManager>();
        }
        if (_Manager == null)
        {
            GameObject entry = ResourceManager.CreatePrefabInstance("NativeKeyboardManager", "NativeKeyboardManager");
            if (entry != null)
            {
                entry.name = "NativeKeyboardManager";
                entry.transform.position = Vector3.zero;
                entry.transform.localScale = Vector3.one;

                _Manager = entry.GetComponent<NativeKeyboardManager>();

                Log.Write("[NativeKeyboardManager] NativeKeyboardManager Create Success!");
            }
        }

        return _Manager;
    }

    #endregion
    //===========================================================================================
    #region Unity Call

    private void Awake()
    {
        this.gameObject.name = "NativeKeyboardManager";

        if (_Manager == null)
        {
            _Manager = this;
            DontDestroyOnLoad(this.gameObject);
        }

        Log.Write("[NativeKeyboardManager] Awake()");
    }

    private void Start()
    {
        this.gameObject.SetActive(true);

        if (m_TextBox != null)
        {            
            m_TextBoxCanvasGroup.alpha = 1;
            m_TextBox.rectTransform.anchoredPosition = new Vector2(0, -100);
        }
    }
    
    private void OnApplicationFocus(bool focus)
    {
        if (!focus)
        {            
            if (IsShowKeyboard)
            {
                IsShowKeyboard = false;
                AndroidJavaObject input = new AndroidJavaObject("com.rg.nativekeyboard.NativeKeyboardPlugin");
                input.Call("close");
                KeyboardHideInternal();
            }   
        }
    }

    private void OnApplicationQuit()
    {
        _Manager = null;
    }

    #endregion
    //===========================================================================================
    public void ShowAndroidNativeKeyboard(string text, Action<string> _textChanged, Action<string> _textEndEdit)
    {
        if (Application.platform != RuntimePlatform.Android)
            return;
        if (IsShowKeyboard)
        {
            Log.WriteError("Already keyboard was Appeared");
            return;
        }

        IsShowKeyboard = true;

        OnTextChangedAction = _textChanged;
        OnTextEndEditAction = _textEndEdit;

        AndroidJavaObject input = new AndroidJavaObject("com.rg.nativekeyboard.NativeKeyboardPlugin");
        bool hasSoftMenu = input.Call<bool>("hasSoftMenu");
        if (hasSoftMenu)
        {
            SoftManuHeight = -1;
            Log.Write("Soft Menu Finded!");
            input.Call("getSoftMenuHeight");
        }
        input.Call("show", text, false);

        isChecking = true;
        StartCoroutine(CoCheckKeyboardVisible());

        Rg.Log.Write("ShowAndroidNativeKeyboard");
    }

    private bool isChecking = true;
    private IEnumerator CoCheckKeyboardVisible()
    {
        yield return new WaitForSeconds(1f);
        while (isChecking)
        {
            bool isVisible = IsKeyboardVisible();
            if (isVisible == false)
            {
                isChecking = false;
                IsShowKeyboard = false;
                KeyboardHideInternal();
            }

            yield return new WaitForSeconds(0.5f);
        }
    }

    public void HideAndroidNativeKeyboard()
    {
        if (Application.platform != RuntimePlatform.Android)
            return;

        IsShowKeyboard = false;

        AndroidJavaObject input = new AndroidJavaObject("com.rg.nativekeyboard.NativeKeyboardPlugin");
        input.Call("close");

        isChecking = false;
        StopAllCoroutines();

        Rg.Log.Write("HideAndroidNativeKeyboard");
    }
        
    private void OnKeyboardShow(string str)
    {
        Rg.Log.Write("OnKeyboardShow : {0}", str);

        IsShowKeyboard = true;

        if (NativeKeyboardHeight > 0)
        {
            Invoke("KeyboardShowInternal", 0.2f);            
        }

        if (m_TextInputField != null)
        {
            m_TextInputField.text = str;
        }
    }

    private void OnKeyboardHide(string dummy)
    {
        Rg.Log.Write("OnKeyboardHide : {0}", dummy);

        IsShowKeyboard = false;

        if (OnTextEndEditAction != null)
            OnTextEndEditAction(currentInputString);

        Invoke("KeyboardHideInternal", 0.2f);
    }

    private void OnKeyboardHeight(string keyboradHeight)
    {
        int height = -1;
        int.TryParse(keyboradHeight, out height);
        
        // 최초 Native Keyboard의 높이를 가져온다.
        if (height > 0 && NativeKeyboardHeight < 0)
        {
            NativeKeyboardHeight = height;
            KeyboardShowInternal();
            Debug.LogFormat("OnKeyboardHeight : {0}", height);
        }        
    }

    private void OnKeyboardInputChanged(string str)
    {
        Rg.Log.Write("OnKeyboardInputChanged : {0}", str);

        currentInputString = str;

        if (m_TextInputField != null)
        {
            m_TextInputField.text = currentInputString;
        }

        if (OnTextChangedAction != null)
            OnTextChangedAction(currentInputString);
    }
    
    private void KeyboardShowInternal()
    {
        Rg.Log.Write("KeyboardShowInternal");
        
        if (m_TextBox != null)
        {            
            if (SoftManuHeight > 0)
            {
                float width = SystemEntry.OriginalScreenSize.x / GetResolutionRatio();
                float height = m_TextBox.rectTransform.sizeDelta.y;
                float softKeyHeight = SoftManuHeight / GetResolutionRatio();
                float newWidth = width - softKeyHeight;
                m_TextBox.rectTransform.sizeDelta = new Vector2(newWidth, height);
            }

            float inputBoxH = NativeKeyboardHeight / GetResolutionRatio();
            Log.Write("Native Keyboard input box H : {0},{1}", inputBoxH, SystemEntry.OriginalScreenSize.x);
            m_TextBox.rectTransform.anchoredPosition = new Vector2(0, inputBoxH);
        }
    }

    private void KeyboardHideInternal()
    {
        Rg.Log.Write("KeyboardHideInternal");

        if (OnTextEndEditAction != null)
            OnTextEndEditAction(currentInputString);

        if (m_TextBox != null)
        {            
            m_TextBox.rectTransform.anchoredPosition = new Vector2(0, -100);
        }
    }

    public bool IsKeyboardVisible()
    {
        AndroidJavaObject input = new AndroidJavaObject("com.rg.nativekeyboard.NativeKeyboardPlugin");
        int height = input.Call<int>("isSoftKeyboardShown");        
        bool isVisible = (height > 100) ? true : false;
        if (isVisible == true && NativeKeyboardHeight != height)
        {
            NativeKeyboardHeight = height;
            KeyboardShowInternal();
        }
        Log.Write("IsKeyboardVisible : {0}", isVisible);
        return isVisible;
    }

    public int GetKeyboardHeight()
    {
        AndroidJavaObject input = new AndroidJavaObject("com.rg.nativekeyboard.NativeKeyboardPlugin");
        int height = input.Call<int>("GetKeyboardHeight");
        Log.Write("GetKeyboardHeight : {0}", height);
        return height;
    }

    public float GetResolutionRatio()
    {
        float defaultWidth = 1280;
        //float defaultHeight = 720;
        return SystemEntry.OriginalScreenSize.x / defaultWidth;
    }

    private void OnSoftkeyHeight(string str)
    {
        int softHeight = 0;
        int.TryParse(str, out softHeight);

        SoftManuHeight = softHeight;
        Log.Write("SoftkeyHeight : {0}", softHeight);
    }
}
