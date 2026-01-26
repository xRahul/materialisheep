package io.github.sheepdestroyer.materialisheep.data;

import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MigrationTest {

    @Test
    public void testMigration5to6() {
        // Mock the builder
        RoomDatabase.Builder<MaterialisticDatabase> builder = mock(RoomDatabase.Builder.class);
        when(builder.addMigrations(any())).thenReturn(builder);

        // Call the method that adds migrations
        MaterialisticDatabase.setupBuilder(builder);

        // Capture the migrations
        ArgumentCaptor<Migration> captor = ArgumentCaptor.forClass(Migration.class);
        // addMigrations takes varargs, and we pass 3 migrations in setupBuilder
        verify(builder).addMigrations(captor.capture(), captor.capture(), captor.capture());

        List<Migration> migrations = captor.getAllValues();

        Migration migration5to6 = null;
        for (Migration m : migrations) {
            if (m.startVersion == 5 && m.endVersion == 6) {
                migration5to6 = m;
                break;
            }
        }

        assertNotNull("Migration 5->6 should be added", migration5to6);

        // Test the migration execution
        SupportSQLiteDatabase db = mock(SupportSQLiteDatabase.class);
        migration5to6.migrate(db);

        verify(db).execSQL("CREATE INDEX IF NOT EXISTS index_read_itemid ON read(itemid)");
    }
}
