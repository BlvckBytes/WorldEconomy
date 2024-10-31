package me.blvckbytes.world_economy;

import com.google.gson.*;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.world_economy.config.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EconomyDataRegistry implements BalanceConstraint {

  private static final Gson GSON_INSTANCE = new GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapter(
      EconomyAccount.class,
      (JsonSerializer<EconomyAccount>) (account, type, jsonSerializationContext) -> (
        new JsonPrimitive(account.getBalance())
      )
    )
    .create();

  private final Plugin plugin;
  private final File playersFolder;
  private final OfflinePlayerHelper offlinePlayerHelper;
  private final WorldGroupRegistry worldGroupRegistry;
  private final ConfigKeeper<MainSection> config;
  private final Logger logger;

  private final Map<WorldGroup, EconomyAccountRegistry> accountRegistryByWorldGroup;
  private boolean isShuttingDown;

  public EconomyDataRegistry(
    Plugin plugin,
    OfflinePlayerHelper offlinePlayerHelper,
    WorldGroupRegistry worldGroupRegistry,
    ConfigKeeper<MainSection> config,
    Logger logger
  ) {
    this.playersFolder = new File(plugin.getDataFolder(), "players");

    if (playersFolder.exists()) {
      if (!playersFolder.isDirectory())
        throw new IllegalStateException("Expected " + playersFolder + " to be a directory");
    } else {
      if (!playersFolder.mkdirs())
        throw new IllegalStateException("Could not create directories for path " + playersFolder);
    }

    this.plugin = plugin;
    this.offlinePlayerHelper = offlinePlayerHelper;
    this.worldGroupRegistry = worldGroupRegistry;
    this.config = config;
    this.logger = logger;

    this.accountRegistryByWorldGroup = new HashMap<>();

    var numberOfLoadedFiles = loadAllFiles();
    logger.info("Loaded " + numberOfLoadedFiles + " player-files");

    invokeNextWritePeriod();
  }

  // ================================================================================
  // Public API
  // ================================================================================

  public void onShutdown() {
    synchronized (this) {
      isShuttingDown = true;
    }

    writeDirtyAccounts();
  }

  public EconomyAccountRegistry getAccountRegistry(WorldGroup worldGroup) {
    return accountRegistryByWorldGroup.computeIfAbsent(worldGroup, key ->
      new EconomyAccountRegistry(worldGroup, this)
    );
  }

  public @Nullable EconomyAccount getForLastWorld(OfflinePlayer player) {
    synchronized (this) {
      var worldGroup = offlinePlayerHelper.getLastLocation(player).worldGroup();

      if (worldGroup == null)
        return null;

      var accountRegistry = accountRegistryByWorldGroup.get(worldGroup);
      return accountRegistry.getAccount(player);
    }
  }

  public @Nullable EconomyAccount getForWorldName(OfflinePlayer player, String worldName) {
    synchronized (this) {
      var worldGroup = worldGroupRegistry.getWorldGroupByMemberNameIgnoreCase(worldName);

      if (worldGroup == null)
        return null;

      var accountRegistry = accountRegistryByWorldGroup.get(worldGroup);
      return accountRegistry.getAccount(player);
    }
  }

  public boolean createForLastWorld(OfflinePlayer player) {
    return getForLastWorld(player) != null;
  }

  public boolean createForWorldName(OfflinePlayer player, String worldName) {
    return getForWorldName(player, worldName) != null;
  }

  @Override
  public boolean isWithinRange(EconomyAccount account, double balance) {
    Double constraint;

    // While it would be elegant to do a return clampValue(balance) == balance, I'm a bit afraid of
    // double comparing precision issues, and thereby prefer the verbose alternative - for now, at least.

    if ((constraint = config.rootSection.economy.minMoney) != null) {
      if (balance < constraint)
        return false;
    }

    if ((constraint = config.rootSection.economy.maxMoney) != null) {
      return !(balance > constraint);
    }

    return true;
  }

  // ================================================================================
  // File-Persistence
  // ================================================================================

  private void invokeNextWritePeriod() {
    Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
      writeDirtyAccounts();

      synchronized (this) {
        if (!isShuttingDown)
          invokeNextWritePeriod();
      }
    }, config.rootSection.economy.cacheWritePeriodSeconds * 20);
  }

  private int loadAllFiles() {
    var playerFiles = playersFolder.listFiles();

    if (playerFiles == null)
      return 0;

    var loadedPlayerFileCount = 0;

    for (var playerFile : playerFiles) {
      if (!playerFile.isFile())
        continue;

      if (!playerFile.getName().endsWith(".json"))
        continue;

      var fileName = playerFile.getName();
      var dotIndex = fileName.lastIndexOf('.');

      if (dotIndex < 0)
        continue;

      var fileNameWithoutExtension = fileName.substring(0, dotIndex);

      UUID playerId;

      try {
        playerId = UUID.fromString(fileNameWithoutExtension);
      } catch (Exception e) {
        continue;
      }

      var holder = offlinePlayerHelper.getById(playerId);
      var playerData = loadPlayerFile(playerFile, holder);

      if (playerData == null)
        continue;

      for (var dataEntry : playerData.entrySet()) {
        var accountRegistry = getAccountRegistry(dataEntry.getKey());
        accountRegistry.registerAccount(holder, dataEntry.getValue());
      }

      ++loadedPlayerFileCount;
    }

    return loadedPlayerFileCount;
  }

  private void writeDirtyAccounts() {
    var accountRegistries = accountRegistryByWorldGroup.values();

    for (var accountRegistry : accountRegistries) {
      for (var account : accountRegistry.getAccounts()) {
        if (!account.isDirty())
          continue;

        var holderAccounts = new ArrayList<EconomyAccount>();

        for (var _accountRegistry : accountRegistries) {
          var _account = _accountRegistry.getAccount(account.holder);

          if (_account == null)
            continue;

          _account.clearDirty();
          holderAccounts.add(_account);
        }

        storePlayerFile(account.holder, holderAccounts);
      }
    }
  }

  private @Nullable HashMap<WorldGroup, EconomyAccount> loadPlayerFile(File playerFile, OfflinePlayer holder) {
    if (!playerFile.isFile())
      return null;

    try (
      var fileReader = new FileReader(playerFile)
    ) {
      var playerFileObject = GSON_INSTANCE.fromJson(fileReader, JsonObject.class);
      var result = new HashMap<WorldGroup, EconomyAccount>();

      for (var objectEntry : playerFileObject.entrySet()) {
        var worldGroupName = objectEntry.getKey();

        if (!(
          objectEntry.getValue() instanceof JsonPrimitive value && value.isNumber()
        )) {
          logger.log(Level.SEVERE, "Value of world-group \"" + worldGroupName + "\" of \"" + playerFile + "\" is not a number; skipping");
          continue;
        }

        var worldGroup = worldGroupRegistry.getWorldGroupByIdentifierNameIgnoreCase(worldGroupName);

        if (worldGroup == null) {
          logger.log(Level.WARNING, "World-group \"" + worldGroupName + "\" of \"" + playerFile + "\" could not be matched with an existing world-group; skipping");
          continue;
        }

        var balance = value.getAsDouble();

        if (config.rootSection.economy.doClampOnLoad)
          balance = clampValue(balance);

        result.put(worldGroup, new EconomyAccount(holder, worldGroup, balance, this));
      }

      return result;
    } catch (Exception loadException) {
      logger.log(Level.SEVERE, "Encountered corrupted player-file " + playerFile + "; copying to .bak and starting anew", loadException);

      try {
        Files.copy(
          playerFile.toPath(),
          new File(playersFolder, playerFile.getName() + ".bak").toPath(),
          StandardCopyOption.REPLACE_EXISTING
        );
      } catch (Exception copyException) {
        logger.log(Level.SEVERE, "Could not make a backup of " + playerFile + " by copying to .bak and starting anew", copyException);
      }

      return new HashMap<>();
    }
  }

  private void storePlayerFile(OfflinePlayer holder, Collection<EconomyAccount> accounts) {
    var playerId = holder.getUniqueId();
    var playerFile = new File(playersFolder, playerId + ".json");

    if (!playerFile.exists()) {
      try {
        if (!playerFile.createNewFile())
          throw new IllegalStateException("Create was unsuccessful for unknown reasons");
      } catch (Exception creationException) {
        logger.log(Level.SEVERE, "Could not create player-file " + playerFile, creationException);
        return;
      }
    }

    var dataMap = new HashMap<String, Double>();

    for (var account : accounts)
      dataMap.put(account.worldGroup.identifierNameLower(), account.getBalance());

    try {
      var fileContents = GSON_INSTANCE.toJson(dataMap);

      try (
        var fileWriter = new FileWriter(playerFile, false)
      ) {
        fileWriter.write(fileContents);
      }
    } catch (Exception storeException) {
      logger.log(Level.SEVERE, "Could not store player-data to player-file " + playerFile, storeException);
    }
  }

  private double clampValue(double value) {
    Double constraint;

    if ((constraint = config.rootSection.economy.minMoney) != null) {
      if (value < constraint)
        value = constraint;
    }

    if ((constraint = config.rootSection.economy.maxMoney) != null) {
      if (value > constraint)
        value = constraint;
    }

    return value;
  }
}
