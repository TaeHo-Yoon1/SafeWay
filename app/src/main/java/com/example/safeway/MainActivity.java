package com.example.safeway;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * MainActivity (간략화 버전)
 *  - 상단: 도보, 대중교통, 내비 모드 선택
 *  - EditText: 도착지 주소 입력
 *  - 버튼 클릭 시:
 *      1) 주소 → 위경도 변환 (Geocode)
 *      2) 현재 위치 → 도착지 위치 Route API 호출
 *      3) 경로 굵은 선(PathOverlay)으로 지도에 그리기
 *      4) 예상 소요 시간 TextView에 표시
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    // ▶ 본인의 Naver Cloud Platform “지도/길찾기/장소 검색” API 키로 바꿔주세요!
    private static final String CLIENT_ID     = "3b1s9745hn";
    private static final String CLIENT_SECRET = "LWR0NA25f3TS7GmO5wVjzN7MuODkHy4kS6inttvH";

    // UI 요소
    private Button btnModeWalking, btnModeTransit, btnModeDriving;
    private EditText inputDestination;
    private Button btnSearch;
    private TextView estimatedTime;

    // 네이버 맵 관련
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private LatLng currentLocation;  // 현재 기기 위치가 저장됨

    // 선택된 길찾기 모드 (walking/transit/driving)
    private String selectedMode = "driving";

    // PathOverlay와 Marker를 저장해두는 리스트 (이전 경로를 지우기 위해)
    private final List<PathOverlay> pathOverlays = new ArrayList<>();
    private final List<Marker> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- (A) Naver Map SDK 초기화 (AndroidManifest.xml에 CLIENT_ID meta-data가 반드시 있어야 함) ---
        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NcpKeyClient(CLIENT_ID));

        // --- (B) 뷰 바인딩 ---
        btnModeWalking   = findViewById(R.id.btn_mode_walking);
        btnModeTransit   = findViewById(R.id.btn_mode_transit);
        btnModeDriving   = findViewById(R.id.btn_mode_driving);
        inputDestination = findViewById(R.id.input_destination);
        btnSearch        = findViewById(R.id.btn_search);
        estimatedTime    = findViewById(R.id.estimated_time);

        // --- (C) 모드 선택 버튼 클릭 리스너 ---
        btnModeWalking.setOnClickListener(v -> {
            selectedMode = "walking";
            highlightModeButton();
        });
        btnModeTransit.setOnClickListener(v -> {
            selectedMode = "transit";
            highlightModeButton();
        });
        btnModeDriving.setOnClickListener(v -> {
            selectedMode = "driving";
            highlightModeButton();
        });
        highlightModeButton(); // 초기 “driving” 모드 강조

        // --- (D) “길찾기 시작” 버튼 클릭 리스너 ---
        btnSearch.setOnClickListener(v -> {
            String address = inputDestination.getText().toString().trim();
            if (address.isEmpty()) {
                Toast.makeText(MainActivity.this,
                        "도착지 주소를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentLocation == null) {
                Toast.makeText(MainActivity.this,
                        "현재 위치를 불러오는 중입니다.\n에뮬레이터에서는 Location을 설정하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            // 1) 입력된 문자열을 위경도(위도, 경도)로 변환
            try {
                geocodeAddress(address);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this,
                        "주소 인코딩 오류", Toast.LENGTH_SHORT).show();
            }
        });

        // --- (E) 위치 권한 요청 및 FusedLocationSource 초기화 ---
        locationSource = new FusedLocationSource(this, 1000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    1000
            );
        }

        // --- (F) Naver MapFragment 초기화 ---
        MapFragment mapFragment = (MapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.map_fragment, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }

    // ================================================
    // onMapReady: 지도가 준비되면 호출
    // ================================================
    @Override
    public void onMapReady(@NonNull NaverMap map) {
        this.naverMap = map;

        // 현재 위치 기능 활성화
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        // 위치가 변경될 때마다 currentLocation에 저장
        naverMap.addOnLocationChangeListener(location -> {
            currentLocation = new LatLng(
                    location.getLatitude(),
                    location.getLongitude()
            );
        });
    }

    // ================================================
    // (1) EditText에 입력된 주소를 Geocode API로 변환 → getCoordinatesForAddress()
    // ================================================
    private void geocodeAddress(String address) throws UnsupportedEncodingException {
        String encodedAddress = URLEncoder.encode(address, "UTF-8");
        String url = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode" +
                "?query=" + encodedAddress;
        Log.d("GEOCODE_URL", url);

        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder()
                .url(url)
                .addHeader("X-NCP-APIGW-API-KEY-ID", CLIENT_ID)
                .addHeader("X-NCP-APIGW-API-KEY", CLIENT_SECRET)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e("GEOCODE_ERROR", "Geocode 실패: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                "주소 변환(Geocode) 실패", Toast.LENGTH_SHORT).show()
                );
            }
            @Override public void onResponse(Call call, Response resp) throws IOException {
                if (!resp.isSuccessful()) {
                    Log.e("GEOCODE_ERROR", "HTTP 코드: " + resp.code());
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "주소 변환 응답 에러: " + resp.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
                try {
                    JSONObject root = new JSONObject(resp.body().string());
                    JSONArray results = root.optJSONArray("addresses");
                    if (results == null || results.length() == 0) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this,
                                        "유효한 주소가 아닙니다.", Toast.LENGTH_SHORT).show()
                        );
                        return;
                    }
                    JSONObject first = results.getJSONObject(0);
                    double lat = first.getDouble("y");
                    double lng = first.getDouble("x");

                    LatLng destLocation = new LatLng(lat, lng);

                    runOnUiThread(() -> {
                        // (2) 이제 현재 위치 → destLocation 간 경로 그리기 및 시간 계산
                        drawRoute(currentLocation, destLocation);
                    });
                } catch (JSONException e) {
                    Log.e("GEOCODE_ERROR", "JSON 파싱 에러: " + e.getMessage());
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "주소 변환 파싱 에러", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    // ================================================
    // (2) 현재 위치 → 목적지 좌표로 Route API 호출 → 경로 그리기 + 예상 시간 표시
    // ================================================
    private void drawRoute(LatLng start, LatLng end) {
        // (A) 기존 경로와 마커 삭제
        clearOverlays();

        // (B) 모드별 Endpoint 및 옵션 결정
        String baseUrl, optionParam = "";
        if ("walking".equals(selectedMode)) {
            baseUrl = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/walking";
            optionParam = "&option=mainroad";  // 큰길 우선
        } else if ("transit".equals(selectedMode)) {
            baseUrl = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/transit";
            // 대중교통 기본 옵션 (optionParam 없음)
        } else { // driving
            baseUrl = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving";
            optionParam = "&option=trafast";   // 빠른 길 우선
        }

        String p1 = start.longitude + "," + start.latitude;
        String p2 = end.longitude   + "," + end.latitude;
        String url = baseUrl + "?start=" + p1 + "&goal=" + p2 + optionParam;
        Log.d("ROUTE_URL", url);

        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder()
                .url(url)
                .addHeader("X-NCP-APIGW-API-KEY-ID", CLIENT_ID)
                .addHeader("X-NCP-APIGW-API-KEY", CLIENT_SECRET)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e("ROUTE_ERROR", "길찾기 실패: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                "길찾기 요청 실패", Toast.LENGTH_SHORT).show()
                );
            }
            @Override public void onResponse(Call call, Response resp) throws IOException {
                if (!resp.isSuccessful()) {
                    Log.e("ROUTE_ERROR", "HTTP 코드: " + resp.code());
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "길찾기 응답 에러: " + resp.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
                try {
                    JSONObject root  = new JSONObject(resp.body().string());
                    JSONObject route = root.getJSONObject("route");

                    JSONArray optArray;
                    JSONObject summary;
                    JSONArray pathArray;

                    if ("driving".equals(selectedMode)) {
                        optArray  = route.getJSONArray("traoptimal");
                        summary   = optArray.getJSONObject(0).getJSONObject("summary");
                        pathArray = optArray.getJSONObject(0).getJSONArray("path");
                    } else if ("walking".equals(selectedMode)) {
                        optArray  = route.getJSONArray("paths");
                        summary   = optArray.getJSONObject(0).getJSONObject("summary");
                        pathArray = optArray.getJSONObject(0).getJSONArray("path");
                    } else { // transit
                        optArray = route.optJSONArray("trafast");
                        if (optArray == null || optArray.length() == 0) {
                            optArray = route.getJSONArray("path");
                        }
                        summary   = optArray.getJSONObject(0).getJSONObject("summary");
                        pathArray = optArray.getJSONObject(0).getJSONArray("path");
                    }

                    int durationSec = summary.getInt("duration");
                    int durationMin = durationSec / 60;

                    List<LatLng> coords = new ArrayList<>();
                    for (int i = 0; i < pathArray.length(); i++) {
                        JSONArray point = pathArray.getJSONArray(i);
                        double lng = point.getDouble(0);
                        double lat = point.getDouble(1);
                        coords.add(new LatLng(lat, lng));
                    }

                    runOnUiThread(() -> {
                        // (C) 예상 소요 시간 표시
                        estimatedTime.setText("예상 소요 시간: " + durationMin + "분");

                        // (D) 지도에 굵은 붉은 선(PathOverlay)으로 경로 그리기
                        PathOverlay po = new PathOverlay();
                        po.setCoords(coords);
                        po.setWidth(15);
                        po.setColor(0xAAFF0000);
                        po.setMap(naverMap);
                        pathOverlays.add(po);

                        // (E) 기기 위치와 목적지에 간단한 마커 표시 (선택 사항)
                        Marker mStart = new Marker();
                        mStart.setPosition(start);
                        mStart.setCaptionText("출발");
                        mStart.setMap(naverMap);
                        markers.add(mStart);

                        Marker mEnd = new Marker();
                        mEnd.setPosition(end);
                        mEnd.setCaptionText("도착");
                        mEnd.setMap(naverMap);
                        markers.add(mEnd);
                    });
                } catch (JSONException e) {
                    Log.e("ROUTE_ERROR", "JSON 파싱 에러: " + e.getMessage());
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "길찾기 파싱 에러", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    // ================================================
    // 지도에 그려진 모든 경로 및 마커 제거
    // ================================================
    private void clearOverlays() {
        for (PathOverlay p : pathOverlays) {
            p.setMap(null);
        }
        pathOverlays.clear();

        for (Marker m : markers) {
            m.setMap(null);
        }
        markers.clear();
    }

    // ================================================
    // 모드 버튼 강조 처리 (배경 색깔 단순 변경)
    // ================================================
    private void highlightModeButton() {
        int gray = ContextCompat.getColor(this, android.R.color.darker_gray);
        int blue = ContextCompat.getColor(this, android.R.color.holo_blue_dark);

        btnModeWalking.setBackgroundColor(gray);
        btnModeTransit.setBackgroundColor(gray);
        btnModeDriving.setBackgroundColor(gray);

        switch (selectedMode) {
            case "walking":
                btnModeWalking.setBackgroundColor(blue);
                break;
            case "transit":
                btnModeTransit.setBackgroundColor(blue);
                break;
            default:
                btnModeDriving.setBackgroundColor(blue);
        }
    }

    // ================================================
    // (F) 위치 권한 요청 결과 처리
    // ================================================
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) {
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
