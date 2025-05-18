package com.fuseforge.cardash.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fuseforge.cardash.services.obd.OBDService
import com.fuseforge.cardash.services.obd.OBDServiceWithDiagnostics

class MetricViewModelFactory(
    private val obdService: OBDService,
    private val obdServiceWithDiagnostics: OBDServiceWithDiagnostics
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetricViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MetricViewModel(obdService, obdServiceWithDiagnostics) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
