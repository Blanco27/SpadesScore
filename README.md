# SpadesScore

SpadesScore is a simple Android app that takes the pen-and-paper work out of a Spades-style trick-prediction card game. Set up the table, let each player bid their tricks, and the app keeps every round's score for you – right on your phone!

## Features
- **3 or 4 players** – pick your table size when you start a new game.
- **Trick prediction built in** – each player declares how many tricks they expect to win before the round, then you simply confirm afterwards who was right.
- **Automatic scoring** – a correct prediction is worth the bid plus 5 points, and the app tracks the running total for every player.
- **Two halves with a half-time summary** – the card count ramps up over the first half and back down over the second, with a score overview at the break and a final results table at the end.
- **Random starting dealer** – optionally let the app pick who deals first.
- **English & German** – switch the language right from the start screen.
- **Light & dark theme** – follows your device's appearance.
- Clean, focused interface so you can keep your attention on the game instead of the maths.

## How a Game Works
1. Choose 3 or 4 players and your language.
2. Enter the player names and decide whether the starting dealer is picked at random.
3. Each round the app shows who deals and how many cards are in play.
4. Every player declares their predicted number of tricks (the table's bids can't add up to exactly the number of tricks available).
5. After the round, tick off who hit their prediction – scores update automatically.
6. At half-time you get a score summary; at the end of the game the full results table is shown.

## Screenshots

![SpadesScore](https://github.com/user-attachments/assets/f6c3dc0d-3f92-4467-8cc5-71fcf4755ced)

- Start screen
- Enter points after each round
- Half-time score
- Final score

## Getting Started
1. Download the current APK from the latest release.
2. Install it on your Android device (Android 7.0 / API 24 or newer).
3. Launch the app and start a new game.
4. Bid, confirm, and watch the totals update automatically as you play.

## Building from Source
SpadesScore is a standard Gradle Android project. With the Android SDK and a JDK 11+ installed:

```bash
./gradlew assembleDebug   # build a debug APK (output in app/build/outputs/apk/)
./gradlew installDebug    # install on a connected device or emulator
```

On Windows use `.\gradlew.bat` instead of `./gradlew`. The UI is built with XML layouts and view binding; the code is mostly Java with a little Kotlin.

## Roadmap
- [ ] Persist the current game so it survives closing the app.
- [ ] Support additional player counts.
- [ ] Add more languages.

## Contributing
Contributions are welcome! Feel free to open an issue or submit a pull request.

## License
SpadesScore is licensed under the [MIT License](LICENSE).

## Feedback
If you encounter any issues or have suggestions, please create an issue in this repository.

---
Enjoy your games with SpadesScore! Let us know how we can make it even better.
