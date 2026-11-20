# Dependency Management

This document outlines the policy for managing dependencies in the Android Browser Helper project.

## Core Goal: Keep the Library Lightweight

Android Browser Helper is a library that other Android applications depend on. To minimize the impact on the size of the consuming applications (the "app size"), we must keep our dependency footprint as small as possible.

## Encouraged Dependencies

*   **AndroidX libraries**: Prefer using official AndroidX libraries (e.g., `androidx.core`, `androidx.browser`) for compatibility and standard functionality.
*   **Existing dependencies**: Reuse existing dependencies listed in `gradle/libs.versions.toml` (e.g., Guava, though we should use it sparingly if standard Java alternatives exist).

## Banned / Discouraged Dependencies

*   **No New External Libraries**: Do not add new external libraries (libraries not currently in `libs.versions.toml`) without a strong justification and approval from maintainers.
*   **Kotlin (in core library)**: The core `:androidbrowserhelper` module is currently pure Java. Avoid adding Kotlin dependencies to the core library to prevent transitive dependency issues for pure Java apps using this library. (Kotlin is acceptable in demos or optional modules if justified, but currently not used).
*   **Heavy Frameworks**: Avoid adding large frameworks that significantly increase the binary size.
