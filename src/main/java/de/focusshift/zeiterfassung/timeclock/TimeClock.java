package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.ZonedDateTime;
import java.util.Optional;

public record TimeClock(Long id, UserId userId, ZonedDateTime startedAt, String comment, boolean isBreak, Optional<ZonedDateTime> stoppedAt) {

    TimeClock(UserId userId, ZonedDateTime startedAt) {
        this(null, userId, startedAt, "", false, Optional.empty());
    }

    static Builder builder(TimeClock timeClock) {
        return new Builder(timeClock);
    }

    static class Builder {
        private final Long id;
        private final UserId userId;
        private ZonedDateTime startedAt;
        private String comment;
        private boolean isBreak;
        private ZonedDateTime stoppedAt;

        private Builder(TimeClock timeClock) {
            this.id = timeClock.id();
            this.userId = timeClock.userId();
        }

        public Builder startedAt(ZonedDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder isBreak(boolean isBreak) {
            this.isBreak = isBreak;
            return this;
        }

        public Builder stoppedAt(ZonedDateTime stoppedAt) {
            this.stoppedAt = stoppedAt;
            return this;
        }

        public TimeClock build() {
            return new TimeClock(id, userId, startedAt, comment, isBreak, Optional.ofNullable(stoppedAt));
        }
    }
}
