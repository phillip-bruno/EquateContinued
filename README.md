# Equate Continued

A fast, open-source unit-converting calculator for Android. Type an expression, tap a source unit, tap a target unit, done.

**Example:** To convert 1/4 cup to tablespoons, type `1÷4`, press **cup**, then press **tbsp**.

## Features

- 15 unit categories with 430+ individual units, including length, weight, volume, temperature, energy, fuel economy, digital storage and currency
- 98 live currencies with rates from [FloatRates](https://www.floatrates.com/), plus cryptocurrency via [Coinpaprika](https://coinpaprika.com/)
- Historical USD inflation adjustment using CPI data (1913-2025)
- Scientific calculator with order of operations, parentheses, and exponents
- Instant result preview as you type
- Expression history with recall
- Material You (Material 3) theming with full light/dark mode support
- Portrait and landscape layouts
- Samsung Multi-Window support

## Requirements

| | Version |
|---|---|
| Android | 8.0+ (API 26) |
| Target SDK | 36 |
| JDK (build) | 21 |

## Building

```bash
git clone https://github.com/phillip-bruno/EquateContinued.git
cd EquateContinued
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/`.

For a signed release build (`./gradlew assembleRelease`), see [Release Signing](#release-signing).

## Creating a New Release

### 1. Bump the Version

Edit **`app/build.gradle`** and increment both version fields in `defaultConfig`:

```groovy
defaultConfig {
    ...
    versionCode XX          // increment by 1 each release
    versionName "2.2.XX"    // follow semantic versioning: MAJOR.MINOR.PATCH
}
```

Edit **`app/src/main/res/values/strings.xml`** and update the release notes:

```xml
<string name="whats_new">What is new in v2.2.XX</string>
<string name="version_description">Short summary of this release.</string>
```

### 2. Commit the Change

```bash
git add app/build.gradle app/src/main/res/values/strings.xml
git commit -m "Bump version to 2.2.XX (versionCode XX)"
git push origin master
```

### 3. Tag the Release

Push a tag that matches your `versionName`:

```bash
git tag v2.2.XX
git push origin v2.2.XX
```

The tag triggers the [CI/CD](#cicd) release build, which signs the APK and AAB and publishes a GitHub Release with auto-generated notes.

## Release Signing

One-time setup. The release build reads its signing config from environment variables:

| Variable | Default | Description |
|---|---|---|
| `KEYSTORE_FILE` | `release-key.jks` in the project root | Path to the keystore |
| `KEYSTORE_PASSWORD` | *(empty)* | Password for the keystore file |
| `KEY_ALIAS` | `equate` | Key alias |
| `KEY_PASSWORD` | *(empty)* | Password for the key |

### Keystore Setup

Generate a keystore if you don't have one:

```bash
keytool -genkeypair -v -keystore release-key.jks -alias equate \
  -keyalg RSA -keysize 2048 -validity 10000
```

### GitHub Secrets

For CI, encode the keystore:

```bash
base64 -w0 release-key.jks
```

Then add these secrets under **Settings → Secrets and variables → Actions**:

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded keystore (output of the command above) |
| `KEYSTORE_PASSWORD` | Password for the keystore file |
| `KEY_ALIAS` | Key alias (e.g. `equate`) |
| `KEY_PASSWORD` | Password for the key |

## CI/CD

GitHub Actions runs on every push and PR to `master`, and on `v*` tags:

| Trigger | Jobs |
|---|---|
| Push / PR to `master` | Build debug APK, run JVM unit tests, compile instrumented tests, upload artifacts |
| `v*` tag | All of the above + signed release APK & AAB + GitHub Release |

Workflow definition: [`.github/workflows/android.yml`](.github/workflows/android.yml)

## Project Structure

```
app/src/main/
  java/com/wolfcola/equatecontinued/
    Calculator.java          Core expression engine
    unit/                    Unit definitions and conversion logic
    unit/updater/            Currency rate fetchers (FloatRates, Coinpaprika)
    view/                    Activities, custom views, button management
  res/
    layout/                  Portrait layouts
    layout-land/             Landscape layouts
    drawable/                Button drawables, icons
    values/                  Light theme colours, styles, strings, dimensions
    values-night/            Dark theme colours
app/src/test/                JVM unit tests (./gradlew test)
app/src/androidTest/         Instrumented / Espresso tests (need a device or emulator)
```

## Contributing

Contributions are welcome. Please open an issue or pull request on GitHub.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

Originally created by Evan Respaut (2017). Continued and maintained by [Phillip Bruno](mailto:phillip.bruno@outlook.com).
