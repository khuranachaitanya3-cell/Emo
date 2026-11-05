# EmoBuddy — Android starter project

An EMO-inspired expressive face companion with Android speech recognition and cute robotic text-to-speech.

## What works in this starter
- Android speech-to-text using the device speech recognition service.
- Cute voice output using Android TextToSpeech (pitch raised slightly).
- On-screen emoji expressions for listening, thinking, speaking and idle.
- Optional hands-free loop: listen → reply → listen again.
- AI replies through a small private backend (the API key stays on the backend, not in the APK).

## Important: this is source code, not a compiled APK
This workspace does not have the Android SDK/Gradle installed, so an APK could not be built here. Open this project in Android Studio and build it there.

## Build the Android app
1. Install Android Studio on a computer and let it install Android SDK Platform 35.
2. Open this folder (`EmoBuddyAndroid`) in Android Studio.
3. Allow Gradle sync to finish.
4. Connect an Android phone/tablet with USB debugging enabled, or use an emulator.
5. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. Android Studio will show the generated APK location, normally `app/build/outputs/apk/debug/app-debug.apk`.

## Enable real AI conversation
The app uses a backend so you do not expose an AI secret key inside an APK.

1. On a computer, install Python 3.10+.
2. In the `backend` folder run:
   ```bash
   pip install -r requirements.txt
   ```
3. Set your API key as an environment variable (never paste it into the Android app):
   - Windows PowerShell: `$env:OPENAI_API_KEY="your_key"`
   - macOS/Linux: `export OPENAI_API_KEY="your_key"`
4. Start the backend:
   ```bash
   uvicorn server:app --host 0.0.0.0 --port 8000
   ```
5. Find the computer's local Wi-Fi IP address, e.g. `192.168.1.5`.
6. In EmoBuddy, enter `http://192.168.1.5:8000/chat` in the server URL field. Both devices must be on the same Wi-Fi network.

For a hosted HTTPS backend, enter its `/chat` URL instead. Do not expose an unauthenticated backend to the public internet.

## Notes
- Voice recognition quality and available languages depend on Android's speech service and installed language packs.
- Android TextToSpeech voices vary by device. Install/select a Hindi or English voice in Android settings if needed.
- This is a starter implementation. The face currently uses expressive emoji states, not a full 3D animated EMO model.
- Keep API keys private and ask a trusted adult to help configure any paid API service.
