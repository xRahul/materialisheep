/*
 * Copyright (c) 2018 Ha Duy Trung
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

package io.github.sheepdestroyer.materialisheep.data

import androidx.annotation.WorkerThread
import io.github.sheepdestroyer.materialisheep.DataModule
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * A data repository for session state.
 */
@Singleton
class SessionManager(
    private val ioDispatcher: CoroutineDispatcher,
    private val cache: LocalCache,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
) {

    @Inject
    constructor(
        @Named(DataModule.IO_THREAD)
        ioScheduler: Scheduler,
        cache: LocalCache
    ) : this(
        Dispatchers.IO,
        cache,
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    /**
     * Checks if an item has been viewed.
     *
     * @param itemId the ID of the item to check
     * @return an [Observable] that emits `true` if the item has been viewed, `false` otherwise
     */
    @WorkerThread
    fun isViewed(itemId: String?): Observable<Boolean> = Observable.fromCallable {
        if (itemId.isNullOrEmpty()) {
            false
        } else {
            cache.isViewed(itemId)
        }
    }

    /**
     * Checks if an item has been viewed (coroutine suspend version).
     */
    suspend fun isItemViewed(itemId: String?): Boolean = withContext(ioDispatcher) {
        if (itemId.isNullOrEmpty()) false else cache.isViewed(itemId)
    }

    /**
     * Checks if multiple items have been viewed.
     *
     * @param itemIds the IDs of the items to check
     * @return an [Observable] that emits a list of booleans indicating if each item has been viewed
     */
    @WorkerThread
    fun isViewed(itemIds: List<String>): Observable<List<Boolean>> = Observable.fromCallable {
        if (itemIds.isEmpty()) {
            emptyList()
        } else {
            cache.isViewed(itemIds)
        }
    }

    /**
     * Checks if multiple items have been viewed (coroutine suspend version).
     */
    suspend fun areItemsViewed(itemIds: List<String>): List<Boolean> = withContext(ioDispatcher) {
        if (itemIds.isEmpty()) emptyList() else cache.isViewed(itemIds)
    }

    /**
     * Marks an item as having been viewed using the structured application CoroutineScope.
     *
     * @param itemId the ID of the item that has been viewed
     */
    fun view(itemId: String?) {
        if (itemId.isNullOrEmpty()) return
        applicationScope.launch {
            try {
                cache.setViewed(itemId)
            } catch (t: Throwable) {
                android.util.Log.e("SessionManager", "Failed to set viewed", t)
            }
        }
    }

    /**
     * Marks an item as having been viewed in a suspending manner.
     */
    suspend fun setViewed(itemId: String?) = withContext(ioDispatcher) {
        if (!itemId.isNullOrEmpty()) {
            cache.setViewed(itemId)
        }
    }
}
