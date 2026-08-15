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

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val prefs = context.getSharedPreferences("menu_prefs", 0)

    var menuList = mutableStateOf<List<MenuItemUI>>(emptyList())
        private set

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
        viewModelScope.launch {
            menuList.value = withContext(Dispatchers.IO) { ExcelParser.parseExcel(context, uri) }
        }
    }

    private fun loadLastImportedFile() {
        val saved = prefs.getString("last_file_uri", null) ?: return
        val uri = Uri.parse(saved)
        viewModelScope.launch {
            menuList.value = withContext(Dispatchers.IO) { ExcelParser.parseExcel(context, uri) }
        }
    }
}
