package com.webstudio.easybrowser.utils;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchSuggestionProvider {
    private static final String TAG = "SearchSuggestions";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.SECONDS)
            .build();

    public interface SuggestionsCallback {
        void onSuggestions(List<String> suggestions);
    }

    // Backward-compat overload — uses DuckDuckGo autocomplete.
    public void fetchSuggestions(String query, SuggestionsCallback callback) {
        fetchSuggestions(query, null, callback);
    }

    // Fetches suggestions from the autocomplete endpoint that matches searchEngineBaseUrl.
    public void fetchSuggestions(String query, String searchEngineBaseUrl, SuggestionsCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onSuggestions(new ArrayList<>());
            return;
        }
        String base = UrlUtils.getSuggestionUrl(searchEngineBaseUrl);
        String encoded = Uri.encode(query.trim());
        // DuckDuckGo requires &type=list to return the OpenSearch JSON array format.
        String url = base.contains("duckduckgo.com")
                ? base + encoded + "&type=list"
                : base + encoded;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onSuggestions(new ArrayList<>());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> suggestions = new ArrayList<>();
                try (response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onSuggestions(suggestions);
                        return;
                    }
                    JSONArray root = new JSONArray(response.body().string());
                    if (root.length() >= 2) {
                        JSONArray items = root.getJSONArray(1);
                        for (int i = 0; i < Math.min(items.length(), 6); i++) {
                            suggestions.add(items.getString(i));
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse suggestion response", e);
                }
                callback.onSuggestions(suggestions);
            }
        });
    }
}
