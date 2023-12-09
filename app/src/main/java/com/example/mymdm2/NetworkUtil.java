package com.example.mymdm2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkUtil {

    public static void fetchJsonFromUrl(final String urlString, final ResponseListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(urlString);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");

                    int responseCode = urlConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;

                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();

                        // 리스너를 통해 결과 반환
                        if (listener != null) {
                            listener.onResponse(response.toString());
                        }
                    } else {
                        // 에러 처리
                        if (listener != null) {
                            listener.onError("Server responded with: " + responseCode);
                        }
                    }
                } catch (Exception e) {
                    if (listener != null) {
                        listener.onError("Error: " + e.getMessage());
                    }
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
        }).start();
    }

    public interface ResponseListener {
        void onResponse(String response);
        void onError(String error);
    }
}
