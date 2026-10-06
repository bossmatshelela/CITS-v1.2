package com.matshelela.childimmunisationtrackingsystemappv1

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.matshelela.childimmunisationtrackingsystemappv1.ui.ImmunisationTrackerApp
import com.matshelela.childimmunisationtrackingsystemappv1.ui.theme.MyApplicationTheme
import com.matshelela.childimmunisationtrackingsystemappv1.viewmodel.AppViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = AppViewModel(context)
    composeTestRule.setContent {
      MyApplicationTheme {
        ImmunisationTrackerApp(viewModel = viewModel)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
