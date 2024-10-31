package me.blvckbytes.world_economy;

import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.world_economy.config.MainSection;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WorldGroupRegistry {

  private final Logger logger;
  private final ConfigKeeper<MainSection> config;
  private final Map<String, WorldGroup> worldGroupByIdentifierNameLower;
  private final Map<String, WorldGroup> worldGroupByMemberNameLower;
  private final List<String> worldGroupIdentifiers;

  public WorldGroupRegistry(
    ConfigKeeper<MainSection> config,
    Logger logger
  ) {
    this.config = config;
    this.logger = logger;
    this.worldGroupByIdentifierNameLower = new LinkedHashMap<>();
    this.worldGroupByMemberNameLower = new HashMap<>();
    this.worldGroupIdentifiers = new ArrayList<>();

    this.config.registerReloadListener(this::loadFromConfig);
    this.loadFromConfig();
  }

  public Collection<WorldGroup> getWorldGroups() {
    return Collections.unmodifiableCollection(worldGroupByIdentifierNameLower.values());
  }

  public List<String> createSuggestions(@Nullable String input) {
    return worldGroupIdentifiers.stream().filter(it -> input == null || StringUtils.containsIgnoreCase(it, input)).toList();
  }

  public @Nullable WorldGroup getWorldGroupByMemberNameIgnoreCase(String memberName) {
    return worldGroupByMemberNameLower.get(memberName.toLowerCase().trim());
  }

  public @Nullable WorldGroup getWorldGroupByIdentifierNameIgnoreCase(String identifierName) {
    return worldGroupByIdentifierNameLower.get(identifierName.toLowerCase().trim());
  }

  private void loadFromConfig() {
    this.worldGroupByIdentifierNameLower.clear();
    this.worldGroupByMemberNameLower.clear();
    this.worldGroupIdentifiers.clear();

    for (var worldGroupEntry : this.config.rootSection.worldGroups.worldGroups.entrySet()) {
      var identifierName = worldGroupEntry.getKey();
      var identifierNameLower = identifierName.toLowerCase();
      var groupDataSection = worldGroupEntry.getValue();

      var worldGroup = new WorldGroup(
        identifierNameLower,
        groupDataSection.displayName.asScalar(ScalarType.STRING, groupDataSection.builtBaseEnvironment),
        groupDataSection.members
          .stream()
          .map(String::toLowerCase)
          .collect(Collectors.toSet()),
        groupDataSection.startingBalance
      );

      worldGroupByIdentifierNameLower.put(identifierNameLower, worldGroup);
      worldGroupIdentifiers.add(identifierName);

      for (var memberNameLower : worldGroup.memberWorldNamesLower())
        worldGroupByMemberNameLower.put(memberNameLower, worldGroup);
    }

    logger.info("Loaded " + worldGroupByIdentifierNameLower.size() + " world-groups");
  }
}
