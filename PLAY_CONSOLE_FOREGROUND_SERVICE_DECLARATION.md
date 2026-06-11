# Play Console Foreground Service Declaration

## Foreground Service Type

`mediaPlayback`

## Feature

Background Play for web media in Easy Browser.

## User-Facing Purpose

Easy Browser uses a foreground media playback service only when the user plays
audio or video in the browser and has enabled Background Play. The service keeps
Android media controls visible in the notification shade and lock screen, and
lets the user play or pause the active web media without reopening the browser.

## Justification

This is a user-initiated media playback feature. The foreground service starts
only while browser media is active, publishes a visible media notification, and
stops when playback stops, the media session ends, Background Play is disabled,
or the browser activity is destroyed.

## Manifest Coverage

The app declares:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

<service
    android:name=".managers.BackgroundMediaService"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />
```
