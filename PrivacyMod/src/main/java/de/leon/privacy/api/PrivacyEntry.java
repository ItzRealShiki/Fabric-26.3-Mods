package de.leon.privacy.api;

import java.util.Objects;

/** A privacy status item contributed by the core mod or an integration. */
public record PrivacyEntry(String id, String title, String explanation, PrivacyVisibility visibility) {
    public PrivacyEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(explanation, "explanation");
        Objects.requireNonNull(visibility, "visibility");
    }
}
