# Migrate Digital Goods API to Google Play Billing Library 8

Google Play has announced that starting **August 31, 2026**, all new apps and app updates must use Play Billing Library (PBL) version 8 or newer. To support this, Android Browser Helper's Play Billing module has been updated to `1.2.0`, which uses Play Billing Library 8.3.0.

While existing apps already published with PBL 7 will continue to function after the deadline, you will not be able to publish any new app updates to the Play Store until you migrate. (An extension path until November 1, 2026 is available via the Play Console for affected apps).

## Who is affected?

Any Trusted Web Activity (TWA) app that uses Google Play Billing through the Android Browser Helper (ABH) and the Digital Goods API is affected. Developers using these tools must update their dependencies and rebuild their Android package to continue publishing updates.

## How to update

Depending on how your project was generated, follow the appropriate update path below:

### Bubblewrap users

If you originally generated your TWA using [Bubblewrap](https://github.com/GoogleChromeLabs/bubblewrap), the easiest way to update is to upgrade your Bubblewrap CLI and regenerate the project.

Bubblewrap version `1.25.0` updates the default billing dependency to `1.2.0` (and also bumps the `targetSdkVersion` to 36).

1. Update your Bubblewrap CLI:
   ```bash
   npm i -g @bubblewrap/cli@1.25.0
   ```
2. Navigate to your TWA project directory and update the project:
   ```bash
   cd my-twa-project
   bubblewrap update
   ```
   *Note: `bubblewrap update` may overwrite manual modifications you have made to the generated Android project files.*
3. Rebuild your app to generate the new APK/AAB:
   ```bash
   bubblewrap build
   ```

### Android Browser Helper users

If you manually maintain your TWA Android project, you only need to update the billing artifact version in your `build.gradle` (or `build.gradle.kts`) file.

Locate your Play Billing dependency and update it from `1.1.0` to `1.2.0`:

```gradle
dependencies {
    // Update this line
    implementation("com.google.androidbrowserhelper:billing:1.2.0")
}
```

*Note: The `billing` artifact has its own versioning, separate from the core `androidbrowserhelper` artifact.*

After updating the dependency, rebuild and publish your new Android App Bundle.

## Changes to Digital Goods API behavior

The underlying transition from Play Billing Library 7 to 8 forced two significant changes to how Android Browser Helper implements the Digital Goods API (DGAPI). Please note that the DGAPI web specification has not changed, but the data returned by the Android provider has.

### `listPurchaseHistory()`

PBL 8 removed the underlying `queryPurchaseHistoryAsync()` API. To prevent existing web code from crashing, Android Browser Helper 1.2.0 leaves the DGAPI command intact but makes it a no-op.

When your JavaScript calls:
```javascript
const history = await service.listPurchaseHistory();
console.log(history); // []
```
It will now always return an empty list (`[]`).

**Action required:** If your app relies on purchase history, you must [track historical purchases on your own backend server](https://developer.android.com/google/play/billing/query-purchase-history#track_historical_purchases), as recommended in the official Google Play Billing documentation.

### `getDetails()` for subscriptions

Google Play's subscription model is hierarchical (Subscriptions > Base Plans > Offers), but the DGAPI specification expects a single flat structure (`ItemDetails`). Because of this mismatch, Android Browser Helper now performs a **lossy conversion** for subscriptions and returns the **first eligible element**. 

*Note: Previously, developers could mark a specific base plan or offer as **backward-compatible**, and the legacy `querySkuDetailsAsync()` API would respect that designation. This is no longer the case: the library now returns the first eligible base plan or offer it finds.*

**Action required:** Because DGAPI can represent only one flat subscription configuration per product, ABH exposes only one of the eligible Play subscription plans/offers returned for that product. Other base plans and offers aren't represented through `getDetails()`. If you use multiple base plans or offers per subscription, be aware that you **cannot currently inspect all of them** via DGAPI.
