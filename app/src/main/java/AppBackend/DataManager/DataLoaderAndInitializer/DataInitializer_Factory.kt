package AppBackend.DataManager.DataLoaderAndInitializer

interface DataInitializer_Factory {
    fun createDataInitializer(): DataInitializer
}