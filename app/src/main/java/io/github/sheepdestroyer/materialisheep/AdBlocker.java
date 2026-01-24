/*
 * Copyright (c) 2016 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.sheepdestroyer.materialisheep;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import androidx.annotation.WorkerThread;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.HttpUrl;
import okio.BufferedSource;
import okio.Okio;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;

/**
 * A simple ad blocker that blocks network requests to hosts listed in the ad
 * hosts file.
 */
public class AdBlocker {
    private static final String AD_HOSTS_FILE = "pgl.yoyo.org.txt";
    private static volatile TrieNode AD_HOSTS = new TrieNode();

    /**
     * Initializes the ad blocker by loading the ad hosts from the assets file.
     *
     * @param context   The application context.
     * @param scheduler The RxJava scheduler to perform the operation on.
     */
    @SuppressLint("CheckResult")
    public static void init(Context context, Scheduler scheduler) {
        Observable.fromCallable(() -> loadFromAssets(context))
                .subscribeOn(scheduler)
                .subscribe(result -> AD_HOSTS = result,
                        t -> android.util.Log.e(AdBlocker.class.getSimpleName(), "Error loading ad hosts", t));
    }

    /**
     * Checks if a given URL is an ad.
     *
     * @param url The URL to check.
     * @return True if the URL is an ad, false otherwise.
     */
    public static boolean isAd(String url) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        return isAdHost(httpUrl != null ? httpUrl.host() : "");
    }

    /**
     * Creates an empty WebResourceResponse to block a network request.
     *
     * @return An empty WebResourceResponse.
     */
    public static WebResourceResponse createEmptyResource() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    }

    @WorkerThread
    private static TrieNode loadFromAssets(Context context) throws IOException {
        TrieNode root = new TrieNode();
        try (InputStream stream = context.getAssets().open(AD_HOSTS_FILE);
                BufferedSource buffer = Okio.buffer(Okio.source(stream))) {
            String line;
            while ((line = buffer.readUtf8Line()) != null) {
                root.add(line);
            }
        }
        return root;
    }

    /**
     * Iteratively walking up sub domain chain until we exhaust or find a match,
     * effectively doing a longest substring matching here
     */
    private static boolean isAdHost(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        TrieNode node = AD_HOSTS;
        for (int i = host.length() - 1; i >= 0; i--) {
            node = node.getChild(host.charAt(i));
            if (node == null) {
                return false;
            }
            if (node.isEnd) {
                // Check if we are at a dot or start of string
                if (i == 0 || host.charAt(i - 1) == '.') {
                    return true;
                }
            }
        }
        return false;
    }

    static class TrieNode {
        private final java.util.Map<Character, TrieNode> children = new java.util.HashMap<>();
        boolean isEnd;

        void add(String host) {
            TrieNode node = this;
            for (int i = host.length() - 1; i >= 0; i--) {
                node = node.children.computeIfAbsent(host.charAt(i), k -> new TrieNode());
            }
            node.isEnd = true;
        }

        TrieNode getChild(char c) {
            return children.get(c);
        }
    }
}
