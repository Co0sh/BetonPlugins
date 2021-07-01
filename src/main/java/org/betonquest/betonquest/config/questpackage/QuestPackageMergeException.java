package org.betonquest.betonquest.config.questpackage;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when the instruction string has a wrong format.
 */
public class QuestPackageMergeException extends Exception {
    private static final long serialVersionUID = 3930029985307642083L;

    @Getter
    private final QuestPackage questPackage;
    private final List<String> duplicateKeys;

    /**
     * {@link Exception#Exception(String)}
     *
     * @param message the exceptions message.
     */
    public QuestPackageMergeException(final QuestPackage questPackage, final String message) {
        super(message);
        this.questPackage = questPackage;
        duplicateKeys = new ArrayList<>();
    }

    public void addDuplicateKey(final String key) {
        if (!duplicateKeys.contains(key)) {
            duplicateKeys.add(key);
        }
    }

    @Override
    public String getMessage() {
        final String packages = " Duplicate Keys: " + String.join(", ", duplicateKeys) + "!";
        return super.getMessage() + packages;
    }

    public boolean hasDuplicates() {
        return !duplicateKeys.isEmpty();
    }
}
