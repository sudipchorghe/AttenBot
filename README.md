# AttenBot

**Offline classroom attendance, no internet and no server needed.**

AttenBot is an ESP32-based attendance tracker with an Android companion app. Students mark attendance by connecting to the ESP32's own Wi-Fi network, and the teacher uses the app to download, store and review the records.


<img width="300" alt="Screenshot_20261030_164437" src="https://github.com/user-attachments/assets/b84d26bf-9c51-4628-a531-ee0c7415e187" />

<img width="300" alt="Screenshot_20261030_164501" src="https://github.com/user-attachments/assets/9ee494ef-78ae-4bc1-ad75-81f70a46ad55" />

<img width="300"  alt="Screenshot_20261030_164450" src="https://github.com/user-attachments/assets/9dca8c02-0299-40a3-9cca-86c75e27cc3a" />




---

## Why AttenBot?

Taking attendance on paper is slow and easy to fake. Cloud-based systems need internet, accounts and a backend. AttenBot works fully offline: the ESP32 creates its own Wi-Fi network, students mark attendance from a web page on their phone, and the teacher pulls the data into the Android app.

## Features

**Android app**
- Download attendance data directly from an ESP32 over Wi-Fi
- 3-tab navigation: **Dashboard**, **History**, **Settings**
- Assign a classroom name to each ESP32 IP address and switch classrooms from the Dashboard
- Auto-refresh on the Dashboard
- Attendance sessions stored on the teacher's phone for later review
- Built with Kotlin and Jetpack Compose

**ESP32 firmware**
- Runs as a Wi-Fi access point (no router or internet needed)
- Students open a web page and enter their **roll number** and a **session code**
- Built-in teacher page at `/teacher`

## How it works

```
 Student phone ──Wi-Fi──►  ESP32 (softAP)  ◄──Wi-Fi──  Teacher's phone
 (web page: roll no.       stores attendance           (AttenBot app:
  + session code)          for the session              download, store, review)
```

1. Students connect to the ESP32's Wi-Fi and submit their roll number and the code.
2. The teacher connects to the same Wi-Fi, opens AttenBot and downloads the attendance.
3. The app stores the session on the phone and shows it under **History**.

## Hardware required

| Part | Notes |
|------|-------|
| ESP32 development board | Any ESP32 DevKit |
| USB cable | For flashing and power |
| Power source | USB power bank or adapter |
| Display (optional) | Planned, not required |

## Getting started

### 1. Flash the ESP32

1. Install [Arduino IDE 2](https://www.arduino.cc/en/software).
2. Add ESP32 board support (Boards Manager → search "esp32" by Espressif). Tested with **ESP32 core 3.3.12**.
3. Open the firmware sketch in `firmware/` *(update this path to match your repo)*.
4. Select your board and port, then click **Upload**.
5. Open the Serial Monitor to confirm the access point started and note the IP address (usually `192.168.4.1`).

### 2. Build the Android app or download the latest release

1. Install [Android Studio](https://developer.android.com/studio).
2. Clone the repo:
   ```bash
   git clone https://github.com/sudipchorghe/AttenBot
   ```
3. Open the project in Android Studio and let Gradle sync.
4. Run on a physical device or emulator, or build an APK from **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

**Requirements:** Android Studio (latest stable), Android 8.0+ device.


## Usage

1. Power the ESP32 and connect your phone to its Wi-Fi network.
2. Open AttenBot and go to **Settings** (or **Teacher**) to add the ESP32's IP address and give the classroom a name.
3. On the **Dashboard**, select the classroom and download attendance.
4. Review past sessions under **History**.

*For students:* connect to the ESP32 Wi-Fi, open the page in your browser, enter your roll number then submit.

## Tech stack

- **App:** Kotlin, Jetpack Compose, Android Studio
- **Firmware:** C/C++ (Arduino framework) on ESP32
- **Communication:** HTTP over ESP32 softAP Wi-Fi

## Project structure

```
attenbot/
├── app/            # Android app (Kotlin + Jetpack Compose)
├── firmware/       # ESP32 sketch
├── docs/           # Screenshots, diagrams, report
├── LICENSE
└── README.md
```



## Roadmap

- [ ] RTC support for accurate timestamps on the ESP32
- [ ] Display on the ESP32 showing session code and status
- [ ] Analytics: late arrivals and attendance trends per student
- [ ] Sync to a backend / ERP for student-side attendance view
- [ ] Export to CSV / Excel

## Known limitations

- No real-time clock on the ESP32 yet, so timestamps depend on the app.
- Attendance is only as trustworthy as the session code is secret, so rotate it each session.
- Devices must be on the ESP32's Wi-Fi to mark or download attendance.

## Contributing

Contributions are welcome.

1. Fork the repo.
2. Create a branch: `git checkout -b feature/your-feature`
3. Commit your changes and push.
4. Open a pull request describing what you changed and why.

Please note that any derivative work must also be released under GPL-3.0.

## License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.

## Author

Built by **Sudip** as an embedded systems microproject.
