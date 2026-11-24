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
                        "Hardware: " + obj.optString("hw") + "\n" +
                        "Firmware: " + obj.optString("fw") + "\n" +
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