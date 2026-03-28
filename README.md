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

## CI/CD

GitHub Actions runs on every push and PR to `master`:

1. Checkout + JDK 21 setup
2. Gradle build
3. Unit tests
4. Artifact upload (APK + test reports)

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
