package com.example.fractal

import android.app.Application
import android.util.Log
import AppBackend.Network.Server_DAO.Server_DAO
import AppGlobal.GlobalState
import AppGlobal.app_config
import AppGlobal.Utils.FileOperations
import AppGlobal.Utils.GlobalUtils

class FractalApplication : Application() {

    lateinit var globalState: GlobalState
    lateinit var globalUtils: GlobalUtils
    lateinit var appConfig: app_config

    override fun onCreate() {
        super.onCreate()

        // ------------------------------
        // 1. Instantiate FileOperations
        // ------------------------------
        val fileOps = FileOperations(this)

        val fileName = "app_config.json"
        val defaultAsset = "app_config_default.json"

        // ------------------------------
        // 2. Copy default JSON if needed
        // ------------------------------
        fileOps.copyDefaultFromAssets(defaultAsset, fileName)

        // ------------------------------
        // 3. Load config file
        // ------------------------------
        val loadedConfig = fileOps.readJson<app_config>(fileName)
        appConfig = loadedConfig ?: app_config()
        if (loadedConfig == null) {
            fileOps.writeJson(fileName, appConfig)
        }

        // ------------------------------
        // 4. Setup server
        // ------------------------------
        val serverDao = Server_DAO()

        // ------------------------------
        // 5. Setup GlobalState
        // ------------------------------
        globalState = GlobalState()
        globalState.server = serverDao
        globalState.appConfig = appConfig

        // ------------------------------
        // 6. Utils
        // ------------------------------
        globalUtils = GlobalUtils()

        Log.d("FractalApp", "FractalApplication started. Config Loaded: $appConfig")
    }




}
