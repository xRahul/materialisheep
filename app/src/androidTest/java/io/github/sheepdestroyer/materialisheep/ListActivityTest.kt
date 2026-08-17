package io.github.sheepdestroyer.materialisheep

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListActivityTest {

    @Test
    fun testListActivityLaunchesSuccessfully() {
        ActivityScenario.launch(ListActivity::class.java).use { scenario ->
            // Verify toolbar is displayed
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()))

            // Verify main content frame is displayed
            onView(withId(R.id.content_frame)).check(matches(isDisplayed()))
        }
    }
}
