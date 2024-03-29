package com.trumpetx.beer.domain;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import org.apache.commons.lang3.builder.ToStringBuilder;

@DatabaseTable(tableName = "SERVER")
public class Server {
  @DatabaseField(canBeNull = false, id = true)
  private long id;

  @DatabaseField(canBeNull = false)
  private String name;

  @DatabaseField private Long lastPercentMessage;

  @DatabaseField private Long lastCountMessage;

  @DatabaseField private Long lastHelpMessage;

  @ForeignCollectionField private ForeignCollection<Item> items;

  public Server() {}

  public Server(long id, String name) {
    this.id = id;
    this.name = name;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public ForeignCollection<Item> getItems() {
    return items;
  }

  public void setItems(ForeignCollection<Item> items) {
    this.items = items;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getLastPercentMessage() {
    return lastPercentMessage;
  }

  public void setLastPercentMessage(Long lastPercentMessage) {
    this.lastPercentMessage = lastPercentMessage;
  }

  public Long getLastCountMessage() {
    return lastCountMessage;
  }

  public void setLastCountMessage(Long lastCountMessage) {
    this.lastCountMessage = lastCountMessage;
  }

  public Long getLastHelpMessage() {
    return lastHelpMessage;
  }

  public void setLastHelpMessage(Long lastHelpMessage) {
    this.lastHelpMessage = lastHelpMessage;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("id", id).append("name", name).toString();
  }
}
