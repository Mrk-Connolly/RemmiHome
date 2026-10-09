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
│       │       │   ├── ai/              # RemmiAiCapability, DefaultAiCapability, RemmiAiCommandValidator, DefaultAiCommandValidator
│       │       │   ├── android/         # Android capability contracts & implementations
│       │       │   │   ├── audio/       # RemmiAudioCapability, AndroidAudioCapability (AudioRecord PCM 16kHz capture)
│       │       │   │   ├── notification/# RemmiNotificationCapability, DefaultNotificationCapability (POST_NOTIFICATIONS check)
│       │       │   │   ├── google/      # RemmiGoogleAccount, RemmiGoogleAuthCapability, DefaultGoogleAuthCapability, RemmiGoogleServiceCapability, DefaultGoogleServiceCapability, RemmiCalendarEvent, RemmiGoogleCalendarCapability, DefaultGoogleCalendarCapability, RemmiDataCategory
│       │       │   │   ├── alarm/       # RemmiAlarmCapability, DefaultAlarmCapability, AlarmBootReceiver
│       │       │   │   ├── systemInfo/  # RemmiSystemInfo, RemmiSystemInfoCapability, AndroidSystemInfoCapability
│       │       │   │   └── launcher/    # RemmiAppInfo, RemmiLauncherCapability, AndroidLauncherCapability
│       │       │   ├── controller/      # RemmiController
│       │       │   ├── eventBus/        # RemmiEventBus
│       │       │   │   ├── commands/    # RemmiCommand, LaunchAppCommand, NavigateCommand, RemmiCommandResult
│       │       │   │   └── events/      # RemmiEvent
│       │       │   ├── host/            # RemmiHost
│       │       │   └── plugin/          # RemmiPluginRegistry
│       │       ├── ui/                  # MainActivity, RemmiApp, Theme
│       │       │   ├── home/            # HomeDestination, AppCategory, HomeScreen, AppsScreen, WorkspacesScreen launcher UI
│       │       │   ├── search/          # GlobalSearch domain model & search ranking
│       │       │   └── settings/        # SettingsScreen (Appearance, AI Services, Centralized Permissions)
│       │       └── plugins/             # RemmiPlugin contract & api
│       │           ├── RemmiPlugin.kt
│       │           ├── api/
│       │           │   └── RemmiPluginContext.kt
│       │           ├── planning/        # Planning Workspace plugin (PlanningPlugin, PlanningTaskRepository, PlanningWorkspaceScreen)
│       │           └── knowledge/       # Knowledge Workspace plugin (KnowledgePlugin, Lecture, LectureClass, RecordingState, ClassRepository, LectureRepository, LectureRecordingSession, LectureRecoveryManager, KnowledgeWorkspaceScreen, SubjectListScreen, AddSubjectScreen, SubjectLecturesScreen, SavedLectureDetailsScreen, LectureRecordingScreen, RecordingDiagnostics, LiveDiagnosticsPanel)
│       ├── test/
│       │   └── java/com/remmi/          # Automated unit tests
│       │       ├── core/                # RemmiEventBusTest, RemmiHostTest, RemmiPluginRegistryTest, CommandArchitectureTest
│       │       │   ├── ai/              # AiCapabilityBoundaryTest, AiCommandResolutionTest, AiExecutionSafetyTest
│       │       │   ├── controller/      # CommandRoutingTest
│       │       │   └── android/         # AndroidSystemInfoCapabilityTest, AndroidLauncherCapabilityTest, GoogleAuthCapabilityTest, GoogleServiceCapabilityTest, GoogleCalendarCapabilityTest, LocalFirstGoogleIntegrationTest
│       │       ├── ui/                  # HomeSearchTest, HomeLauncherFlowTest, HomePerformanceTest, AppCategoryTest, RecentAppsTest, FavoriteAppsResolutionTest, RemmiPreferencesTest, GlobalSearchTest, CommandIntegrationTest, WorkspacesNavigationTest, PermissionsSettingsTest
│       │       └── plugins/             # RemmiPluginTest, PlanningTaskRepositoryTest, PlanningPluginLifecycleTest, KnowledgePluginTest, ClassRepositoryTest, LectureRepositoryTest, LectureRecordingSessionTest, LectureRecoveryManagerTest, GeminiLiveTranscriptionTest, SubjectRedesignTest, SecurityAuditTest, KnowledgeIntegrationTest, KnowledgePerformanceTest, MicrophonePipelineTest, GeminiConfigTest, RecordingDiagnosticsTest
│       └── androidTest/
│           └── java/com/remmi/          # Automated Android Compose UI tests
│               └── ui/home/             # HomeScreenTest
├── build.gradle.kts                     # Root build configuration
├── settings.gradle.kts                  # Gradle settings & repositories
├── gradle.properties                    # JVM & AndroidX properties
├── local.properties                     # Android SDK path configuration
└── gradlew                              # Gradle wrapper script
```
