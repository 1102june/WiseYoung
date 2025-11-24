val opens: List<String>
    get() = with(java.util.Properties()) {
        val fileName = "modules.properties"
        val file = if (File(fileName).exists()) File(fileName) else File("../$fileName")
        file.inputStream().use { load(it) }
        val modules = getProperty("OPEN_MODULES").split(",")
        val samples = getProperty("OPEN_SAMPLES").split(",")
        modules + samples
    }

opens.forEach {
    include(":$it")
}
