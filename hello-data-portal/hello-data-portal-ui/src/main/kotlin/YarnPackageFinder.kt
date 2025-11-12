import java.io.File

object YarnPackageFinder {

  data class YarnEntry(val name: String, val version: String)

  private fun parseAffectedPackages(): List<YarnEntry> {
    val packages = YarnPackageFinder::class.java.classLoader
      .getResource("affected-packages.txt")!!
      .readText()

    return packages.lines().mapNotNull { line ->
      val l = line.trim()
      if (l.isEmpty()) return@mapNotNull null

      val startsWithAt = l.startsWith("@")
      val prefix = if (startsWithAt) "@" else ""
      val toSplit = if (startsWithAt) l.drop(1) else l
      val (pkg, ver) = toSplit.split("@").map { it.trim() }
      YarnEntry("$prefix$pkg", ver)
    }
  }

  private fun parseYarnLock(file: File): List<YarnEntry> {
    val entries = mutableListOf<YarnEntry>()
    val lines = file.readLines()

    var currentNames: List<String>? = null

    for (line in lines) {
      if (!line.startsWith(" ")) {
        // New package block starts
        val raw = line.substringBeforeLast(":").trim().removeSurrounding("\"")
        // Could be multiple selectors: "lodash@^4.17.15", lodash@^4.17.20
        currentNames = raw.split(",").map { it.trim().removeSurrounding("\"") }
      } else if (line.trim().startsWith("version")) {
        // Yarn v1: version "0.14.10"
        // Yarn v2+: version: "0.14.10"
        val version = line.substringAfter("version").replace(":", "").trim().removeSurrounding("\"")
        currentNames?.forEach { selector ->
          // Normalize selector: package@range
          val name = selector.substringBeforeLast("@")
            .substringBefore("@npm:") // remove npm: prefix if present
          entries.add(YarnEntry(name, version))
        }
        currentNames = null
      }
    }
    return entries
  }

  @JvmStatic
  fun main(args: Array<String>) {
    println("Checking for forbidden packages in frontend...")

    val yarnLock = File("yarn.lock")
    if (!yarnLock.exists()) {
      println("yarn.lock not found in module root")
      return
    }

    val forbidden = parseAffectedPackages()
    val resolved = parseYarnLock(yarnLock)

    val found = forbidden.filter { f ->
      resolved.any { it.name == f.name && it.version == f.version }
    }

    if (found.isNotEmpty()) {
      found.forEach {
        println("[NOK] Forbidden dependency ${it.name}@${it.version} found in yarn.lock")
      }
      throw IllegalStateException("Forbidden dependencies found: $found")
    }

    println("[OK] No forbidden packages found in yarn.lock")
  }
}
