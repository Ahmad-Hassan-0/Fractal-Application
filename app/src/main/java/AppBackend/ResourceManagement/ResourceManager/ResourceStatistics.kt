package AppBackend.ResourceManagement.ResourceManager

data class ResourceStatistics(
    var overallPerformance: String = "0%",
    var estimatedTimeLeft: String = "Calculating...",
    var epochsCompleted: String = "0 / 0",
    var inferenceTesting: String = "Pending"
)