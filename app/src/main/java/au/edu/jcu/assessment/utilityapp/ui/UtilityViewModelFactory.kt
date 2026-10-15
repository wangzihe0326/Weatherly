package au.edu.jcu.assessment.utilityapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import au.edu.jcu.assessment.utilityapp.data.repository.WeatherRepository

class UtilityViewModelFactory(private val repository: WeatherRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(UtilityViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return UtilityViewModel(repository) as T
    }
}
