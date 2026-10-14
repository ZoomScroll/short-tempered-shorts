# Short Tempered Shorts

An Android app that automatically swipes to the next **YouTube Short** or **Instagram Reel** after a set number of seconds.

## How it works
- Runs as an Android accessibility service, which is what lets it perform the swipe gesture.
- Only active inside YouTube and Instagram, and by default only when the Shorts / Reels player is on screen.
- A small floating panel shows the countdown, with **pause** and **skip** buttons. Drag it by the timer.
- Choose 3–90 seconds per video. Nothing is collected or sent anywhere.

## Getting the APK
Every push to `main` builds the app on GitHub Actions. Download `ShortTemperedShorts.apk` from the repo's **Releases** page.

## Installing
1. Open the downloaded APK and allow your browser/files app to install unknown apps when asked.
2. Open **Short Tempered Shorts** → **Turn on auto-scroll** → find it under *Installed apps / Downloaded apps* → switch it on.
3. Android 13+: if the switch is greyed out, go back to the app, tap **Open app info** → ⋮ menu → **Allow restricted settings**, then try again.
4. Open YouTube Shorts or Instagram Reels.

## If it stops scrolling after an app update
YouTube/Instagram sometimes rename their screens. Turn off *Only scroll on the Shorts / Reels screen* in the app and it will scroll anywhere in those apps (use pause when browsing normally).

## iOS
iOS doesn't let third-party apps control other apps, so an equivalent iPhone app isn't possible.
