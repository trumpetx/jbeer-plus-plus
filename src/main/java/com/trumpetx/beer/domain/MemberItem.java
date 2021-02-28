package com.trumpetx.beer.domain;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "MEMBER_ITEM")
public class MemberItem {
  @DatabaseField(canBeNull = false, generatedId = true)
  private long id;

  @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false, uniqueCombo = true)
  private Member member;

  @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false, uniqueCombo = true)
  private Item item;

  @DatabaseField(canBeNull = false, defaultValue = "0")
  private long count;

  public MemberItem() {
  }

  public MemberItem(Member member, Item item, long count) {
    this.member = member;
    this.item = item;
    this.count = count;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

  public Member getMember() {
    return member;
  }

  public void setMember(Member member) {
    this.member = member;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public MemberItem incrementCount() {
    setCount(getCount() + 1);
    return this;
  }

  public MemberItem decrementCount() {
    setCount(Math.max(getCount() - 1, 0));
    return this;
  }
}
