package com.example.messmenuapp.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardState(
    val menu: List<MenuItemUI> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val prefs = context.getSharedPreferences("menu_prefs", 0)

    var state = mutableStateOf(DashboardState())
        private set
    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        loadLastImportedFile()
    }

    fun importExcel(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Some providers don't support persistable permissions; the file still parses fine
            // for this session, it just won't survive an app restart.
        }
        prefs.edit().putString("last_file_uri", uri.toString()).apply()
        load(uri)
    }

    private fun loadLastImportedFile() {
        val saved = prefs.getString("last_file_uri", null) ?: return
        val uri = Uri.parse(saved)
        load(uri)
    }

    fun refresh() = loadLastImportedFile()

    private fun load(uri: Uri) {
        loadJob?.cancel()
        state.value = DashboardState(isLoading = true)
        loadJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { ExcelParser.parseExcel(context, uri) }
            state.value = DashboardState(menu = result.menu, message = result.message)
        }
    }
}
