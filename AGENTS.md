# AI Agent Harness for Android Browser Helper

This page defines the rules, skills, and prompts for AI agents working on this project.

## Rule Inheritance

This harness inherits from:
- [common.md](_agents/prompts/common.md)

## Common Directives

Instructions that are useful for Android Browser Helper development.

### Paths

* All files in the project's source can be read relative to the workspace root.

### Building

* Do not attempt a build without first establishing the correct target.
* Build the project using Gradle:
  * To build the entire project: `./gradlew build`
  * To build a specific module (e.g., `androidbrowserhelper`): `./gradlew :androidbrowserhelper:assembleDebug`

### Testing

* Run tests using Gradle:
  * To run all unit tests: `./gradlew test`
  * To run unit tests for a specific module: `./gradlew :androidbrowserhelper:test`
  * To run instrumentation tests (if emulator is available): `./gradlew connectedAndroidTest`

### Coding

* Stay on task: Do not address code health issues or TODOs in code unless it is required to achieve your given task.
* Add code comments sparingly: Focus on *why* something is done, not *what* is done.
* **Documentation**: Keep documentation fresh. Update Javadoc for public API changes, and update relevant markdown files (e.g., `README.md`, `docs/`) when changing behavior or APIs.

### Git Operations

* **Always branched:** Ensure you are not on the `main` branch if you are making commits. If you are, first do `git checkout -b {BRANCH_NAME}`.
* **Commit messages:**
  * Use active voice and avoid passive voice.
  * Use present tense or imperative mood (e.g., "Change foo" instead of "Changed foo").
  * Wrap the commit message at 72 characters when possible.

---

## Canonical Documentation

- [README.md](README.md)
- [docs/](docs/)

## Project Rules

Refer to the specific rules in `_agents/rules/`:
- [CODE_STRUCTURE.md](_agents/rules/CODE_STRUCTURE.md)
- [DEPENDENCIES.md](_agents/rules/DEPENDENCIES.md)
- [STYLE.md](_agents/rules/STYLE.md)
- [TESTING.md](_agents/rules/TESTING.md)
- [REVIEWS.md](_agents/rules/REVIEWS.md)
