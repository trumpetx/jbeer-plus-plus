package com.trumpetx.beer;

import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.Server;
import discord4j.core.object.entity.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;
import java.util.function.Function;

class GuildInitializer implements Consumer<Guild>, Function<Guild, Mono<Void>> {
  private static final Logger LOG = LoggerFactory.getLogger(GuildInitializer.class);
  private final DaoProvider daoProvider;

  GuildInitializer(DaoProvider daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public void accept(Guild guild) {
    long guildId = guild.getId().asLong();
    try {
      if (!daoProvider.serverDao.idExists(guildId)) {
        LOG.debug("Initializing new guild: {}", guild.getName());
        Server server = new Server(guildId, guild.getName());
        daoProvider.serverDao.create(server);
        boolean self = true;
        daoProvider.itemDao.create(new Item(server, "beer", ":beer:", ":beers:", self, true, self, true));
      } else {
        Server server = daoProvider.serverDao.queryForId(guildId);
        if (!server.getName().equals(guild.getName())) {
          LOG.debug("Updating guild: {} => {}", server.getName(), guild.getName());
          server.setName(guild.getName());
          daoProvider.serverDao.update(server);
        }
      }
    } catch (RuntimeException e) {
      LOG.error("Error initializing guild: {}", guildId);
    }
  }

  @Override
  public Mono<Void> apply(Guild guild) {
    accept(guild);
    return Mono.empty();
  }
}
