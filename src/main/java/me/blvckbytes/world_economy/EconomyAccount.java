package me.blvckbytes.world_economy;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EconomyAccount {

  private double previousBalance;
  private double balance;
  private boolean dirty;

  public @Nullable Runnable afterBalanceUpdate;

  public final OfflinePlayer holder;
  public final WorldGroup worldGroup;

  private final BalanceConstraint balanceConstraint;

  public EconomyAccount(
    OfflinePlayer holder,
    WorldGroup worldGroup,
    double balance,
    BalanceConstraint balanceConstraint
  ) {
    this.holder = holder;
    this.worldGroup = worldGroup;
    this.balance = balance;
    this.previousBalance = balance;
    this.balanceConstraint = balanceConstraint;
  }

  /**
   * Get the current balance of this account
   */
  public double getBalance() {
    synchronized (this) {
      return balance;
    }
  }

  /**
   * Get the balance prior to the last transaction; returns the same as {@link #getBalance()}
   * if no transactions have yet taken place on this account.
   */
  public double getPreviousBalance() {
    return previousBalance;
  }

  public boolean hasBalance(double value) {
    synchronized (this) {
      return balanceConstraint.isWithinRange(this, balance - value);
    }
  }

  public boolean withdraw(double value) {
    synchronized (this) {
      if (!balanceConstraint.isWithinRange(this, balance - value))
        return false;

      this.previousBalance = balance;
      balance -= value;
      markDirty();
      return true;
    }
  }

  public boolean deposit(double value) {
    synchronized (this) {
      if (!balanceConstraint.isWithinRange(this, balance + value))
        return false;

      this.previousBalance = balance;
      balance += value;
      markDirty();
      return true;
    }
  }

  public boolean set(double value) {
    synchronized (this) {
      if (!balanceConstraint.isWithinRange(this, value))
        return false;

      this.previousBalance = balance;
      balance = value;
      markDirty();
      return true;
    }
  }

  public boolean isDirty() {
    synchronized (this) {
      return dirty;
    }
  }

  public void clearDirty() {
    synchronized (this) {
      this.dirty = false;
    }
  }

  private void markDirty() {
    this.dirty = true;

    if (this.afterBalanceUpdate != null)
      this.afterBalanceUpdate.run();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EconomyAccount that)) return false;
    return Objects.equals(holder.getUniqueId(), that.holder.getUniqueId()) && Objects.equals(worldGroup, that.worldGroup);
  }

  @Override
  public int hashCode() {
    return Objects.hash(holder.getUniqueId(), worldGroup);
  }
}
