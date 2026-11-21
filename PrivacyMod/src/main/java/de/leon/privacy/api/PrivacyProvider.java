package de.leon.privacy.api;

import java.util.Collection;

/** Implement this interface to contribute privacy checks from another mod. */
@FunctionalInterface
public interface PrivacyProvider {
    Collection<PrivacyEntry> getEntries();
}
