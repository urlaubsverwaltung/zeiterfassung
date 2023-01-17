# Operations example with docker compose

## TL;DR

```bash
docker-compose up -d
```

goto http://localhost:8080 and login with one of the [demo users](https://github.com/urlaubsverwaltung/zeiterfassung#demo-data-users)

## Details

This is an example for an environment of Zeiterfassung with [docker-compose](https://docs.docker.com/compose/), including:

* zeiterfassung, the application itself
* postgres, database for storing application data
* keycloak, oidc-compatible identity and access management for user and access handling, configured with demo users
* mailhog, as mailserver mock, which should be replaced with your mail server

Additionally, you need to make sure:
* to add persistence for keycloak, see [keycloak documentation](https://www.keycloak.org/server/db)
* to install OS updates on host system
* to have all specified dependencies (keycloak, postgres) up-to-date
* create backups (application data, keycloak data) on regularly bases
* keep Zeiterfassung up-to-date

If you need any assistance for installation, you can reach out via [E-Mail](mailto:info@urlaubsverwaltung.cloud?subject=Zeiterfassung%20-%20OnPremise%20Support)
