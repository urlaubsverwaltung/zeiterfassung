# Zeiterfassung [![Build Status](https://github.com/urlaubsverwaltung/zeiterfassung/workflows/Build/badge.svg)](https://github.com/urlaubsverwaltung/zeiterfassung/actions?query=workflow%3A%22Build)

Ist bei euch die Zeiterfassung auch gerade ein großes Thema? Das trifft sich gut!

Durchstarten mit der Zeiterfassung! Folgende Funktionen sind bereits enthalten:

* Erfassen von Zeiten: Kommt-Geht Erfassung oder einzelne Zeitbuchungen
* Stoppuhr-Modus
* Visualisierung der geleisteten Stunden auf Wochen und Monatsebene
* Berichte für eigene erfasste Stunden (CSV-Download)
* Berichte für Vorgesetzte (CSV-Download)

Unsere Entwicklung lebt von eurem Feedback. Wir freuen uns die Zeiterfassung mit euch zusammen voranzubringen.
* [GitHub Issue](https://github.com/urlaubsverwaltung/zeiterfassung/issues/new/choose)
* [E-Mail](mailto:info@urlaubsverwaltung.cloud?subject=Zeiterfassung%20-%20Nutzer%20Feedback)

Wenn du mehr Informationen und Bilder über dieses Projekt sehen möchtest dann schaue auf unserer [Landingpage](https://urlaubsverwaltung.cloud/zeiterfassung/) vorbei.

![Ein Bild der Zeiterfassung auf verschiedenen Endgeräten](docs/zeiterfassung-screens.png)

# Dokumentation Entwicklung

⚠️ Die Dokumentation Entwicklung ist aktuell nur in englischer Sprache verfügbar.

## Development

### Prerequisites

* [JDK 17](https://adoptium.net)
* [Docker 20.10.+](https://docs.docker.com/get-docker/)
* [Docker Compose](https://docs.docker.com/compose/install/)


### Clone the repository

Without GitHub account

```bash
https://github.com/urlaubsverwaltung/zeiterfassung.git
```

with GitHub account

```bash
git clone git@github.com:urlaubsverwaltung/zeiterfassung.git
```

### Start the Zeiterfassung

Start the needed dependencies like the database and the keycloak server with

```shell
docker-compose up -d
```

The Zeiterfassung is a [Spring Boot](http://projects.spring.io/spring-boot/) application and can be started 
with the `dev-singletenant` profile that will also generate demo data.

```shell
./mvnw clean spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev-singletenant"
```

and for Windows user with

```bash
./mvnw.cmd clean spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=singletenant"
```

### How to use the Zeiterfassung

The application can then be controlled in the browser via [http://localhost:8060/](http://localhost:8060/).

With the `dev-singletenant` profile a Postgres database is used and demo data is created,
i.e. time entries, tenants and users. Therefore, you can now log into the web interface with different
users.

#### Demo data users 

As a user of a tenant can log in via `http://localhost:8060/oauth2/authorization/b0838c26` and visit `http://localhost:8060/`:

| username            | password | role             |
|---------------------|----------|------------------|
| boss@example.org    | secret   | view_reports_all |
| office@example.org  | secret   | view_reports_all |
| user@example.org    | secret   |                  |


### git hooks (optional)

There are some app specific git hooks to automate stuff like:

* Install NodeJS dependencies after `git pull` when `package-lock.json` has changed
* Format files on commit

If you want to take advantage of this automation you can run:

```bash
git config core.hooksPath '.githooks' 
```

The git hooks can be found in the [.githooks](./.githooks/) directory.

### Import DB Dump

You can import a db dump created via `pg_dump` into docker-compose based postgres:

* paste dump here into root directory and name it `dumpfile.sql`
* edit docker-compose.yml 
```yaml
services:
  postgres:
  ...
    volumes:
      - ./dumpfile.sql:/tmp/dumpfile.sql:ro
```
* start with a fresh docker-compose postgres
* open shell to postgres docker container `docker-compose exec postgres bash`
* exec command inside postgres docker container `PGPASSWORD=$POSTGRES_PASSWORD psql -U $POSTGRES_USER -d $POSTGRES_DB -f /tmp/dumpfile.sql`

## Release

### GitHub action

Go to the GitHub action with the name [release trigger][github-action-release-trigger].
* Click on "Run workflow"
* Add the "Milestone ID" (see in the uri of a milestone)
* Add "Release version"
* Add "Next version"
* Run the workflow

## 3rd party resources

* Icons: https://heroicons.dev
* Sloth image: https://pixabay.com/de/illustrations/faultiere-säugetiere-pelzig-5599313/

[github-action-release-trigger]: https://github.com/focus-shift/zeiterfassung/actions/workflows/release-trigger.yml "Release Trigger"
