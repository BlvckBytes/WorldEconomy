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

public class PayCommand extends EconomyCommandBase implements CommandExecutor, TabCompleter {

  private final OfflinePlayerHelper offlinePlayerHelper;
  private final EconomyDataRegistry economyDataRegistry;
  private final WorldGroupRegistry worldGroupRegistry;

  public PayCommand(
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
    this.economyProvider = economyProvider;
    this.config = config;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    var isPayGroupCommand = config.rootSection.commands.payGroup.isLabel(label);

    BukkitEvaluable message;

    if (!(sender instanceof Player player)) {
      if ((message = config.rootSection.playerMessages.playerOnlyPayCommand) != null)
        message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

      return true;
    }

    if (missingCommandPermission(player, isPayGroupCommand)) {
      if (isPayGroupCommand)
        message = config.rootSection.playerMessages.missingPermissionPayGroupCommand;
      else
        message = config.rootSection.playerMessages.missingPermissionPayCommand;

      if (message != null)
        message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

      return true;
    }

    var canSpecifySource = PluginPermission.COMMAND_PAYGROUP_SOURCE.has(sender);

    OfflinePlayer targetPlayer;
    Double amount;
    WorldGroup targetWorldGroup;
    WorldGroup sourceWorldGroup;

    if (args.length == 2 || args.length == 3 || args.length == 4) {
      targetPlayer = offlinePlayerHelper.getByName(args[0]);

      if (targetPlayer == sender) {
        if ((message = config.rootSection.playerMessages.cannotPaySelf) != null)
          message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

        return true;
      }

      if ((amount = parseAndValidateValueOrNullAndSendMessage(sender, args[1])) == null)
        return true;

      // Specified target world-group
      if (args.length >= 3) {
        if (!isPayGroupCommand) {
          sendUsageMessage(sender, label, false, false);
          return true;
        }

        targetWorldGroup = worldGroupRegistry.getWorldGroupByIdentifierNameIgnoreCase(args[2]);

        if (targetWorldGroup == null) {
          if ((message = config.rootSection.playerMessages.unknownWorldGroup) != null) {
            message.sendMessage(
              sender,
              config.rootSection.getBaseEnvironment()
                .withStaticVariable("name", args[2])
                .withStaticVariable("group_names", worldGroupRegistry.createSuggestions(null))
                .build()
            );
          }

          return true;
        }
      }

      else {
        // Target-group is mandatory
        if (isPayGroupCommand) {
          sendUsageMessage(sender, label, true, canSpecifySource);
          return true;
        }

        var targetLastLocation = offlinePlayerHelper.getLastLocation(player);
        targetWorldGroup = targetLastLocation.worldGroup();

        if (targetWorldGroup == null) {
          sendUnknownWorldGroupMessage(targetLastLocation, player, sender);
          return true;
        }
      }

      // Specified source world-group
      if (args.length == 4) {
        if (!canSpecifySource) {
          if ((message = config.rootSection.playerMessages.missingPermissionCommandPayGroupSource) != null)
            message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

          return true;
        }

        sourceWorldGroup = worldGroupRegistry.getWorldGroupByIdentifierNameIgnoreCase(args[3]);

        if (sourceWorldGroup == null) {
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

      else {
        var sourceLastLocation = offlinePlayerHelper.getLastLocation(player);
        sourceWorldGroup = sourceLastLocation.worldGroup();

        if (sourceWorldGroup == null) {
          sendUnknownWorldGroupMessage(sourceLastLocation, player, sender);
          return true;
        }
      }
    }

    else {
      sendUsageMessage(sender, label, isPayGroupCommand, canSpecifySource);
      return true;
    }

    var targetAccountRegistry = economyDataRegistry.getAccountRegistry(targetWorldGroup);
    var targetAccount = targetAccountRegistry.getAccount(targetPlayer);

    if (targetAccount == null) {
      sendUnknownAccountMessage(targetWorldGroup, targetPlayer, sender);
      return true;
    }

    var sourceAccountRegistry = economyDataRegistry.getAccountRegistry(sourceWorldGroup);
    var sourceAccount = sourceAccountRegistry.getAccount(player);

    if (sourceAccount == null) {
      sendUnknownAccountMessage(sourceWorldGroup, player, sender);
      return true;
    }

    var sourceOldBalance = sourceAccount.getBalance();
    var targetOldBalance = targetAccount.getBalance();

    // ========== Transaction Begin ==========

    if (!sourceAccount.withdraw(amount)) {
      if (sourceWorldGroup.contains(player.getWorld()))
        message = config.rootSection.playerMessages.notEnoughMoneyToPayThisGroup;
      else
        message = config.rootSection.playerMessages.notEnoughMoneyToPayOtherGroup;

      if (message != null) {
        message.sendMessage(
          sender,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("balance", economyProvider.format(sourceAccount.getBalance()))
            .withStaticVariable("amount", economyProvider.format(amount))
            .withStaticVariable("world_group", sourceWorldGroup.evaluatedDisplayName())
            .build()
        );
      }

      return true;
    }

    if (!targetAccount.deposit(amount)) {
      sourceAccount.deposit(amount); // Rollback previous withdrawal

      if (targetWorldGroup.contains(player.getWorld()))
        message = config.rootSection.playerMessages.paymentExceedsReceiversBalanceThisGroup;
      else
        message = config.rootSection.playerMessages.paymentExceedsReceiversBalanceOtherGroup;

      if (message != null) {
        message.sendMessage(
          sender,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("balance", economyProvider.format(targetAccount.getBalance()))
            .withStaticVariable("amount", economyProvider.format(amount))
            .withStaticVariable("world_group", targetWorldGroup.evaluatedDisplayName())
            .withStaticVariable("name", targetPlayer.getName())
            .build()
        );
      }

      return true;
    }

    // ========== Transaction End ==========

    var transactionEnvironment = config.rootSection.getBaseEnvironment()
      .withStaticVariable("source_old_balance", economyProvider.format(sourceOldBalance))
      .withStaticVariable("target_old_balance", economyProvider.format(targetOldBalance))
      .withStaticVariable("source_new_balance", economyProvider.format(sourceAccount.getBalance()))
      .withStaticVariable("target_new_balance", economyProvider.format(targetAccount.getBalance()))
      .withStaticVariable("amount", economyProvider.format(amount))
      .withStaticVariable("target_world_group", targetWorldGroup.evaluatedDisplayName())
      .withStaticVariable("source_world_group", sourceWorldGroup.evaluatedDisplayName())
      .withStaticVariable("sender_name", player.getName())
      .withStaticVariable("receiver_name", targetPlayer.getName())
      .build();

    // Target is mandatory, always display; source is optional, display if not this
    if (isPayGroupCommand) {
      if (sourceWorldGroup.contains(player.getWorld()))
        message = config.rootSection.playerMessages.payGroupSentToPlayerThisSource;
      else
        message = config.rootSection.playerMessages.payGroupSentToPlayerOtherSource;
    }

    // Source is always this; target is determined automatically, display if not this
    else {
      if (targetWorldGroup.contains(player.getWorld()))
        message = config.rootSection.playerMessages.paySentToPlayerThisTarget;
      else
        message = config.rootSection.playerMessages.paySentToPlayerOtherTarget;
    }

    if (message != null)
      message.sendMessage(sender, transactionEnvironment);

    Player messageReceiver;

    if ((messageReceiver = targetPlayer.getPlayer()) != null) {
      var receiverWorld = messageReceiver.getWorld();

      if (sourceWorldGroup.contains(receiverWorld)) {
        if (targetWorldGroup.contains(receiverWorld))
          message = config.rootSection.playerMessages.payReceivedFromPlayerThisSourceThisTarget;
        else
          message = config.rootSection.playerMessages.payReceivedFromPlayerThisSourceOtherTarget;
      }

      else {
        if (targetWorldGroup.contains(receiverWorld))
          message = config.rootSection.playerMessages.payReceivedFromPlayerOtherSourceThisTarget;
        else
          message = config.rootSection.playerMessages.payReceivedFromPlayerOtherSourceOtherTarget;
      }

      if (message != null)
        message.sendMessage(messageReceiver, transactionEnvironment);
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player))
      return List.of();

    var isPayGroupCommand = config.rootSection.commands.payGroup.isLabel(label);

    if (missingCommandPermission(player, isPayGroupCommand))
      return List.of();

    if (args.length == 1)
      return offlinePlayerHelper.createSuggestions(args[0]);

    if (!isPayGroupCommand)
      return List.of();

    if (args.length == 3)
      return worldGroupRegistry.createSuggestions(args[2]);

    if (PluginPermission.COMMAND_PAYGROUP_SOURCE.has(sender) && args.length == 4)
      return worldGroupRegistry.createSuggestions(args[3]);

    return List.of();
  }

  private void sendUsageMessage(CommandSender sender, String label, boolean supportsGroups, boolean canSpecifySource) {
    BukkitEvaluable message;

    if (supportsGroups) {
      if (canSpecifySource)
        message = config.rootSection.playerMessages.usagePayGroupCommandSource;
      else
        message = config.rootSection.playerMessages.usagePayGroupCommand;
    } else
      message = config.rootSection.playerMessages.usagePayCommand;

    if (message != null) {
      message.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("group_names", worldGroupRegistry.createSuggestions(null))
          .withStaticVariable("label", label)
          .build()
      );
    }
  }

  private boolean missingCommandPermission(Player sender, boolean isPayGroupCommand) {
    var commandPermission = (
      isPayGroupCommand
        ? PluginPermission.COMMAND_PAYGROUP
        : PluginPermission.COMMAND_PAY
    );

    return !commandPermission.has(sender);
  }
}
