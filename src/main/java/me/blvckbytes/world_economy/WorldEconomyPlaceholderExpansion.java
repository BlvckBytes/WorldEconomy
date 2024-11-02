package me.blvckbytes.world_economy;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldEconomyPlaceholderExpansion extends PlaceholderExpansion {

  private final Plugin plugin;
  private final EconomyDataRegistry economyDataRegistry;
  private final WorldGroupRegistry worldGroupRegistry;
  private final Economy economyProvider;

  public WorldEconomyPlaceholderExpansion(
    Plugin plugin,
    EconomyDataRegistry economyDataRegistry,
    WorldGroupRegistry worldGroupRegistry,
    Economy economyProvider
  ) {
    this.plugin = plugin;
    this.economyDataRegistry = economyDataRegistry;
    this.worldGroupRegistry = worldGroupRegistry;
    this.economyProvider = economyProvider;
  }

  @Override
  public @NotNull String getIdentifier() {
    return "worldeconomy";
  }

  @Override
  public @NotNull String getAuthor() {
    return String.join(", ", plugin.getDescription().getAuthors());
  }

  @Override
  public @NotNull String getVersion() {
    return plugin.getDescription().getVersion();
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public boolean canRegister() {
    return true;
  }

  @Override
  public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
    var commandDelimiterIndex = params.indexOf('_');

    // Reject commands not followed by _, as all commands will have additional parameters
    if (commandDelimiterIndex < 0)
      return null;

    var command = params.substring(0, commandDelimiterIndex);

    if (command.equalsIgnoreCase("baltop"))
      return handleBalTopRequest(params, commandDelimiterIndex + 1);

    return "<unknown command '" + command + "'>";
  }

  // baltop_<world-group-identifier>_<place>_<"name"/"balance">
  private @Nullable String handleBalTopRequest(@NotNull String params, int start) {
    var worldGroupDelimiterIndex = params.indexOf('_', start);

    if (worldGroupDelimiterIndex < 0)
      return null;

    var worldGroupIdentifier = params.substring(start, worldGroupDelimiterIndex);
    var worldGroup = worldGroupRegistry.getWorldGroupByIdentifierNameIgnoreCase(worldGroupIdentifier);

    if (worldGroup == null)
      return "<unknown world-group '" + worldGroupIdentifier + "'>";

    var accountRegistry = economyDataRegistry.getAccountRegistry(worldGroup);

    var placeSequenceStart = worldGroupDelimiterIndex + 1;
    var placeDelimiterIndex = params.indexOf('_', placeSequenceStart);

    if (placeDelimiterIndex < 0)
      return "<missing place>";

    int place = tryParseIntegerSequence(params, placeSequenceStart, placeDelimiterIndex - 1);

    if (place < 0)
      return "<invalid place '" + params.substring(placeSequenceStart, placeDelimiterIndex) + "'>";

    var targetTopAccount = accountRegistry.getTopAccount(place);

    if (targetTopAccount == null)
      return "<empty place>";

    if (placeDelimiterIndex == params.length() - 1)
      return "<missing property>";

    var propertyName = params.substring(placeDelimiterIndex + 1);

    if (propertyName.equalsIgnoreCase("name"))
      return targetTopAccount.holder.getName();

    if (propertyName.equalsIgnoreCase("balance"))
      return this.economyProvider.format(targetTopAccount.getBalance());

    return "<unknown property '" + propertyName + "'>";
  }

  private int tryParseIntegerSequence(String input, int start, int end) {
    var result = 0;
    var placeValue = 1;

    for (var inputIndex = end; inputIndex >= start; --inputIndex) {
      var currentChar = input.charAt(inputIndex);

      if (!(currentChar >= '0' && currentChar <= '9'))
        return -1;

      var digitValue = currentChar - '0';
      result += digitValue * placeValue;
      placeValue *= 10;
    }

    return result;
  }
}
