package de.focusshift.zeiterfassung.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties("zeiterfassung.menu")
@Validated
public class MenuProperties {

    @Valid
    private Help help = new Help();

    public Help getHelp() {
        return help;
    }

    public void setHelp(Help help) {
        this.help = help;
    }

    public static class Help {

        @URL
        @NotEmpty
        private String url = "https://urlaubsverwaltung.cloud/hilfe/?utm_source=zeiterfassung-open-source#dokumentation-zeiterfassung";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
