package AppBackend.Factory.PackageTypeTrainer

interface Builder {
    fun build(task_name: String,dataInitializer_name: String,trainer_name: String,validator_name: String): PackageTypeTrainer
}