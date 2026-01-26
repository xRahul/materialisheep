package io.github.sheepdestroyer.materialisheep;

import android.app.Application;
import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import io.github.sheepdestroyer.materialisheep.accounts.UserServices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class SubmitActivityTest {
    @Mock
    UserServices userServices;
    @Mock
    AlertDialogBuilder alertDialogBuilder;
    @Mock
    ApplicationComponent applicationComponent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        MaterialisticApplication application = ApplicationProvider.getApplicationContext();
        application.applicationComponent = applicationComponent;

        // Mock injection
        org.mockito.Mockito.doAnswer(invocation -> {
            SubmitActivity activity = invocation.getArgument(0);
            activity.mUserServices = userServices;
            activity.mAlertDialogBuilder = alertDialogBuilder;
            return null;
        }).when(applicationComponent).inject(any(SubmitActivity.class));
    }

    @Test
    public void testSuccessfulSubmissionRedirectsToUserActivity() {
        Preferences.setUsername(ApplicationProvider.getApplicationContext(), "testuser");

        try (ActivityScenario<SubmitActivity> scenario = ActivityScenario.launch(SubmitActivity.class)) {
            scenario.onActivity(activity -> {
                // Trigger submission
                TextView title = activity.findViewById(R.id.edittext_title);
                TextView content = activity.findViewById(R.id.edittext_content);
                title.setText("Test Title");
                content.setText("http://example.com");

                // Simulate menu click to send (or just call submit directly if accessible, but menu is better integration)
                // Accessing private method submit() or synthetic onSubmitted().
                // Since onSubmitted is package-private/synthetic, we can call it directly or via reflection if needed.
                // Or capture the callback passed to userServices.submit and invoke it.

                // Invoke submit via private method call? Or just simulate the flow.
                // Let's call the synthetic 'onSubmitted' method directly to verify redirection logic,
                // which is what we want to change.
                // However, 'onSubmitted' is package-private, so we can access it if we are in the same package.
                // This test class is in the same package.

                activity.onSubmitted(true);

                // Verify redirection
                Intent expectedIntent = new Intent(activity, UserActivity.class);
                Intent actualIntent = Shadows.shadowOf(activity).getNextStartedActivity();

                assertNotNull("Should start an activity", actualIntent);
                assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
                assertEquals("testuser", actualIntent.getStringExtra(UserActivity.EXTRA_USERNAME));
            });
        }
    }
}
