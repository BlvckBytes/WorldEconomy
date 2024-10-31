package me.blvckbytes.world_economy.commands;

import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.world_economy.*;
import me.blvckbytes.world_economy.config.MainSection;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class MoneyCommand extends EconomyCommandBase implements CommandExecutor, TabCompleter {

  private final OfflinePlayerHelper offlinePlayerHelper;
  private final EconomyDataRegistry economyDataRegistry;
  private final WorldGroupRegistry worldGroupRegistry;

  public MoneyCommand(
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

    if (!PluginPermission.COMMAND_MONEY.has(sender)) {
      if ((message = config.rootSection.playerMessages.missingPermissionMoneyCommand) != null)
        message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

      return true;
    }

    MoneyAction action;
    OfflinePlayer targetPlayer;
    Double amount;
    WorldGroup targetWorldGroup;

    if (args.length == 3 || args.length == 4) {
      if ((action = MoneyAction.getByName(args[0])) == null) {
        if ((message = config.rootSection.playerMessages.unknownMoneyCommandAction) != null) {
          message.sendMessage(
            sender,
            config.rootSection.getBaseEnvironment()
              .withStaticVariable("input", args[0])
              .withStaticVariable("actions", MoneyAction.createSuggestions(null))
              .build()
          );
        }

        return true;
      }

      targetPlayer = offlinePlayerHelper.getByName(args[1]);

      if ((amount = parseAndValidateValueOrNullAndSendMessage(sender, args[2])) == null)
        return true;

      if (args.length == 3) {
        if (!(sender instanceof Player player)) {
          if ((message = config.rootSection.playerMessages.playerOnlyMoneyCommandNoWorldGroup) != null)
            message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

          return false;
        }

        var targetLastLocation = offlinePlayerHelper.getLastLocation(player);
        targetWorldGroup = targetLastLocation.worldGroup();

        if (targetWorldGroup == null) {
          sendUnknownWorldGroupMessage(targetLastLocation, player, sender);
          return true;
        }
      }

      else {
        targetWorldGroup = worldGroupRegistry.getWorldGroupByIdentifierNameIgnoreCase(args[3]);

        if (targetWorldGroup == null) {
          if ((message = config.rootSection.playerMessages.unknownWorldGroup) != null) {
            message.sendMessage(
              sender,
              config.rootSection.getBaseEnvironment()
                .withStaticVariable("name", args[3])
                .withStaticVariable("group_names", worldGroupRegistry.createSuggestions(null))
                .build()
            );
          }

          return true;
        }
      }
    }

    else {
      if ((message = config.rootSection.playerMessages.usageMoneyCommand) != null) {
        message.sendMessage(
          sender,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("label", label)
            .withStaticVariable("actions", MoneyAction.createSuggestions(null))
            .withStaticVariable("group_names", worldGroupRegistry.createSuggestions(null))
            .build()
        );
      }

      return true;
    }

    var accountRegistry = economyDataRegistry.getAccountRegistry(targetWorldGroup);
    var targetAccount = accountRegistry.getAccount(targetPlayer);

    if (targetAccount == null) {
      sendUnknownAccountMessage(targetWorldGroup, targetPlayer, sender);
      return true;
    }

    String executorName;

    if (sender instanceof Player player)
      executorName = player.getName();
    else {
      if ((executorName = config.rootSection.playerMessages.evaluatedMoneyCommandConsoleName) == null)
        executorName = "Console";
    }

    var actionEnvironmentBase = config.rootSection.getBaseEnvironment()
      .withStaticVariable("target_old_balance", economyProvider.format(targetAccount.getBalance()))
      .withStaticVariable("amount", economyProvider.format(amount))
      .withStaticVariable("world_group", targetWorldGroup.evaluatedDisplayName())
      .withStaticVariable("target_name", targetPlayer.getName())
      .withStaticVariable("executor_name", executorName)
      .withStaticVariable("executor_name", executorName)
      .withStaticVariable("balance_max", economyProvider.format(config.rootSection.economy.maxMoney))
      .withStaticVariable("balance_min", economyProvider.format(config.rootSection.economy.minMoney));

    switch (action) {
      case Add -> {
        if (!targetAccount.deposit(amount)) {
          if ((message = config.rootSection.playerMessages.moneyAddExceedsReceiversBalance) != null)
            message.sendMessage(sender, actionEnvironmentBase.build());

          return true;
        }
      }

      case Remove -> {
        if (!targetAccount.withdraw(amount)) {
          if ((message = config.rootSection.playerMessages.moneyRemoveExceedsReceiversBalance) != null)
            message.sendMessage(sender, actionEnvironmentBase.build());

          return true;
        }
      }

      case Set -> {
        if (!targetAccount.set(amount)) {
          if ((message = config.rootSection.playerMessages.moneySetExceedsReceiversBalance) != null)
            message.sendMessage(sender, actionEnvironmentBase.build());

          return true;
        }
      }
    }

    var actionEnvironment = actionEnvironmentBase
      .withStaticVariable("target_new_balance", economyProvider.format(targetAccount.getBalance()))
      .build();

    BukkitEvaluable executorMessage;
    BukkitEvaluable targetMessage;

    switch (action) {
      case Add -> {
        executorMessage = config.rootSection.playerMessages.moneyCommandAddExecutor;
        targetMessage = config.rootSection.playerMessages.moneyCommandAddTarget;
      }

      case Remove -> {
        executorMessage = config.rootSection.playerMessages.moneyCommandRemoveExecutor;
        targetMessage = config.rootSection.playerMessages.moneyCommandRemoveTarget;
      }

      case Set -> {
        executorMessage = config.rootSection.playerMessages.moneyCommandSetExecutor;
        targetMessage = config.rootSection.playerMessages.moneyCommandSetTarget;
      }

      default -> {
        return true;
      }
    }

    if (executorMessage != null)
      executorMessage.sendMessage(sender, actionEnvironment);

    Player messageReceiver;

    if (targetPlayer != sender && (messageReceiver = targetPlayer.getPlayer()) != null && targetMessage != null)
      targetMessage.sendMessage(messageReceiver, actionEnvironment);

    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
    if (!PluginPermission.COMMAND_MONEY.has(sender))
      return List.of();

    if (args.length == 1)
      return MoneyAction.createSuggestions(args[0]);

    if (args.length == 2)
      return offlinePlayerHelper.createSuggestions(args[1]);

    if (args.length == 4)
      return worldGroupRegistry.createSuggestions(args[3]);

    return List.of();
  }
}
