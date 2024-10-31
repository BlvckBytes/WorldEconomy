package me.blvckbytes.world_economy.config;

import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

public class PlayerMessagesSection extends AConfigSection {

  public @Nullable BukkitEvaluable unknownWorldGroup;
  public @Nullable BukkitEvaluable notInAnyWorldGroupSelf;
  public @Nullable BukkitEvaluable notInAnyWorldGroupOther;
  public @Nullable BukkitEvaluable couldNotLoadAccountSelf;
  public @Nullable BukkitEvaluable couldNotLoadAccountOther;
  public @Nullable BukkitEvaluable valueIsNotADouble;
  public @Nullable BukkitEvaluable valueIsNotStrictlyPositive;
  public @Nullable BukkitEvaluable valueIsNotAMultipleOfTransactionStepSize;

  public @Nullable BukkitEvaluable playerOnlyBalancesCommandSelf;
  public @Nullable BukkitEvaluable missingPermissionBalancesCommandSelf;
  public @Nullable BukkitEvaluable missingPermissionBalancesCommandOther;
  public @Nullable BukkitEvaluable usageBalancesCommandSelf;
  public @Nullable BukkitEvaluable usageBalancesCommandOther;
  public @Nullable BukkitEvaluable balancesScreen;

  public @Nullable BukkitEvaluable playerOnlyBalanceCommandSelf;
  public @Nullable BukkitEvaluable playerOnlyBalanceGroupCommandSelf;
  public @Nullable BukkitEvaluable missingPermissionBalanceCommandSelf;
  public @Nullable BukkitEvaluable missingPermissionBalanceGroupCommandSelf;
  public @Nullable BukkitEvaluable missingPermissionBalanceCommandOther;
  public @Nullable BukkitEvaluable usageBalanceCommandSelf;
  public @Nullable BukkitEvaluable usageBalanceCommandOther;
  public @Nullable BukkitEvaluable usageBalanceGroupCommandSelf;
  public @Nullable BukkitEvaluable usageBalanceGroupCommandOther;
  public @Nullable BukkitEvaluable balanceMessageSelfThisGroup;
  public @Nullable BukkitEvaluable balanceMessageSelfOtherGroup;
  public @Nullable BukkitEvaluable balanceMessageOtherThisGroup;
  public @Nullable BukkitEvaluable balanceMessageOtherOtherGroup;

  public @Nullable BukkitEvaluable playerOnlyPayCommand;
  public @Nullable BukkitEvaluable missingPermissionPayCommand;
  public @Nullable BukkitEvaluable missingPermissionPayGroupCommand;
  public @Nullable BukkitEvaluable cannotPaySelf;
  public @Nullable BukkitEvaluable usagePayCommand;
  public @Nullable BukkitEvaluable usagePayGroupCommand;
  public @Nullable BukkitEvaluable usagePayGroupCommandSource;
  public @Nullable BukkitEvaluable missingPermissionCommandPayGroupSource;
  public @Nullable BukkitEvaluable notEnoughMoneyToPayThisGroup;
  public @Nullable BukkitEvaluable notEnoughMoneyToPayOtherGroup;
  public @Nullable BukkitEvaluable paymentExceedsReceiversBalanceThisGroup;
  public @Nullable BukkitEvaluable paymentExceedsReceiversBalanceOtherGroup;
  public @Nullable BukkitEvaluable payGroupSentToPlayerThisSource;
  public @Nullable BukkitEvaluable payGroupSentToPlayerOtherSource;
  public @Nullable BukkitEvaluable paySentToPlayerThisTarget;
  public @Nullable BukkitEvaluable paySentToPlayerOtherTarget;
  public @Nullable BukkitEvaluable payReceivedFromPlayerThisSourceThisTarget;
  public @Nullable BukkitEvaluable payReceivedFromPlayerThisSourceOtherTarget;
  public @Nullable BukkitEvaluable payReceivedFromPlayerOtherSourceThisTarget;
  public @Nullable BukkitEvaluable payReceivedFromPlayerOtherSourceOtherTarget;

  public @Nullable BukkitEvaluable missingPermissionMoneyCommand;
  public @Nullable BukkitEvaluable unknownMoneyCommandAction;
  public @Nullable BukkitEvaluable usageMoneyCommand;
  public @Nullable BukkitEvaluable playerOnlyMoneyCommandNoWorldGroup;
  public @Nullable BukkitEvaluable moneyCommandConsoleName;
  public @Nullable String evaluatedMoneyCommandConsoleName;
  public @Nullable BukkitEvaluable moneyAddExceedsReceiversBalance;
  public @Nullable BukkitEvaluable moneyRemoveExceedsReceiversBalance;
  public @Nullable BukkitEvaluable moneySetExceedsReceiversBalance;
  public @Nullable BukkitEvaluable moneyCommandAddExecutor;
  public @Nullable BukkitEvaluable moneyCommandRemoveExecutor;
  public @Nullable BukkitEvaluable moneyCommandSetExecutor;
  public @Nullable BukkitEvaluable moneyCommandAddTarget;
  public @Nullable BukkitEvaluable moneyCommandRemoveTarget;
  public @Nullable BukkitEvaluable moneyCommandSetTarget;

  public @Nullable BukkitEvaluable missingPermissionBaltopCommand;
  public @Nullable BukkitEvaluable playerOnlyBaltopCommandNoWorldGroup;
  public @Nullable BukkitEvaluable missingPermissionBaltopCommandOtherGroups;
  public @Nullable BukkitEvaluable usageBalTopCommand;
  public @Nullable BukkitEvaluable usageBalTopCommandOtherGroups;
  public @Nullable BukkitEvaluable balTopScreen;

  public @Nullable BukkitEvaluable pluginReloadedSuccess;
  public @Nullable BukkitEvaluable pluginReloadedError;
  public @Nullable BukkitEvaluable missingPermissionReloadCommand;

  public PlayerMessagesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (moneyCommandConsoleName != null)
      evaluatedMoneyCommandConsoleName = moneyCommandConsoleName.asScalar(ScalarType.STRING, builtBaseEnvironment);
  }
}
