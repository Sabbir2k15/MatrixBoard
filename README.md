# MatrixBoard — Glass Avro Keyboard for Android

**MatrixBoard** is a colourful, glass-style Android keyboard with:
- **English QWERTY** typing
- **Bangla Avro (phonetic)** typing — type in Roman letters, get Bangla. Example: `ami` → `আমি`, `tumi` → `তুমি`, `bhalo` → `ভালো`, `bangla` → `বাংলা`, `amar` → `আমার`
- **Glass UI + Matrix colours** — the exact vibe from the screenshot: yellow / blue / black / white / red keys, rounded, semi-transparent, with a soft white border, on a dark glass background
- **4 mechanical key sounds** — Clicky, Thock, Linear, Pop (synthesised in code, no audio files needed, selectable in Settings)
- Vibration toggle, Shift for Avro capitals (`T` = ট, `D` = ড, `S` = ষ, `N` = ণ, etc.)

## Name
MatrixBoard — "Matrix" diye naam, keyboard er jonno.

## How to build (Android Studio)
1. Open this folder in Android Studio (Hedgehog or newer).
2. Let Gradle sync. `minSdk 24`, `targetSdk 34`, Kotlin.
3. Run on a phone / emulator, or Build > Generate Signed App Bundle / APK > APK > debug.
4. Install the APK on your phone.

## How to enable on your phone
1. Open the **MatrixBoard Settings** app.
2. Tap **Open Keyboard Settings** → turn ON **MatrixBoard**.
3. Tap **Choose Keyboard** (or: any text field → keyboard icon in the navigation bar) → select **MatrixBoard**.
4. On the keyboard itself, tap **EN / BN** to switch English ↔ Bangla Avro.

## Using Avro mode
- Type Roman letters normally. Bangla appears as composing text while you type.
- Press **Space** to confirm the word, **Backspace** to delete the last Roman letter.
- Capital letters matter in Avro: `t` = ত, `T` = ট, `d` = দ, `D` = ড, `s` = স, `S` = ষ, `sh` = শ, `n` = ন, `N` = ণ, `rri` = ঋ.
- Common words have exact overrides so `ami`, `tumi`, `amar`, `kemon`, `acho`, `bhalo`, `bangla`, `dhonnobad` come out perfectly. Other words use the general Avro phonetic rules (consonant + vowel sign, `্` for conjuncts when two consonants meet).

## Sounds
Settings → Sound profile:
1. **Clicky** — sharp, high click (like blue switches)
2. **Thock** — deep, soft thock (like lubed switches)
3. **Linear** — quiet, soft tap
4. **Pop** — short bubble pop

Sounds are generated with `AudioTrack` in `MechanicalSound.kt`, so the APK stays small and needs no `.wav` files.

## Project structure
```
app/src/main/java/com/matrixboard/keyboard/
  MatrixKeyboardService.kt  — InputMethodService, glass colourful keys, EN/BN/symbol layouts
  AvroTransliterator.kt     — Roman → Bangla phonetic engine + common-word overrides
  MechanicalSound.kt        — 4 synthesised mechanical sounds
  SettingsActivity.kt       — sound / vibration / enable-keyboard screen
app/src/main/res/xml/method.xml
app/src/main/AndroidManifest.xml
```

## Notes / limits (honest)
- This is a full working keyboard, not a theme for Gboard. It works in any app that accepts text input.
- Avro here is a clean-room phonetic implementation, not the official Avro Keyboard code. Very long conjunct words may differ slightly from desktop Avro — the common-word list covers the everyday words first.
- Glass blur on the keyboard background uses a translucent dark panel (true backdrop blur needs Android 12+ window blur APIs and varies by phone, so the panel is kept translucent + bordered to look glassy on every phone).
- No internet, no ads, no keylogging: the service only sends keystrokes to the currently focused text field, exactly like any keyboard must, and stores only your sound/vibration choice in SharedPreferences.
