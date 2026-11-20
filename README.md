# MUISTA! – Android-muistutussovellus

**MUISTA!** on suomenkielinen Android-sovellus (Kotlin + Jetpack Compose), jonka avulla voit luoda viikoittaisia ja kuukausittaisia äänellä ja puheella (TTS) toimivia muistutuksia.

## Ominaisuudet
- **PÄÄLLÄ / POIS -painike**: Pääkytkin yläpalkissa sekä oma PÄÄLLÄ / POIS -painike jokaiselle muistutukselle.
- **VIIKKO MUISTUTUS / KUUKAUSI MUISTUTUS -painike**: Valitse toistuuko muistutus viikoittain vai kuukausittain.
- **Mitä pitää muistaa?**: Tekstikenttä muistutuksen aiheelle.
- **Mitä puhelin sanoo muistutushetkenä / hälytysääni**: Tekstikenttä suomenkieliselle puhesynteesille (TTS), valinta puheen/hälytysäänen välillä, hälytysäänen valitsin sekä **Testaa ääni** -painike.
- **Päivät (Ma, Ti, Ke, To, Pe, La, Su)**: Rastita viikonpäivät, joina muistutus tulee (tai kuukauden päivä 1–31 kuukausimuistutuksessa).
- **Kellonaika**: 24 tunnin kellonajan valitsin.
- **VOLUME-liukusäädin**: Äänenvoimakkuus 0–100 %.
- **Useita muistutuksia**: Lisää niin monta muistutusta kuin haluat, myös samalle päivälle.
- **Sovelluksen kuvake**: Mukana toimitettu MUISTA!-kuva on muunnettu kaikkiin `mipmap-*`-resoluutioihin ja `drawable/muista_logo.png`-tiedostoksi.

## Kääntäminen ja asennus Android-puhelimeen
1. Avaa Android Studio ja valitse **Open** → `C:\Users\Marko Uusivuori\.gemini\antigravity\scratch\MuistaApp`.
2. Anna Gradlen synkronoida projekti.
3. Kytke Android-puhelin USB-kaapelilla (USB-virheenkorjaus päällä) tai käynnistä emulaattori ja paina **Run (▶)**, tai valitse **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
