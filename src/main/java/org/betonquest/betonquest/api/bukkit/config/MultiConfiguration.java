package org.betonquest.betonquest.api.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class MultiConfiguration extends MemoryConfiguration implements ConfigurationSection {

    private final ConfigurationSection[] sourceConfigs;

    private final ConfigurationSection unassociatedKeys;

    private final Map<String, List<ConfigurationSection>> keyList;

    public MultiConfiguration(final ConfigurationSection... configs) throws KeyConflictConfigurationException {
        super();
        this.sourceConfigs = configs;
        this.unassociatedKeys = new MemoryConfiguration();
        this.keyList = new ConcurrentHashMap<>();
        loadKeyList(sourceConfigs);
        validateMerge();
        merge();
    }

    /**
     * Only validate if the given configurations can be merged. This method does not merge the configs!
     *
     * @throws KeyConflictConfigurationException when the given section contain conflicting keys
     */
    private void validateMerge() throws KeyConflictConfigurationException {
        final Map<String, List<ConfigurationSection>> duplicates = findDuplicateKeys(keyList);
        if (!duplicates.isEmpty()) {
            throw new KeyConflictConfigurationException(duplicates);
        }
    }

    /**
     * Extract all keys from all section and put them into a key to section-list map.
     *
     * @param sourceConfigs configuration sections to load
     * @return key to section-list map of given configs
     */
    private void loadKeyList(final ConfigurationSection... sourceConfigs) {
        Arrays.stream(sourceConfigs).forEach(sourceConfig -> sourceConfig.getKeys(true).stream()
                .filter(sectionKey -> !sourceConfig.isConfigurationSection(sectionKey))
                .forEach(sectionKey -> addToList(keyList, sectionKey, sourceConfig)));
    }

    /**
     * Analyzes key to section map for duplicated keys. It will also check for key/section collisions.
     *
     * @param keyList key to section-list map
     * @return map of duplicates
     */
    @NotNull
    private Map<String, List<ConfigurationSection>> findDuplicateKeys(final Map<String, List<ConfigurationSection>> keyList) {
        final Map<String, List<ConfigurationSection>> duplicates = keyList.entrySet().stream()
                .filter(key -> key.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        keyList.keySet().forEach(sectionKey -> Arrays.stream(sourceConfigs)
                .filter(sourceConfig -> sourceConfig.isConfigurationSection(sectionKey))
                .forEach(sourceConfig -> addToList(duplicates, sectionKey, sourceConfig)));
        return duplicates;
    }

    /**
     * Adds a value to a list inside a map. If the List doesn't exist yet it will be created.
     *
     * @param keyList      map containing lists with values
     * @param sectionKey   key to use for map
     * @param sourceConfig config to add to list
     */
    private void addToList(final Map<String, List<ConfigurationSection>> keyList, final String sectionKey, final ConfigurationSection sourceConfig) {
        keyList.computeIfAbsent(sectionKey, _key -> new CopyOnWriteArrayList<>()).add(sourceConfig);
    }

    private void merge() {
        keyList.forEach((key, value) -> set(key, value.get(0).get(key)));
    }

    @Override
    public void set(@NotNull final String path, @Nullable final Object value) {
        checkDuplicateKeys(path);

        super.set(path, value);
        if (keyList.containsKey(path)) {
            keyList.get(path).get(0).set(path, value);
        } else {
            addToList(keyList, path, unassociatedKeys);
            unassociatedKeys.set(path, value);
        }
    }

    private void checkDuplicateKeys(final @NotNull String path) {
        final Map<String, List<ConfigurationSection>> duplicates = new HashMap<>();
        Arrays.stream(sourceConfigs)
                .filter(config -> config.isConfigurationSection(path))
                .forEach(config -> addToList(duplicates, path, config));
        if (!duplicates.isEmpty()) {
            throw new UncheckedKeyConflictConfigurationException(new KeyConflictConfigurationException(duplicates));
        }
    }

    public ConfigurationSection getUnassociatedKeys() {
        return new UnmodifiableConfigurationSection(unassociatedKeys);
    }
}
