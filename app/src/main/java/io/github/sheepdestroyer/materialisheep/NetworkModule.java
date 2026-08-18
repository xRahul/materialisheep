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

import android.content.Context;
import android.net.TrafficStats;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;
import javax.net.SocketFactory;

import dagger.Module;
import dagger.Provides;
import io.github.sheepdestroyer.materialisheep.data.AlgoliaClient;
import io.github.sheepdestroyer.materialisheep.data.FileDownloader;
import io.github.sheepdestroyer.materialisheep.data.HackerNewsClient;
import io.github.sheepdestroyer.materialisheep.data.ReadabilityClient;
import io.github.sheepdestroyer.materialisheep.data.RestServiceFactory;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * A Dagger module that provides network-related dependencies.
 */
@Module
public class NetworkModule {
    private static final String TAG_OK_HTTP = "OkHttp";
    private static final long CACHE_SIZE = 20 * 1024 * 1024; // 20 MB

    /**
     * Provides a singleton instance of {@link RestServiceFactory}.
     *
     * @param callFactory The {@link Call.Factory} instance.
     * @return The singleton instance of {@link RestServiceFactory}.
     */
    @Provides
    @Singleton
    public RestServiceFactory provideRestServiceFactory(Call.Factory callFactory) {
        return new RestServiceFactory.Impl(callFactory);
    }

    /**
     * Provides a singleton instance of {@link Call.Factory}.
     *
     * @param context The application context.
     * @return The singleton instance of {@link Call.Factory}.
     */
    @Provides
    @Singleton
    public Call.Factory provideCallFactory(Context context) {
        return new OkHttpClient.Builder()
                .socketFactory(new SocketFactory() {
                    private SocketFactory mDefaultFactory = SocketFactory.getDefault();

                    @Override
                    public Socket createSocket() throws IOException {
                        Socket socket = mDefaultFactory.createSocket();
                        TrafficStats.setThreadStatsTag(1);
                        return socket;
                    }

                    @Override
                    public Socket createSocket(String host, int port) throws IOException {
                        Socket socket = mDefaultFactory.createSocket(host, port);
                        TrafficStats.setThreadStatsTag(1);
                        return socket;
                    }

                    @Override
                    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
                            throws IOException {
                        Socket socket = mDefaultFactory.createSocket(host, port, localHost, localPort);
                        TrafficStats.setThreadStatsTag(1);
                        return socket;
                    }

                    @Override
                    public Socket createSocket(InetAddress host, int port) throws IOException {
                        Socket socket = mDefaultFactory.createSocket(host, port);
                        TrafficStats.setThreadStatsTag(1);
                        return socket;
                    }

                    @Override
                    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
                            throws IOException {
                        Socket socket = mDefaultFactory.createSocket(address, port, localAddress, localPort);
                        TrafficStats.setThreadStatsTag(1);
                        return socket;
                    }
                })
                .cache(new Cache(context.getApplicationContext().getCacheDir(), CACHE_SIZE))
                .addNetworkInterceptor(new CacheOverrideNetworkInterceptor())
                .addInterceptor(new ConnectionAwareInterceptor(context))
                .addInterceptor(new LoggingInterceptor())
                .followRedirects(false)
                .build();
    }

    /**
     * Provides a singleton instance of {@link FileDownloader}.
     *
     * @param context     The application context.
     * @param callFactory The {@link Call.Factory} instance.
     * @return The singleton instance of {@link FileDownloader}.
     */
    @Provides
    @Singleton
    public FileDownloader provideFileDownloader(Context context, Call.Factory callFactory) {
        return new FileDownloader(context, callFactory);
    }

    static class ConnectionAwareInterceptor implements Interceptor {

        static final Set<String> CACHE_ENABLED_HOSTS = new HashSet<>(Arrays.asList(
                HackerNewsClient.HOST,
                AlgoliaClient.HOST
        ));
        private final Context mContext;

        ConnectionAwareInterceptor(Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            boolean forceCache = CACHE_ENABLED_HOSTS.contains(request.url().host()) &&
                    !AppUtils.hasConnection(mContext);
            return chain.proceed(forceCache ? request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build() : request);
        }
    }

    static class CacheOverrideNetworkInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);
            String host = request.url().host();
            if (!ConnectionAwareInterceptor.CACHE_ENABLED_HOSTS.contains(host)) {
                return response;
            }

            // Only cache successful HTTP responses (do not cache 4xx/5xx errors)
            if (!response.isSuccessful()) {
                return response;
            }

            // If request already explicitly demanded no-cache or force-network, respect it
            String requestCacheControl = request.header("Cache-Control");
            if (requestCacheControl != null &&
                    (requestCacheControl.contains("no-cache") || requestCacheControl.contains("max-age=0"))) {
                return response;
            }

            String path = request.url().encodedPath();
            String cacheControlHeader;
            if (path.endsWith("stories.json") || path.endsWith("maxitem.json") ||
                    path.endsWith("updates.json") || path.startsWith("/api/v1/")) {
                // Feed indices and search queries: short cache (1 min) so pull-to-refresh stays fresh
                cacheControlHeader = "max-age=60";
            } else if (path.startsWith("/v0/user/")) {
                // User profiles: 5 min cache
                cacheControlHeader = "max-age=300";
            } else {
                // Item details and comments: 30 min cache
                cacheControlHeader = "max-age=" + (30 * 60);
            }

            return response.newBuilder()
                    .header("Cache-Control", cacheControlHeader)
                    .build();
        }
    }

    static class LoggingInterceptor implements Interceptor {
        private final Interceptor debugInterceptor = new HttpLoggingInterceptor(
                message -> Log.d(TAG_OK_HTTP, message))
                .setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

        @Override
        public Response intercept(Chain chain) throws IOException {
            return debugInterceptor.intercept(chain);
        }
    }
}
