package com.example.cardash.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cardash.services.obd.OBDService

class MetricViewModelFactory(private val obdService: OBDService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetricViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MetricViewModel(obdService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
