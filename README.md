# The Matrix Background

A faithful Kotlin/Compose Multiplatform port of the classic GLMatrix screensaver from the XScreenSaver project by Jamie Zawinski.

This implementation renders the iconic "digital rain" effect from The Matrix films using Compose Canvas, supporting both Desktop (JVM) and Web (WebAssembly) platforms.

## Features

- Faithful conversion of the original GLMatrix algorithm
- Compose Multiplatform support (Desktop + Web)
- Configurable animation parameters:
  - Speed control
  - Density (number of falling strips)
  - Fog effect (depth-based brightness)
  - Brightness waves
  - Auto-rotating camera view
  - Multiple character modes (Matrix, DNA, Binary, Hexadecimal, Decimal)

## Building

### Prerequisites

- JDK 17 or later
- Gradle 8.x (wrapper included)

### Desktop Application

```bash
# Run the desktop application
./gradlew :composeApp:run

# Build a distributable package
./gradlew :composeApp:packageDistributionForCurrentOS
```

### Web Application (WebAssembly)

```bash
# Run the development server
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Build production distribution
./gradlew :composeApp:wasmJsBrowserDistribution
```

The production files will be in `composeApp/build/dist/wasmJs/productionExecutable/`.

## Usage

### As a Composable

```kotlin
import com.matrix.background.MatrixBackground
import com.matrix.background.MatrixMode

@Composable
fun MyScreen() {
    MatrixBackground(
        modifier = Modifier.fillMaxSize(),
        speed = 1.0f,           // Animation speed multiplier
        density = 20f,          // Number of falling strips
        doFog = true,           // Enable depth-based fog
        doWaves = true,         // Enable brightness waves
        doRotate = true,        // Enable auto-rotation
        mode = MatrixMode.MATRIX // Character mode
    )
}
```

### Character Modes

- `MatrixMode.MATRIX` - Half-width Katakana characters (classic Matrix look)
- `MatrixMode.DNA` - DNA bases (A, C, G, T)
- `MatrixMode.BINARY` - Binary digits (0, 1)
- `MatrixMode.HEXADECIMAL` - Hexadecimal digits (0-9, A-F)
- `MatrixMode.DECIMAL` - Decimal digits (0-9)

## Testing

### Puppeteer Test (Web)

```bash
cd puppeteer-test
npm install
npm test
```

This will:
1. Start a local server with the web build
2. Launch a headless browser
3. Capture screenshots of the animation
4. Verify the canvas is rendering properly

## Project Structure

```
TheMatrixBackground/
├── composeApp/
│   └── src/
│       ├── commonMain/kotlin/com/matrix/background/
│       │   ├── MatrixBackground.kt    # Main Composable
│       │   ├── MatrixConstants.kt     # Constants and encodings
│       │   ├── MatrixGlyphs.kt        # Unicode character mappings
│       │   ├── MatrixState.kt         # Animation state and logic
│       │   └── Strip.kt               # Strip data structure
│       ├── desktopMain/kotlin/        # Desktop entry point
│       └── wasmJsMain/                # Web entry point
├── puppeteer-test/                    # Automated browser testing
└── build.gradle.kts
```

## Attribution

This is a port of [GLMatrix](https://www.jwz.org/xscreensaver/) from the XScreenSaver collection.

Original GLMatrix:
- Copyright (c) 2003-2018 Jamie Zawinski
- Licensed under a permissive license allowing use, copy, modify, and distribute

The original source code can be found at:
- Official site: https://www.jwz.org/xscreensaver/
- GitHub mirror: https://github.com/Zygo/xscreensaver

## License

This Kotlin port follows the same permissive license as the original GLMatrix:

Permission to use, copy, modify, distribute, and sell this software and its documentation for any purpose is hereby granted without fee, provided that the above copyright notice appear in all copies. No representations are made about the suitability of this software for any purpose. It is provided "as is" without express or implied warranty.
