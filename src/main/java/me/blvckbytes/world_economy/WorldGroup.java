package me.blvckbytes.world_economy;

import org.bukkit.World;

import java.util.Objects;
import java.util.Set;

public record WorldGroup(
  String identifierNameLower,
  String evaluatedDisplayName,
  Set<String> memberWorldNamesLower,
  double startingBalance
) {
  public boolean contains(World world) {
    return memberWorldNamesLower.contains(world.getName().toLowerCase());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof WorldGroup that)) return false;
    return Objects.equals(identifierNameLower, that.identifierNameLower);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(identifierNameLower);
  }
}
