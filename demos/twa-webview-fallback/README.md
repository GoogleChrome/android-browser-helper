# TWA WebView Fallback

When a user starts a Trusted Web Activity (TWA) based application, a browser that
supports TWAs may not be available on the device. In other cases, starting the TWA itself may fail.

In those situations, the default implementation will fallback to opening the URL in a Custom Tab or
the default browser, if a browser that supports Custom Tabs is not found.

The current fallback approach may be undesirable, a it breaks the full-screen experience for users.

This demo aims to provide an alternative fallback approach, that opens the content in a full-screen
WebView, and handles TWA specific situations like:

- Correctly handling the user leaving the verified origin and returning through the back button or
a link
- Applying the customizations for the TWA, like the status bar color and toolbar color in the
fallback experience. 
 

**Warning:** This demo is still under development and experimental, and does not cover many of the
important user-journeys necessary to fulfill the requirements for a good WebView fallback. Please,
file issues for missing features on the demo.
