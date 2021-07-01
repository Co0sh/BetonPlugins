package org.betonquest.betonquest.config.questpackage;

import lombok.CustomLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class QuestPackageManager {
    private final Map<String, QuestPackage> questPackages;

    public QuestPackageManager(final File rootFolder, final String mainFileName) {
        questPackages = new HashMap<>();
        searchForPackages(rootFolder, rootFolder, mainFileName);
    }

    private List<File> searchForPackages(final File rootFolder, final File currentFolder, final String mainFileName) {
        if (!currentFolder.isDirectory()) {
            return Collections.singletonList(currentFolder);
        }
        final File[] content = currentFolder.listFiles();
        if (content == null) {
            LOG.error(null, "Could not get files for directory '" + currentFolder.getPath() + "'!");
            return Collections.emptyList();
        }

        final Optional<File> mainFile = Arrays.stream(content).filter(file -> file.getName().equals(mainFileName)).findFirst();
        final List<File> files = new ArrayList<>();
        for (final File file : content) {
            files.addAll(searchForPackages(rootFolder, file, mainFileName));
        }

        if (mainFile.isPresent()) {
            createQuestPackage(rootFolder, currentFolder, mainFile.get(), files);
            return Collections.emptyList();
        }
        return files;
    }

    private void createQuestPackage(final File rootFolder, final File currentFolder, final File mainFile, final List<File> files) {
        final String questPackagePath = rootFolder.toURI().relativize(currentFolder.toURI()).toString()
                .replace('/', '.').trim().replace(' ', '_');
        try {
            questPackages.put(questPackagePath, new QuestPackage(questPackagePath, mainFile, files));
        } catch (final QuestPackageMergeException e) {
            LOG.error(e.getQuestPackage(), "Could not load QuestPackage!", e);
        }
    }

    public QuestPackage getQuestPackage(final String name) {
        return questPackages.get(name);
    }
}
