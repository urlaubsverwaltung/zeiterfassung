# Events

## Overtime

Publishing overtime related messages can be enabled/disabled with:

```properties
zeiterfassung.integration.overtime.enabled=true
```

Following events are published:

* [Overtime](../src/main/java/de/focusshift/zeiterfassung/integration/overtime/OvertimeRabbitEvent.java): Overtime has been made or updated for a specific date

## TimeEntry

Publishing time entry related messages can be enabled/disabled with:

```properties
zeiterfassung.integration.timeentry.enabled=true
```

Following events are published:

* [TimeEntryCreated](../src/main/java/de/focusshift/zeiterfassung/integration/timeentry/TimeEntryCreatedRabbitEvent.java): A time entry has been created
* [TimeEntryUpdated](../src/main/java/de/focusshift/zeiterfassung/integration/timeentry/TimeEntryUpdatedRabbitEvent.java): A time entry has been updated
* [TimeEntryDeleted](../src/main/java/de/focusshift/zeiterfassung/integration/timeentry/TimeEntryDeletedRabbitEvent.java): A time entry has been deleted

## TimeClock

Publishing time clock related messages can be enabled/disabled with:

```properties
zeiterfassung.integration.timeclock.enabled=true
```

Following events are published:

* [TimeClockStarted](../src/main/java/de/focusshift/zeiterfassung/integration/timeclock/TimeClockStartedRabbitEvent.java): A time clock has been started
* [TimeClockUpdated](../src/main/java/de/focusshift/zeiterfassung/integration/timeclock/TimeClockUpdatedRabbitEvent.java): A time clock has been updated
* [TimeClockStopped](../src/main/java/de/focusshift/zeiterfassung/integration/timeclock/TimeClockStoppedRabbitEvent.java): A time clock has been stopped

## WorkingTime

Publishing working time related messages can be enabled/disabled with:

```properties
zeiterfassung.integration.workingtime.enabled=true
```

Following events are published:

* [WorkingTimeCreated](../src/main/java/de/focusshift/zeiterfassung/integration/workingtime/WorkingTimeCreatedRabbitEvent.java): A working time has been created
* [WorkingTimeUpdated](../src/main/java/de/focusshift/zeiterfassung/integration/workingtime/WorkingTimeUpdatedRabbitEvent.java): A working time has been updated
* [WorkingTimeDeleted](../src/main/java/de/focusshift/zeiterfassung/integration/workingtime/WorkingTimeDeletedRabbitEvent.java): A working time has been deleted

## OvertimeAccount

Publishing overtime account related messages can be enabled/disabled with:

```properties
zeiterfassung.integration.overtime-account.enabled=true
```

Following events are published:

* [OvertimeAccountUpdated](../src/main/java/de/focusshift/zeiterfassung/integration/overtimeaccount/OvertimeAccountUpdatedRabbitEvent.java): An overtime account has been updated
