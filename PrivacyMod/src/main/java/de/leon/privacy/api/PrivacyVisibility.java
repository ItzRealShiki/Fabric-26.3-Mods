package de.leon.privacy.api;

/**
 * Describes what this client can verify about an information category.
 * UNKNOWN is deliberately distinct from PRIVATE: absence of evidence is not protection.
 */
public enum PrivacyVisibility {
    PRIVATE,
    PARTIALLY_EXPOSED,
    EXPOSED,
    UNKNOWN
}
