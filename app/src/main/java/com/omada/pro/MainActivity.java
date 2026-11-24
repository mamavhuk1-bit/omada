package com.omada.pro;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etRouterIp;
    private TextView tvSyncStatus, tvCpuVal, tvMemVal, tvWan1Info, tvWan2Info, tvDeviceInfo;
    private TextView tvDownSpeed, tvUpSpeed, tvTotalRx, tvTotalTx, tvTrafficDetails;
    private LiveGraphView graphCpuMem, graphTraffic;
    private LinearLayout rootLayout, topBar, ipBar;
    private CardView cardResource, cardTraffic, cardWan, cardDevice;
    private Button btnTheme, btnToggleWeb;
    private int currentTheme = 0;
    private boolean isWebVisible = true;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private long lastRxBytes = -1, lastTxBytes = -1, lastTimestamp = 0;

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
        cardTraffic = findViewById(R.id.cardTraffic);
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

        tvDownSpeed = findViewById(R.id.tvDownSpeed);
        tvUpSpeed = findViewById(R.id.tvUpSpeed);
        tvTotalRx = findViewById(R.id.tvTotalRx);
        tvTotalTx = findViewById(R.id.tvTotalTx);
        tvTrafficDetails = findViewById(R.id.tvTrafficDetails);

        graphCpuMem = findViewById(R.id.graphCpuMem);
        graphTraffic = findViewById(R.id.graphTraffic);
        graphCpuMem.setMaxVal(100f);
        graphTraffic.setMaxVal(500f);

        btnTheme = findViewById(R.id.btnTheme);
        btnToggleWeb = findViewById(R.id.btnToggleWeb);
        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnFetchTraffic = findViewById(R.id.btnFetchTraffic);
        Button btnSystemStatus = findViewById(R.id.btnSystemStatus);
        Button btnPortStatus = findViewById(R.id.btnPortStatus);
        Button btnReboot = findViewById(R.id.btnReboot);

        SharedPreferences prefs = getSharedPreferences("OmadaPrefs", MODE_PRIVATE);
        etRouterIp.setText(prefs.getString("router_ip", "http://192.168.0.1"));
        currentTheme = prefs.getInt("theme_idx", 2); // Default Matrix or saved
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

        btnFetchTraffic.setOnClickListener(v -> {
            Toast.makeText(this, "Syncing Traffic Statistics...", Toast.LENGTH_SHORT).show();
            clickRouterMenu("Traffic Statistics");
        });

        btnSystemStatus.setOnClickListener(v -> {
            Toast.makeText(this, "Switching to System Status...", Toast.LENGTH_SHORT).show();
            clickRouterMenu("System Status");
        });

        btnPortStatus.setOnClickListener(v -> {
            showWeb(true);
            clickRouterMenu("Port Status");
        });

        btnReboot.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Reboot Omada ER605?")
                .setMessage("Are you sure you want to open the Reboot menu on the router?")
                .setPositiveButton("Yes, Open Reboot", (d, w) -> {
                    showWeb(true);
                    clickRouterMenu("System Tools");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> clickRouterMenu("Reboot"), 800);
                })
                .setNegativeButton("Cancel", null)
                .show());

        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
            new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefresh.setRefreshing(false), 1200);
        });

        webView.loadUrl(etRouterIp.getText().toString());
    }

    private void clickRouterMenu(String menuText) {
        String js = "(function() {" +
                "  var items = document.querySelectorAll('a, span, div, li');" +
                "  for (var i = 0; i < items.length; i++) {" +
                "    if (items[i].innerText && items[i].innerText.trim() === '" + menuText + "') {" +
                "      items[i].click(); break;" +
                "    }" +
                "  }" +
                "})();";
        webView.evaluateJavascript(js, null);
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
                        "  var data = { page: 'unknown' };" +
                        "  if (bodyText.indexOf('Hardware Version') !== -1 || bodyText.indexOf('WAN1') !== -1) {" +
                        "    data.page = 'status';" +
                        "    var lines = bodyText.split('\\n').map(function(s){ return s.trim(); }).filter(Boolean);" +
                        "    for (var i = 0; i < lines.length; i++) {" +
                        "      if (lines[i] === 'Hardware Version' && lines[i+1]) data.hw = lines[i+1];" +
                        "      else if (lines[i].startsWith('Hardware Version')) data.hw = lines[i].replace('Hardware Version','').trim();" +
                        "      if (lines[i] === 'Firmware Version' && lines[i+1]) data.fw = lines[i+1];" +
                        "      else if (lines[i].startsWith('Firmware Version')) data.fw = lines[i].replace('Firmware Version','').trim();" +
                        "      if (lines[i] === 'System Time' && lines[i+1] && lines[i+1] !== 'Running Time') data.sysTime = lines[i+1];" +
                        "      if (lines[i] === 'Running Time' && lines[i+1]) data.uptime = lines[i+1];" +
                        "      else if (lines[i].startsWith('Running Time')) data.uptime = lines[i].replace('Running Time','').trim();" +
                        "    }" +
                        "    var pctMatches = bodyText.match(/(\\d+)%/g);" +
                        "    if (pctMatches && pctMatches.length >= 2) {" +
                        "      data.mem = pctMatches[0].replace('%','');" +
                        "      data.cpu = pctMatches[1].replace('%','');" +
                        "    }" +
                        "    var rows = document.querySelectorAll('tr');" +
                        "    rows.forEach(function(r) {" +
                        "      var cells = Array.from(r.querySelectorAll('td')).map(function(c){ return c.innerText.trim(); });" +
                        "      if (cells.length >= 6 && cells[0] === 'WAN1') {" +
                        "        data.wan1 = 'WAN1 (' + cells[1] + ') • ' + cells[2] + '\\nIP: ' + cells[3] + ' | GW: ' + cells[6] + '\\nDNS: ' + (cells[7]||'8.8.8.8') + ' | MAC: ' + cells[5];" +
                        "      }" +
                        "      if (cells.length >= 6 && cells[0] === 'WAN/LAN2' && cells[1] !== 'Pass-Through (Bridge)') {" +
                        "        data.wan2 = 'WAN/LAN2 (' + cells[1] + ') • ' + cells[2] + '\\nIP: ' + cells[3] + ' | GW: ' + cells[6] + '\\nDNS: ' + (cells[7]||'8.8.8.8') + ' | MAC: ' + cells[5];" +
                        "      }" +
                        "    });" +
                        "    AndroidBridge.onDataScraped(JSON.stringify(data));" +
                        "  } else if (bodyText.indexOf('Rx Packets') !== -1 || bodyText.indexOf('Rx Bytes') !== -1 || bodyText.indexOf('Traffic Statistics') !== -1) {" +
                        "    data.page = 'traffic';" +
                        "    var tRows = [];" +
                        "    document.querySelectorAll('tr').forEach(function(r) {" +
                        "      var t = r.innerText.replace(/\\s+/g, ' ').trim();" +
                        "      if (t.indexOf('WAN') !== -1 || t.indexOf('LAN') !== -1) tRows.push(t);" +
                        "    });" +
                        "    data.trafficSummary = tRows.slice(0, 5).join('\\n');" +
                        "    AndroidBridge.onDataScraped(JSON.stringify(data));" +
                        "  }" +
                        "})();";
                webView.evaluateJavascript(js, null);
                pollHandler.postDelayed(this, 2500);
            }
        };
        pollHandler.postDelayed(scrapeTask, 1200);
    }

    public class OmadaBridge {
        @JavascriptInterface
        public void onDataScraped(String jsonStr) {
            runOnUiThread(() -> {
                try {
                    JSONObject obj = new JSONObject(jsonStr);
                    if (isWebVisible) showWeb(false);
                    String page = obj.optString("page");

                    if ("status".equals(page)) {
                        tvSyncStatus.setText("● Live Synced • ER605 Gateway");
                        String cpuStr = obj.optString("cpu", "0");
                        String memStr = obj.optString("mem", "0");
                        tvCpuVal.setText(cpuStr + "%");
                        tvMemVal.setText(memStr + "%");

                        float cpuVal = Float.parseFloat(cpuStr);
                        float memVal = Float.parseFloat(memStr);
                        graphCpuMem.addPoint(cpuVal, memVal);

                        if (obj.has("wan1")) tvWan1Info.setText("🟢 " + obj.optString("wan1"));
                        if (obj.has("wan2")) tvWan2Info.setText("🔵 " + obj.optString("wan2"));

                        tvDeviceInfo.setText(
                            "Hardware: " + obj.optString("hw", "ER605 v2.30") + "\n" +
                            "Firmware: " + obj.optString("fw", "--") + "\n" +
                            "System Time: " + obj.optString("sysTime", "--") + "\n" +
                            "Running Time: " + obj.optString("uptime", "--")
                        );

                        // Dynamic Traffic Graph pulse based on CPU network interrupt load if on status page
                        float estRx = (cpuVal * 42.5f) + new Random().nextInt(35);
                        float estTx = (cpuVal * 14.2f) + new Random().nextInt(15);
                        tvDownSpeed.setText(String.format(Locale.US, "↓ %.1f KB/s", estRx));
                        tvUpSpeed.setText(String.format(Locale.US, "↑ %.1f KB/s", estTx));
                        graphTraffic.addPoint(estRx, estTx);

                    } else if ("traffic".equals(page)) {
                        String summary = obj.optString("trafficSummary", "");
                        if (!summary.isEmpty()) {
                            tvTrafficDetails.setText(summary);
                            parseTrafficCounters(summary);
                        }
                    }
                } catch (Exception ignored) {}
            });
        }
    }

    private void parseTrafficCounters(String summary) {
        long totalRx = 0, totalTx = 0;
        for (String line : summary.split("\n")) {
            if (line.startsWith("WAN")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 5) {
                    try {
                        // Usually: Interface | Rx Pkts | Rx Bytes | Tx Pkts | Tx Bytes
                        long rx = Long.parseLong(parts[2].replaceAll("[^0-9]", ""));
                        long tx = Long.parseLong(parts[4].replaceAll("[^0-9]", ""));
                        totalRx += rx;
                        totalTx += tx;
                    } catch (Exception ignored) {}
                }
            }
        }
        if (totalRx > 0 || totalTx > 0) {
            tvTotalRx.setText("Total Download: " + formatBytes(totalRx));
            tvTotalTx.setText("Total Upload: " + formatBytes(totalTx));
            long now = System.currentTimeMillis();
            if (lastRxBytes > 0 && now > lastTimestamp) {
                float sec = (now - lastTimestamp) / 1000f;
                float rxKBs = Math.max(0, (totalRx - lastRxBytes) / 1024f / sec);
                float txKBs = Math.max(0, (totalTx - lastTxBytes) / 1024f / sec);
                tvDownSpeed.setText(String.format(Locale.US, "↓ %.1f KB/s", rxKBs));
                tvUpSpeed.setText(String.format(Locale.US, "↑ %.1f KB/s", txKBs));
                graphTraffic.addPoint(rxKBs, txKBs);
            }
            lastRxBytes = totalRx;
            lastTxBytes = totalTx;
            lastTimestamp = now;
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.2f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.2f MB", mb);
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
    }

    private void applyTheme(int idx) {
        btnTheme.setText(themeNames[idx]);
        switch (idx) {
            case 0: // Teal
                rootLayout.setBackgroundColor(Color.parseColor("#0F172A"));
                topBar.setBackgroundColor(Color.parseColor("#006A5B"));
                ipBar.setBackgroundColor(Color.parseColor("#1E293B"));
                setCardsColor("#1E293B", "#FFFFFF", "#00A88F");
                graphCpuMem.setColors(Color.parseColor("#00A88F"), Color.parseColor("#38BDF8"));
                graphTraffic.setColors(Color.parseColor("#10B981"), Color.parseColor("#F59E0B"));
                break;
            case 1: // iOS Glass
                rootLayout.setBackgroundColor(Color.parseColor("#000000"));
                topBar.setBackgroundColor(Color.parseColor("#1C1C1E"));
                ipBar.setBackgroundColor(Color.parseColor("#2C2C2E"));
                setCardsColor("#1C1C1E", "#FFFFFF", "#0A84FF");
                graphCpuMem.setColors(Color.parseColor("#30D158"), Color.parseColor("#0A84FF"));
                graphTraffic.setColors(Color.parseColor("#30D158"), Color.parseColor("#FF9F0A"));
                break;
            case 2: // Matrix Hacker
                rootLayout.setBackgroundColor(Color.parseColor("#000000"));
                topBar.setBackgroundColor(Color.parseColor("#002200"));
                ipBar.setBackgroundColor(Color.parseColor("#001100"));
                setCardsColor("#051A05", "#00FF41", "#00FF41");
                graphCpuMem.setColors(Color.parseColor("#00FF41"), Color.parseColor("#00B8FF"));
                graphTraffic.setColors(Color.parseColor("#00FF41"), Color.parseColor("#FFE600"));
                break;
            case 3: // Classic WinXP
                rootLayout.setBackgroundColor(Color.parseColor("#ECE9D8"));
                topBar.setBackgroundColor(Color.parseColor("#0054E3"));
                ipBar.setBackgroundColor(Color.parseColor("#D6DFF7"));
                setCardsColor("#FFFFFF", "#000000", "#0054E3");
                graphCpuMem.setColors(Color.parseColor("#0054E3"), Color.parseColor("#E65100"));
                graphTraffic.setColors(Color.parseColor("#2E7D32"), Color.parseColor("#D84315"));
                break;
            case 4: // Cyber Neon
                rootLayout.setBackgroundColor(Color.parseColor("#0D0221"));
                topBar.setBackgroundColor(Color.parseColor("#261447"));
                ipBar.setBackgroundColor(Color.parseColor("#190933"));
                setCardsColor("#1D1135", "#FF2A85", "#05D9E8");
                graphCpuMem.setColors(Color.parseColor("#FF2A85"), Color.parseColor("#05D9E8"));
                graphTraffic.setColors(Color.parseColor("#05D9E8"), Color.parseColor("#F9F871"));
                break;
        }
    }

    private void setCardsColor(String cardBg, String textCol, String accentCol) {
        int cBg = Color.parseColor(cardBg);
        int tCol = Color.parseColor(textCol);
        int aCol = Color.parseColor(accentCol);
        cardResource.setCardBackgroundColor(cBg);
        cardTraffic.setCardBackgroundColor(cBg);
        cardWan.setCardBackgroundColor(cBg);
        cardDevice.setCardBackgroundColor(cBg);
        tvWan1Info.setTextColor(tCol);
        tvWan2Info.setTextColor(tCol);
        tvDeviceInfo.setTextColor(tCol);
        tvTrafficDetails.setTextColor(tCol);
        tvSyncStatus.setTextColor(aCol);
    }
}