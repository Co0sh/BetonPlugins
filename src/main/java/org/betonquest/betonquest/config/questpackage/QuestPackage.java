package org.betonquest.betonquest.config.questpackage;

import lombok.Getter;
import org.betonquest.betonquest.config.ConfigFileAccessor;
import org.betonquest.betonquest.utils.Zipper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class QuestPackage {
    @Getter
    private final String name;
    private final MemoryConfiguration configurationSection;
    private final File mainFile;
    private final List<File> files;

    public QuestPackage(final String name, final File mainFile, final List<File> files) throws QuestPackageMergeException {
        this.name = name;
        this.mainFile = mainFile;
        this.files = files;
        this.configurationSection = new MemoryConfiguration();
        mergeAllFiles();
    }

    private void mergeAllFiles() throws QuestPackageMergeException {
        final QuestPackageMergeException mergeException = new QuestPackageMergeException(this,
                "The QuestPackage '" + name + "' contains duplicate keys!");
        for (final File file : files) {
            final ConfigFileAccessor accessor = new ConfigFileAccessor(file);
            final ConfigurationSection section = accessor.getConfig();
            for (final String key : section.getKeys(true)) {
                if (section.isConfigurationSection(key)) {
                    continue;
                }
                final String newKey = key.contains(":") ? key : "betonquest:" + key;
                if (configurationSection.contains(newKey)) {
                    mergeException.addDuplicateKey(newKey);
                } else {
                    configurationSection.set(newKey, section.get(key));
                }
            }
        }
        if (mergeException.hasDuplicates()) {
            throw mergeException;
        }
    }

    public void zipQuestPackage(@NotNull final String outputPath) {
        new Zipper(files, outputPath);
    }

    public QuestPackageSection getQuestPackageSection(final String key) {
        return new QuestPackageSection("betonquest", key);
    }

    public QuestPackageSection getQuestPackageSection(final JavaPlugin plugin, final String key) {
        return new QuestPackageSection(plugin.getName().toLowerCase(Locale.ROOT), key);
    }

    public class QuestPackageSection {
        private final String nameSpace;
        private final String key;

        protected QuestPackageSection(final String nameSpace, final String key) {
            this.nameSpace = nameSpace;
            this.key = key;
        }

        public ConfigurationSection getConfig() {
            return configurationSection.getConfigurationSection(nameSpace + ":" + key);
        }

        public void setValue(@NotNull final String path, final Object object) throws IOException {
            setValue(null, path, object);
        }

        public void setValue(@Nullable final String file, @NotNull final String path, final Object object) throws IOException {
            final ConfigFileAccessor configFileAccessor;
            if (file == null) {
                final ConfigFileAccessor accessor = findQuestFileAccessor(path);
                configFileAccessor = accessor == null ? new ConfigFileAccessor(mainFile) : accessor;
            } else {
                configFileAccessor = new ConfigFileAccessor(new File(mainFile.getParentFile(), file));
            }
            final String nameSpacedKey;
            if (configFileAccessor.getConfig().contains(nameSpace + ":" + key)) {
                nameSpacedKey = nameSpace + ":" + key;
            } else {
                nameSpacedKey = key;
            }
            configFileAccessor.getConfig().set(nameSpacedKey + "." + path, object);
            configFileAccessor.saveConfig();
        }

        @Nullable
        private ConfigFileAccessor findQuestFileAccessor(@NotNull final String path) {
            for (final File file : files) {
                final ConfigFileAccessor accessor = new ConfigFileAccessor(file);
                final ConfigurationSection section = accessor.getConfig();
                if (!section.contains(nameSpace + ":" + key + "." + path)
                        || "betonquest".equals(nameSpace) && !section.contains(key + "." + path)) {
                    return accessor;
                }
            }
            return null;
        }
    }
}
