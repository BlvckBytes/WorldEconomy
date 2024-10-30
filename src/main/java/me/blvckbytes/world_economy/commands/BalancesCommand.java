package me.blvckbytes.world_economy.commands;

import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.world_economy.*;
import me.blvckbytes.world_economy.config.MainSection;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class BalancesCommand extends EconomyCommandBase implements CommandExecutor, TabCompleter {

  private final EconomyDataRegistry economyDataRegistry;
  private final WorldGroupRegistry worldGroupRegistry;
  private final OfflinePlayerCache offlinePlayerCache;

  public BalancesCommand(
    EconomyDataRegistry economyDataRegistry,
    WorldEconomyProvider economyProvider,
    WorldGroupRegistry worldGroupRegistry,
    OfflinePlayerCache offlinePlayerCache,
    ConfigKeeper<MainSection> config
  ) {
    super(config, economyProvider);

    this.economyDataRegistry = economyDataRegistry;
    this.worldGroupRegistry = worldGroupRegistry;
    this.offlinePlayerCache = offlinePlayerCache;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    BukkitEvaluable message;

    if (!PluginPermission.COMMAND_BALANCES.has(sender)) {
      if ((message = config.rootSection.playerMessages.missingPermissionBalancesSelfCommand) != null)
        message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

      return true;
    }

    var canViewOthers = PluginPermission.COMMAND_BALANCES_OTHER.has(sender);

    OfflinePlayer targetPlayer;

    if (args.length == 0) {
      if (!(sender instanceof Player player)) {
        if ((message = config.rootSection.playerMessages.playerOnlyBalancesSelfCommand) != null)
          message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

        return true;
      }

      targetPlayer = player;
    }

    else if (args.length == 1) {
      targetPlayer = offlinePlayerCache.getByName(args[0]);

      if (targetPlayer != sender && !canViewOthers) {
        if ((message = config.rootSection.playerMessages.missingPermissionBalancesOtherCommand) != null)
          message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

        return true;
      }
    }

    else {
      if (canViewOthers)
        message = config.rootSection.playerMessages.usageBalancesCommandOther;
      else
        message = config.rootSection.playerMessages.usageBalancesCommandSelf;

      if (message != null) {
        message.sendMessage(
          sender,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("label", label)
            .build()
        );
      }

      return true;
    }

    Map<String, String> formattedBalanceByWorldGroupDisplayName = new LinkedHashMap<>();

    for (var worldGroup : worldGroupRegistry.getWorldGroups()) {
      var accountRegistry = economyDataRegistry.getAccountRegistry(worldGroup);
      var targetAccount = accountRegistry.getAccount(targetPlayer);

      if (targetAccount == null) {
        message = (
          targetPlayer == sender
            ? config.rootSection.playerMessages.couldNotLoadAccountSelf
            : config.rootSection.playerMessages.couldNotLoadAccountOther
        );

        if (message != null) {
          message.sendMessage(
            sender,
            config.rootSection.getBaseEnvironment()
              .withStaticVariable("name", targetPlayer.getName())
              .build()
          );
        }

        return true;
      }

      formattedBalanceByWorldGroupDisplayName.put(
        targetAccount.worldGroup.displayName().asScalar(ScalarType.STRING, config.rootSection.builtBaseEnvironment),
        economyProvider.format(targetAccount.getBalance())
      );
    }

    if ((message = config.rootSection.playerMessages.balancesScreen) != null) {
      message.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("holder", targetPlayer.getName())
          .withStaticVariable("balances", formattedBalanceByWorldGroupDisplayName.entrySet())
          .build()
      );
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1 && PluginPermission.COMMAND_BALANCES_OTHER.has(sender))
      return offlinePlayerCache.createSuggestions(args[0]);

    return List.of();
  }
}
