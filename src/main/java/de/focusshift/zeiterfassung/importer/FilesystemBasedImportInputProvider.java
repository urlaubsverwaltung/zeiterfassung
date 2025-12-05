package de.focusshift.zeiterfassung.importer;

import de.focusshift.zeiterfassung.importer.model.TenantExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

class FilesystemBasedImportInputProvider implements ImportInputProvider {

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemBasedImportInputProvider.class);

    private final JsonMapper jsonMapper;
    private final Path path;

    FilesystemBasedImportInputProvider(JsonMapper jsonMapper, String importFile) {
        this.jsonMapper = jsonMapper;
        this.path = Path.of(importFile);
    }

    @Override
    public Optional<TenantExport> fromExport() {
        LOG.info("Going to read users to import from file={}", this.path.toAbsolutePath());
        try (FileInputStream fos = new FileInputStream(path.toFile())) {
            return Optional.of(jsonMapper.readValue(fos, TenantExport.class));
        } catch (IOException e) {
            LOG.error("Error occurred while deserializing the JSON object", e);
            return Optional.empty();
        }
    }
}
