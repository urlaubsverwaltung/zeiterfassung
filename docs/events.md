# Events

## Overtime

Publishing overtime related messages can be enabled/disabled with:

```properties
zeiterfassung.integration.overtime.enabled=true
```

Following events are published:

* [Overtime](../src/main/java/de/focusshift/zeiterfassung/integration/overtime/OvertimeRabbitEvent.java): Overtime has been made or updated for a specific date
