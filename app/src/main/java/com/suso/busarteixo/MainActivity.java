package com.suso.busarteixo;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bus Arteixo: la pantalla es una página HTML (assets/index.html).
 * Java solo hace de puente: peticiones a internet (sin las limitaciones del
 * navegador), guardar ajustes, copiar al portapapeles y avisos.
 */
public class MainActivity extends Activity {

    private WebView web;
    private SharedPreferences prefs;
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("busarteixo", MODE_PRIVATE);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setTextZoom(100); // el tamaño de letra lo controla la app
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.evaluateJavascript("window.onAppResume && window.onAppResume()", null);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.onBack ? window.onBack() : false", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (!"true".equals(value)) finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        pool.shutdownNow();
        super.onDestroy();
    }

    private void callback(final String id, final int status, final String text) {
        final String js = "window.__httpCb(" + JSONObject.quote(id) + "," + status + ","
                + JSONObject.quote(text == null ? "" : text) + ")";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (web != null) web.evaluateJavascript(js, null);
            }
        });
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toString("UTF-8");
    }

    public class Bridge {

        /** Petición HTTP en segundo plano. Responde llamando a window.__httpCb(id, estado, texto). */
        @JavascriptInterface
        public void http(final String id, final String method, final String url, final String body,
                         final String contentType, final String soapAction) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    HttpURLConnection c = null;
                    try {
                        c = (HttpURLConnection) new URL(url).openConnection();
                        c.setConnectTimeout(15000);
                        c.setReadTimeout(25000);
                        c.setRequestMethod(method);
                        c.setRequestProperty("Accept", "application/json, text/xml, */*");
                        c.setRequestProperty("Accept-Language", "es-ES,es;q=0.9,gl;q=0.8");
                        c.setRequestProperty("User-Agent", "BusArteixo/0.1 (Android)");
                        if (soapAction != null && soapAction.length() > 0) {
                            c.setRequestProperty("soapaction", soapAction);
                        }
                        if (body != null && body.length() > 0) {
                            c.setDoOutput(true);
                            c.setRequestProperty("Content-Type",
                                    contentType == null || contentType.length() == 0 ? "application/json" : contentType);
                            byte[] data = body.getBytes("UTF-8");
                            OutputStream os = c.getOutputStream();
                            os.write(data);
                            os.close();
                        }
                        int code = c.getResponseCode();
                        String text = readAll(code >= 400 ? c.getErrorStream() : c.getInputStream());
                        callback(id, code, text);
                    } catch (Throwable e) {
                        callback(id, -1, e.toString());
                    } finally {
                        if (c != null) c.disconnect();
                    }
                }
            });
        }

        @JavascriptInterface
        public String load(String key) {
            return prefs.getString(key, "");
        }

        @JavascriptInterface
        public void save(String key, String value) {
            prefs.edit().putString(key, value).apply();
        }

        @JavascriptInterface
        public void copy(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Bus Arteixo", text));
                    Toast.makeText(MainActivity.this, "Copiado", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void toast(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
                }
            });
        }

        @JavascriptInterface
        public String version() {
            return "0.1";
        }
    }
}
