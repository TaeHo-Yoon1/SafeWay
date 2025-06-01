package com.example.safeway;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

/**
 * WebView 안의 JavaScript → Android Native 코드 호출을 위한 인터페이스
 */
public class WebAppInterface {

    private final Activity activity;

    public WebAppInterface(Activity activity) {
        this.activity = activity;
    }

    /**
     * JavaScript에서 window.Android.postAddress(...) 형태로 호출됩니다.
     * 이 메서드가 호출되면, MainActivity 로 선택된 주소를 리턴합니다.
     *
     * @param data JSON 문자열 → JavaScript에서 호출 시 “{zip:'우편번호', addr:'주소'}” 형태로 전달
     */
    @JavascriptInterface
    public void postAddress(String data) {
        // data 예시: {"zonecode":"135-804", "address":"서울특별시 강남구 ..."}
        // Android 5.0 이상에서는 runOnUiThread 없이도 Toast 가능하지만,
        // 혹시 UI 작업이 필요하면 runOnUiThread 사용 권장
        activity.runOnUiThread(() -> {
            Toast.makeText(activity, "선택된 주소: " + data, Toast.LENGTH_SHORT).show();

            // MainActivity에게 주소 결과를 리턴
            Intent resultIntent = new Intent();
            resultIntent.putExtra("postData", data);
            activity.setResult(Activity.RESULT_OK, resultIntent);
            activity.finish(); // WebViewPostcodeActivity 종료
        });
    }
}
