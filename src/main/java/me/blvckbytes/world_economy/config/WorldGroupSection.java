package me.blvckbytes.world_economy.config;

import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.List;

public class WorldGroupSection extends AConfigSection {

  public BukkitEvaluable displayName;
  public List<String> members;
  public double startingBalance;

  public WorldGroupSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

    this.members = List.of();
    this.startingBalance = 0;
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (startingBalance < 0)
      throw new MappingError("Starting-balance cannot be less than zero");

    if (displayName == null)
      throw new MappingError("Key \"displayName\" cannot be absent");

    for (var member : members) {
      if (member.contains(" "))
        throw new MappingError("Illegal member \"" + member + "\"; members cannot contain spaces");
    }
  }
}
