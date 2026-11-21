# Signing Fruit Catcher

Use JDK 17, the Gradle wrapper, and Android SDK Platform 36. Keep the keystore and both passwords in an encrypted password manager and make a separate encrypted backup. The release key alias is `fruit-catcher-upload`. The private package contains the keystore and its credential file.

From a shell, set the path and read the passwords without echoing them or saving them in shell history.

```bash
export FRUIT_CATCHER_KEYSTORE=/absolute/path/to/fruit-catcher-release.jks
export FRUIT_CATCHER_KEY_ALIAS=fruit-catcher-upload
read -rsp 'Keystore password: ' FRUIT_CATCHER_STORE_PASSWORD; printf '\n'
read -rsp 'Key password: ' FRUIT_CATCHER_KEY_PASSWORD; printf '\n'
export FRUIT_CATCHER_STORE_PASSWORD FRUIT_CATCHER_KEY_PASSWORD
./gradlew :app:assembleRelease :app:bundleRelease
unset FRUIT_CATCHER_STORE_PASSWORD FRUIT_CATCHER_KEY_PASSWORD
```

For GitHub Actions, add encrypted repository secrets named `FRUIT_CATCHER_KEYSTORE_BASE64`, `FRUIT_CATCHER_STORE_PASSWORD`, `FRUIT_CATCHER_KEY_ALIAS`, and `FRUIT_CATCHER_KEY_PASSWORD`. Base64 only transfers bytes; repository secret storage protects the encoded keystore. Run the release workflow after committing the intended version. Preserve the same signing credentials for updates to directly distributed APKs.

Use Play App Signing for Google Play. Register the supplied upload certificate if Play Console requests it. Play-generated APKs may use a separate Google Play app-signing certificate. An APK signed with a different certificate cannot update an installed build.
