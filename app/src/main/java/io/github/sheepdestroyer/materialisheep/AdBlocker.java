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
import android.net.Uri;
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
    private static final byte[] EMPTY_BYTES = new byte[0];
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
     * Checks if a given URI is an ad.
     *
     * @param uri The URI to check.
     * @return True if the URI is an ad, false otherwise.
     */
    public static boolean isAd(Uri uri) {
        return isAdHost(uri != null ? uri.getHost() : "");
    }

    /**
     * Creates an empty WebResourceResponse to block a network request.
     *
     * @return An empty WebResourceResponse.
     */
    public static WebResourceResponse createEmptyResource() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(EMPTY_BYTES));
    }

    @WorkerThread
    private static TrieNode loadFromAssets(Context context) throws IOException {
        TrieBuilder root = new TrieBuilder();
        try (InputStream stream = context.getAssets().open(AD_HOSTS_FILE);
                BufferedSource buffer = Okio.buffer(Okio.source(stream))) {
            String line;
            while ((line = buffer.readUtf8Line()) != null) {
                root.add(line);
            }
        }
        return root.toTrieNode();
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

    static class TrieBuilder {
        private char[] keys;
        private TrieBuilder[] children;
        private int size;
        private boolean isEnd;

        TrieBuilder() {
            keys = new char[4];
            children = new TrieBuilder[4];
            size = 0;
        }

        void add(String host) {
            TrieBuilder node = this;
            for (int i = host.length() - 1; i >= 0; i--) {
                char c = host.charAt(i);
                node = node.getOrCreateChild(c);
            }
            node.isEnd = true;
        }

        private TrieBuilder getOrCreateChild(char c) {
            int idx = java.util.Arrays.binarySearch(keys, 0, size, c);
            if (idx >= 0) {
                return children[idx];
            }
            int insertPos = -(idx + 1);

            if (size == keys.length) {
                int newLen = size * 2;
                char[] newKeys = new char[newLen];
                TrieBuilder[] newChildren = new TrieBuilder[newLen];
                System.arraycopy(keys, 0, newKeys, 0, size);
                System.arraycopy(children, 0, newChildren, 0, size);
                keys = newKeys;
                children = newChildren;
            }

            if (insertPos < size) {
                System.arraycopy(keys, insertPos, keys, insertPos + 1, size - insertPos);
                System.arraycopy(children, insertPos, children, insertPos + 1, size - insertPos);
            }

            keys[insertPos] = c;
            TrieBuilder newNode = new TrieBuilder();
            children[insertPos] = newNode;
            size++;
            return newNode;
        }

        TrieNode toTrieNode() {
            TrieNode node = new TrieNode();
            node.isEnd = isEnd;
            if (size > 0) {
                if (keys.length == size) {
                    node.keys = keys;
                } else {
                    node.keys = new char[size];
                    System.arraycopy(keys, 0, node.keys, 0, size);
                }

                node.children = new TrieNode[size];
                for (int i = 0; i < size; i++) {
                    node.children[i] = children[i].toTrieNode();
                }
            }
            return node;
        }
    }

    static class TrieNode {
        char[] keys = new char[0];
        TrieNode[] children = new TrieNode[0];
        boolean isEnd;

        void add(String host) {
            TrieNode node = this;
            for (int i = host.length() - 1; i >= 0; i--) {
                char c = host.charAt(i);
                node = node.getOrCreateChild(c);
            }
            node.isEnd = true;
        }

        TrieNode getChild(char c) {
            int idx = java.util.Arrays.binarySearch(keys, c);
            if (idx >= 0) {
                return children[idx];
            }
            return null;
        }

        private TrieNode getOrCreateChild(char c) {
            int idx = java.util.Arrays.binarySearch(keys, c);
            if (idx >= 0) {
                return children[idx];
            }
            // Insert
            int insertPos = -(idx + 1);
            int len = keys.length;
            char[] newKeys = new char[len + 1];
            TrieNode[] newChildren = new TrieNode[len + 1];

            if (insertPos > 0) {
                System.arraycopy(keys, 0, newKeys, 0, insertPos);
                System.arraycopy(children, 0, newChildren, 0, insertPos);
            }

            newKeys[insertPos] = c;
            TrieNode newNode = new TrieNode();
            newChildren[insertPos] = newNode;

            if (insertPos < len) {
                System.arraycopy(keys, insertPos, newKeys, insertPos + 1, len - insertPos);
                System.arraycopy(children, insertPos, newChildren, insertPos + 1, len - insertPos);
            }

            keys = newKeys;
            children = newChildren;

            return newNode;
        }
    }
}
