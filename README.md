# assaee-mobile

Flutter app for [As-Sa'ee](https://github.com/as-saee/assaee-docs): a personal life OS built in public as a
DevOps / cloud-architect portfolio. This repository began as **Readflow**, a PDF reader whose home-screen widget lets
you read a book without opening the app. It becomes the reader module of As-Sa'ee and, later, the mobile client for
the other modules.

## Status
Early work in progress. The Android app and the reading widget exist; the widget has known problems, diagnosed in
[docs/reader-roadmap.md](docs/reader-roadmap.md), and has not been verified on a device yet. The roadmap and the
architecture decisions are in [assaee-docs](https://github.com/as-saee/assaee-docs).

## Layout
- `lib/main.dart`: the Flutter app (a single file for now).
- `android/app/src/main/kotlin/com/readflow/app/`: the native home-screen widget.
- `docs/reader-roadmap.md`: the original reader and widget plan.

## Development
Requires the Flutter SDK and, for Android builds, JDK 17 and the Android SDK.

```bash
flutter pub get
flutter analyze
flutter test
flutter run
```

## Related repositories
[assaee-api](https://github.com/as-saee/assaee-api) ·
[assaee-infra](https://github.com/as-saee/assaee-infra) ·
[assaee-gitops](https://github.com/as-saee/assaee-gitops) ·
[readflow-web](https://github.com/as-saee/readflow-web) (the original React reader, archived)

## License
Apache License 2.0, see [LICENSE](LICENSE).
