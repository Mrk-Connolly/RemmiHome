# Remmi Project Map

## Project Layout

```text
Remmi/
├── GEMINI.md                            # Root engineering principles & context
├── docs/                                # Project documentation
│   ├── ARCHITECTURE.md                  # System architecture and boundary definitions
│   ├── PROJECT_MAP.md                   # File map and directory ownership
│   └── DECISIONS.md                     # Architecture Decision Record
├── gradle/
│   ├── libs.versions.toml               # Dependency version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties   # Gradle wrapper configuration
├── app/
│   ├── build.gradle.kts                 # Application build configuration
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml      # App manifest with HOME intent filter
│       │   └── java/com/remmi/
│       │       ├── core/                # RemmiApplication
│       │       │   ├── android/         # Android capability contracts & implementations
│       │       │   │   ├── systemInfo/  # RemmiSystemInfo, RemmiSystemInfoCapability, AndroidSystemInfoCapability
│       │       │   │   └── launcher/    # RemmiAppInfo, RemmiLauncherCapability, AndroidLauncherCapability
│       │       │   ├── controller/      # RemmiController
│       │       │   ├── eventBus/        # RemmiEventBus
│       │       │   │   ├── commands/    # RemmiCommand, LaunchAppCommand
│       │       │   │   └── events/      # RemmiEvent
│       │       │   ├── host/            # RemmiHost
│       │       │   └── plugin/          # RemmiPluginRegistry
│       │       ├── ui/                  # MainActivity, RemmiApp, Theme
│       │       │   ├── home/            # HomeDestination, AppCategory, HomeScreen, AppsScreen launcher UI
│       │       │   └── search/          # GlobalSearch domain model & search ranking
│       │       └── plugins/             # RemmiPlugin contract & api
│       │           ├── RemmiPlugin.kt
│       │           └── api/
│       │               └── RemmiPluginContext.kt
│       ├── test/
│       │   └── java/com/remmi/          # Automated unit tests
│       │       ├── core/                # RemmiEventBusTest, RemmiHostTest, RemmiPluginRegistryTest, CommandArchitectureTest
│       │       │   └── android/         # AndroidSystemInfoCapabilityTest, AndroidLauncherCapabilityTest
│       │       ├── ui/                  # HomeSearchTest, HomeLauncherFlowTest, HomePerformanceTest, AppCategoryTest, RecentAppsTest, FavoriteAppsResolutionTest, RemmiPreferencesTest, GlobalSearchTest
│       │       └── plugins/             # RemmiPluginTest
│       └── androidTest/
│           └── java/com/remmi/          # Automated Android Compose UI tests
│               └── ui/home/             # HomeScreenTest
├── build.gradle.kts                     # Root build configuration
├── settings.gradle.kts                  # Gradle settings & repositories
├── gradle.properties                    # JVM & AndroidX properties
├── local.properties                     # Android SDK path configuration
└── gradlew                              # Gradle wrapper script
```
