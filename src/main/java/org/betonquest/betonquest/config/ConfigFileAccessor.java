package org.betonquest.betonquest.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class ConfigFileAccessor {
    private final File configFile;
    @Getter
    private final ConfigurationSection config;

    public ConfigFileAccessor(@NotNull final File configFile) {
        this.configFile = configFile;
        config = YamlConfiguration.loadConfiguration(this.configFile);
    }

    public void saveConfig() throws IOException {
        if (config.getKeys(true).isEmpty()) {
            if (!configFile.delete()) {
                throw new IOException("Could not delete file '" + configFile.getName() + "' in folder '" + configFile.getPath() + "'!");
            }
        } else {
            ((YamlConfiguration) config).save(configFile);
        }
    }
}
