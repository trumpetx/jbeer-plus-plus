package com.trumpetx.beer.domain;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.commons.lang3.builder.ToStringBuilder;

@DatabaseTable(tableName = "ITEM")
public class Item {
  @DatabaseField(canBeNull = false, generatedId = true)
  private long id;

  @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false, uniqueCombo = true)
  private Server server;

  @DatabaseField(canBeNull = false, uniqueCombo = true)
  private String keyword;

  @DatabaseField(canBeNull = false, uniqueCombo = true)
  private String emoji;

  @DatabaseField(canBeNull = false, uniqueCombo = true)
  private String emojiPlural;

  @DatabaseField(canBeNull = false)
  private boolean selfIncrement;

  @DatabaseField(canBeNull = false)
  private boolean userIncrement;

  @DatabaseField(canBeNull = false)
  private boolean selfDecrement;

  @DatabaseField(canBeNull = false)
  private boolean userDecrement;

  public Item() {}

  public Item(
      Server server,
      String keyword,
      String emoji,
      String emojiPlural,
      boolean selfIncrement,
      boolean userIncrement,
      boolean selfDecrement,
      boolean userDecrement) {
    this.server = server;
    this.keyword = keyword;
    this.emoji = emoji;
    this.emojiPlural = emojiPlural;
    this.selfIncrement = selfIncrement;
    this.userIncrement = userIncrement;
    this.selfDecrement = selfDecrement;
    this.userDecrement = userDecrement;
  }

  public Server getServer() {
    return server;
  }

  public void setServer(Server server) {
    this.server = server;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getEmoji() {
    return emoji;
  }

  public void setEmoji(String emoji) {
    this.emoji = emoji;
  }

  public boolean isSelfIncrement() {
    return selfIncrement;
  }

  public void setSelfIncrement(boolean selfIncrement) {
    this.selfIncrement = selfIncrement;
  }

  public boolean isUserIncrement() {
    return userIncrement;
  }

  public void setUserIncrement(boolean userIncrement) {
    this.userIncrement = userIncrement;
  }

  public boolean isSelfDecrement() {
    return selfDecrement;
  }

  public void setSelfDecrement(boolean selfDecrement) {
    this.selfDecrement = selfDecrement;
  }

  public boolean isUserDecrement() {
    return userDecrement;
  }

  public void setUserDecrement(boolean userDecrement) {
    this.userDecrement = userDecrement;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getEmojiPlural() {
    return emojiPlural;
  }

  public void setEmojiPlural(String emojiPlural) {
    this.emojiPlural = emojiPlural;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", id)
        .append("server", server)
        .append("keyword", keyword)
        .append("emoji", emoji)
        .append("emojiPlural", emojiPlural)
        .toString();
  }
}
