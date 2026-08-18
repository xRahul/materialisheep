/*
 * Copyright (c) 2015 Ha Duy Trung
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
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe cache and loader for Typeface objects supporting both res/font resources
 * and legacy assets with safe exception handling and graceful fallbacks.
 */
public class FontCache {

    private static final String TAG = "FontCache";
    private static volatile FontCache sInstance;
    private final ConcurrentHashMap<String, Typeface> mTypefaceMap = new ConcurrentHashMap<>();
    private static final Map<String, Integer> FONT_RESOURCE_MAP = new HashMap<>();

    static {
        FONT_RESOURCE_MAP.put("DroidSans.ttf", R.font.droid_sans);
        FONT_RESOURCE_MAP.put("droid_sans", R.font.droid_sans);
        FONT_RESOURCE_MAP.put("DroidSerif.ttf", R.font.droid_serif);
        FONT_RESOURCE_MAP.put("droid_serif", R.font.droid_serif);
        FONT_RESOURCE_MAP.put("LibreBaskerville-Regular.ttf", R.font.libre_baskerville_regular);
        FONT_RESOURCE_MAP.put("libre_baskerville_regular", R.font.libre_baskerville_regular);
        FONT_RESOURCE_MAP.put("RobotoSlab-Regular.ttf", R.font.roboto_slab_regular);
        FONT_RESOURCE_MAP.put("roboto_slab_regular", R.font.roboto_slab_regular);
    }

    /**
     * Gets the singleton instance of the FontCache.
     *
     * @return The singleton instance of the FontCache.
     */
    public static FontCache getInstance() {
        if (sInstance == null) {
            synchronized (FontCache.class) {
                if (sInstance == null) {
                    sInstance = new FontCache();
                }
            }
        }
        return sInstance;
    }

    private FontCache() { }

    /**
     * Gets a Typeface from the cache, res/font, or assets with fallback to Typeface.DEFAULT.
     *
     * @param context      The context.
     * @param typefaceName The name of the typeface or asset file.
     * @return The Typeface object or null if name is empty.
     */
    @Nullable
    public Typeface get(Context context, String typefaceName) {
        if (context == null || TextUtils.isEmpty(typefaceName)) {
            return null;
        }
        return mTypefaceMap.computeIfAbsent(typefaceName, name -> loadTypeface(context, name));
    }

    private Typeface loadTypeface(Context context, String name) {
        Integer fontResId = FONT_RESOURCE_MAP.get(name);
        if (fontResId != null) {
            try {
                Typeface typeface = ResourcesCompat.getFont(context, fontResId);
                if (typeface != null) {
                    return typeface;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to load font from resource: " + fontResId, e);
            }
        }

        // Fallback to asset loading
        try {
            return Typeface.createFromAsset(context.getAssets(), name);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create typeface from asset: " + name + ", falling back to DEFAULT", e);
            return Typeface.DEFAULT;
        }
    }
}
