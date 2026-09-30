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
│       │   ├── AndroidManifest.xml      # App manifest
│       │   └── java/com/remmi/
│       │       ├── core/                # RemmiApplication
│       │       ├── ui/                  # MainActivity, RemmiApp, Theme
│       │       └── plugins/             # Feature plugin extensions
│       └── test/
│           └── java/com/remmi/          # Automated unit tests
├── build.gradle.kts                     # Root build configuration
├── settings.gradle.kts                  # Gradle settings & repositories
├── gradle.properties                    # JVM & AndroidX properties
├── local.properties                     # Android SDK path configuration
└── gradlew                              # Gradle wrapper script
```
