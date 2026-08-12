# Coding Style Guide

This document outlines the coding style conventions for the Android Browser Helper project.

## Language

*   This project is primarily written in **Java**.
*   New code should be written in Java, matching the existing codebase.

## Style Guide

*   Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
*   Use 4 spaces for indentation (standard for Android development).

## Copyright Headers

Every new or modified Java source file must include the Apache 2.0 copyright header using line comments, matching the existing codebase style:

```java
// Copyright 2026 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
```


## Best Practices

*   **Imports**: Clean up unused imports. Do not use wildcard imports (e.g., `import java.util.*`).
*   **Nullability**: Use `@NonNull` and `@Nullable` annotations from `androidx.annotation` to assist with static analysis.
*   **Deprecation**: If using deprecated APIs, explain why in a comment and plan for migration if possible.
