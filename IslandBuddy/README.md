# Island Buddy 2.0

Android companion overlay for Honor X8b / Android 14. It adds a customizable animated camera island and a voice/text assistant.

## Features
- Floating island overlay above other apps.
- Touch editing inside the app's Honor phone preview: drag the island to reposition it, or drag the eight round handles to resize it.
- Direct drag on the live overlay to move it after enabling it.
- Robot, cat and alien faces; seven eye colors; five accessories.
- Six eye animation styles plus a master animation switch.
- Tap the live island to open the assistant.
- Russian voice input using Android speech recognition and spoken replies using Android Text-to-Speech.
- A white reply card appears near the bottom of the screen above other apps, with a close button.
- Local demo responses when no API key is configured.
- Optional OpenAI-compatible HTTPS Chat Completions endpoint with a connection test.
- The API key is stored encrypted using Android Keystore.

## Build
The standalone Gradle project is in this folder. CI builds a debug APK for Android 8.0+ (API 26+) with target API 34.

## First launch
1. Install and open Island Buddy.
2. In the preview, drag the island or its round handles to set its position and size.
3. Choose eyes, color, animation and an accessory.
4. Tap "Включить островок поверх приложений" and grant overlay permission.
5. Tap the live island to open the assistant. Speech recognition asks for microphone permission only after tapping the voice button.

A real AI response requires an API provider account, a valid HTTPS endpoint, a model available to that account, and a private API key. The app does not ship with a key. Without one, the app clearly remains in local demo mode.
