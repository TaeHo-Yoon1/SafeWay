package com.example.safeway;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * “다음 우편번호 검색” WebView 화면.
 * Daum(카카오)에서 제공하는 Postcode JavaScript를 로드한 뒤,
 * 사용자가 주소를 선택하면 WebAppInterface.postAddress(...)가 호출됩니다.
 */
public class WebViewPostcodeActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_webview_postcode);
        webView = findViewById(R.id.webview_postcode);

        // 1) WebSettings 설정
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);           // JavaScript 사용 허용
        ws.setDomStorageEnabled(true);           // 로컬 스토리지 허용 (필요 시)

        // 2) WebViewClient 설정: 내부에서 페이지 로드
        webView.setWebViewClient(new WebViewClient());

        // 3) JavaScript 인터페이스 등록: window.Android 로 접근
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        // 4) 로컬 HTML 로드
        //    → assets/postcode.html 파일을 내려받아서 로컬 웹페이지로 사용
        webView.loadUrl("file:///android_asset/postcode.html");
    }
}
