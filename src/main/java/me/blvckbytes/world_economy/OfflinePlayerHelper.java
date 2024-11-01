package me.blvckbytes.world_economy;

import de.tr7zw.nbtapi.NBTFile;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.world_economy.config.MainSection;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OfflinePlayerHelper implements Listener {

  private static final LastLocation NULL_LOCATION = new LastLocation(null, null);

  public record LastLocation(
    @Nullable String worldName,
    @Nullable WorldGroup worldGroup
  ) {}

  private final WorldGroupRegistry worldGroupRegistry;
  private final Logger logger;
  private final File mainWorldPlayerDataFolder;

  private final Map<UUID, LastLocation> lastLocationByUuidCache;

  private final Set<String> knownPlayerNames;
  private final Map<String, OfflinePlayer> offlinePlayerByLowerName;
  private final Map<UUID, String> offlinePlayerLowerNameByUuid;
  private final Map<UUID, OfflinePlayer> offlinePlayerById;

  public OfflinePlayerHelper(
    WorldGroupRegistry worldGroupRegistry,
    ConfigKeeper<MainSection> config,
    Logger logger
  ) {
    // NBT of OfflinePlayer-s is always stored within the playerdata-folder of the main world
    var mainWorld = Bukkit.getServer().getWorld("world");

    if (mainWorld == null)
      throw new IllegalStateException("Could not get a reference to the main-world");

    var mainWorldFolder = mainWorld.getWorldFolder();

    if (mainWorldFolder == null)
      throw new IllegalStateException("Could not get a reference to the main-world's world-folder");

    this.mainWorldPlayerDataFolder = new File(mainWorldFolder, "playerdata");
    this.worldGroupRegistry = worldGroupRegistry;
    this.logger = logger;
    this.lastLocationByUuidCache = new HashMap<>();

    config.registerReloadListener(lastLocationByUuidCache::clear);

    this.knownPlayerNames = new HashSet<>();
    this.offlinePlayerByLowerName = new WeakHashMap<>();
    this.offlinePlayerLowerNameByUuid = new HashMap<>();
    this.offlinePlayerById = new WeakHashMap<>();

    for (var player : Bukkit.getOfflinePlayers()) {
      offlinePlayerByLowerName.put(player.getName().toLowerCase(), player);
      knownPlayerNames.add(player.getName());
    }
  }

  public LastLocation getLastLocation(OfflinePlayer player) {
    synchronized (lastLocationByUuidCache) {
      var playerId = player.getUniqueId();

      if (player instanceof Player onlinePlayer) {
        lastLocationByUuidCache.remove(playerId);
        var worldName = onlinePlayer.getWorld().getName();
        return new LastLocation(worldName, worldGroupRegistry.getWorldGroupByMemberNameIgnoreCase(worldName));
      }

      LastLocation result;

      if ((result = lastLocationByUuidCache.get(playerId)) != null)
        return result;

      var worldName = getWorldNameLowerFromPlayerDataFile(playerId);

      if (worldName == null)
        return NULL_LOCATION;

      var worldGroup = worldGroupRegistry.getWorldGroupByMemberNameIgnoreCase(worldName);

      result = new LastLocation(worldName, worldGroup);
      lastLocationByUuidCache.put(playerId, result);
      return result;
    }
  }

  private @Nullable String getWorldNameLowerFromPlayerDataFile(UUID uuid) {
    var playerDataFile = new File(mainWorldPlayerDataFolder, uuid.toString() + ".dat");

    if (!playerDataFile.isFile())
      return null;

    try {
      var nbtFile = new NBTFile(playerDataFile);
      var dimensionValue = nbtFile.getString("Dimension");

      int keyDelimiter;

      if ((keyDelimiter = dimensionValue.indexOf(':')) < 0) {
        logger.severe("Expected separator-colon within \"Dimension\" in " + playerDataFile);
        return null;
      }

      var worldNameLower = dimensionValue.substring(keyDelimiter + 1).toLowerCase();

      if (worldNameLower.isBlank()) {
        logger.severe("Expected non-blank name for \"Dimension\" in " + playerDataFile);
        return null;
      }

      // https://minecraftology.fandom.com/wiki/List_of_Dimensions
      return switch (worldNameLower) {
        case "overworld" -> "world";
        case "the_nether" -> "world_nether";
        case "the_end" -> "world_the_end";
        default -> worldNameLower;
      };
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not access playerdata-file " + playerDataFile, e);
      return null;
    }
  }

  @SuppressWarnings("deprecation")
  public OfflinePlayer getByName(String name) {
    var lowerName = name.toLowerCase();

    OfflinePlayer result;

    if ((result = offlinePlayerByLowerName.get(lowerName)) != null)
      return result;

    result = Bukkit.getOfflinePlayer(lowerName);

    offlinePlayerByLowerName.put(lowerName, result);
    offlinePlayerLowerNameByUuid.put(result.getUniqueId(), lowerName);

    return result;
  }

  public OfflinePlayer getById(UUID id) {
    return offlinePlayerById.computeIfAbsent(id, Bukkit::getOfflinePlayer);
  }

  public List<String> createSuggestions(String input) {
    return knownPlayerNames
      .stream()
      .filter(name -> StringUtils.containsIgnoreCase(name, input))
      .limit(20)
      .toList();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();

    knownPlayerNames.add(player.getName());

    // Try to account for name-changes

    var cacheNameEntry = offlinePlayerLowerNameByUuid.remove(player.getUniqueId());

    if (cacheNameEntry != null)
      offlinePlayerByLowerName.remove(cacheNameEntry);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    synchronized (lastLocationByUuidCache) {
      var player = event.getPlayer();
      lastLocationByUuidCache.remove(player.getUniqueId());
    }
  }
}
