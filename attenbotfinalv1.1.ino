#include <Arduino.h>
#include <WiFi.h>
#include <WebServer.h>
#include <Wire.h>
#include <LittleFS.h>
#include <FS.h>
#include <RTClib.h>
#include "esp_system.h"

// =====================================================
// USER CONFIGURATION
// =====================================================

// Change this value for every ESP32/classroom.
#define DEVICE_ID "CLASSROOM_A"

// Wi-Fi access point settings.
#define AP_PASSWORD ""

// Teacher/admin PIN.
// Change this before using the system.
#define ADMIN_PIN "0000"

// ESP32-S3 I2C pins.
// Change these according to your board.
#define SDA_PIN 41
#define SCL_PIN 42

// Optional: requires the teacher PIN for CSV download.
// true is safer because downloading closes attendance.
#define REQUIRE_PIN_FOR_DOWNLOAD true

// =====================================================
// FILES
// =====================================================

const char *CSV_FILE = "/attendance.csv";
const char *TOKENS_FILE = "/used_tokens.txt";
const char *LOCK_FILE = "/attendance_closed.lock";

// =====================================================
// SERVER AND RTC
// =====================================================

WebServer server(80);
RTC_DS3231 rtc;

bool attendanceClosed = false;

const char *REQUEST_HEADERS[] = {
  "Cookie",
  "User-Agent"
};

// =====================================================
// BASIC HELPERS
// =====================================================

String htmlEscape(const String &value) {
  String result = value;

  result.replace("&", "&amp;");
  result.replace("<", "&lt;");
  result.replace(">", "&gt;");
  result.replace("\"", "&quot;");
  result.replace("'", "&#39;");

  return result;
}

String jsonEscape(const String &value) {
  String result;

  for (size_t i = 0; i < value.length(); i++) {
    char character = value[i];

    if (character == '\"') {
      result += "\\\"";
    } else if (character == '\\') {
      result += "\\\\";
    } else if (character == '\n') {
      result += "\\n";
    } else if (character == '\r') {
      result += "\\r";
    } else {
      result += character;
    }
  }

  return result;
}

String csvEscape(const String &value) {
  String result = value;

  result.replace("\"", "\"\"");

  if (
    result.indexOf(',') >= 0 ||
    result.indexOf('"') >= 0 ||
    result.indexOf('\n') >= 0 ||
    result.indexOf('\r') >= 0
  ) {
    result = "\"" + result + "\"";
  }

  return result;
}

String createRandomToken() {
  uint32_t randomA = esp_random();
  uint32_t randomB = esp_random();

  char token[40];

  snprintf(
    token,
    sizeof(token),
    "%08lX%08lX",
    static_cast<unsigned long>(randomA),
    static_cast<unsigned long>(randomB)
  );

  return String(token);
}

String getCookieValue(
  const String &cookieHeader,
  const String &cookieName
) {
  String search = cookieName + "=";
  int start = cookieHeader.indexOf(search);

  if (start < 0) {
    return "";
  }

  start += search.length();

  int end = cookieHeader.indexOf(';', start);

  if (end < 0) {
    end = cookieHeader.length();
  }

  String value = cookieHeader.substring(start, end);
  value.trim();

  return value;
}

String getAttendanceTokenFromRequest() {
  if (!server.hasHeader("Cookie")) {
    return "";
  }

  return getCookieValue(
    server.header("Cookie"),
    "ATTENDANCE_TOKEN"
  );
}

void sendCorsHeaders() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.sendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  server.sendHeader(
    "Access-Control-Allow-Headers",
    "Content-Type, Cookie"
  );
  server.sendHeader("Cache-Control", "no-store");
}

void handleOptions() {
  sendCorsHeaders();
  server.send(204, "text/plain", "");
}

// =====================================================
// RTC HELPERS
// =====================================================

String getTimestamp() {
  DateTime now = rtc.now();

  char timestamp[24];

  snprintf(
    timestamp,
    sizeof(timestamp),
    "%04d-%02d-%02d %02d:%02d:%02d",
    now.year(),
    now.month(),
    now.day(),
    now.hour(),
    now.minute(),
    now.second()
  );

  return String(timestamp);
}

// =====================================================
// TOKEN STORAGE
// =====================================================

bool tokenWasUsed(const String &token) {
  if (token.length() == 0) {
    return false;
  }

  if (!LittleFS.exists(TOKENS_FILE)) {
    return false;
  }

  File file = LittleFS.open(TOKENS_FILE, FILE_READ);

  if (!file) {
    return false;
  }

  bool found = false;

  while (file.available()) {
    String storedToken = file.readStringUntil('\n');
    storedToken.trim();

    if (storedToken == token) {
      found = true;
      break;
    }
  }

  file.close();

  return found;
}

bool storeToken(const String &token) {
  File file = LittleFS.open(TOKENS_FILE, FILE_APPEND);

  if (!file) {
    return false;
  }

  file.println(token);
  file.close();

  return true;
}

void clearTokens() {
  if (LittleFS.exists(TOKENS_FILE)) {
    LittleFS.remove(TOKENS_FILE);
  }
}

// =====================================================
// CSV STORAGE
// =====================================================

void createCsvIfRequired() {
  if (LittleFS.exists(CSV_FILE)) {
    return;
  }

  File file = LittleFS.open(CSV_FILE, FILE_WRITE);

  if (!file) {
    Serial.println("ERROR: Could not create attendance.csv");
    return;
  }

  file.println(
    "device_id,name,roll_number,timestamp"
  );

  file.close();
}

size_t getAttendanceCount() {
  if (!LittleFS.exists(CSV_FILE)) {
    return 0;
  }

  File file = LittleFS.open(CSV_FILE, FILE_READ);

  if (!file) {
    return 0;
  }

  size_t count = 0;

  while (file.available()) {
    String line = file.readStringUntil('\n');
    line.trim();

    if (
      line.length() > 0 &&
      !line.startsWith("device_id")
    ) {
      count++;
    }
  }

  file.close();

  return count;
}

bool appendAttendance(
  const String &name,
  const String &rollNumber,
  const String &token
) {
  File file = LittleFS.open(CSV_FILE, FILE_APPEND);

  if (!file) {
    return false;
  }

  file.print(csvEscape(String(DEVICE_ID)));
  file.print(",");

  file.print(csvEscape(name));
  file.print(",");

  file.print(csvEscape(rollNumber));
  file.print(",");

  file.println(csvEscape(getTimestamp()));

  file.close();

  if (!storeToken(token)) {
    Serial.println("WARNING: Could not store browser token");
  }

  return true;
}

void closeAttendance() {
  attendanceClosed = true;

  File file = LittleFS.open(LOCK_FILE, FILE_WRITE);

  if (file) {
    file.println("Attendance closed after CSV download.");
    file.close();
  }
}

void resetAttendance() {
  if (LittleFS.exists(CSV_FILE)) {
    LittleFS.remove(CSV_FILE);
  }

  if (LittleFS.exists(TOKENS_FILE)) {
    LittleFS.remove(TOKENS_FILE);
  }

  if (LittleFS.exists(LOCK_FILE)) {
    LittleFS.remove(LOCK_FILE);
  }

  attendanceClosed = false;

  createCsvIfRequired();
}

// =====================================================
// AUTHENTICATION
// =====================================================

bool adminPinIsValid() {
  if (!server.hasArg("pin")) {
    return false;
  }

  String suppliedPin = server.arg("pin");
  suppliedPin.trim();

  return suppliedPin == String(ADMIN_PIN);
}

// =====================================================
// STUDENT WEB PAGE
// =====================================================

String buildStudentPage(const String &token) {
  String page = R"HTML(
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta
    name="viewport"
    content="width=device-width, initial-scale=1"
  >
  <title>AttendBot Attendance</title>

  <style>
    body {
      margin: 0;
      padding: 20px;
      background: #eef2f5;
      font-family: Arial, sans-serif;
    }

    .card {
      max-width: 520px;
      margin: auto;
      padding: 24px;
      background: white;
      border-radius: 14px;
      box-shadow: 0 3px 14px rgba(0, 0, 0, 0.15);
    }

    h1 {
      color: #174a68;
    }

    input,
    button {
      width: 100%;
      box-sizing: border-box;
      padding: 13px;
      margin-top: 10px;
      font-size: 16px;
    }

    input {
      border: 1px solid #b8c2cc;
      border-radius: 6px;
    }

    button {
      border: 0;
      border-radius: 6px;
      color: white;
      background: #198754;
    }

    .info {
      padding: 12px;
      background: #e7f1ff;
      border-radius: 8px;
      margin: 14px 0;
    }

    .warning {
      padding: 12px;
      background: #fff3cd;
      border-radius: 8px;
      margin-top: 14px;
    }

    .error {
      padding: 12px;
      background: #f8d7da;
      border-radius: 8px;
      margin-top: 14px;
    }

    .success {
      padding: 12px;
      background: #d1e7dd;
      border-radius: 8px;
      margin-top: 14px;
    }

    a {
      color: #174a68;
    }
  </style>
</head>

<body>
  <div class="card">
    <h1>Student Attendance</h1>

    <div class="info">
      Classroom:
      <strong>)HTML";

  page += htmlEscape(String(DEVICE_ID));

  page += R"HTML(</strong><br>
      Current time: )HTML";

  page += htmlEscape(getTimestamp());

  page += R"HTML(
    </div>
)HTML";

  if (attendanceClosed) {
    page += R"HTML(
    <div class="error">
      Attendance is closed because the teacher downloaded the CSV file.
    </div>
)HTML";
  } else if (tokenWasUsed(token)) {
    page += R"HTML(
    <div class="warning">
      Attendance has already been submitted from this browser.
    </div>
)HTML";
  } else {
    page += R"HTML(
    <form method="POST" action="/submit">
      <label for="name">Student name</label>
      <input
        id="name"
        name="name"
        type="text"
        maxlength="80"
        autocomplete="name"
        required
      >

      <label for="roll_number">Roll number</label>
      <input
        id="roll_number"
        name="roll_number"
        type="text"
        maxlength="30"
        autocomplete="off"
        required
      >

      <button type="submit">
        Mark Attendance
      </button>
    </form>
)HTML";
  }

  page += R"HTML(
    <hr>

    <p>
      Records collected:
      <strong>)HTML";

  page += String(getAttendanceCount());

  page += R"HTML(</strong>
    </p>

    <p>
      Teacher controls:
      <a href="/teacher">Open teacher page</a>
    </p>
  </div>
</body>
</html>
)HTML";

  return page;
}

void handleStudentHome() {
  String token = getAttendanceTokenFromRequest();

  if (token.length() == 0) {
    token = createRandomToken();

    server.sendHeader(
      "Set-Cookie",
      "ATTENDANCE_TOKEN=" + token +
      "; Max-Age=3600; Path=/; SameSite=Lax"
    );
  }

  server.sendHeader("Cache-Control", "no-store");

  server.send(
    200,
    "text/html; charset=utf-8",
    buildStudentPage(token)
  );
}

// =====================================================
// STUDENT SUBMISSION
// =====================================================

void handleStudentSubmit() {
  if (attendanceClosed) {
    server.send(
      403,
      "text/html; charset=utf-8",
      "<h2>Attendance is closed.</h2>"
    );
    return;
  }

  String token = getAttendanceTokenFromRequest();

  if (token.length() == 0) {
    server.send(
      400,
      "text/html; charset=utf-8",
      "<h2>Attendance cookie is missing. Reload the page.</h2>"
    );
    return;
  }

  if (tokenWasUsed(token)) {
    server.send(
      409,
      "text/html; charset=utf-8",
      "<h2>Attendance was already submitted from this browser.</h2>"
    );
    return;
  }

  if (!server.hasArg("name") ||
      !server.hasArg("roll_number")) {
    server.send(
      400,
      "text/html; charset=utf-8",
      "<h2>Name and roll number are required.</h2>"
    );
    return;
  }

  String name = server.arg("name");
  String rollNumber = server.arg("roll_number");

  name.trim();
  rollNumber.trim();

  if (name.length() == 0 ||
      rollNumber.length() == 0) {
    server.send(
      400,
      "text/html; charset=utf-8",
      "<h2>Name and roll number are required.</h2>"
    );
    return;
  }

  if (name.length() > 80 ||
      rollNumber.length() > 30) {
    server.send(
      400,
      "text/html; charset=utf-8",
      "<h2>Input is too long.</h2>"
    );
    return;
  }

  if (!appendAttendance(name, rollNumber, token)) {
    server.send(
      500,
      "text/html; charset=utf-8",
      "<h2>Could not save attendance.</h2>"
    );
    return;
  }

  server.sendHeader("Location", "/success");
  server.send(303, "text/plain", "Attendance recorded");
}

void handleSuccess() {
  server.send(
    200,
    "text/html; charset=utf-8",
    "<html><body>"
    "<h2>Attendance recorded successfully.</h2>"
    "<p>You may close this page.</p>"
    "<a href='/'>Back</a>"
    "</body></html>"
  );
}

// =====================================================
// TEACHER PAGE
// =====================================================

String buildTeacherPage() {
  String page = R"HTML(
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta
    name="viewport"
    content="width=device-width, initial-scale=1"
  >
  <title>AttendBot Teacher Controls</title>

  <style>
    body {
      margin: 0;
      padding: 20px;
      background: #eef2f5;
      font-family: Arial, sans-serif;
    }

    .card {
      max-width: 520px;
      margin: auto;
      padding: 24px;
      background: white;
      border-radius: 14px;
      box-shadow: 0 3px 14px rgba(0, 0, 0, 0.15);
    }

    input,
    button {
      width: 100%;
      box-sizing: border-box;
      padding: 13px;
      margin-top: 10px;
      font-size: 16px;
    }

    button {
      border: 0;
      border-radius: 6px;
      color: white;
      background: #174a68;
    }

    .danger {
      background: #b02a37;
    }

    .info {
      padding: 12px;
      background: #e7f1ff;
      border-radius: 8px;
    }

    a {
      color: #174a68;
    }
  </style>
</head>

<body>
  <div class="card">
    <h1>Teacher Controls</h1>

    <div class="info">
      Device:
      <strong>)HTML";

  page += htmlEscape(String(DEVICE_ID));

  page += R"HTML(</strong><br>
      Records:
      <strong>)HTML";

  page += String(getAttendanceCount());

  page += R"HTML(</strong><br>
      Status:
      <strong>)HTML";

  page += attendanceClosed ? "CLOSED" : "OPEN";

  page += R"HTML(</strong>
    </div>

    <h3>Download CSV</h3>

    <form method="GET" action="/download">
      <input
        name="pin"
        type="password"
        placeholder="Teacher PIN"
        required
      >

      <button type="submit">
        Download CSV and Close Attendance
      </button>
    </form>

    <h3>Next lecture</h3>

    <form method="POST" action="/api/reset">
      <input
        name="pin"
        type="password"
        placeholder="Teacher PIN"
        required
      >

      <button class="danger" type="submit">
        Clear Attendance for Next Lecture
      </button>
    </form>

    <p>
      <a href="/">Back to student page</a>
    </p>
  </div>
</body>
</html>
)HTML";

  return page;
}

void handleTeacherPage() {
  server.send(
    200,
    "text/html; charset=utf-8",
    buildTeacherPage()
  );
}

// =====================================================
// STATUS API
// =====================================================

void handleStatus() {
  String response = "{";

  response += "\"device_id\":\"";
  response += jsonEscape(String(DEVICE_ID));
  response += "\",";

  response += "\"closed\":";
  response += attendanceClosed ? "true" : "false";
  response += ",";

  response += "\"count\":";
  response += String(getAttendanceCount());

  response += "}";

  sendCorsHeaders();

  server.send(
    200,
    "application/json",
    response
  );
}

// =====================================================
// CSV PARSER
// =====================================================

bool parseCsvLine(
  const String &line,
  String fields[],
  const int fieldCount
) {
  int currentField = 0;
  bool insideQuotes = false;
  String currentValue;

  for (size_t i = 0; i < line.length(); i++) {
    char character = line[i];

    if (character == '"') {
      if (
        insideQuotes &&
        i + 1 < line.length() &&
        line[i + 1] == '"'
      ) {
        currentValue += '"';
        i++;
      } else {
        insideQuotes = !insideQuotes;
      }
    } else if (character == ',' && !insideQuotes) {
      if (currentField >= fieldCount) {
        return false;
      }

      fields[currentField] = currentValue;
      currentValue = "";
      currentField++;
    } else {
      currentValue += character;
    }
  }

  if (currentField >= fieldCount) {
    return false;
  }

  fields[currentField] = currentValue;
  currentField++;

  return currentField == fieldCount;
}

// =====================================================
// ATTENDANCE JSON API
// =====================================================

void handleAttendanceJson() {
  if (!LittleFS.exists(CSV_FILE)) {
    sendCorsHeaders();
    server.send(200, "application/json", "[]");
    return;
  }

  File file = LittleFS.open(CSV_FILE, FILE_READ);

  if (!file) {
    sendCorsHeaders();
    server.send(
      500,
      "application/json",
      "{\"error\":\"Could not open attendance file\"}"
    );
    return;
  }

  String response = "[";
  bool firstRecord = true;

  while (file.available()) {
    String line = file.readStringUntil('\n');
    line.trim();

    if (
      line.length() == 0 ||
      line.startsWith("device_id")
    ) {
      continue;
    }

    String fields[4];

    if (!parseCsvLine(line, fields, 4)) {
      continue;
    }

    if (!firstRecord) {
      response += ",";
    }

    response += "{";

    response += "\"device_id\":\"";
    response += jsonEscape(fields[0]);
    response += "\",";

    response += "\"name\":\"";
    response += jsonEscape(fields[1]);
    response += "\",";

    response += "\"roll_number\":\"";
    response += jsonEscape(fields[2]);
    response += "\",";

    response += "\"timestamp\":\"";
    response += jsonEscape(fields[3]);
    response += "\"";

    response += "}";

    firstRecord = false;
  }

  file.close();

  response += "]";

  sendCorsHeaders();

  server.send(
    200,
    "application/json",
    response
  );
}

// =====================================================
// CSV DOWNLOAD
// =====================================================

void handleDownload() {
  if (!LittleFS.exists(CSV_FILE)) {
    server.send(
      404,
      "text/plain",
      "Attendance CSV does not exist"
    );
    return;
  }

#if REQUIRE_PIN_FOR_DOWNLOAD
  if (!adminPinIsValid()) {
    server.send(
      401,
      "text/plain",
      "Teacher PIN required or invalid"
    );
    return;
  }
#endif

  File file = LittleFS.open(CSV_FILE, FILE_READ);

  if (!file) {
    server.send(
      500,
      "text/plain",
      "Could not open attendance CSV"
    );
    return;
  }

  String fileName =
    "attendance_" +
    String(DEVICE_ID) +
    ".csv";

  server.sendHeader(
    "Content-Disposition",
    "attachment; filename=\"" + fileName + "\""
  );

  server.sendHeader(
    "Cache-Control",
    "no-store"
  );

  server.streamFile(file, "text/csv");

  file.close();

  // Downloading the CSV closes the current lecture.
  closeAttendance();

  Serial.println(
    "Attendance closed after CSV download."
  );
}

// =====================================================
// RESET API
// =====================================================

void handleReset() {
  if (server.method() != HTTP_POST) {
    server.send(
      405,
      "text/plain",
      "Use HTTP POST for reset"
    );
    return;
  }

  if (!adminPinIsValid()) {
    sendCorsHeaders();

    server.send(
      401,
      "application/json",
      "{\"success\":false,\"message\":\"Invalid teacher PIN\"}"
    );
    return;
  }

  resetAttendance();

  Serial.println(
    "Attendance cleared for the next lecture."
  );

  sendCorsHeaders();

  server.send(
    200,
    "application/json",
    "{\"success\":true,"
    "\"message\":\"Attendance cleared for the next lecture\"}"
  );
}

// =====================================================
// OPTIONAL RTC SETTING API
// =====================================================
//
// Android can send:
// POST /api/set-time
// pin=2468
// datetime=2026-10-15 10:30:00
//
// This is optional but useful for setting the DS3231.

bool parseDateTime(
  const String &value,
  int &year,
  int &month,
  int &day,
  int &hour,
  int &minute,
  int &second
) {
  if (value.length() != 19) {
    return false;
  }

  if (
    value[4] != '-' ||
    value[7] != '-' ||
    value[10] != ' ' ||
    value[13] != ':' ||
    value[16] != ':'
  ) {
    return false;
  }

  year = value.substring(0, 4).toInt();
  month = value.substring(5, 7).toInt();
  day = value.substring(8, 10).toInt();
  hour = value.substring(11, 13).toInt();
  minute = value.substring(14, 16).toInt();
  second = value.substring(17, 19).toInt();

  if (year < 2020 || year > 2099) {
    return false;
  }

  if (month < 1 || month > 12) {
    return false;
  }

  if (day < 1 || day > 31) {
    return false;
  }

  if (hour < 0 || hour > 23) {
    return false;
  }

  if (minute < 0 || minute > 59) {
    return false;
  }

  if (second < 0 || second > 59) {
    return false;
  }

  return true;
}

void handleSetTime() {
  if (server.method() != HTTP_POST) {
    server.send(
      405,
      "text/plain",
      "Use HTTP POST"
    );
    return;
  }

  if (!adminPinIsValid()) {
    sendCorsHeaders();

    server.send(
      401,
      "application/json",
      "{\"success\":false,\"message\":\"Invalid teacher PIN\"}"
    );
    return;
  }

  if (!server.hasArg("datetime")) {
    sendCorsHeaders();

    server.send(
      400,
      "application/json",
      "{\"success\":false,\"message\":\"datetime is required\"}"
    );
    return;
  }

  int year;
  int month;
  int day;
  int hour;
  int minute;
  int second;

  String datetime = server.arg("datetime");

  if (!parseDateTime(
    datetime,
    year,
    month,
    day,
    hour,
    minute,
    second
  )) {
    sendCorsHeaders();

    server.send(
      400,
      "application/json",
      "{\"success\":false,\"message\":"
      "\"Invalid format. Use YYYY-MM-DD HH:MM:SS\"}"
    );
    return;
  }

  rtc.adjust(
    DateTime(
      year,
      month,
      day,
      hour,
      minute,
      second
    )
  );

  sendCorsHeaders();

  server.send(
    200,
    "application/json",
    "{\"success\":true,\"message\":\"RTC time updated\"}"
  );
}

// =====================================================
// 404 HANDLER
// =====================================================

void handleNotFound() {
  server.send(
    404,
    "text/plain",
    "Endpoint not found"
  );
}

// =====================================================
// SETUP
// =====================================================

void setup() {
  Serial.begin(115200);
  delay(1000);

  Serial.println();
  Serial.println("==============================");
  Serial.println("AttendBot ESP32-S3 Starting");
  Serial.println("==============================");

  // Start I2C for DS3231.
  Wire.begin(SDA_PIN, SCL_PIN);

  if (!rtc.begin()) {
    Serial.println("ERROR: DS3231 RTC not detected.");
    Serial.println("Check VCC, GND, SDA and SCL.");

    while (true) {
      delay(1000);
    }
  }

  if (rtc.lostPower()) {
    Serial.println(
      "WARNING: DS3231 lost power."
    );

    Serial.println(
      "Setting RTC using compile date/time."
    );

    rtc.adjust(
      DateTime(
        F(__DATE__),
        F(__TIME__)
      )
    );
  }

  // Mount LittleFS without automatically erasing data.
  if (!LittleFS.begin(false)) {
    Serial.println("ERROR: LittleFS mount failed.");

    while (true) {
      delay(1000);
    }
  }

  createCsvIfRequired();

  // If lock exists, attendance remains closed after reboot.
  attendanceClosed = LittleFS.exists(LOCK_FILE);

  String ssid =
    "AttendBot-" +
    String(DEVICE_ID);

  WiFi.mode(WIFI_AP);

  bool apStarted = WiFi.softAP(
    ssid.c_str(),
    AP_PASSWORD
  );

  if (!apStarted) {
    Serial.println("ERROR: Could not start Wi-Fi AP.");

    while (true) {
      delay(1000);
    }
  }

  IPAddress ip = WiFi.softAPIP();

  Serial.println();
  Serial.println("Wi-Fi access point started");
  Serial.print("SSID: ");
  Serial.println(ssid);

  Serial.print("Password: ");
  Serial.println(AP_PASSWORD);

  Serial.print("IP address: ");
  Serial.println(ip);

  Serial.print("Device ID: ");
  Serial.println(DEVICE_ID);

  Serial.print("Attendance status: ");
  Serial.println(
    attendanceClosed ? "CLOSED" : "OPEN"
  );

  Serial.print("Attendance records: ");
  Serial.println(getAttendanceCount());

  server.collectHeaders(
    REQUEST_HEADERS,
    2
  );

  // Student routes.
  server.on(
    "/",
    HTTP_GET,
    handleStudentHome
  );

  server.on(
    "/submit",
    HTTP_POST,
    handleStudentSubmit
  );

  server.on(
    "/success",
    HTTP_GET,
    handleSuccess
  );

  // Teacher page.
  server.on(
    "/teacher",
    HTTP_GET,
    handleTeacherPage
  );

  // Android API routes.
  server.on(
    "/api/status",
    HTTP_GET,
    handleStatus
  );

  server.on(
    "/api/attendance",
    HTTP_GET,
    handleAttendanceJson
  );

  server.on(
    "/api/reset",
    HTTP_POST,
    handleReset
  );

  server.on(
    "/api/set-time",
    HTTP_POST,
    handleSetTime
  );

  // CSV download.
  server.on(
    "/download",
    HTTP_GET,
    handleDownload
  );

  // CORS preflight.
  server.on(
    "/api/status",
    HTTP_OPTIONS,
    handleOptions
  );

  server.on(
    "/api/attendance",
    HTTP_OPTIONS,
    handleOptions
  );

  server.on(
    "/api/reset",
    HTTP_OPTIONS,
    handleOptions
  );

  server.on(
    "/api/set-time",
    HTTP_OPTIONS,
    handleOptions
  );

  server.onNotFound(handleNotFound);

  server.begin();

  Serial.println("HTTP server started");
  Serial.println();
  Serial.println("Student page:");
  Serial.println("http://192.168.4.1/");

  Serial.println();
  Serial.println("Teacher page:");
  Serial.println("http://192.168.4.1/teacher");

  Serial.println();
  Serial.println("Android status API:");
  Serial.println("http://192.168.4.1/api/status");

  Serial.println();
  Serial.println("Android attendance API:");
  Serial.println("http://192.168.4.1/api/attendance");
}

// =====================================================
// MAIN LOOP
// =====================================================

void loop() {
  server.handleClient();

  // Allow other ESP32 tasks to run.
  delay(2);
}