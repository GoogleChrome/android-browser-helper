# Trusted Web Activity / Location Delegation Demo

This demo application shows how to make the TWA app use Android's location permission instead of
the browser's permission:
1. Add the location delegation dependency to build.gradle.
2. Create an ExtraFeaturesService that extends DelegationService, register
LocationDelegationExtraCommandHandler in its constructor.
3. Add a reference to ExtraFeaturesService to the AndroidManifest.xml.
