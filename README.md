# Sunrise Dental Clinic - Appointment and Patient Management System

CIS6003 Advanced Programming (WRIT1). A distributed web application built with
**plain Java, Servlets, MySQL, Maven and Apache Tomcat** - no Spring and no Bootstrap.
The browser pages (HTML + CSS + JavaScript) talk to the Java back end only through
JSON web services.

## What the system does

| # | Function | Page | Web service |
|---|----------|------|-------------|
| 1 | User authentication (login) | `login.html` | `POST /api/auth/login` |
| 2 | Register new appointment | `register.html` | `POST /api/appointments` |
| 3 | Display appointment details | `search.html` | `GET /api/appointments/{appointmentNo}` |
| 4 | Calculate and print bill | `billing.html` | `GET /api/bills/preview`, `POST /api/bills`, `GET /api/bills/{appointmentNo}` |
| 5 | Help section | `help.html` | - |
| 6 | Exit system | Exit button on every page | `POST /api/auth/logout` |
| + | Daily schedule | `schedule.html` | `GET /api/appointments?date=` |
| + | Cancel appointment | `search.html` | `PUT /api/appointments/{appointmentNo}/status` |
| + | Management reports (admin) | `reports.html` | `GET /api/reports/revenue`, `GET /api/reports/workload` |

Login accounts created by the database script:

| Username | Password | Role |
|----------|----------|------|
| `admin` | `Admin@123` | ADMIN (everything, including reports) |
| `reception` | `Recep@123` | RECEPTIONIST (everything except reports) |

## Three-tier architecture

```
Presentation tier   src/main/webapp (HTML, CSS, JS)  +  servlet/ , filter/
        |  JSON over HTTP
Business tier       service/  (rules, validation, billing, observers)
        |  DAO interfaces
Data tier           dao/ , dao/jdbc/  ->  MySQL (tables, view, procedure, function, triggers)
```

```
src/main/java/com/sunrisedental
├── model        User, Patient, Dentist, Treatment, Appointment, Bill, enums
├── dto          request / response records
├── dao          DAO interfaces + DAOFactory
│   └── jdbc     JDBC (MySQL) implementations
├── service      AuthService, AppointmentService, BillingService, ReportService, BillCalculator
│   ├── validation   AppointmentValidator, InputParser
│   ├── discount     DiscountStrategy + 3 strategies + factory
│   └── observer     publisher + ActivityLogObserver + PatientNotificationObserver
├── servlet      REST web services (JSON)
├── filter       AuthFilter (login + role check)
├── util         DBConnection, PasswordUtil, JsonUtil
└── exception    ValidationException, ConflictException, ...
```

## Design patterns used

| Pattern | Where | Why |
|---------|-------|-----|
| Singleton | `util/DBConnection`, `service/ServiceRegistry` | one shared configuration / one set of services |
| DAO | `dao/*DAO` + `dao/jdbc/Jdbc*DAO` | keeps SQL out of servlets and services |
| Factory | `dao/DAOFactory`, `service/discount/DiscountStrategyFactory` | callers depend on interfaces, not concrete classes |
| Strategy | `service/discount/*` | each discount rule is its own class; new rules need no change to billing |
| Observer | `service/observer/*` | activity log and patient message react to events without the services knowing them |
| Intercepting Filter | `filter/AuthFilter` | one place that enforces login and role for every request |
| Template Method | `servlet/BaseServlet.service()` | one place that turns exceptions into HTTP status codes |

## Database features

`database/sunrise_dental.sql` creates everything:

- 9 tables with primary/foreign keys, `UNIQUE`, `CHECK` constraints
- stored procedure `sp_register_appointment` (patient + number + appointment in one transaction)
- function `fn_format_appointment_no` (builds `APT-2026-0001`)
- triggers: no double booking, status rules, audit log, bill marks appointment `COMPLETED`
- generated column + unique key `slot_key` (double booking impossible even with two users at once)
- view `vw_appointment_details`

`database/sunrise_dental_test.sql` is the **same script with the database name
`sunrise_dental_test`**; the automated tests use it so they never touch real data.
If you change one script, make the same change in the other.

## How to run (Windows and macOS)

The code is the same on both systems. Only the way you install the tools differs.

### 1. Install the tools (once)

| Tool | Windows | macOS |
|------|---------|-------|
| JDK 17 | Installer from <https://adoptium.net> (tick "Set JAVA_HOME") | `brew install openjdk@17` |
| MySQL 8 + Workbench | MySQL Installer from <https://dev.mysql.com/downloads/installer/> | `brew install mysql` then `brew services start mysql`; Workbench from <https://dev.mysql.com/downloads/workbench/> |
| Apache Tomcat 10.1 | "64-bit Windows zip" from <https://tomcat.apache.org/download-10.cgi>, unzip to `C:\tomcat10` | `brew install tomcat@10` |
| Maven | Bundled with IntelliJ IDEA (Maven tool window), or zip from <https://maven.apache.org/download.cgi> | `brew install maven` |

Use Tomcat **10.1** (not 9 or 11): the code uses the `jakarta.servlet` API from Lecture 10.

### 2. Create the databases (same on both)

Open MySQL Workbench, open `database/sunrise_dental.sql` and run the whole file
(lightning icon). Do the same with `database/sunrise_dental_test.sql`.

### 3. Set your MySQL password (same on both)

Edit `db.password` in `src/main/resources/db.properties` and
`src/test/resources/db.properties`.

### 4. Run the tests and build the WAR

In IntelliJ: Maven tool window > Lifecycle > double-click `verify`.
Or in a terminal opened in the project folder (same command on both systems):

```
mvn clean verify
```

The result is `target/SunriseDentalClinic.war`.

### 5. Deploy on Tomcat

**With IntelliJ IDEA Ultimate (same on both, as in Lecture 10):**
Run > Edit Configurations > + > Tomcat Server > Local > choose the Tomcat folder.
Deployment tab > + > Artifact > `SunriseDentalClinic:war exploded`,
application context `/SunriseDentalClinic`, then Run.

**Without IntelliJ - Windows (Command Prompt):**

```
copy target\SunriseDentalClinic.war C:\tomcat10\webapps\
C:\tomcat10\bin\catalina.bat run
```

**Without IntelliJ - macOS (Terminal):**

```
cp target/SunriseDentalClinic.war "$(brew --prefix tomcat@10)/libexec/webapps/"
"$(brew --prefix tomcat@10)/bin/catalina" run
```

### 6. Open the system

<http://localhost:8080/SunriseDentalClinic/> and log in.

### If something does not start

| Problem | Fix |
|---------|-----|
| `JAVA_HOME is not defined` (Windows) | Set the `JAVA_HOME` environment variable to the JDK 17 folder and open a new Command Prompt. |
| Port 8080 already in use | Stop the other program, or change `port="8080"` in Tomcat's `conf/server.xml`. |
| `Access denied for user 'root'` | The password in `db.properties` is not your MySQL password. Rebuild after changing it. |
| `Unknown database 'sunrise_dental'` | Step 2 was not run. |

## Tests

```bash
mvn test
```

- Unit tests (no database needed): validation, billing, discounts, services, observers, filter, password hashing.
- DAO integration tests: run against `sunrise_dental_test`. They are **skipped** (not failed) if MySQL is not running.
- The same command works on Windows and macOS.

## Git workflow (Task D)

```bash
git init
git branch -M main
git add .
git commit -m "Initial commit: project structure and database script"
git remote add origin https://github.com/<your-username>/SunriseDentalClinic.git
git push -u origin main

git branch development
git checkout development
# ...make a change, then:
git add .
git commit -m "Describe the change"
git push -u origin development
# open a Pull Request on GitHub (development -> main), wait for the CI check, merge

git checkout main && git pull
git tag v1.0.0
git push origin v1.0.0        # starts the release workflow
```

- `.github/workflows/ci.yml` - on every push / pull request: starts MySQL, runs all tests, builds the WAR (Linux), then repeats the unit tests on Windows and macOS.
- `.github/workflows/release.yml` - on a `v*` tag: tests again, then publishes the WAR as a GitHub Release.

## Assumptions

1. Two staff roles: ADMIN (manager) and RECEPTIONIST. Only ADMIN sees financial reports.
2. Staff accounts are created by the administrator in the database; there is no self-registration.
3. Clinic hours are 08:00-20:00; appointments start every 30 minutes, last start 19:30.
4. Appointments can be booked from today up to 180 days ahead.
5. Contact numbers are Sri Lankan: 10 digits starting with 0.
6. A dentist sees one patient per time slot. A cancelled slot can be booked again.
7. Bill total = treatment cost + dentist's consultation fee - discount (senior citizen 10%, child 5%).
8. One bill per appointment; saving the bill marks the appointment COMPLETED. A saved bill cannot be edited.
9. A returning patient is recognised by name + contact number and is stored once.
10. Patient messages are queued in the `notifications` table; no real SMS is sent (that needs a paid gateway).
11. The activity log is written to the `SunriseDental` folder inside the user's home folder
    (`C:\Users\<name>\SunriseDental\activity-log.txt` on Windows, `/Users/<name>/SunriseDental/activity-log.txt` on macOS).
