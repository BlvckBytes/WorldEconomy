package me.blvckbytes.world_economy;

import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.world_economy.config.MainSection;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WorldEconomyProvider implements Economy {

  private static final EconomyResponse UNSUPPORTED_BANKS_RESPONSE = new EconomyResponse(
    0, 0, EconomyResponse.ResponseType.FAILURE, "Banks are not supported by this implementation"
  );

  private static final EconomyResponse NO_SUCH_ACCOUNT_RESPONSE = new EconomyResponse(
    0, 0, EconomyResponse.ResponseType.FAILURE, "Could not locate the requested account"
  );

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;
  private final EconomyDataRegistry economyDataRegistry;
  private final OfflinePlayerHelper offlinePlayerHelper;

  public WorldEconomyProvider(
    Plugin plugin,
    ConfigKeeper<MainSection> config,
    EconomyDataRegistry economyDataRegistry,
    OfflinePlayerHelper offlinePlayerHelper
  ) {
    this.plugin = plugin;
    this.config = config;
    this.economyDataRegistry = economyDataRegistry;
    this.offlinePlayerHelper = offlinePlayerHelper;
  }

  // ================================================================================
  // Provider
  // ================================================================================

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public String getName() {
    return plugin.getName() + " Economy Provider";
  }

  // ================================================================================
  // Currency
  // ================================================================================

  @Override
  public String format(double value) {
    var formattedValue = config.rootSection.economy.currencyFormat.getFormat().format(value);
    return config.rootSection.economy.appendFormatPrefixSuffix(formattedValue);
  }

  @Override
  public String currencyNamePlural() {
    return config.rootSection.economy.namePlural;
  }

  @Override
  public String currencyNameSingular() {
    return config.rootSection.economy.nameSingular;
  }

  @Override
  public int fractionalDigits() {
    return config.rootSection.economy.currencyFormat.getNumberOfDecimalDigits();
  }

  // ================================================================================
  // Account
  // ================================================================================

  @Override
  public boolean hasAccount(String playerName) {
    return hasAccount(offlinePlayerHelper.getByName(playerName));
  }

  @Override
  public boolean hasAccount(OfflinePlayer player) {
    return economyDataRegistry.getForLastWorld(player) != null;
  }

  @Override
  public boolean hasAccount(String playerName, String worldName) {
    return hasAccount(offlinePlayerHelper.getByName(playerName), worldName);
  }

  @Override
  public boolean hasAccount(OfflinePlayer player, String worldName) {
    return economyDataRegistry.getForWorldName(player, worldName) != null;
  }

  @Override
  public boolean createPlayerAccount(String playerName) {
    return createPlayerAccount(offlinePlayerHelper.getByName(playerName));
  }

  @Override
  public boolean createPlayerAccount(OfflinePlayer player) {
    return economyDataRegistry.createForLastWorld(player);
  }

  @Override
  public boolean createPlayerAccount(String playerName, String worldName) {
    return createPlayerAccount(offlinePlayerHelper.getByName(playerName), worldName);
  }

  @Override
  public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
    return economyDataRegistry.createForWorldName(player, worldName);
  }

  // ================================================================================
  // Balance
  // ================================================================================

  @Override
  public double getBalance(String playerName) {
    return getBalance(offlinePlayerHelper.getByName(playerName));
  }

  @Override
  public double getBalance(OfflinePlayer player) {
    var account = economyDataRegistry.getForLastWorld(player);
    return account == null ? 0 : account.getBalance();
  }

  @Override
  public double getBalance(String playerName, String worldName) {
    return getBalance(offlinePlayerHelper.getByName(playerName), worldName);
  }

  @Override
  public double getBalance(OfflinePlayer player, String worldName) {
    var account = economyDataRegistry.getForWorldName(player, worldName);
    return account == null ? 0 : account.getBalance();
  }

  @Override
  public boolean has(String playerName, double value) {
    return has(offlinePlayerHelper.getByName(playerName), value);
  }

  @Override
  public boolean has(OfflinePlayer player, double value) {
    var account = economyDataRegistry.getForLastWorld(player);
    return account != null && account.hasBalance(value);
  }

  @Override
  public boolean has(String playerName, String worldName, double value) {
    return has(offlinePlayerHelper.getByName(playerName), worldName, value);
  }

  @Override
  public boolean has(OfflinePlayer player, String worldName, double value) {
    var account = economyDataRegistry.getForWorldName(player, worldName);
    return account != null && account.hasBalance(value);
  }

  // ================================================================================
  // Withdrawing
  // ================================================================================

  @Override
  public EconomyResponse withdrawPlayer(String playerName, double value) {
    return withdrawPlayer(offlinePlayerHelper.getByName(playerName), value);
  }

  @Override
  public EconomyResponse withdrawPlayer(OfflinePlayer player, double value) {
    return handleWithdraw(economyDataRegistry.getForLastWorld(player), value);
  }

  @Override
  public EconomyResponse withdrawPlayer(String playerName, String worldName, double value) {
    return withdrawPlayer(offlinePlayerHelper.getByName(playerName), worldName, value);
  }

  @Override
  public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double value) {
    return handleWithdraw(economyDataRegistry.getForWorldName(player, worldName), value);
  }

  private EconomyResponse handleWithdraw(@Nullable EconomyAccount account, double value) {
    if (account == null)
      return NO_SUCH_ACCOUNT_RESPONSE;

    if (!account.withdraw(value))
      return new EconomyResponse(0, account.getBalance(), EconomyResponse.ResponseType.FAILURE, "Insufficient balance");

    return new EconomyResponse(value, account.getBalance(), EconomyResponse.ResponseType.SUCCESS, null);
  }

  // ================================================================================
  // Depositing
  // ================================================================================

  @Override
  public EconomyResponse depositPlayer(String playerName, double value) {
    return depositPlayer(offlinePlayerHelper.getByName(playerName), value);
  }

  @Override
  public EconomyResponse depositPlayer(OfflinePlayer player, double value) {
    return handleDeposit(economyDataRegistry.getForLastWorld(player), value);
  }

  @Override
  public EconomyResponse depositPlayer(String playerName, String worldName, double value) {
    return depositPlayer(offlinePlayerHelper.getByName(playerName), worldName, value);
  }

  @Override
  public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double value) {
    return handleDeposit(economyDataRegistry.getForWorldName(player, worldName), value);
  }

  private EconomyResponse handleDeposit(@Nullable EconomyAccount account, double value) {
    if (account == null)
      return NO_SUCH_ACCOUNT_RESPONSE;

    if (!account.deposit(value))
      return new EconomyResponse(0, account.getBalance(), EconomyResponse.ResponseType.FAILURE, "Exceeded maximum balance");

    return new EconomyResponse(value, account.getBalance(), EconomyResponse.ResponseType.SUCCESS, null);
  }

  // ================================================================================
  // Bank
  // ================================================================================

  @Override
  public boolean hasBankSupport() {
    return false;
  }

  @Override
  public EconomyResponse createBank(String bankName, String playerName) {
    return UNSUPPORTED_BANKS_RESPONSE;
  }

  @Override
  public EconomyResponse createBank(String bankName, OfflinePlayer player) {
    return UNSUPPORTED_BANKS_RESPONSE;
  }

  @Override
  public EconomyResponse deleteBank(String bankName) {
    return UNSUPPORTED_BANKS_RESPONSE;
  }

  @Override
  public EconomyResponse bankBalance(String bankName) {
    return UNSUPPORTED_BANKS_RESPONSE;
  }

  @Override
  public EconomyResponse bankHas(String bankName, double value) {
    return UNSUPPORTED_BANKS_RESPONSE;
  }

  @Override
  public EconomyResponse bankWithdraw(String bankName, double value) {
    return UNSUPPORTED_BANKS_RESPONSE;
  }

  @Override
  public EconomyResponse bankDeposit(String bankName, double value) {
    return UNSUPPORTED_BANKS_RESPONSE;
  }

  @Override
  public EconomyResponse isBankOwner(String bankName, String playerName) {
    return UNSUPPORTED_BANKS_RESPONSE;
  }

  @Override
  public EconomyResponse isBankOwner(String bankName, OfflinePlayer player) {
    return UNSUPPORTED_BANKS_RESPONSE;
  }

  @Override
  public EconomyResponse isBankMember(String bankName, String playerName) {
    return UNSUPPORTED_BANKS_RESPONSE;
  }

  @Override
  public EconomyResponse isBankMember(String bankName, OfflinePlayer player) {
    return UNSUPPORTED_BANKS_RESPONSE;
  }

  @Override
  public List<String> getBanks() {
    return List.of();
  }
}
