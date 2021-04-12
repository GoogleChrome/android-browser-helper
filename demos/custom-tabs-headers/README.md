Custom Tabs Custom Headers Demo
===============================

This demo shows how to add custom headers to a web request when opening an URL with Custom Tabs.

Adding custom headers only works when the developer owns both the application using Custom Tabs and
the origin being opened, and can prove ownership via [Digital Asset Links][2].

Check out [this blogpost][1] for more details on how the implementation works and steps to setup
the Digital Asset Links validation.

:bangbang: **Important:** This project is using a demo page hosted at Glitch. However, the demo will
only work if the Digital Asset Links validation is successful. So, when testing on your computer,
remix the project at https://glitch.com/edit/#!/custom-tabs-custom-he, edit the file under
`public/.well-known/assetlinks.json` with your own SHA-256 fingerprint (use Tools > Terminal
to find and edit the file), and update the URL opened by the application.

[1]: https://developer.chrome.com/docs/android/custom-tabs/headers/
[2]: https://developers.google.com/digital-asset-links