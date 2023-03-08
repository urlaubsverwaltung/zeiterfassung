package de.focusshift.zeiterfassung.development;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.PeriodUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Period;

import static java.time.temporal.ChronoUnit.DAYS;

@Validated
@ConfigurationProperties("zeiterfassung.development.demodata")
public final class DemoDataProperties {

    private boolean create = false;
    @PeriodUnit(DAYS)
    private Period past = Period.ofDays(30);
    @PeriodUnit(DAYS)
    private Period future = Period.ofDays(0);

    public boolean isCreate() {
        return create;
    }

    public Period getPast() {
        return past;
    }

    public Period getFuture() {
        return future;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public void setPast(Period past) {
        this.past = past;
    }

    public void setFuture(Period future) {
        this.future = future;
    }
}
