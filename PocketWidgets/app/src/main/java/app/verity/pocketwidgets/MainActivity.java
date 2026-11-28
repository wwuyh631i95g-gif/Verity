package app.verity.pocketwidgets;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private WebView web;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF090B12);
        getWindow().setNavigationBarColor(0xFF090B12);
        web = new WebView(this);
        web.setBackgroundColor(0xFF090B12);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new PhoneBridge(), "Android");
        web.setWebChromeClient(new WebChromeClient());
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    private final class PhoneBridge {
        @JavascriptInterface public void openSettings(String key) {
            runOnUiThread(() -> {
                Intent i;
                switch (key == null ? "" : key) {
                    case "wifi": i = new Intent(Settings.ACTION_WIFI_SETTINGS); break;
                    case "bluetooth": i = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS); break;
                    case "display": i = new Intent(Settings.ACTION_DISPLAY_SETTINGS); break;
                    case "sound": i = new Intent(Settings.ACTION_SOUND_SETTINGS); break;
                    case "notifications": i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()); break;
                    default: Toast.makeText(MainActivity.this, "Раздел недоступен", Toast.LENGTH_SHORT).show(); return;
                }
                try { startActivity(i); } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (web != null) { web.removeJavascriptInterface("Android"); web.destroy(); web = null; }
        super.onDestroy();
    }
}