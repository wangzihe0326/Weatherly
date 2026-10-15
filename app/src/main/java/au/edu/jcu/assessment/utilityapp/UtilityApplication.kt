package au.edu.jcu.assessment.utilityapp

import android.app.Application
import au.edu.jcu.assessment.utilityapp.di.AppContainer

class UtilityApplication : Application() {
    val container: AppContainer by lazy { AppContainer() }
}
