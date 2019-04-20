/*
 * Copyright (C) 2017-2019 SpiritCroc
 * Email: spiritcroc@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.spiritcroc.remotepurchaselist;

import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaderFactory;
import com.bumptech.glide.load.model.LazyHeaders;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import at.bitfire.cert4android.CustomCertManager;

public class ServerCommunicator {

    private static final String TAG = ServerCommunicator.class.getSimpleName();

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String MULTIPART_BOUNDARY = "*****";
    private static final String CRLF = "\r\n";
    private static final String TWO_HYPHENS = "--";

    private static AtomicBoolean mHttpsSetup = new AtomicBoolean(false);
    private static volatile boolean mHttpsInitialized = false;
    // Lock to restrict access to mHttpsInitialized and mHttpsSetupFinishListeners
    private static Lock mHttpsSetupLock = new ReentrantLock();
    private static volatile ArrayList<OnHttpsSetupFinishListener> mHttpsSetupFinishListeners =
            new ArrayList<>();

    private ServerCommunicator() {}

    public interface OnHttpsSetupFinishListener {
        void onHttpsReady();
    }

    public interface OnFileUploadListener {
        void onAttemptFileUpload(File file);
        void onUploadDone();
    }

    /**
     * Sets up the app-wide HTTPS communication to use cert4android's CustomCertManager.
     * This is not required for basic purchase list interaction since we use CustomCertManager
     * directly there, but we need it for the Glide library.
     * We could add a Glide module to use a different TrustManager, but this part of the Glide
     * library needs to operate in the UI thread, while the CustomCertManager creation must not
     * run in UI thread.
     * As a workaround, we set up global HTTPS usage from a background thread for CustomCertManager
     * and delay any Glide usage until we are ready for that.
     * If we do not delay Glide usage, Glide is initialized with unmodified defaults and
     * CustomCertManager won't have any effect on Glide usage.
     * @param listener
     * Listener to https setup finish, only used when setup is not already done or cannot run in
     * current thread. Duplicates will be ignored
     * @return
     * True if https is now set up, false if it will be set up in another thread and optionally
     * listener will be notified when done.
     */
    public static boolean setupHttps(Context context, final OnHttpsSetupFinishListener listener) {
        final Context appContext = context.getApplicationContext();
        if (!mHttpsSetup.compareAndSet(false, true)) {
            // Already setup running
            boolean setupFinished;
            try {
                mHttpsSetupLock.lock();
                if (mHttpsInitialized) {
                    setupFinished = true;
                } else {
                    setupFinished = false;
                    if (!mHttpsSetupFinishListeners.contains(listener)) {
                        mHttpsSetupFinishListeners.add(listener);
                    }
                }
            } finally {
                mHttpsSetupLock.unlock();
            }
            return setupFinished;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (listener != null) {
                try {
                    mHttpsSetupLock.lock();
                    mHttpsSetupFinishListeners.add(listener);
                } finally {
                    mHttpsSetupLock.unlock();
                }
            }
            // We can't create CustomCertManagers here
            new Thread() {
                @Override
                public void run() {
                    setupHttpsImpl(appContext);
                }
            }.start();
            return false;
        } else {
            setupHttpsImpl(appContext);
            return true;
        }
    }

    private static void setupHttpsImpl(Context context) {
        final Context appContext = context.getApplicationContext();
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManager[] trustManagers = new TrustManager[]{new CustomCertManager(appContext)};
            sslContext.init(null, trustManagers, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            final HostnameVerifier defaultHostnameVerifier =
                    HttpsURLConnection.getDefaultHostnameVerifier();
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    String ownUrl = Settings.getString(appContext, Settings.SERVER_URL);
                    if (!TextUtils.isEmpty(ownUrl)) {
                        try {
                            URI uri = new URI(ownUrl);
                            if (s.equals(uri.getHost())) {
                                return true;
                            }
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    return defaultHostnameVerifier.verify(s, sslSession);
                }
            });
        } catch (NoSuchAlgorithmException|KeyManagementException e) {
            e.printStackTrace();
        }
        mHttpsSetupLock.lock();
        mHttpsInitialized = true;
        mHttpsSetupLock.unlock();
        // mHttpsInitialized = true -> nobody should modify this list anymore, we can safely use it
        while (!mHttpsSetupFinishListeners.isEmpty()) {
            mHttpsSetupFinishListeners.remove(0).onHttpsReady();
        }
    }

    public static JSONObject requestHttp(Context context, String site, Object parameters) {
        return requestHttp(context, site, parameters, null);
    }

    public static JSONObject requestHttp(Context context, String site, Object parameters,
                                         OnFileUploadListener onFileUploadListener) {
        String address = Settings.getString(context, Settings.SERVER_URL);
        return requestHttp(context, address, site, parameters, getAuthorization(context),
                onFileUploadListener);
    }

    private static String getAuthorization(Context context) {
        String username = Settings.getString(context, Settings.SERVER_LOGIN_USERNAME);
        String password = Settings.getString(context, Settings.SERVER_LOGIN_PASSWORD);
        String authorization;
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            authorization = "Basic " + Base64.encodeToString((username + ":" + password).getBytes(),
                    Base64.NO_WRAP);
        } else {
            authorization = null;
        }
        return authorization;
    }

    private static LazyHeaders buildAuthorizationHeader(final String auth) {
        return new LazyHeaders.Builder().addHeader("Authorization", new LazyHeaderFactory() {
            @Nullable
            @Override
            public String buildHeader() {
                return auth;
            }
        }).build();
    }

    public static Object getPictureUrl(Context context, Item item) {
        if (TextUtils.isEmpty(item.pictureUrl)) {
            // null will make Glide use the fallback drawable
            return null;
        }
        if (item.pictureUrl.contains("://")) {
            return item.pictureUrl;
        }
        // Relative URL, as from own upload
        String url = Settings.getString(context, Settings.SERVER_URL) + "/" + item.pictureUrl;
        // Authorization
        String authorization = ServerCommunicator.getAuthorization(context);
        if (authorization == null) {
            return url;
        } else {
            return new GlideUrl(url, ServerCommunicator.buildAuthorizationHeader(authorization));
        }
    }

    /**
     * @param context
     * Context
     * @param address
     * Base server address where remote list is located
     * @param site
     * Actual sub-site that will be called
     * @param parameters
     * Parameters for the POST call. If null, do a GET call
     * @param authorization
     * Authorization string for server access (probably generated from username and password).
     * Can be null if not required
     * @return
     * The JSONObject that the server returned on success, or null on failure
     */
    private static JSONObject requestHttp(Context context, String address, String site,
                                          Object parameters, String authorization,
                                          OnFileUploadListener onFileUploadListener) {

        address += site;

        if (DEBUG) Log.d(TAG, "Requesting to " + address + " with parameters " + parameters);

        HttpURLConnection connection = null;
        CustomCertManager certManager = null;
        OutputStreamWriter request = null;
        DataOutputStream request1 = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        String json = null;
        JSONObject jObj = null;

        if (Settings.getBoolean(context, Settings.SIMULATE_SLOW_INTERNET)) {
            if (DEBUG) Log.d(TAG, "Simulating slow internet");
            try {
                Thread.sleep(7000);
            } catch (InterruptedException e) {
            }
            if (DEBUG) Log.d(TAG, "Done simulating slow internet");
        }

        try {
            InputStream in;
            if (Settings.getBoolean(context, Settings.DEMO_LIST)) {
                in = context.getAssets().open("demo-request.txt");
            } else {
                URL url = new URL(address);
                certManager = new CustomCertManager(context, true, true, true);
                connection = getHttpUrlConnection(certManager, url);
                if (authorization != null) {
                    connection.setRequestProperty("Authorization", authorization);
                }
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                if (parameters == null) {
                    connection.setRequestMethod("GET");
                } else if (parameters instanceof String) {
                    connection.setRequestMethod("POST");
                    request = new OutputStreamWriter(connection.getOutputStream());
                    request.write((String) parameters);
                    request.flush();
                } else if (parameters instanceof Iterable) {
                    // Multipart POST request inspired by
                    // https://blog.morizyun.com/blog/android-httpurlconnection-post-multipart/index.html
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("Cache-Control", "no-cache");
                    connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" +
                            MULTIPART_BOUNDARY);
                    request1 = new DataOutputStream(connection.getOutputStream());
                    for (Object param: (Iterable) parameters) {
                        if (!(param instanceof MultiPartRequestParameter)) {
                            Log.e(TAG, "requestHttp with invalid parameter of type " +
                                    param.getClass() + ", skipping");
                            continue;
                        }
                        MultiPartRequestParameter mp = (MultiPartRequestParameter) param;
                        if (mp.value instanceof String) {
                            request1.writeBytes(TWO_HYPHENS + MULTIPART_BOUNDARY + CRLF);
                            request1.writeBytes("Content-Disposition: form-data; name=\"" + mp.key +
                                    "\"" + CRLF);
                            request1.writeBytes("Content-Type: text/plain; charset=UTF-8" + CRLF);
                            request1.writeBytes(CRLF);
                            request1.writeBytes(mp.value + CRLF);
                            request1.flush();
                        } else if (mp.value instanceof File) {
                            File file = (File) mp.value;
                            request1.writeBytes(TWO_HYPHENS + MULTIPART_BOUNDARY + CRLF);
                            request1.writeBytes("Content-Disposition: form-data; name=\"" + mp.key +
                                    "\";filename=\"" + file.getName() + "\"" + CRLF);
                            request1.writeBytes(CRLF);
                            if (DEBUG) Log.d(TAG, "Upload file " + file.getName() + " " +
                                    file.getPath());
                            if (!file.exists()) {
                                Log.e(TAG, "Upload file doesn't exist");
                            }
                            int size = (int) file.length();
                            byte[] bytes = new byte[size];
                            try {
                                BufferedInputStream buf = new BufferedInputStream(
                                        new FileInputStream(file));
                                buf.read(bytes, 0, bytes.length);
                                buf.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                                continue;
                            }
                            request1.write(bytes);
                            if (onFileUploadListener != null) {
                                onFileUploadListener.onAttemptFileUpload((File) mp.value);
                            }
                        } else {
                            Log.e(TAG, "requestHttp with invalid wrapped parameter of type " +
                                    param.getClass() + ", skipping");
                        }
                    }
                    request1.writeBytes(CRLF);
                    request1.writeBytes(TWO_HYPHENS + MULTIPART_BOUNDARY + TWO_HYPHENS + CRLF);
                    request1.flush();
                } else {
                    Log.e(TAG, "requestHttp with invalid parameters of type " +
                            parameters.getClass() + ", discarding parameters");
                    connection.setRequestMethod("GET");
                }
                int status = connection.getResponseCode();
                Log.d(TAG, "Connection status response: " + status);
                in = connection.getInputStream();
            }
            String line;
            isr = new InputStreamReader(in);
            reader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            json = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (request != null) {
                try {
                    request.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (request1 != null) {
                try {
                    request1.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (certManager != null) {
                certManager.close();
            }
        }

        if (json == null) {
            return null;
        }
        if (DEBUG) Log.d(TAG, "Received result:\n" + json);
        if (onFileUploadListener != null) {
            onFileUploadListener.onUploadDone();
        }
        try {
            jObj = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return jObj;

    }

    public static String initializeParameter(Context context) {
        String params = null;
        params = addParameter(params, Constants.JSON.USER_SECRET,
                Settings.getString(context, Settings.USER_SECRET));
        return params;
    }

    public static List<HttpPostOfflineCache.MultiPartRequestParameter>
    initializeMultipartParameter(Context context)
    {
        List<HttpPostOfflineCache.MultiPartRequestParameter> params = new ArrayList<>();
        params = addParameter(params, Constants.JSON.USER_SECRET,
                Settings.getString(context, Settings.USER_SECRET));
        return params;
    }

    public static String addParameter(String currentParameters, String newParameter,
                                      String newParameterValue) {
        try {
            newParameterValue = URLEncoder.encode(newParameterValue, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String result = newParameter + "=" + newParameterValue;
        if (currentParameters != null) {
            result = currentParameters + "&" + result;
        }
        return result;
    }

    public static List<HttpPostOfflineCache.MultiPartRequestParameter>
    addParameter(List<HttpPostOfflineCache.MultiPartRequestParameter> currentParameters,
                 String newParameter, Object newParameterValue)
    {
        currentParameters.add(new HttpPostOfflineCache.MultiPartRequestParameter(newParameter,
                newParameterValue));
        return currentParameters;
    }

    private static HttpURLConnection getHttpUrlConnection(CustomCertManager certManager, URL url)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (connection instanceof HttpsURLConnection) {
            final HttpsURLConnection sConnection = (HttpsURLConnection) connection;
            sConnection.setHostnameVerifier(certManager.hostnameVerifier(
                    sConnection.getHostnameVerifier()));
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{certManager}, null);
                sConnection.setSSLSocketFactory(sslContext.getSocketFactory());
            } catch (NoSuchAlgorithmException|KeyManagementException e) {
                // Use default TrustManager
            }
        }
        connection.setConnectTimeout(10000);
        return connection;
    }

    public static class MultiPartRequestParameter {
        public String key;
        public Object value;

        public MultiPartRequestParameter(String key, Object value) {
            this.key = key;
            this.value = value;
        }
    }
}
