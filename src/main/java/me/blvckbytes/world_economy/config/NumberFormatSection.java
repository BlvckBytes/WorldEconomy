package me.blvckbytes.world_economy.config;

import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSIgnore;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class NumberFormatSection extends AConfigSection {

  @CSIgnore
  private NumberFormat format;

  @CSIgnore
  private int numberOfDecimalDigits;

  // https://en.wikipedia.org/wiki/IETF_language_tag#List_of_common_primary_language_subtags
  public @Nullable String subtagName;
  // https://docs.oracle.com/javase/8/docs/api/index.html?java/math/RoundingMode.html
  public @Nullable RoundingMode roundingMode;
  public boolean useGrouping;
  public int minimumFractionDigits;
  public int maximumFractionDigits;

  public NumberFormatSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (subtagName == null)
      throw new MappingError("Require a present subtag-name");

    Locale locale;

    try {
      locale = Locale.forLanguageTag(subtagName);
    } catch (Exception e) {
      throw new MappingError("Could not parse locale from subtag \"" + subtagName + "\": " + e.getMessage());
    }

    format = NumberFormat.getInstance(locale);

    if (roundingMode == null) {
      format.setRoundingMode(RoundingMode.UNNECESSARY);
      numberOfDecimalDigits = -1;
    } else {
      format.setRoundingMode(roundingMode);
      numberOfDecimalDigits = roundingMode == RoundingMode.UNNECESSARY ? -1 : maximumFractionDigits;
    }

    format.setGroupingUsed(useGrouping);
    format.setMinimumFractionDigits(minimumFractionDigits);
    format.setMaximumFractionDigits(maximumFractionDigits);
  }

  public int getNumberOfDecimalDigits() {
    return numberOfDecimalDigits;
  }

  public NumberFormat getFormat() {
    return format;
  }
}
