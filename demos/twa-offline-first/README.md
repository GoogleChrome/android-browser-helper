# Offline First TWA Demo

This demo shows how to build a TWA that renders an offline screen when there's no connectivity, even the first time the user opens the app (when the the service worker hasn't been registered yet).

To achieve that, the app creates a custom Activity that checks connection and decides to render an offline screen or launch the TWA, depending on the network status.
The app only remembers a successful launch once the browser reports a finished page load through the `CustomTabsCallback`, so a first launch that is interrupted before the page (and its service worker registration) loaded still shows the offline screen next time.
