package io.github.sheepdestroyer.materialisheep.widget;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import io.github.sheepdestroyer.materialisheep.R;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24, 33})
public class NavFloatingActionButtonTest {

    @Test
    public void testInstantiation() {
        Context context = ApplicationProvider.getApplicationContext();
        context.setTheme(R.style.AppTheme);
        NavFloatingActionButton fab = new NavFloatingActionButton(context);
        assertNotNull(fab);
    }

    @Test
    public void testBindNavigationPad() {
         Context context = ApplicationProvider.getApplicationContext();
         context.setTheme(R.style.AppTheme);
         NavFloatingActionButton fab = new NavFloatingActionButton(context);
         // This triggers the GestureDetector creation code path
         fab.bindNavigationPad();
    }
}
