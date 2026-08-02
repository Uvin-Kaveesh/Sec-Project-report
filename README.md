# Club Projects

A project workspace for the club committee — redesigned frontend on top of a
Java **Spring Boot** backend with a **SQLite** database.

The old single-file `club-projects_1.html` kept everything in browser storage,
so the data lived on one laptop. Now every project is a row in a real database
that the whole committee can share.

---

## What you need

| Tool | Version | Check with |
| --- | --- | --- |
| JDK | 17 or newer | `java -version` |
| Maven | 3.8+ (or use your IDE's bundled one) | `mvn -version` |

Neither is currently installed on this machine. Install a JDK (for example
[Temurin 21](https://adoptium.net/)) and [Maven](https://maven.apache.org/download.cgi),
or just open this folder in IntelliJ IDEA / VS Code with the Java extension pack,
which ships its own Maven.

## Run it

```bash
mvn spring-boot:run
```

Then open <http://localhost:8080>.

On the very first run the app creates `data/club-projects.db` and seeds it with
the 17 committee members and the 14 projects from the original page. After that
it never touches your data again.

## Build a runnable jar

```bash
mvn clean package
```

```bash
java -jar target/club-projects-1.0.0.jar
```

To put the database somewhere else, point the datasource at another file:

```bash
java -jar target/club-projects-1.0.0.jar --spring.datasource.url=jdbc:sqlite:C:/club/projects.db
```

---

## API

| Method | Path | Does |
| --- | --- | --- |
| `GET` | `/api/projects?q=&status=` | List projects, optionally searched/filtered |
| `GET` | `/api/projects/{id}` | One project |
| `POST` | `/api/projects` | Create |
| `PUT` | `/api/projects/{id}` | Replace (the UI autosaves through this) |
| `DELETE` | `/api/projects/{id}` | Delete |
| `GET` | `/api/projects/stats` | Counts for the dashboard tiles |
| `GET` | `/api/projects/export.tsv` | Tab-separated export for spreadsheets |
| `POST` | `/api/admin/login` | Exchange the password for a token — `{"password":"..."}` |
| `POST` | `/api/admin/logout` | End the admin session |
| `GET` | `/api/admin/session` | Is the stored token still valid? |
| `POST` | `/api/projects/{id}/thumbnail` | Upload a photo (multipart, field `file`) |
| `GET` | `/api/projects/{id}/thumbnail` | The image bytes |
| `DELETE` | `/api/projects/{id}/thumbnail` | Remove the photo |
| `GET` | `/api/site` | Whether a logo exists, and its version |
| `GET` | `/api/site/logo` | The logo image bytes |
| `POST` | `/api/site/logo` | Upload a logo (multipart, field `file`) — admin |
| `DELETE` | `/api/site/logo` | Remove the logo — admin |
| `GET` | `/api/meta` | Types, categories, statuses, committee, logo — one round trip |
| `GET` | `/api/committee` | Members as `{id, name, role, projectCount}` |
| `POST` | `/api/committee` | Add a member — body `{"name":"..."}` |
| `DELETE` | `/api/committee/{id}` | Remove a member |

## Admins and deleting

Everyone can add and edit projects, upload photos and add committee members.
**Only admins can delete** a project or a committee member. Anyone else who
clicks a delete button gets a message saying so.

### The admin door

It is deliberately unmarked — there is no "Admin" link in the interface. Two
ways in, both worth writing down somewhere:

- Go to **`http://localhost:8080/#/admin`**
- Or **click the club badge (top-left) five times quickly**

Either opens a password prompt. Signing in reveals the admin panel, which lists
every project and committee member with a delete button beside each.

### The password

Set in `application.properties`:

```properties
club.admin.password=${CLUB_ADMIN_PASSWORD:leo-admin-2026}
```

**Change it before the club uses this.** Either edit that line, or leave it and
set an environment variable instead, which keeps the password out of the file:

```bash
set CLUB_ADMIN_PASSWORD=your-password-here
```

### What this protects, and what it doesn't

The check that matters is [`AdminGuard`](src/main/java/lk/leoclub/clubprojects/web/AdminGuard.java),
a server-side interceptor. It rejects any `DELETE` on `/api/projects/{id}` or
`/api/committee/{id}` without a valid token — so calling the API directly with
`curl` or the browser console fails the same way the button does. Hiding buttons
in the browser alone would have protected nothing.

Being straight about the limits:

- It is **one shared password**, not user accounts. Anyone who knows it is an
  admin, and there is no record of who deleted what.
- The hidden door is **convenience, not security**. Anyone reading `app.js` can
  find it. The password is what actually stops people.
- Sessions live **in memory and last 8 hours**; restarting the app signs
  everyone out. The token is kept in `sessionStorage`, so closing the tab also
  ends it.
- Photos are **not** restricted — anyone can change or remove a project's
  picture, since that is editing rather than destroying a record.

If the club ever needs real accounts with per-person logins and an audit trail,
that means Spring Security and a users table — a much bigger change than this.

## Project photos

Each project can carry a photo, which fills its card on the home screen as a
faint background so the board is scannable at a glance. Open a project and use
the tile beside the title — click it to browse, or drop an image straight onto
it. The **×** removes it.

The card photo sits behind a scrim that **follows the theme**: it darkens the
image in dark mode and lightens it in light mode. A fixed dark overlay would
have buried the dark text on a white card. Both are tuned by four variables
(`--cover-strength`, `--cover-scrim-1..3`) at the top of `app.css` if you want
the photo bolder or fainter.

Some deliberate choices:

- **The browser shrinks the image before uploading.** A photo is scaled to fit
  480px on its long edge and re-encoded as JPEG, which turned a 95 KB test image
  into 5 KB. Phone snaps of several megabytes become tens of kilobytes, so the
  database stays small. 480px is deliberately larger than the card needs, so the
  background still looks sharp on a high-resolution screen. GIFs are left alone
  so animation survives.
- **Bytes live in their own table** (`project_thumbnails`). Listing projects for
  the home screen would otherwise drag every image out of the database with it.
  The project row keeps only the content type and a timestamp.
- **The image URL carries a `?v=` stamp** taken from that timestamp, so it can be
  cached hard for a month yet still update the instant a photo is replaced.
- Server-side limits: 2 MB, and only JPEG, PNG, WebP or GIF. Deleting a project
  deletes its image too.

## Club logo

The admin panel has a **Club logo** section. Click the square or drop an image
on it; the logo replaces the "LC" badge in the header on every page, for
everyone. **Remove** puts the badge back.

- Admin-only to change, since it appears site-wide.
- PNG, JPEG or WebP, up to 1 MB. The browser shrinks it to 512px first.
- **A transparent PNG stays a PNG.** Project thumbnails get re-encoded as JPEG
  to save space, but doing that to a logo would paint a solid block behind it,
  so images with transparency keep their format.
- **SVG is deliberately refused.** An SVG can carry script, and serving one from
  this origin would let it run with the site's privileges. PNG covers what a
  logo needs.
- The image URL carries a `?v=` stamp, so it caches hard yet updates the moment
  it is replaced.

## Card structure — types and categories

The admin panel has a **Card structure** section for the two pick-lists on the
project form. Add an option with the inline field; remove one with the **×** that
appears on hover. The small number beside an option is how many projects use it.

- **Changing these is admin-only** — including *adding*, not just deleting,
  because both lists shape every project card. Non-admins get
  "Only admins can change project types and categories."
- **Removing is non-destructive.** Projects keep whatever value they were given;
  the option simply stops being offered for new work. Because project type is a
  dropdown, a retired value is still listed on projects using it — otherwise
  opening such a project and editing any field would silently rewrite its type
  to whichever option came first.
- **The last option cannot be removed.** A form with an empty dropdown would be
  unusable, so the final type or category is refused with a clear message.
- Duplicates are rejected and spacing is normalised, so `"  Zone   Project "`
  matches the existing `"Zone Project"`.

The lists live in the `catalog_items` table and are seeded once from
`Catalog.java` on first run. After that the database is the source of truth —
editing `Catalog.java` will not change an existing installation.

## Managing the committee

The committee panel on the home screen is editable. **Add member** opens an
inline field; each member chip has a **×** that appears on hover, and the small
number beside a name is how many projects they are assigned to.

Removing someone is deliberately *not* destructive. Assignees are stored as
names, so a member who leaves **keeps their name on every project they already
ran** — the history stays true, they simply stop appearing as a choice for new
work. The confirmation says how many projects are affected before you commit.

This has one consequence worth knowing about: the assignee picker rebuilds a
project's list from the buttons on screen, so a former member still shows up
there — marked with a dashed outline and "No longer on the committee" — and
stays ticked. Without that, opening an old project and editing any field would
quietly erase them from it.

Duplicate names are rejected, and extra spacing is tidied up, so
`"  Nimesha   Perera "` and `"Nimesha Perera"` are treated as the same person.

Deleting a member or a project asks for confirmation through an in-page
`<dialog>` rather than `window.confirm()`. That is deliberate: embedded browsers
and preview panes often suppress native dialogs outright, and the page then
reads the refusal as "cancel" — the delete button appears to do nothing at all.

## Layout

```
src/main/java/lk/leoclub/clubprojects/
  ClubProjectsApplication.java   entry point (also creates the data/ folder)
  config/     DataSeeder, WebConfig (CORS for local dev)
  model/      Project, SubTask, CommitteeMember  — JPA entities
  repository/ Spring Data repositories + the search query
  service/    ProjectService (rules, sorting, TSV export), ProjectMapper, Catalog
  web/        REST controllers + error handling
src/main/resources/
  application.properties
  static/     index.html, css/app.css, js/app.js  — the UI
```

## How the progress bar works

There is no slider to drag. Progress **measures how much of the record has been
filled in** — the bar moves on its own as the committee completes the form.

Twenty-two things are counted, each worth about 4.5%:

| Group | Counted |
| --- | --- |
| The basics | start date, end date, duration, venue, the two Leo District questions |
| Who is running it | chairman, secretary, treasurer, participation |
| Impact & reporting | beneficiaries, service hours, project value, funds, community, data collection, community need, service opportunity |
| Guests & notes | chief guest, other guests, special note |
| People | at least one person assigned |

Project **type** and **category** are excluded on purpose — they always carry a
default value, so counting them would hand out free progress.

The list lives in one place, [`ProjectCompletion.java`](src/main/java/lk/leoclub/clubprojects/service/ProjectCompletion.java).
The server recomputes the percentage on every save and ignores whatever the
browser sends, while `/api/meta` publishes the field names so the bar can update
live as you type without the two ever disagreeing. To change what counts, edit
`trackedFields()` — the UI follows automatically.

Status and progress are now independent: marking a project **Done** no longer
forces the bar to 100%, so a finished project with unfinished paperwork still
shows the gap.

## Notes on the design

- **Dates are stored as ISO text** (`2026-07-18`). SQLite has no date type, and
  text sorts correctly in ISO format, so this keeps queries simple and avoids
  driver-level date conversion quirks.
- **One connection in the pool.** SQLite allows a single writer, so Hikari is
  capped at one connection to sidestep `SQLITE_BUSY` errors.
- **Ids are UUID strings** assigned by the app rather than autoincrement
  integers, which keeps inserts simple across SQLite and any future database.
- **Roles are name chips, not free text.** Chairman, secretary and treasurer
  offer the committee as you type — pick from the list, or type someone who is
  not a member. Each name becomes a removable chip; committee members show their
  initials so it is obvious who is from the club. Stored as a comma-separated
  string, so the database and the export are unchanged.
- **Guests carry a name and a designation.** Fill both boxes and press Add or
  Enter; each guest becomes a chip showing "Name │ Designation". The pair is
  stored in the same single column as `Name (Designation); Next Name (…)`.
  Semicolons separate people **because designations so often contain a comma**
  ("Vice Chancellor, University of Colombo") and a comma-separated list would
  split straight through one. Records saved before designations existed are
  plain comma-separated names and still load correctly.
- **The dropdowns are custom, not native.** A `<select>` popup and `<datalist>`
  suggestion list are drawn by the browser and ignore CSS completely, so they
  never matched the rest of the page. Both are rebuilt in `app.js` as a combo
  box: searchable, keyboard-driven (arrows, Enter, Escape), with a tick on the
  current value. A hidden input carries the value, so the autosave layer did not
  have to change. The category one still accepts free text — type anything and
  press Enter.
- **The frontend has no build step** — plain HTML, CSS and framework-free JS,
  served straight out of `static/`. Themes follow your OS by default and the
  choice is remembered in `localStorage`.
- **Keyboard**: `/` focuses search, `Esc` closes an open project.
