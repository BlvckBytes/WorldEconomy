package me.blvckbytes.world_economy.commands;

import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.world_economy.*;
import me.blvckbytes.world_economy.config.MainSection;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;

public class BalanceTopCommand extends EconomyCommandBase implements CommandExecutor, TabCompleter {

  private final OfflinePlayerHelper offlinePlayerHelper;
  private final EconomyDataRegistry economyDataRegistry;
  private final WorldGroupRegistry worldGroupRegistry;

  public BalanceTopCommand(
    OfflinePlayerHelper offlinePlayerHelper,
    EconomyDataRegistry economyDataRegistry,
    WorldGroupRegistry worldGroupRegistry,
    Economy economyProvider,
    ConfigKeeper<MainSection> config
  ) {
    super(config, economyProvider);

    this.offlinePlayerHelper = offlinePlayerHelper;
    this.economyDataRegistry = economyDataRegistry;
    this.worldGroupRegistry = worldGroupRegistry;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    BukkitEvaluable message;

    if (!PluginPermission.COMMAND_BALANCETOP.has(sender)) {
      if ((message = config.rootSection.playerMessages.missingPermissionBaltopCommand) != null)
        message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

      return true;
    }

    var canSpecifyGroup = PluginPermission.COMMAND_BALANCETOP_GROUP.has(sender);
    WorldGroup targetWorldGroup;

    if (args.length == 0) {
      if (!(sender instanceof Player player)) {
        if ((message = config.rootSection.playerMessages.playerOnlyBaltopCommandNoWorldGroup) != null)
          message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

        return true;
      }

      var targetLastLocation = offlinePlayerHelper.getLastLocation(player);
      targetWorldGroup = targetLastLocation.worldGroup();

      if (targetWorldGroup == null) {
        sendUnknownWorldGroupMessage(targetLastLocation, player, sender);
        return true;
      }
    }

    else if (args.length == 1) {
      if (!canSpecifyGroup) {
        if ((message = config.rootSection.playerMessages.missingPermissionBaltopCommandOtherGroups) != null)
          message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

        return true;
      }

      targetWorldGroup = worldGroupRegistry.getWorldGroupByIdentifierNameIgnoreCase(args[0]);

      if (targetWorldGroup == null) {
        if ((message = config.rootSection.playerMessages.unknownWorldGroup) != null) {
          message.sendMessage(
            sender,
            config.rootSection.getBaseEnvironment()
              .withStaticVariable("name", args[0])
              .withStaticVariable("group_names", worldGroupRegistry.createSuggestions(null))
              .build()
          );
        }

        return true;
      }
    }

    else {
      message = (
        canSpecifyGroup
        ? config.rootSection.playerMessages.usageBalTopCommandOtherGroups
        : config.rootSection.playerMessages.usageBalTopCommand
      );

      if (message != null) {
        message.sendMessage(
          sender,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("label", label)
            .withStaticVariable("group_names", worldGroupRegistry.createSuggestions(null))
            .build()
        );
      }

      return true;
    }

    var accountRegistry = economyDataRegistry.getAccountRegistry(targetWorldGroup);
    var entries = new LinkedHashMap<String, String>();

    for (var account : accountRegistry.getTopAccounts(config.rootSection.economy.topListSize))
      entries.put(account.holder.getName(), economyProvider.format(account.getBalance()));

    if ((message = config.rootSection.playerMessages.balTopScreen) != null) {
      message.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("world_group", targetWorldGroup.evaluatedDisplayName())
          .withStaticVariable("entries", entries)
          .build()
      );
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1) {
      if (sender instanceof Player player && !PluginPermission.COMMAND_BALANCETOP_GROUP.has(player))
        return List.of();

      return worldGroupRegistry.createSuggestions(args[0]);
    }

    return List.of();
  }
}
