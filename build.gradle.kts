/**
 * Root aggregator — Phase 1 + Phase 2.
 */
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.0-Beta2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0-Beta2" apply false
    id("com.github.spotbugs") version "6.5.9" apply false
}

allprojects {
    group = "br.project"
    version = "3.1.0"
}

subprojects {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
        maven { url = uri("https://artifacts.deepl.com/maven/") }
    }

    // Apply SpotBugs to any subproject that uses the Java plugin.
    // Kotlin sources are out of scope for SpotBugs (use detekt for those).
    plugins.withId("java") {
        apply(plugin = "com.github.spotbugs")

        extensions.configure<com.github.spotbugs.snom.SpotBugsExtension>("spotbugs") {
            ignoreFailures.set(true)
            showStackTraces.set(false)
            effort.set(com.github.spotbugs.snom.Effort.MAX)
            reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)

            val excludeFile = rootProject.file("config/spotbugs/exclude.xml")
            if (excludeFile.exists()) {
                excludeFilter.set(excludeFile)
            }
        }

        // Wire up HTML + XML + SARIF reports so CI can publish them as artifacts.
        tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
            reports.create("html") {
                required.set(true)
                outputLocation.set(layout.buildDirectory.file("reports/spotbugs/${name}/spotbugs.html"))
            }
            reports.create("xml") {
                required.set(true)
                outputLocation.set(layout.buildDirectory.file("reports/spotbugs/${name}/spotbugs.xml"))
            }
            reports.create("sarif") {
                required.set(true)
                outputLocation.set(layout.buildDirectory.file("reports/spotbugs/${name}/spotbugs.sarif"))
            }
        }
    }
}

tasks.register<Delete>("clean") {
    group = "build"
    description = "Deletes root/subproject build outputs and libs/server.jar"
    delete(layout.buildDirectory)
    delete(subprojects.map { it.layout.buildDirectory })
    delete(layout.projectDirectory.file("libs/server.jar"))
}

tasks.register("compileAll") {
    group = "build"
    description = "Compiles core + SPI + optional mods"
    dependsOn(":extensions-spi:classes", ":game-server-core:classes", ":proxy:classes")
    if (findProject(":mod-boss-zerg") != null) {
        dependsOn(":mod-boss-zerg:classes")
    }
}


tasks.register("build") {
    group = "build"
    description = "Tests SPI + packages server.jar"
    dependsOn(":extensions-spi:test", ":game-server-core:test", ":mod-pix:test", ":proxy:test", ":app-dist:jar", ":db-migrate:test")
}

tasks.register("compileJava") {
    group = "build"
    dependsOn(":game-server-core:compileJava")
}

tasks.register("compileKotlin") {
    group = "build"
    dependsOn(":game-server-core:compileKotlin")
}

val databaseSqlPatterns = linkedMapOf(
    "ON_DUPLICATE_KEY_UPDATE" to "ON DUPLICATE KEY UPDATE",
    "REPLACE_INTO" to "REPLACE INTO",
    "INSERT_OR_REPLACE_INTO" to "INSERT OR REPLACE INTO",
)

tasks.register("checkDatabaseSql") {
    group = "verification"
    description = "Blocks new vendor-specific SQL outside the DatabaseDialect compatibility boundary"

    doLast {
        val baselineFile = rootProject.file("docs/database/sql-compatibility-baseline.properties")
        val sourceFiles = rootProject.fileTree("modules") {
            include("**/*.java")
            include("**/*.kt")
            exclude("**/src/test/**")
            exclude("**/DatabaseDialect.java")
            exclude("**/SqlDialect.java")
        }
        val counts = databaseSqlPatterns.mapValues { (_, token) ->
            sourceFiles.sumOf { file -> file.readLines().count { line -> line.contains(token) } }
        }

        if (project.hasProperty("updateDatabaseSqlBaseline")) {
            baselineFile.parentFile.mkdirs()
            baselineFile.writeText(counts.entries.joinToString("\n") { (key, value) -> "$key=$value" } + "\n")
            logger.lifecycle("Database SQL compatibility baseline updated: $counts")
            return@doLast
        }

        if (!baselineFile.exists())
            throw GradleException("Missing $baselineFile. Run with -PupdateDatabaseSqlBaseline once to register existing legacy debt.")

        val baseline = baselineFile.readLines()
            .mapNotNull { line ->
                val parts = line.split('=', limit = 2)
                if (parts.size == 2) parts[0] to parts[1].toInt() else null
            }
            .toMap()
        val newFindings = counts.filter { (key, count) -> count > (baseline[key] ?: 0) }
        logger.lifecycle("Database SQL compatibility scan: $counts (baseline=$baseline)")
        if (newFindings.isNotEmpty())
            throw GradleException("New vendor-specific SQL detected outside DatabaseDialect: $newFindings. Migrate it or update the baseline with review.")
    }
}

tasks.register("checkDatabaseConnectionBoundary") {
    group = "verification"
    description = "Blocks direct ConnectionPool access outside the database adapter boundary"

    doLast {
        val sourceFiles = rootProject.fileTree("modules") {
            include("**/*.java")
            include("**/*.kt")
            exclude("**/src/test/**")
            exclude("**/bin/**")
            exclude("**/build/**")
            exclude("**/ConnectionPool.java")
            exclude("**/JdbcDatabaseConnectionAdapter.java")
            exclude("**/DatabaseConnection.java")
            exclude("**/DatabaseConnectionPort.java")
            exclude("**/data/adapter/**")
            exclude("**/persistence/**")
            exclude("**/sql/**")
            exclude("**/gameapi/db/**")
        }
        val findings = sourceFiles.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                if (line.contains("ConnectionPool.getConnection(")) "${file.path}:${index + 1}" else null
            }
        }
        if (findings.isNotEmpty())
            throw GradleException("Direct ConnectionPool access outside the adapter boundary: $findings")
        logger.lifecycle("Database connection boundary scan: clean")
    }
}

tasks.register("checkPersistenceConstructionBoundary") {
    group = "verification"
    description = "Blocks direct JDBC adapter construction outside the GameServer composition root"

    doLast {
        val sourceFiles = rootProject.fileTree("modules/game-server-core/src/main/java/ext/mods/gameserver") {
            include("**/*.java")
            exclude("**/data/adapter/**")
            exclude("**/data/PersistenceRegistry.java")
        }
        val findings = sourceFiles.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                if (Regex("new Jdbc[A-Za-z0-9]+Store\\(").containsMatchIn(line)) "${file.path}:${index + 1}" else null
            }
        }
        if (findings.isNotEmpty())
            throw GradleException("Direct JDBC adapter construction outside PersistenceRegistry: $findings")
        logger.lifecycle("Persistence construction boundary scan: clean")
    }
}

tasks.register("checkRuntimeScripts") {
    group = "verification"
    description = "Validates the canonical Docker runtime commands and compatibility wrappers"

    doLast {
        val canonicalPowerShell = rootProject.file("tools/runtime/l2newera.ps1")
        val canonicalShell = rootProject.file("tools/runtime/l2newera.sh")
        val wrapperPowerShell = rootProject.file("StartL2NewEra.ps1")
        val wrapperShell = rootProject.file("StartL2NewEra.sh")
        val wrapperBatch = rootProject.file("StartL2NewEra.bat")

        val requiredFiles = listOf(
            canonicalPowerShell,
            canonicalShell,
            wrapperPowerShell,
            wrapperShell,
            wrapperBatch,
        )
        val missing = requiredFiles.filterNot { it.isFile }.map { it.relativeTo(rootProject.projectDir).path }
        if (missing.isNotEmpty())
            throw GradleException("Missing canonical runtime scripts: $missing")

        val requiredActions = listOf("init", "validate", "build", "up", "down", "restart", "status", "logs")
        val canonicalScripts = listOf(canonicalPowerShell, canonicalShell)
        canonicalScripts.forEach { script ->
            val content = script.readText()
            val absentActions = requiredActions.filterNot(content::contains)
            if (absentActions.isNotEmpty())
                throw GradleException("${script.path} does not implement runtime actions: $absentActions")
            if (!content.contains("deploy/docker/docker-compose.yml") && !content.contains("deploy\\docker\\docker-compose.yml"))
                throw GradleException("${script.path} does not use the canonical Compose file")
            if (content.contains("down -v") || content.contains("\"down\", \"-v\""))
                throw GradleException("${script.path} must not expose destructive volume removal")
        }

        mapOf(
            wrapperPowerShell to "tools\\runtime\\l2newera.ps1",
            wrapperShell to "tools/runtime/l2newera.sh",
            wrapperBatch to "StartL2NewEra.ps1",
        ).forEach { (wrapper, target) ->
            if (!wrapper.readText().contains(target))
                throw GradleException("${wrapper.path} no longer delegates to $target")
        }

        logger.lifecycle("Runtime script contract: clean")
    }
}

tasks.register("checkLegacyLauncherHelpers") {
    group = "verification"
    description = "Validates the boundary between legacy launcher helpers and generated cache state"

    doLast {
        val helperRoot = rootProject.file("tools/legacy/launcher-helpers")
        val requiredHelpers = listOf(
            "brproject-ansi.inc.bat",
            "brproject-cds-check.inc.bat",
            "brproject-cds-check.inc.sh",
            "brproject-classpath.inc.bat",
            "brproject-classpath.inc.sh",
            "brproject-g1-reclaim.inc.bat",
            "brproject-g1-reclaim.inc.sh",
            "brproject-java.inc.bat",
            "brproject-java.inc.sh",
        )
        val missing = requiredHelpers.filterNot { helperRoot.resolve(it).isFile }
        if (missing.isNotEmpty())
            throw GradleException("Missing legacy launcher helpers: $missing")

        val trackedCacheHelpers = rootProject.fileTree("cache") {
            include("brproject-*.inc.*", ".appcds-fp")
        }.files
        if (trackedCacheHelpers.isNotEmpty())
            throw GradleException("Generated cache still contains tracked launcher helpers: $trackedCacheHelpers")

        val launchers = listOf(
            "StartBrproject.bat",
            "StartGame_SemDashboard.bat",
            "StartGame_SemDashboard.sh",
            "StartLogin_SemDashboard.bat",
            "StartLogin_SemDashboard.sh",
            "StartBrproject.sh",
            "RegisterGameServer.sh",
        ).map(rootProject::file)
        val staleReferences = launchers.flatMap { launcher ->
            launcher.readLines().mapIndexedNotNull { index, line ->
                if (line.contains("cache/brproject-") || line.contains("cache\\\\brproject-"))
                    "${launcher.path}:${index + 1}"
                else null
            }
        }
        if (staleReferences.isNotEmpty())
            throw GradleException("Launchers still reference helpers under generated cache: $staleReferences")

        if (!rootProject.file(".gitignore").readLines().any { it.trim() == "cache/" })
            throw GradleException("Root cache/ must be ignored because it is generated runtime state")

        logger.lifecycle("Legacy launcher helper boundary: clean")
    }
}

tasks.register("checkComponentBoundaries") {
    group = "verification"
    description = "Protects the boundary between the official server and optional legacy components"

    doLast {
        val boundaryDocs = listOf(
            "docs/architecture/component-boundaries.md",
            "site/README.md",
            "libs/README.md",
            "tools/README.md",
        ).map(rootProject::file)
        val missingDocs = boundaryDocs.filterNot { it.isFile }.map { it.relativeTo(rootProject.projectDir).path }
        if (missingDocs.isNotEmpty())
            throw GradleException("Missing component boundary documentation: $missingDocs")

        val dockerIgnore = rootProject.file(".dockerignore").readLines().map { it.trim() }.toSet()
        val missingDockerExcludes = listOf("site").filterNot(dockerIgnore::contains)
        if (missingDockerExcludes.isNotEmpty())
            throw GradleException("Optional components leaked into the official Docker context: $missingDockerExcludes")

        val compose = rootProject.file("deploy/docker/docker-compose.yml").readText()
        val forbiddenComposeReferences = listOf("../site", "../../site")
            .filter(compose::contains)
        if (forbiddenComposeReferences.isNotEmpty())
            throw GradleException("Official Compose references optional legacy components: $forbiddenComposeReferences")

        val processManager = rootProject.file(
            "modules/game-server-core/src/main/java/ext/mods/commons/gui/services/ProcessManagerService.java"
        ).readText()
        val removedRepositoryPaths = listOf(
            "new File(projectRoot, \"bin/",
            "new File(\"bin/",
        ).filter(processManager::contains)
        if (removedRepositoryPaths.isNotEmpty())
            throw GradleException("Legacy GUI still references removed repository paths: $removedRepositoryPaths")

        val accidentalCoupling = rootProject.fileTree("modules") {
            include("**/*.java", "**/*.kt")
            exclude("**/ProcessManagerService.java", "**/src/test/**", "**/build/**", "**/bin/**")
        }.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val filesystemMarkers = listOf(
                    "new File(\"site/", "new File(\"site\\\\",
                    "Path.of(\"site/",
                    "Paths.get(\"site/",
                    "resolve(\"site/",
                )
                if (filesystemMarkers.any(line::contains))
                    "${file.path}:${index + 1}"
                else null
            }
        }
        if (accidentalCoupling.isNotEmpty())
            throw GradleException("Server source depends directly on client/site snapshots: $accidentalCoupling")

        logger.lifecycle("Component boundary contract: clean")
    }
}

tasks.named("build") {
    dependsOn("checkDatabaseSql")
    dependsOn("checkDatabaseConnectionBoundary")
    dependsOn("checkPersistenceConstructionBoundary")
    dependsOn("checkRuntimeScripts")
    dependsOn("checkLegacyLauncherHelpers")
    dependsOn("checkComponentBoundaries")
}

tasks.register("brCompileIncremental") {
    group = "build"
    description = "Build incremental: compila o que mudou + gera libs/server.jar"
    dependsOn(
        ":commons:jar",
        ":extensions-spi:jar",
        ":login-server:jar",
        ":db-migrate:jar",
        ":game-server-core:jar",
        ":proxy:jar",
        ":app-dist:jar",
    )
}

tasks.register("brCompileCleanAfterBuild") {
    group = "build"
    description = "Build wrapper (chamado por brCompileClean via gradlew clean brCompileClean)"
    dependsOn(":app-dist:jar")
}

tasks.register("brCompileClean") {
    group = "build"
    description = "Clean + build completo: use `gradlew clean brCompileClean` (gradlew.bat ja chama nesta ordem)"
    dependsOn("brCompileCleanAfterBuild")
}

tasks.register("PrepararTeste") {
    group = "build"
    description = "Prepara ambiente (configs + GUI 1ª vez + hexid) e opcionalmente inicia o servidor"
    dependsOn(":app-dist:jar")

    doLast {
        val libsDir = rootProject.layout.projectDirectory.dir("libs").asFile
        val serverJar = libsDir.resolve("server.jar")
        if (!serverJar.exists()) {
            throw GradleException("libs/server.jar não foi gerado por :app-dist:jar")
        }

        val javaExe = project(":game-server-core").extensions
            .getByType<JavaPluginExtension>().toolchain
            .let { tc ->
                val svc = project.extensions.findByType(org.gradle.jvm.toolchain.JavaToolchainService::class.java)
                    ?: project(":game-server-core").extensions.getByType(org.gradle.jvm.toolchain.JavaToolchainService::class.java)
                svc.launcherFor(tc).get().executablePath.asFile.absolutePath
            }

        val entry = "ext.mods.prepararteste.PrepararTesteEntry"
        val args = mutableListOf("-cp", "libs/server.jar", entry)
        if (project.hasProperty("start")) args.add("--start")
        if (project.hasProperty("noGui")) args.add("--no-gui")

        val cmd = listOf(javaExe) + args
        logger.lifecycle("[PrepararTeste] exec -> ${cmd.joinToString(" ")}")

        val proc = ProcessBuilder(cmd)
            .directory(rootProject.layout.projectDirectory.asFile)
            .inheritIO()
            .start()
        val code = proc.waitFor()
        if (code != 0) {
            throw GradleException("PrepararTesteEntry terminou com exit=$code")
        }
    }
}

gradle.projectsEvaluated {
    project(":app-dist").tasks.named("jar") {
        mustRunAfter(rootProject.tasks.named("clean"))
    }
}

tasks.register("buildWithoutMods") {
    group = "build"
    description = "Limpa tudo + rebuilda libs/server.jar (alias usado por gradlew.bat br-ant-dist-test)"
    dependsOn("clean")
    dependsOn(":app-dist:jar")
}
