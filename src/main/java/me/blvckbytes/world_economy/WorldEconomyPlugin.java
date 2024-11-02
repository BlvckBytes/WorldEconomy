package me.blvckbytes.world_economy;

import me.blvckbytes.bukkitevaluable.CommandUpdater;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.bukkitevaluable.ConfigManager;
import me.blvckbytes.bukkitevaluable.section.ACommandSection;
import me.blvckbytes.gpeee.Tuple;
import me.blvckbytes.world_economy.commands.*;
import me.blvckbytes.world_economy.config.MainSection;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class WorldEconomyPlugin extends JavaPlugin {

  private @Nullable Economy economyProvider;
  private @Nullable EconomyDataRegistry economyDataRegistry;

  @Override
  public void onEnable() {
    var logger = getLogger();

    try {
      var configManager = new ConfigManager(this, "config");
      var config = new ConfigKeeper<>(configManager, "config.yml", MainSection.class);

      if (!Bukkit.getPluginManager().isPluginEnabled("Vault"))
        throw new IllegalStateException("Expected Vault to be present and enabled");

      var worldGroupRegistry = new WorldGroupRegistry(config, logger);

      var offlinePlayerHelper = new OfflinePlayerHelper(worldGroupRegistry, config, logger);
      Bukkit.getServer().getPluginManager().registerEvents(offlinePlayerHelper, this);

      Bukkit.getServer().getPluginManager().registerEvents(offlinePlayerHelper, this);

      economyDataRegistry = new EconomyDataRegistry(this, offlinePlayerHelper, worldGroupRegistry, config, logger);

      Economy provider = new WorldEconomyProvider(this, config, economyDataRegistry, offlinePlayerHelper);

      if (config.rootSection.economy.logProviderCalls)
        provider = new LoggingEconomyWrapper(provider, logger);

      registerProvider(provider);

      var payCommand = new PayCommand(offlinePlayerHelper, economyDataRegistry, worldGroupRegistry, economyProvider, config);
      var balanceCommand = new BalanceCommand(economyDataRegistry, economyProvider, worldGroupRegistry, offlinePlayerHelper, config);

      setupCommands(config, List.of(
        new Tuple<>(config.rootSection.commands.balance, balanceCommand),
        new Tuple<>(config.rootSection.commands.balanceGroup, balanceCommand),
        new Tuple<>(config.rootSection.commands.balances, new BalancesCommand(
          economyDataRegistry, economyProvider, worldGroupRegistry, offlinePlayerHelper, config
        )),
        new Tuple<>(config.rootSection.commands.balanceTop, new BalanceTopCommand(
          offlinePlayerHelper, economyDataRegistry, worldGroupRegistry, economyProvider, config
        )),
        new Tuple<>(config.rootSection.commands.money, new MoneyCommand(
          offlinePlayerHelper, economyDataRegistry, worldGroupRegistry, economyProvider, config
        )),
        new Tuple<>(config.rootSection.commands.pay, payCommand),
        new Tuple<>(config.rootSection.commands.payGroup, payCommand),
        new Tuple<>(config.rootSection.commands.reload, new ReloadCommand(config, logger))
      ));

      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
        new WorldEconomyPlaceholderExpansion(this, economyDataRegistry, worldGroupRegistry, economyProvider).register();
        logger.info("Registered PlaceholderAPI-expansion for top-list access");
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not initialize plugin", e);
      Bukkit.getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    try {
      unregisterProvider();
    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Could not unregister economy-provider", e);
    }

    if (economyDataRegistry != null) {
      economyDataRegistry.onShutdown();
      economyDataRegistry = null;
    }
  }

  private void setupCommands(
    ConfigKeeper<MainSection> config,
    List<Tuple<ACommandSection, CommandExecutor>> commandTuples
  ) {
    var commandUpdater = new CommandUpdater(this);
    var commandUpdateDispatchers = new ArrayList<Runnable>();

    for (var commandTuple : commandTuples) {
      var commandSection = commandTuple.a;
      var command = getCommand(commandSection.initialName);

      if (command == null)
        throw new IllegalStateException("Could not locate command \"" + commandSection.initialName + "\"");

      var executor = commandTuple.b;

      command.setExecutor(executor);
      commandUpdateDispatchers.add(() -> commandSection.apply(command, commandUpdater));
    }

    Runnable updateCommands = () -> {
      for (var commandUpdateDispatcher : commandUpdateDispatchers)
        commandUpdateDispatcher.run();

      commandUpdater.trySyncCommands();
    };

    updateCommands.run();
    config.registerReloadListener(updateCommands);
  }

  @SuppressWarnings("unchecked")
  private void registerProvider(Economy provider) throws ClassNotFoundException {
    this.economyProvider = provider;

    Bukkit.getServer().getServicesManager().register(
      (Class<Object>) Class.forName("net.milkbowl.vault.economy.Economy"),
      provider,
      this, ServicePriority.High
    );
  }

  private void unregisterProvider() {
    if (economyProvider == null)
      return;

    Bukkit.getServer().getServicesManager().unregister(economyProvider);
    economyProvider = null;
  }
}
