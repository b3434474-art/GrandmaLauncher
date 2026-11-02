# GrandmaLauncher

A simple, customizable Android home-screen launcher designed around Grandma's preferences.

## Design
- Familiar Android/Samsung-style home screen
- Black and crimson rose-inspired app icon theme
- Large clock, date, and weather
- Existing wallpaper supplied locally by the device owner
- Searchable app drawer
- Shortcuts for YouTube, Messages, Phone, Camera, Photos, Calendar, Settings, and Calculator
- No emergency tile and no family-contact section

## Weather
Uses Open-Meteo (no API key required) and Android's foreground location permission. Location is requested only while the launcher is in use. If permission is denied, weather shows an unavailable state rather than blocking the launcher.

Open-Meteo: https://open-meteo.com/

## Build
Open in Android Studio and allow Gradle sync. GitHub Actions builds a debug APK on pushes to `main`.

## Wallpaper
Grandma's personal wallpaper is not bundled or uploaded. The launcher will use the photo already set on her phone.
