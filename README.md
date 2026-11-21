# Fruit Catcher

Fruit Catcher is a native offline Android orchard arcade game. Drag the basket across the screen to collect falling fruit. Golden fruit is worth 100 points. Each caught bomb costs one of three hearts. The game speeds up with survival time, and the device remembers the highest score and audio settings.

| Release configuration | Value |
| --- | --- |
| Application ID | `com.fruitcatcher.fruit` |
| Version | `1.0.0` (1) |
| Minimum SDK | 26 |
| Compile and target SDK | 36 |
| Category and orientation | Arcade game, portrait |

## Open and build

Open this folder in Android Studio with JDK 17 and Android SDK Platform 36. Let Gradle sync with the supplied wrapper. Run `./gradlew :app:assembleDebug :app:testReleaseUnitTest :app:lintRelease` to build and check the project without an emulator.

See [Signing instructions](signing-instructions.md) for signed builds and future updates. Run `./gradlew :app:assembleRelease :app:bundleRelease` after configuring the signing environment. The APK and AAB are in `app/build/outputs`. Increment the version code for every Google Play update.

## Architecture

Kotlin advances the simulation at 120 fixed steps per second. The hardware-accelerated Android Canvas is hosted inside Compose and draws cached bitmaps. Falling objects, particles, and floating labels use fixed-size pools. Compose tracks menus and changes to score and hearts. Activity pause freezes the session and audio. A swept crossing test handles collisions at the basket opening.

The launcher uses the supplied reference artwork. The Play assets are in `store/`. Orchard and sprite artwork were created for the game. The original synthesized audio is checked into resources, with its renderer in `tools/create_audio.py`.

## Google Play

The manifest requires touchscreen input and declares portrait hardware optional. The activity requests portrait and the app declares the game category. Interactive overlays respect safe insets. The release has no permission declarations. Included AndroidX native libraries are checked for 16 KB alignment. Review the signed bundle in Play Console Device Catalog to confirm account-level exclusions. See [Privacy Policy](store/privacy-policy.html).
