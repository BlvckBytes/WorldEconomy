package me.blvckbytes.world_economy;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EconomyAccountRegistry {

  private final WorldGroup worldGroup;
  private final Map<UUID, EconomyAccount> accountById;

  private final List<EconomyAccount> topList;

  private final BalanceConstraint balanceConstraint;

  public EconomyAccountRegistry(
    WorldGroup worldGroup,
    BalanceConstraint balanceConstraint
  ) {
    this.worldGroup = worldGroup;

    this.accountById = new HashMap<>();
    this.topList = new ArrayList<>();

    this.balanceConstraint = balanceConstraint;
  }

  public @Nullable EconomyAccount getTopAccount(int place) {
    synchronized (topList) {
      if (place <= 0)
        return null;

      int topListSize = topList.size();

      if (place > topListSize)
        return null;

      return topList.get(topListSize - place);
    }
  }

  public Collection<EconomyAccount> getTopAccounts(int limit) {
    synchronized (topList) {
      var result = new ArrayList<EconomyAccount>();

      for (var i = topList.size() - 1; i >= 0; --i) {
        result.add(topList.get(i));

        if (result.size() == limit)
          break;
      }

      return result;
    }
  }

  public Collection<EconomyAccount> getAccounts() {
    return Collections.unmodifiableCollection(accountById.values());
  }

  public void registerAccount(OfflinePlayer holder, EconomyAccount account) {
    this.accountById.put(holder.getUniqueId(), account);

    account.afterBalanceUpdate = () -> {
      updateTopListPosition(account);
    };

    updateTopListPosition(account);
  }

  public @Nullable EconomyAccount getAccount(OfflinePlayer holder) {
    var holderId = holder.getUniqueId();
    var account = accountById.get(holderId);

    if (account != null)
      return account;

    if (!holder.hasPlayedBefore())
      return null;

    account = new EconomyAccount(holder, worldGroup, worldGroup.startingBalance(), balanceConstraint);
    registerAccount(holder, account);
    return account;
  }

  private void updateTopListPosition(EconomyAccount account) {
    synchronized (topList) {
      var existingIndex = Collections.binarySearch(topList, account, makeTopListComparator(account));

      if (existingIndex >= 0)
        topList.remove(existingIndex);

      var newIndex = Collections.binarySearch(topList, account, makeTopListComparator(null));

      if (newIndex < 0)
        newIndex = -newIndex - 1;

      topList.add(newIndex, account);
    }
  }

  private Comparator<EconomyAccount> makeTopListComparator(@Nullable EconomyAccount previousBalanceAccount) {
    // For binarySearch(list, key, comparator), key is always b in compare(a, b)
    return (a, b) -> {
      double aKey;

      if (a == previousBalanceAccount)
        aKey = a.getPreviousBalance();
      else
        aKey = a.getBalance();

      double bKey;

      if (previousBalanceAccount != null)
        bKey = b.getPreviousBalance();
      else
        bKey = b.getBalance();

      int balanceCompareResult;

      if ((balanceCompareResult = Double.compare(aKey, bKey)) != 0)
        return balanceCompareResult;

      return -(a.holder.getName().compareTo(b.holder.getName()));
    };
  }
}
