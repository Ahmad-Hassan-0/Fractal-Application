package AppBackend.Factory.PackageTypeTrainer

import android.content.Context

interface Builder {

    // Legacy method (kept for compatibility with your existing Builder override)
    fun build(
        task_name: String,
        dataInitializer_name: String,
        trainer_name: String,
        validator_name: String
    ): PackageTypeTrainer

    // NEW: The dynamic builder method used by the Orchestrator
    fun make(
        context: Context,
        packageTypeTrainer_preferences: Array<String>
    ): PackageTypeTrainer

}