import java.io.File

object YarnPackageFinder {

  // Read the list of affected packages from resources
  val packages = YarnPackageFinder::class.java.classLoader
    .getResource("affected-packages.txt")!!
    .readText()

  val packs = packages.split("\n").mapNotNull { line ->
    val l = line.trim()
    if (l.isEmpty()) return@mapNotNull null

    val startsWithAt = l.startsWith("@")
    val prefix = if (startsWithAt) "@" else ""
    val toSplit = if (startsWithAt) l.drop(1) else l
    val (p, v) = toSplit.split("@").map { it.trim() }
    """$prefix$p@npm:$v"""
  }

  @JvmStatic
  fun main(args: Array<String>) {
    println("Checking for forbidden packages in frontend...")
    // println("Infected packs: ${packs}")
    // The yarn-lock.json file inside the portal-ui module
    val yarnLock = File("yarn.lock") // relative to module root

    if (!yarnLock.exists()) {
      println("yarn-lock.json not found in module root")
      return
    }

    checkForPackages(listOf(yarnLock))
    println("[OK] No forbidden packages found in yarn.lock")
  }

  private fun checkForPackages(files: List<File>) {
    files.forEach { f ->
      val content = f.readText()
      packs.forEach { p ->
        println("Checking for package $p in ${f.name}...")
        if (content.contains(p, ignoreCase = true)) {
          println("${f.absolutePath} contains $p")
          throw IllegalStateException(
            "[NOK] Forbidden dependency $p found in ${f.name}"
          )
        }
        println("\t...not found :-)")
      }
    }
  }
}
