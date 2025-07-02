## 2.6.2

* [#520](https://github.com/GoogleChrome/android-browser-helper/pull/520). The TWA launcher will
no longer wait for the animation to finish before launching the Custom Tabs Activity, which
improves launch time. This change does not affect the user-visible launch animation.

This behavior can be reverted by add following lines in TWA app's AndroidManifest.xml inside
the application tag: 
```
<meta-data
    android:name="android.support.customtabs.trusted.START_CHROME_BEFORE_ANIMATION_COMPLETE"
    android:value="false"
/>
```

## 1.1.0 (2020-01-22)

* [#53](https://github.com/GoogleChrome/android-browser-helper/pull/53) Provide an Intent to the 
  browser to focus the TWA. #53 ([@PEConn](https://github.com/PEConn))
  
## 1.0.0 (2020-01-08)

* android-browser-helper is stable :rocket:
* Uses androidx.browser.1.2.0
* Added Dark Mode Support for the Navigation bar and Status bar