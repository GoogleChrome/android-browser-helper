# Offline First TWA Demo

This demo shows how to build a TWA that shows an offline screen when there's no connectivity, even the first time the user opens the app (whe the the service worker hasn't been registered yet).

The app creates a custom Activity that checks connection and decides to show an offline screen or launch the TWA, depending on the network status.