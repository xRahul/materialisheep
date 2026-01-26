package io.github.sheepdestroyer.materialisheep.data;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class MaterialisticDatabaseIndexTest {
    private MaterialisticDatabase db;
    private ReadStoriesDao dao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, MaterialisticDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.getReadStoriesDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void testSelectByItemIdPerformance() {
        // Insert 100,000 items
        int count = 100000;

        db.runInTransaction(() -> {
            for (int i = 0; i < count; i++) {
                dao.insert(new MaterialisticDatabase.ReadStory(String.valueOf(i)));
            }
        });

        // Warm up
        dao.selectByItemId("5000");

        // Measure query time for non-existent item (guarantees full table scan without index)
        long start = System.nanoTime();
        MaterialisticDatabase.ReadStory result = dao.selectByItemId("non_existent");
        long duration = System.nanoTime() - start;

        System.out.println("Query time for non-existent item (ns): " + duration);
        assertNull(result);

        // Measure query time for existing item (worst case if no index and scan order matters)
        start = System.nanoTime();
        result = dao.selectByItemId(String.valueOf(count - 1));
        duration = System.nanoTime() - start;
        System.out.println("Query time for last item (ns): " + duration);
        assertNotNull(result);

        // Check query plan
        android.database.Cursor cursor = db.query("EXPLAIN QUERY PLAN SELECT * FROM read WHERE itemid = ?", new Object[]{"non_existent"});
        try {
            if (cursor.moveToFirst()) {
                do {
                    System.out.println("PLAN: " + cursor.getString(cursor.getColumnIndex("detail")));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
    }
}
