package com.trumpetx.beer;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusUpdater implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(StatusUpdater.class);
  private final GatewayDiscordClient gateway;

  public StatusUpdater(GatewayDiscordClient gateway) {
    this.gateway = gateway;
  }

  @Override
  public void run() {
    try {
      gateway.getGuilds().count().subscribe(this::updateGuildCount).dispose();
    } catch (Exception e) {
      LOG.error("Unexpected error", e);
    }
  }

  private void updateGuildCount(long guildCount) {
    LOG.debug("Updating guild count to {}", guildCount);
    gateway
        .updatePresence(
            ClientPresence.online(ClientActivity.playing("on " + guildCount + " servers")))
        .subscribe();
  }
}
