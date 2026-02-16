# Zeiterfassung REST API

## Übersicht

Die Zeiterfassung REST API ermöglicht es Nutzern, ihre Arbeitszeiteinträge programmatisch zu verwalten.

## Authentifizierung

Die API verwendet API-Keys zur Authentifizierung. Jeder Request muss einen gültigen API-Key im `Authorization` Header enthalten:

```
Authorization: Bearer zf_<your-api-key>
```

### API-Key erstellen

1. Navigiere zu `/apikeys` in der Web-Oberfläche
2. Erstelle einen neuen API-Key mit einer Beschreibung
3. Kopiere den generierten Key - er wird nur einmal angezeigt!

**Hinweis:** Die Permission `ZEITERFASSUNG_API_ACCESS` muss für den Nutzer aktiviert sein.

## Basis-URL

```
https://your-zeiterfassung-instance/api
```

## Endpoint Dokumentation

Die vollständige interaktive OpenAPI-Dokumentation ist verfügbar unter:

```
https://your-zeiterfassung-instance/swagger-ui/index.html
```
