# Offline First TWA Demo

This demo shows how to build a TWA that renders an offline screen when there's no connectivity, even the first time the user opens the app (when the the service worker hasn't been registered yet).

To achieve that, the app creates a custom Activity that checks connection and decides to render an offline screen or launch the TWA, depending on the network status.