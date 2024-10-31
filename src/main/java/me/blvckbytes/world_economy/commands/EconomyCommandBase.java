package me.blvckbytes.world_economy.commands;

import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.world_economy.OfflinePlayerHelper;
import me.blvckbytes.world_economy.WorldGroup;
import me.blvckbytes.world_economy.config.MainSection;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public abstract class EconomyCommandBase {

  protected ConfigKeeper<MainSection> config;
  protected Economy economyProvider;

  protected EconomyCommandBase(ConfigKeeper<MainSection> config, Economy economyProvider) {
    this.config = config;
    this.economyProvider = economyProvider;
  }

  public @Nullable Double parseAndValidateValueOrNullAndSendMessage(CommandSender sender, String value) {
    double amount;

    try {
      amount = Double.parseDouble(value);
    } catch (NumberFormatException e) {
      sendValueMessage(sender, config.rootSection.playerMessages.valueIsNotADouble, value);
      return null;
    }

    if (amount <= 0) {
      sendValueMessage(
        sender, config.rootSection.playerMessages.valueIsNotStrictlyPositive,
        economyProvider.format(amount)
      );
      return null;
    }

    Double transactionStepSize = config.rootSection.economy.transactionStepSize;

    if (transactionStepSize != null && !isMultipleOf(amount, transactionStepSize)) {
      BukkitEvaluable message;

      if ((message = config.rootSection.playerMessages.valueIsNotAMultipleOfTransactionStepSize) != null) {
        message.sendMessage(
          sender,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("value", config.rootSection.economy.appendFormatPrefixSuffix(String.valueOf(amount)))
            .withStaticVariable("step_size", economyProvider.format(transactionStepSize))
            .build()
        );
      }

      return null;
    }

    return amount;
  }

  private boolean isMultipleOf(double value, double step) {
    if (value < step)
      return false;

    var divisionResult = value / step;
    return  (divisionResult == (int) Math.floor(divisionResult));
  }

  private void sendValueMessage(CommandSender sender, @Nullable BukkitEvaluable message, String value) {
    if (message != null) {
      message.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("value", value)
          .build()
      );
    }
  }

  public void sendUnknownWorldGroupMessage(
    OfflinePlayerHelper.LastLocation lastLocation,
    OfflinePlayer targetPlayer,
    CommandSender sender
  ) {
    BukkitEvaluable message;

    if (targetPlayer == sender) {
      if (lastLocation.worldName() == null)
        message = config.rootSection.playerMessages.couldNotLoadAccountSelf;
      else
        message = config.rootSection.playerMessages.notInAnyWorldGroupSelf;
    } else {
      if (lastLocation.worldName() == null)
        message = config.rootSection.playerMessages.couldNotLoadAccountOther;
      else
        message = config.rootSection.playerMessages.notInAnyWorldGroupOther;
    }

    if (message != null) {
      message.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("name", targetPlayer.getName())
          .withStaticVariable("current_world", lastLocation.worldName())
          .build()
      );
    }
  }

  public void sendUnknownAccountMessage(
    WorldGroup worldGroup,
    OfflinePlayer targetPlayer,
    CommandSender sender
  ) {
    BukkitEvaluable message;

    if (targetPlayer == sender)
      message = config.rootSection.playerMessages.couldNotLoadAccountSelf;
    else
      message = config.rootSection.playerMessages.couldNotLoadAccountOther;

    if (message != null) {
      message.sendMessage(
        sender,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("name", targetPlayer.getName())
          .withStaticVariable("world_group", worldGroup.evaluatedDisplayName())
          .build()
      );
    }
  }
}
