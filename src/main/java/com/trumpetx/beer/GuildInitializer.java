package com.trumpetx.beer;

import static java.util.Optional.ofNullable;

import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.MemberItem;
import com.trumpetx.beer.domain.Server;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

public class GuildInitializer implements Function<Guild, Flux<?>> {
  private static final Logger LOG = LoggerFactory.getLogger(GuildInitializer.class);
  private final DaoProvider daoProvider;

  GuildInitializer(DaoProvider daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public Flux<?> apply(Guild guild) {
    long guildId = guild.getId().asLong();
    try {
      if (!daoProvider.serverDao.idExists(guildId)) {
        LOG.debug("Initializing new guild: {}", guild.getName());
        Server server = new Server(guildId, guild.getName());
        daoProvider.serverDao.create(server);
        daoProvider.itemDao.createIfNotExists(
            new Item(server, "beer", ":beer:", ":beers:", false, true, false, true));
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
    return Flux.empty();
  }

  MemberItem getOrCreateMemberItem(Member member, Item item, String guildName) {
    com.trumpetx.beer.domain.Member dbMember =
        daoProvider.memberDao.createIfNotExists(
            new com.trumpetx.beer.domain.Member(member.getId().asLong()));
    return ofNullable(daoProvider.memberItemDao.queryForByMemberAndItem(dbMember, item))
        .orElseGet(
            () -> {
              LOG.debug("Creating new member: {}", member.getDisplayName());
              return daoProvider.memberItemDao.createIfNotExists(new MemberItem(dbMember, item, 0));
            });
  }

  public String toMembersString(
      List<User> userMentions,
      Snowflake guildId,
      Item item,
      Function<MemberItem, MemberItem> modifyItem) {
    AtomicLong count = new AtomicLong();
    String toMembersString =
        userMentions.stream()
            .map(user -> user.asMember(guildId).block())
            .filter(Objects::nonNull)
            .peek(
                member -> {
                  MemberItem memberItem =
                      getOrCreateMemberItem(member, item, item.getServer().getName());
                  daoProvider.memberItemDao.update(modifyItem.apply(memberItem));
                  count.set(memberItem.getCount());
                })
            .map(discord4j.core.object.entity.Member::getMention)
            .collect(Collectors.joining(", "));
    String optionalCount = userMentions.size() == 1 ? (" [ " + count.get() + " ]") : "";
    return toMembersString + optionalCount;
  }
}
