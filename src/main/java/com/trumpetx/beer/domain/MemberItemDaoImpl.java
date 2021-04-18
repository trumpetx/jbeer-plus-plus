package com.trumpetx.beer.domain;

import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MemberItemDaoImpl extends RuntimeExceptionDao<MemberItem, Long> {
  private static final Logger LOG = LoggerFactory.getLogger(MemberItemDaoImpl.class);

  public MemberItemDaoImpl(ConnectionSource connectionSource) throws SQLException {
    super(DaoManager.createDao(connectionSource, MemberItem.class));
  }

  public List<MemberItem> queryForByItem(Item item) {
    Map<String, Object> query = new HashMap<>();
    query.put("item_id", item.getId());
    return queryForFieldValues(query);
  }

  public MemberItem queryForByMemberAndItem(Member member, Item item) {
    Map<String, Object> query = new HashMap<>();
    query.put("member_id", member.getId());
    query.put("item_id", item.getId());
    List<MemberItem> memberItems = queryForFieldValues(query);
    if (memberItems.isEmpty()) {
      return null;
    }
    Iterator<MemberItem> it = memberItems.iterator();
    MemberItem memberItem = it.next();
    while (it.hasNext()) {
      LOG.error("Duplicate MemberItem entry, removing: {}", memberItem);
      delete(it.next());
    }
    return memberItem;
  }
}
