package me.blvckbytes.world_economy.config;

import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

public class EconomySection extends AConfigSection {

  public String nameSingular;
  public String namePlural;
  public Double maxMoney;
  public Double minMoney;
  public @Nullable Double transactionStepSize;
  public boolean doClampOnLoad;
  public long cacheWritePeriodSeconds;
  public int topListSize;
  public boolean logProviderCalls;

  @CSAlways
  public NumberFormatSection currencyFormat;

  public @Nullable String currencyFormatPrefix;
  public @Nullable String currencyFormatSuffix;

  public EconomySection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.nameSingular = "undefined";
    this.namePlural = "undefined";

    this.maxMoney = Double.MAX_VALUE;
    this.minMoney = Double.MIN_VALUE;
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (minMoney != null && maxMoney != null && minMoney > maxMoney)
      throw new MappingError("The minimum amount of money cannot be larger than the maximum amount of money");

    if (transactionStepSize != null && transactionStepSize <= 0)
      throw new MappingError("The transaction step-size cannot be less than or equal to zero");

    if (cacheWritePeriodSeconds < 15)
      throw new MappingError("The cache write period cannot be below 15s, as to not cause needless lag");

    if (topListSize <= 0)
      throw new MappingError("The top-list size must be strictly positive");
  }

  public String appendFormatPrefixSuffix(String value) {
    if (currencyFormatPrefix != null && currencyFormatSuffix != null)
      return currencyFormatPrefix + value + currencyFormatSuffix;

    if (currencyFormatPrefix != null)
      return currencyFormatPrefix + value;

    if (currencyFormatSuffix != null)
      return value + currencyFormatSuffix;

    return value;
  }
}
