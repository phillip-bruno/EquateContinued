# Equate Continued

A fast, open-source unit-converting calculator for Android. Type an expression, tap a source unit, tap a target unit, done.

**Example:** To convert 1/4 cup to tablespoons, type `1÷4`, press **cup**, then press **tbsp**.

## Features

- 15 unit categories with 430+ individual units (length, weight, volume, temperature, pressure, energy, power, force, torque, speed, time, area, fuel economy, digital storage, currency)
- 98 live currencies with rates from [FloatRates](https://www.floatrates.com/) and [ECB](https://www.ecb.europa.eu/), plus cryptocurrency via Coinpaprika
- Historical USD inflation adjustment using CPI data (1913-2024)
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
| Target SDK | 35 |
| JDK (build) | 21 |

## Building

```bash
git clone https://github.com/phillip-bruno/EquateContinued.git
cd EquateContinued
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/`.

For a signed release build, supply your keystore via Gradle properties or environment variables, see the CI workflow below.

## Releasing

Releases are published automatically by pushing a version tag.

1) First change the `android:versionCode` and `android:versionName` in
   `app/src/main/AndroidManifest.xml`
2) Then change the `res/values/strings.xml` the `whats_new` version code to match the versionName
   from step 1

```bash
git tag v2.3.0
git push origin v2.3.0
```

The CI workflow will build a signed release APK and AAB, then create a GitHub Release with both files attached and auto-generated release notes from commit messages.

### Keystore Setup

The release build requires a signing keystore. Generate one if you don't have one:

```bash
keytool -genkeypair -v -keystore release-key.jks -alias equate \
  -keyalg RSA -keysize 2048 -validity 10000
```

Then encode it for use as a GitHub secret:

```bash
base64 -w0 release-key.jks
```

### GitHub Secrets

Add the following secrets to your repository (Settings → Secrets and variables → Actions):

| Secret | Description |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded keystore file (output of the command above) |
| `KEYSTORE_PASSWORD` | Password for the keystore file |
| `KEY_ALIAS` | Key alias (e.g. `equate`) |
| `KEY_PASSWORD` | Password for the key |

## CI/CD

GitHub Actions runs on every push and PR to `master`, and on `v*` tags:

| Trigger | Jobs |
|---|---|
| Push / PR to `master` | Build debug APK, run unit tests, upload artifacts |
| `v*` tag | All of the above + signed release APK & AAB + GitHub Release |

Workflow definition: [`.github/workflows/android.yml`](.github/workflows/android.yml)

## Project Structure

```
app/src/main/
  java/com/wolfcola/equatecontinued/
    Calculator.java          Core expression engine
    unit/                    Unit definitions and conversion logic
    unit/updater/            Currency rate fetchers (FloatRates, ECB, crypto)
    view/                    Activities, custom views, button management
  res/
    layout/                  Portrait layouts
    layout-land/             Landscape layouts
    drawable/                Button drawables, icons
    values/                  Light theme colours, styles, strings, dimensions
    values-night/            Dark theme colours
```

## Contributing

Contributions are welcome. Please open an issue or pull request on GitHub.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

Originally created by Evan Respaut (2017). Continued and maintained by [Phillip Bruno](mailto:phillip.bruno@outlook.com).
