# Code Structure

This document outlines the directory structure and modules of the Android Browser Helper project.

## Modules

The project is divided into the following modules:

*   **[:androidbrowserhelper](../../androidbrowserhelper)**: The core library containing helper classes for Custom Tabs and Trusted Web Activities (TWA).
    *   `src/main/java`: Source code for the core library.
    *   `src/test/java`: Robolectric unit tests.
    *   `src/androidTest/java`: Android instrumentation tests.
*   **[:locationdelegation](../../locationdelegation)**: An optional library to delegate location permission requests from the TWA to the Android app.
*   **[:playbilling](../../playbilling)**: An optional library to enable Google Play Billing inside TWAs.
*   **[:demos](../../demos)**: A collection of demo applications demonstrating various features of the library.

## Key Classes in `:androidbrowserhelper`

*   **[`LauncherActivity`](../../androidbrowserhelper/src/main/java/com/google/androidbrowserhelper/trusted/LauncherActivity.java)**: Entry point activity for launching a TWA.
*   **[`TwaLauncher`](../../androidbrowserhelper/src/main/java/com/google/androidbrowserhelper/trusted/TwaLauncher.java)**: Handles the complexity of connecting to the Custom Tabs service and launching the TWA.
*   **[`TwaProviderPicker`](../../androidbrowserhelper/src/main/java/com/google/androidbrowserhelper/trusted/TwaProviderPicker.java)**: Chooses the best browser on the device to launch the TWA.
