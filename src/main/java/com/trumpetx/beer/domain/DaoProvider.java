package com.trumpetx.beer.domain;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class DaoProvider {
  private static DaoProvider INSTANCE;
  public final RuntimeExceptionDao<Server, Long> serverDao;
  public final RuntimeExceptionDao<Item, Long> itemDao;
  public final RuntimeExceptionDao<Member, Long> memberDao;
  public final MemberItemDaoImpl memberItemDao;
  private DaoProvider(JdbcPooledConnectionSource connectionSource) throws SQLException {
    for (Class<?> c : new Class<?>[]{Member.class, Server.class, Item.class, MemberItem.class}) {
      TableUtils.createTableIfNotExists(connectionSource, c);
    }
    serverDao = RuntimeExceptionDao.createDao(connectionSource, Server.class);
    itemDao = RuntimeExceptionDao.createDao(connectionSource, Item.class);
    memberDao = RuntimeExceptionDao.createDao(connectionSource, Member.class);
    memberItemDao = new MemberItemDaoImpl(connectionSource);
  }

  public static DaoProvider getInstance() {
    if (INSTANCE == null) {
      return initializeInstance(null);
    }
    return INSTANCE;
  }

  public static DaoProvider initializeInstance(String databaseUrl) {
    if (INSTANCE == null) {
      try {
        INSTANCE = new DaoProvider(new JdbcPooledConnectionSource(defaultString(databaseUrl, "jdbc:h2:mem:beer")));
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    return INSTANCE;
  }
}
