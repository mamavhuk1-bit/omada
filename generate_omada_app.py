import os

def create_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")

# 1. GitHub Actions Workflow
workflow_yml = """
name: Build Omada Android APK

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      - name: Build Debug APK
        run: gradle assembleDebug
      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: Omada-Controller-Pro-APK
          path: app/build/outputs/apk/debug/app-debug.apk
"""
create_file(".github/workflows/build.yml", workflow_yml)

# 2. Gradle Files
settings_gradle = """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "OmadaPro"
include ':app'
"""
create_file("settings.gradle", settings_gradle)

root_build_gradle = """
plugins {
    id 'com.android.application' version '8.2.2' apply false
}
"""
create_file("build.gradle", root_build_gradle)

gradle_properties = """
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
"""
create_file("gradle.properties", gradle_properties)

app_build_gradle = """
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.omada.pro'
    compileSdk 34

    defaultConfig {
        applicationId "com.omada.pro"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
}
"""
create_file("app/build.gradle", app_build_gradle)

# 3. AndroidManifest.xml
manifest_xml = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher_omada"
        android:roundIcon="@drawable/ic_launcher_omada"
        android:label="Omada ER605 Pro"
        android:usesCleartextTraffic="true"
        android:theme="@style/Theme.AppCompat.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""
create_file("app/src/main/AndroidManifest.xml", manifest_xml)

# 4. Custom Vector App Logo
logo_xml = """
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#006A5B"
        android:pathData="M0,0h108v108h-108z"/>
    <path
        android:fillColor="#00A88F"
        android:pathData="M54,18C34.1,18 18,34.1 18,54s16.1,36 36,36 36,-16.1 36,-36S73.9,18 54,18zM54,80c-14.4,0 -26,-11.6 -26,-26s11.6,-26 26,-26 26,11.6 26,26S68.4,80 54,80z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M54,36c-9.9,0 -18,8.1 -18,18s8.1,18 18,18 18,-8.1 18,-18S63.9,36 54,36zM54,62c-4.4,0 -8,-3.6 -8,-8s3.6,-8 8,-8 8,3.6 8,8S58.4,62 54,62z"/>
</vector>
"""
create_file("app/src/main/res/drawable/ic_launcher_omada.xml", logo_xml)

# 5. Main Layout
activity_main_xml = """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/rootLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#0F172A">

    <LinearLayout
        android:id="@+id/topBar"
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:background="#006A5B"
        android:gravity="center_vertical"
        android:paddingHorizontal="16dp"
        android:orientation="horizontal">

        <ImageView
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:src="@drawable/ic_launcher_omada" />

        <TextView
            android:id="@+id/tvAppTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="12dp"
            android:text="OMADA ER605"
            android:textColor="#FFFFFF"
            android:textSize="18sp"
            android:textStyle="bold" />

        <Button
            android:id="@+id/btnTheme"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:text="Theme: Teal"
            android:textSize="11sp"
            android:textColor="#FFFFFF"
            android:backgroundTint="#1E293B" />

        <Button
            android:id="@+id/btnToggleWeb"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:layout_marginStart="6dp"
            android:text="Login Web"
            android:textSize="11sp"
            android:textColor="#FFFFFF"
            android:backgroundTint="#0F172A" />
    </LinearLayout>

    <LinearLayout
        android:id="@+id/ipBar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="10dp"
        android:orientation="horizontal"
        android:background="#1E293B">

        <EditText
            android:id="@+id/etRouterIp"
            android:layout_width="0dp"
            android:layout_height="42dp"
            android:layout_weight="1"
            android:hint="Router IP (e.g. 192.168.0.1)"
            android:text="http://192.168.0.1"
            android:textColor="#FFFFFF"
            android:textColorHint="#94A3B8"
            android:paddingHorizontal="12dp"
            android:background="#0F172A"
            android:textSize="14sp"
            android:singleLine="true" />

        <Button
            android:id="@+id/btnConnect"
            android:layout_width="wrap_content"
            android:layout_height="42dp"
            android:layout_marginStart="8dp"
            android:text="CONNECT"
            android:textStyle="bold"
            android:backgroundTint="#00A88F"
            android:textColor="#FFFFFF" />
    </LinearLayout>

    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
            android:id="@+id/swipeRefresh"
            android:layout_width="match_parent"
            android:layout_height="match_parent">

            <ScrollView
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:padding="14dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical">

                    <TextView
                        android:id="@+id/tvSyncStatus"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Status: Waiting for Login..."
                        android:textColor="#38BDF8"
                        android:textSize="13sp"
                        android:layout_marginBottom="12dp" />

                    <androidx.cardview.widget.CardView
                        android:id="@+id/cardResource"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginBottom="12dp"
                        app:cardCornerRadius="14dp"
                        app:cardBackgroundColor="#1E293B">

                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="vertical"
                            android:padding="16dp">

                            <TextView
                                android:id="@+id/lblResource"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="SYSTEM RESOURCES"
                                android:textColor="#00A88F"
                                android:textStyle="bold"
                                android:textSize="12sp" />

                            <LinearLayout
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:layout_marginTop="12dp"
                                android:orientation="horizontal">

                                <LinearLayout
                                    android:layout_width="0dp"
                                    android:layout_height="wrap_content"
                                    android:layout_weight="1"
                                    android:orientation="vertical">
                                    <TextView
                                        android:id="@+id/tvCpuVal"
                                        android:layout_width="wrap_content"
                                        android:layout_height="wrap_content"
                                        android:text="--%"
                                        android:textSize="28sp"
                                        android:textStyle="bold"
                                        android:textColor="#FFFFFF" />
                                    <TextView
                                        android:id="@+id/lblCpu"
                                        android:layout_width="wrap_content"
                                        android:layout_height="wrap_content"
                                        android:text="CPU Load (4-Core)"
                                        android:textColor="#94A3B8"
                                        android:textSize="12sp" />
                                </LinearLayout>

                                <LinearLayout
                                    android:layout_width="0dp"
                                    android:layout_height="wrap_content"
                                    android:layout_weight="1"
                                    android:orientation="vertical">
                                    <TextView
                                        android:id="@+id/tvMemVal"
                                        android:layout_width="wrap_content"
                                        android:layout_height="wrap_content"
                                        android:text="--%"
                                        android:textSize="28sp"
                                        android:textStyle="bold"
                                        android:textColor="#FFFFFF" />
                                    <TextView
                                        android:id="@+id/lblMem"
                                        android:layout_width="wrap_content"
                                        android:layout_height="wrap_content"
                                        android:text="Memory Usage"
                                        android:textColor="#94A3B8"
                                        android:textSize="12sp" />
                                </LinearLayout>
                            </LinearLayout>
                        </LinearLayout>
                    </androidx.cardview.widget.CardView>

                    <androidx.cardview.widget.CardView
                        android:id="@+id/cardWan"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginBottom="12dp"
                        app:cardCornerRadius="14dp"
                        app:cardBackgroundColor="#1E293B">

                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="vertical"
                            android:padding="16dp">

                            <TextView
                                android:id="@+id/lblWan"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="DUAL-WAN INTERFACES"
                                android:textColor="#00A88F"
                                android:textStyle="bold"
                                android:textSize="12sp" />

                            <TextView
                                android:id="@+id/tvWan1Info"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:layout_marginTop="10dp"
                                android:text="WAN1: Waiting for data..."
                                android:textColor="#FFFFFF"
                                android:textSize="14sp" />

                            <View
                                android:layout_width="match_parent"
                                android:layout_height="1dp"
                                android:layout_marginVertical="10dp"
                                android:background="#334155" />

                            <TextView
                                android:id="@+id/tvWan2Info"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:text="WAN/LAN2: Waiting for data..."
                                android:textColor="#FFFFFF"
                                android:textSize="14sp" />
                        </LinearLayout>
                    </androidx.cardview.widget.CardView>

                    <androidx.cardview.widget.CardView
                        android:id="@+id/cardDevice"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginBottom="16dp"
                        app:cardCornerRadius="14dp"
                        app:cardBackgroundColor="#1E293B">

                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="vertical"
                            android:padding="16dp">

                            <TextView
                                android:id="@+id/lblDevice"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="HARDWARE &amp; UPTIME"
                                android:textColor="#00A88F"
                                android:textStyle="bold"
                                android:textSize="12sp" />

                            <TextView
                                android:id="@+id/tvDeviceInfo"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:layout_marginTop="10dp"
                                android:text="Model: ER605\nFirmware: --\nRunning Time: --"
                                android:textColor="#E2E8F0"
                                android:lineSpacingExtra="4dp"
                                android:textSize="14sp" />
                        </LinearLayout>
                    </androidx.cardview.widget.CardView>

                </LinearLayout>
            </ScrollView>
        </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

        <WebView
            android:id="@+id/webView"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="visible" />

    </FrameLayout>
</LinearLayout>
"""
create_file("app/src/main/res/layout/activity_main.xml", activity_main_xml)

# 6. MainActivity.java
main_activity_java = """
package com.omada.pro;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etRouterIp;
    private TextView tvSyncStatus, tvCpuVal, tvMemVal, tvWan1Info, tvWan2Info, tvDeviceInfo;
    private LinearLayout rootLayout, topBar, ipBar;
    private CardView cardResource, cardWan, cardDevice;
    private Button btnTheme, btnToggleWeb;
    private int currentTheme = 0;
    private boolean isWebVisible = true;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());

    private final String[] themeNames = {
        "Theme: Teal", "Theme: iOS Glass", "Theme: Matrix", "Theme: WinXP", "Theme: Neon"
    };

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        topBar = findViewById(R.id.topBar);
        ipBar = findViewById(R.id.ipBar);
        cardResource = findViewById(R.id.cardResource);
        cardWan = findViewById(R.id.cardWan);
        cardDevice = findViewById(R.id.cardDevice);

        webView = findViewById(R.id.webView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        etRouterIp = findViewById(R.id.etRouterIp);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);
        tvCpuVal = findViewById(R.id.tvCpuVal);
        tvMemVal = findViewById(R.id.tvMemVal);
        tvWan1Info = findViewById(R.id.tvWan1Info);
        tvWan2Info = findViewById(R.id.tvWan2Info);
        tvDeviceInfo = findViewById(R.id.tvDeviceInfo);

        btnTheme = findViewById(R.id.btnTheme);
        btnToggleWeb = findViewById(R.id.btnToggleWeb);
        Button btnConnect = findViewById(R.id.btnConnect);

        SharedPreferences prefs = getSharedPreferences("OmadaPrefs", MODE_PRIVATE);
        etRouterIp.setText(prefs.getString("router_ip", "http://192.168.0.1"));
        currentTheme = prefs.getInt("theme_idx", 0);
        applyTheme(currentTheme);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        webView.addJavascriptInterface(new OmadaBridge(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                startScraperLoop();
            }
        });

        btnConnect.setOnClickListener(v -> {
            String ip = etRouterIp.getText().toString().trim();
            if (!ip.startsWith("http")) ip = "http://" + ip;
            prefs.edit().putString("router_ip", ip).apply();
            showWeb(true);
            webView.loadUrl(ip);
        });

        btnToggleWeb.setOnClickListener(v -> showWeb(!isWebVisible));

        btnTheme.setOnClickListener(v -> {
            currentTheme = (currentTheme + 1) % 5;
            prefs.edit().putInt("theme_idx", currentTheme).apply();
            applyTheme(currentTheme);
        });

        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
            new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefresh.setRefreshing(false), 1200);
        });

        webView.loadUrl(etRouterIp.getText().toString());
    }

    private void showWeb(boolean show) {
        isWebVisible = show;
        webView.setVisibility(show ? View.VISIBLE : View.GONE);
        btnToggleWeb.setText(show ? "Dashboard" : "Login Web");
    }

    private void startScraperLoop() {
        pollHandler.removeCallbacksAndMessages(null);
        Runnable scrapeTask = new Runnable() {
            @Override
            public void run() {
                String js = "(function() {" +
                        "  var bodyText = document.body ? document.body.innerText : '';" +
                        "  if (bodyText.indexOf('Hardware Version') !== -1 || bodyText.indexOf('WAN1') !== -1) {" +
                        "    var data = {};" +
                        "    var hwMatch = bodyText.match(/Hardware Version\\s+([^\\n]+)/);" +
                        "    var fwMatch = bodyText.match(/Firmware Version\\s+([^\\n]+)/);" +
                        "    var upMatch = bodyText.match(/Running Time\\s+([^\\n]+)/);" +
                        "    data.hw = hwMatch ? hwMatch[1].trim() : 'ER605';" +
                        "    data.fw = fwMatch ? fwMatch[1].trim() : '--';" +
                        "    data.uptime = upMatch ? upMatch[1].trim() : '--';" +
                        "    var pctMatches = bodyText.match(/(\\d+%)/g);" +
                        "    data.mem = (pctMatches && pctMatches.length > 0) ? pctMatches[0] : '--%';" +
                        "    data.cpu = (pctMatches && pctMatches.length > 1) ? pctMatches[1] : '--%';" +
                        "    var rows = document.querySelectorAll('tr');" +
                        "    data.wan1 = 'Not Detected'; data.wan2 = 'Not Detected';" +
                        "    rows.forEach(function(r) {" +
                        "      var t = r.innerText.replace(/\\s+/g, ' ').trim();" +
                        "      if (t.startsWith('WAN1 ')) data.wan1 = t;" +
                        "      if (t.startsWith('WAN/LAN2 ') && t.indexOf('PPPoE') !== -1) data.wan2 = t;" +
                        "    });" +
                        "    AndroidBridge.onDataScraped(JSON.stringify(data));" +
                        "  }" +
                        "})();";
                webView.evaluateJavascript(js, null);
                pollHandler.postDelayed(this, 3000);
            }
        };
        pollHandler.postDelayed(scrapeTask, 1500);
    }

    public class OmadaBridge {
        @JavascriptInterface
        public void onDataScraped(String jsonStr) {
            runOnUiThread(() -> {
                try {
                    JSONObject obj = new JSONObject(jsonStr);
                    if (isWebVisible) {
                        showWeb(false);
                    }
                    tvSyncStatus.setText("● Live Synced with Omada Gateway");
                    tvCpuVal.setText(obj.optString("cpu", "--%"));
                    tvMemVal.setText(obj.optString("mem", "--%"));
                    tvWan1Info.setText("🟢 " + obj.optString("wan1"));
                    tvWan2Info.setText("🔵 " + obj.optString("wan2"));
                    tvDeviceInfo.setText(
                        "Hardware: " + obj.optString("hw") + "\\n" +
                        "Firmware: " + obj.optString("fw") + "\\n" +
                        "Uptime: " + obj.optString("uptime")
                    );
                } catch (Exception ignored) {}
            });
        }
    }

    private void applyTheme(int idx) {
        btnTheme.setText(themeNames[idx]);
        switch (idx) {
            case 0:
                rootLayout.setBackgroundColor(Color.parseColor("#0F172A"));
                topBar.setBackgroundColor(Color.parseColor("#006A5B"));
                ipBar.setBackgroundColor(Color.parseColor("#1E293B"));
                setCardsColor("#1E293B", "#FFFFFF", "#00A88F");
                break;
            case 1:
                rootLayout.setBackgroundColor(Color.parseColor("#000000"));
                topBar.setBackgroundColor(Color.parseColor("#1C1C1E"));
                ipBar.setBackgroundColor(Color.parseColor("#2C2C2E"));
                setCardsColor("#1C1C1E", "#FFFFFF", "#0A84FF");
                break;
            case 2:
                rootLayout.setBackgroundColor(Color.parseColor("#000000"));
                topBar.setBackgroundColor(Color.parseColor("#002200"));
                ipBar.setBackgroundColor(Color.parseColor("#001100"));
                setCardsColor("#051A05", "#00FF41", "#00FF41");
                break;
            case 3:
                rootLayout.setBackgroundColor(Color.parseColor("#ECE9D8"));
                topBar.setBackgroundColor(Color.parseColor("#0054E3"));
                ipBar.setBackgroundColor(Color.parseColor("#D6DFF7"));
                setCardsColor("#FFFFFF", "#000000", "#0054E3");
                break;
            case 4:
                rootLayout.setBackgroundColor(Color.parseColor("#0D0221"));
                topBar.setBackgroundColor(Color.parseColor("#261447"));
                ipBar.setBackgroundColor(Color.parseColor("#190933"));
                setCardsColor("#1D1135", "#FF2A85", "#05D9E8");
                break;
        }
    }

    private void setCardsColor(String cardBg, String textCol, String accentCol) {
        int cBg = Color.parseColor(cardBg);
        int tCol = Color.parseColor(textCol);
        int aCol = Color.parseColor(accentCol);
        cardResource.setCardBackgroundColor(cBg);
        cardWan.setCardBackgroundColor(cBg);
        cardDevice.setCardBackgroundColor(cBg);
        tvCpuVal.setTextColor(tCol);
        tvMemVal.setTextColor(tCol);
        tvWan1Info.setTextColor(tCol);
        tvWan2Info.setTextColor(tCol);
        tvDeviceInfo.setTextColor(tCol);
        tvSyncStatus.setTextColor(aCol);
    }
}
"""
create_file("app/src/main/java/com/omada/pro/MainActivity.java", main_activity_java)

print("ALL_FILES_READY")
