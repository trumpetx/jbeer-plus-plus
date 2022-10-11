package com.trumpetx.beer.domain;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "MEMBER")
public class Member {
  @DatabaseField(canBeNull = false, id = true)
  private long id;

  @ForeignCollectionField private ForeignCollection<MemberItem> memberItems;

  public Member() {}

  public Member(long id) {
    this.id = id;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public ForeignCollection<MemberItem> getMemberItems() {
    return memberItems;
  }

  public void setMemberItems(ForeignCollection<MemberItem> memberItems) {
    this.memberItems = memberItems;
  }

  public String toMention() {
    return "<@!" + id + ">";
  }
}
