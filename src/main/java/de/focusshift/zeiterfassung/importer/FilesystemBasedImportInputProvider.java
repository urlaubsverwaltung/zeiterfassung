package de.focusshift.zeiterfassung.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.focusshift.zeiterfassung.importer.model.TenantExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

class FilesystemBasedImportInputProvider implements ImportInputProvider {

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemBasedImportInputProvider.class);

    private final ObjectMapper objectMapper;
    private final Path path;

    FilesystemBasedImportInputProvider(ObjectMapper objectMapper, String importFile) {
        this.objectMapper = objectMapper;
        this.path = Path.of(importFile);
    }

    @Override
    public Optional<TenantExport> fromExport() {
        LOG.info("Going to read users to import from file={}", this.path.toAbsolutePath());
        try (FileInputStream fos = new FileInputStream(path.toFile())) {
            return Optional.of(objectMapper.readValue(fos, TenantExport.class));
        } catch (IOException e) {
            LOG.error("Error occurred while deserializing the JSON object", e);
            return Optional.empty();
        }
    }
}
