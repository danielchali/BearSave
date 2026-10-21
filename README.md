# BearTube

BearTube is a libre, privacy-friendly streaming and downloading app for Android. It is currently in beta, so bugs may occur. Please report problems in the project issue tracker.

BearTube is a standalone BearTube application using open media-extraction components and is developed and distributed under the BearTube name. It does not require an account or Google Play Services for its core functionality.

## Features

- Watch videos at resolutions up to 4K.
- Listen to audio in the background.
- Use popup and picture-in-picture playback.
- Watch live streams and browse channels, playlists, and albums.
- Search videos, audio, channels, playlists, and comments.
- Subscribe to channels and receive notifications for new content.
- Create channel groups, local playlists, and feeds.
- View and search watch history.
- Download videos, audio, and subtitles.
- Handle age-restricted material where supported.

## Supported services

BearTube supports YouTube, YouTube Music, PeerTube, Bandcamp, SoundCloud, and media.ccc.de through its media-extraction engine. Support can vary by service and may change when a service changes its website or API.

## Building

Clone the repository, change into the project directory, and assemble a debug APK with Gradle:

```text
git clone https://github.com/danielchali/BearSave.git
cd BearSave
./gradlew assembleDebug
```

The project also contains shared and iOS code. Use the platform-specific build tools and configurations for those targets.

## Contributing

Contributions are welcome, including bug reports, translations, documentation, design improvements, testing, and code. Please describe the problem or proposed change clearly and include reproduction steps when reporting a bug.

## Privacy

BearTube does not collect data without your consent. Network requests are made directly to supported services or their public interfaces.

## License

BearTube is free software released under the GNU General Public License version 3 or later. See the LICENSE file for the complete license text.
