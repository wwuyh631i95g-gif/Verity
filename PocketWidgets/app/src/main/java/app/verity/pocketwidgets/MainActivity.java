package app.verity.pocketwidgets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public final class MainActivity extends Activity {
    private WebView webView;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF0B0D10);
        getWindow().setNavigationBarColor(0xFF0B0D10);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF0B0D10);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setSupportZoom(false);
        webView.addJavascriptInterface(new Bridge(this), "Android");
        webView.setWebViewClient(new WebViewClient());
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    public static final class Bridge {
        private final Context context;
        Bridge(Context context) { this.context = context.getApplicationContext(); }

        @JavascriptInterface public void setCounter(int value) {
            context.getSharedPreferences("pocket_widgets", Context.MODE_PRIVATE).edit()
                    .putInt("counter_67", Math.max(0, value)).apply();
            WidgetActionReceiver.refresh(context);
        }

        @JavascriptInterface public void openSettings(String page) {
            String action;
            switch (page) {
                case "wifi": action = Settings.ACTION_WIFI_SETTINGS; break;
                case "bluetooth": action = Settings.ACTION_BLUETOOTH_SETTINGS; break;
                case "display": action = Settings.ACTION_DISPLAY_SETTINGS; break;
                case "sound": action = Settings.ACTION_SOUND_SETTINGS; break;
                case "notifications": action = Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS; break;
                default: action = Settings.ACTION_SETTINGS;
            }
            try { context.startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (Exception ignored) { }
        }
    }
}
