package com.example.cardash.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cardash.services.obd.ObdConnectionService

class MetricViewModelFactory(private val obdConnectionService: ObdConnectionService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetricViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MetricViewModel(obdConnectionService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
