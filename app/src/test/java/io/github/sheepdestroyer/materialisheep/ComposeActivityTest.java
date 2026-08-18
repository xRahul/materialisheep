package io.github.sheepdestroyer.materialisheep;

import android.content.Intent;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import io.github.sheepdestroyer.materialisheep.accounts.UserServices;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@RunWith(RobolectricTestRunner.class)
public class ComposeActivityTest {
    @Mock
    UserServices userServices;
    @Mock
    AlertDialogBuilder alertDialogBuilder;
    @Mock
    ApplicationComponent applicationComponent;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        MaterialisticApplication application = ApplicationProvider.getApplicationContext();
        application.applicationComponent = applicationComponent;

        org.mockito.Mockito.doAnswer(invocation -> {
            ComposeActivity activity = invocation.getArgument(0);
            activity.mUserServices = userServices;
            activity.mAlertDialogBuilder = alertDialogBuilder;
            return null;
        }).when(applicationComponent).inject(any(ComposeActivity.class));
    }

    @Test
    public void testBackPressedCallbackDisabledWhenEmpty() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ComposeActivity.class);
        intent.putExtra(ComposeActivity.EXTRA_PARENT_ID, "123");

        try (ActivityScenario<ComposeActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.mOnBackPressedCallback);
                assertFalse(activity.mOnBackPressedCallback.isEnabled());
            });
        }
    }

    @Test
    public void testBackPressedCallbackEnabledWhenDraftEntered() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ComposeActivity.class);
        intent.putExtra(ComposeActivity.EXTRA_PARENT_ID, "123");

        try (ActivityScenario<ComposeActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                EditText editText = activity.findViewById(R.id.edittext_body);
                editText.setText("New comment body");
                assertTrue(activity.mOnBackPressedCallback.isEnabled());
            });
        }
    }
}
