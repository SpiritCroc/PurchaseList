/*
 * Copyright (C) 2017 SpiritCroc
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
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import at.bitfire.cert4android.CustomCertManager;

public class ServerCommunicator {

    private static final String TAG = ServerCommunicator.class.getSimpleName();

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private ServerCommunicator() {}

    public static JSONObject requestHttp(Context context, String site, String parameters) {
        String address = Settings.getString(context, Settings.SERVER_URL);
        String username = Settings.getString(context, Settings.SERVER_LOGIN_USERNAME);
        String password = Settings.getString(context, Settings.SERVER_LOGIN_PASSWORD);
        String authorization;
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            authorization = "Basic " + Base64.encodeToString((username + ":" + password).getBytes(),
                    Base64.NO_WRAP);
        } else {
            authorization = null;
        }
        return requestHttp(context, address, site, parameters, authorization);
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
                                         String parameters, String authorization) {

        address += site;

        if (DEBUG) Log.d(TAG, "Requesting to " + address + " with parameters " + parameters);

        HttpURLConnection connection = null;
        CustomCertManager certManager = null;
        OutputStreamWriter request = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        String json = null;
        JSONObject jObj = null;

        try {
            URL url = new URL(address);
            certManager = new CustomCertManager(context, true, null);
            certManager.appInForeground = true;
            connection = getHttpUrlConnection(certManager, url);
            if (authorization != null) {
                connection.setRequestProperty("Authorization", authorization);
            }
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            if (parameters == null) {
                connection.setRequestMethod("GET");
            } else {
                connection.setRequestMethod("POST");
                request = new OutputStreamWriter(connection.getOutputStream());
                request.write(parameters);
                request.flush();
            }
            int status = connection.getResponseCode();
            Log.d(TAG, "Connection status response: " + status);
            String line;
            isr = new InputStreamReader(connection.getInputStream());
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
        try {
            jObj = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return jObj;

    }

    public static String addParamter(String currentParamters, String newParameter,
                                     String newParameterValue) {
        String result = newParameter + "=" + newParameterValue;
        if (currentParamters != null) {
            result = currentParamters + "&" + result;
        }
        return result;
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
}
