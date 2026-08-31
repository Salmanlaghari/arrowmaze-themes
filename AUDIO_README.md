# Audio assets

The WAV files in `app/src/main/res/raw/` are **placeholder sounds** generated programmatically
as simple sine-wave tones (see `scripts/generate_sounds.py`). They are intentionally
minimal and exist so the SoundManager wiring is testable end-to-end.

Replace them with real royalty-free sound effects before shipping:

- `correct_move.wav` — short positive blip
- `wrong_move.wav` — short negative buzz
- `level_complete.wav` — short celebratory chime
- `button_tap.wav` — subtle UI click
