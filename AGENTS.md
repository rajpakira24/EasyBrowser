# Repository Guidelines

## Project Structure & Module Organization

Easy Browser is a single-module Android project. The root contains Gradle wrapper files, shared Gradle configuration, and `settings.gradle`, which includes only `:app`. Application code lives in `app/src/main/java/com/webstudio/easybrowser`, organized by responsibility: `ui/activity`, `adapters`, `database`, `repository`, `managers`, `models`, and `receivers`. Android resources are in `app/src/main/res`, with layouts in `layout`, drawables in `drawable`, menus in `menu`, and app values in `values` and `values-night`. Unit tests belong in `app/src/test/java`; instrumented Android tests belong in `app/src/androidTest/java`.

## Build, Test, and Development Commands

Use the Gradle wrapper from the repository root:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lintDebug
.\gradlew.bat clean
```

`assembleDebug` builds the debug APK with the `.debug` application ID suffix. `testDebugUnitTest` runs JVM unit tests. `connectedDebugAndroidTest` requires a connected emulator or device. `lintDebug` runs Android lint checks. `clean` removes generated build output.

## Coding Style & Naming Conventions

This codebase uses Java 11 and Android XML resources. Keep Java indentation at four spaces and follow Android Studio defaults. Name classes in `PascalCase` (`BookmarkRepository`, `BrowserActivity`), methods and fields in `camelCase`, constants in `UPPER_SNAKE_CASE`, and resources in lowercase `snake_case` (`activity_browser.xml`, `ic_download.xml`). Keep new code within the existing package structure and prefer repository/manager classes for business logic instead of placing it directly in activities.

## Testing Guidelines

Use JUnit for local unit tests under `app/src/test/java` and AndroidX Test/Espresso for instrumented tests under `app/src/androidTest/java`. Name test classes after the subject under test, for example `BookmarkRepositoryTest` or `BrowserActivityTest`. Add focused tests for database, repository, and UI behavior when changing those areas, then run the matching Gradle test command before submitting changes.

## Commit & Pull Request Guidelines

No Git history is available in this checkout, so use concise imperative commit messages such as `Add bookmark sorting` or `Fix download notification handling`. Pull requests should describe the user-visible change, list the Gradle commands run, link related issues, and include screenshots or screen recordings for UI changes.

## Security & Configuration Tips

Do not commit personal signing keys or real release passwords. `local.properties` and generated `build/` output should stay local. Treat `app/google-services.json` as environment-specific Firebase configuration and update it intentionally when changing Firebase projects.
