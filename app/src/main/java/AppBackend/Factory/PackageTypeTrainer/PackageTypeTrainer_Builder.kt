package AppBackend.Factory.PackageTypeTrainer

class PackageTypeTrainerBuilder : Builder {

    private var taskName: String? = null
    private var dataInitializerName: String? = null
    private var trainerName: String? = null
    private var validatorName: String? = null

    override fun build(
        taskName: String,
        dataInitializerName: String,
        trainerName: String,
        validatorName: String
    ): PackageTypeTrainer {
        TODO("Not yet implemented")
    }

    fun make(packageTypeTrainer_preferences: Array<String>): PackageTypeTrainer {
        require(packageTypeTrainer_preferences.size == 4) { "Expected exactly 4 preferences" }

        TODO("Not yet implemented")
    }

    fun addTask(task_name: String): PackageTypeTrainerBuilder{
        TODO("Not yet implemented")
    }

    fun addDataInitializer(dataInitializer_name: String): PackageTypeTrainerBuilder{
        TODO("Not yet implemented")
    }

    fun addTrainer(trainer_name: String): PackageTypeTrainerBuilder{
        TODO("Not yet implemented")
    }
    fun addValidator(validator_name: String): PackageTypeTrainerBuilder{
        TODO("Not yet implemented")
    }

    // This version uses the builder's stored values
    fun build(): PackageTypeTrainer {
        val t = taskName ?: error("taskName not provided")
        val d = dataInitializerName ?: error("dataInitializerName not provided")
        val tr = trainerName ?: error("trainerName not provided")
        val v = validatorName ?: error("validatorName not provided")

        TODO("Not yet implemented");
    }
}
