package brs.db.sql;

import brs.Order;
import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import brs.db.store.OrderStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class SqlOrderStore implements OrderStore {
  protected DbKey.LongKeyFactory<Order.Ask> askOrderDbKeyFactory = new DbKey.LongKeyFactory<Order.Ask>("id") {

      @Override
      public BurstKey newKey(Order.Ask ask) {
        return ask.dbKey;
      }

    };
  protected VersionedEntityTable<Order.Ask> askOrderTable = new VersionedEntitySqlTable<Order.Ask>("ask_order", brs.schema.Tables.ASK_ORDER, askOrderDbKeyFactory) {
      @Override
      protected Order.Ask load(Connection con, ResultSet rs) throws SQLException {
        return new SqlAsk(rs);
      }

      @Override
      protected void save(Connection con, Order.Ask ask) throws SQLException {
        saveAsk(con, table, ask);
      }

      @Override
      protected String defaultSort() {
        return " ORDER BY creation_height DESC ";
      }

    };
  private DbKey.LongKeyFactory<Order.Bid> bidOrderDbKeyFactory = new DbKey.LongKeyFactory<Order.Bid>("id") {

      @Override
      public BurstKey newKey(Order.Bid bid) {
        return bid.dbKey;
      }

    };
  protected VersionedEntityTable<Order.Bid> bidOrderTable = new VersionedEntitySqlTable<Order.Bid>("bid_order", brs.schema.Tables.BID_ORDER, bidOrderDbKeyFactory) {

      @Override
      protected Order.Bid load(Connection con, ResultSet rs) throws SQLException {
        return new SqlBid(rs);
      }

      @Override
      protected void save(Connection con, Order.Bid bid) throws SQLException {
        saveBid(con, table, bid);
      }

      @Override
      protected String defaultSort() {
        return " ORDER BY creation_height DESC ";
      }

    };

  @Override
  public VersionedEntityTable<Order.Bid> getBidOrderTable() {
    return bidOrderTable;
  }

  @Override
  public BurstIterator<Order.Ask> getAskOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
    DbClause dbClause = new DbClause(" account_id = ? AND asset_id = ? ") {
        @Override
        public int set(PreparedStatement pstmt, int index) throws SQLException {
          pstmt.setLong(index++, accountId);
          pstmt.setLong(index++, assetId);
          return index;
        }
      };
    return askOrderTable.getManyBy(dbClause, from, to);
  }

  @Override
  public BurstIterator<Order.Ask> getSortedAsks(long assetId, int from, int to) {
    return askOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to,
                                   " ORDER BY price ASC, creation_height ASC, id ASC ");
  }

  @Override
  public Order.Ask getNextOrder(long assetId) {
    try (Connection con = Db.getConnection();
         PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ask_order WHERE asset_id = ? "
                                                        + "AND latest = TRUE ORDER BY price ASC, creation_height ASC, id ASC" + DbUtils.limitsClause(1))) {
      pstmt.setLong(1, assetId);
      DbUtils.setLimits(2, pstmt, 1);
      try (BurstIterator<Order.Ask> askOrders = askOrderTable.getManyBy(con, pstmt, true)) {
        return askOrders.hasNext() ? askOrders.next() : null;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public BurstIterator<Order.Ask> getAll(int from, int to) {
    return askOrderTable.getAll(from, to);
  }

  @Override
  public BurstIterator<Order.Ask> getAskOrdersByAccount(long accountId, int from, int to) {
    return askOrderTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
  }

  @Override
  public BurstIterator<Order.Ask> getAskOrdersByAsset(long assetId, int from, int to) {
    return askOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
  }

  protected abstract void saveOrder(Connection con, String table, Order order) throws SQLException;

  private void saveAsk(Connection con, String table, Order.Ask ask) throws SQLException {
    saveOrder(con, table, ask);
  }

  @Override
  public DbKey.LongKeyFactory<Order.Ask> getAskOrderDbKeyFactory() {
    return askOrderDbKeyFactory;
  }

  @Override
  public VersionedEntityTable<Order.Ask> getAskOrderTable() {
    return askOrderTable;
  }

  @Override
  public DbKey.LongKeyFactory<Order.Bid> getBidOrderDbKeyFactory() {
    return bidOrderDbKeyFactory;
  }

  @Override
  public BurstIterator<Order.Bid> getBidOrdersByAccount(long accountId, int from, int to) {
    return bidOrderTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
  }

  @Override
  public BurstIterator<Order.Bid> getBidOrdersByAsset(long assetId, int from, int to) {
    return bidOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
  }

  @Override
  public BurstIterator<Order.Bid> getBidOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
    DbClause dbClause = new DbClause(" account_id = ? AND asset_id = ? ") {
        @Override
        public int set(PreparedStatement pstmt, int index) throws SQLException {
          pstmt.setLong(index++, accountId);
          pstmt.setLong(index++, assetId);
          return index;
        }
      };
    return bidOrderTable.getManyBy(dbClause, from, to);
  }

  @Override
  public BurstIterator<Order.Bid> getSortedBids(long assetId, int from, int to) {

    return bidOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to,
                                   " ORDER BY price DESC, creation_height ASC, id ASC ");
  }

  @Override
  public Order.Bid getNextBid(long assetId) {
    try (Connection con = Db.getConnection();
         PreparedStatement pstmt = con.prepareStatement("SELECT * FROM bid_order WHERE asset_id = ? "
                                                        + "AND latest = TRUE ORDER BY price DESC, creation_height ASC, id ASC" + DbUtils.limitsClause(1) )) {
      pstmt.setLong(1, assetId);
      DbUtils.setLimits(2, pstmt, 1);
      try (BurstIterator<Order.Bid> bidOrders = bidOrderTable.getManyBy(con, pstmt, true)) {
        return bidOrders.hasNext() ? bidOrders.next() : null;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  private void saveBid(Connection con, String table, Order.Bid bid) throws SQLException {
    saveOrder(con, table, bid);
  }

  protected class SqlAsk extends Order.Ask {
    private SqlAsk(ResultSet rs) throws SQLException {
      super(
            rs.getLong("id"),
            rs.getLong("account_id"),
            rs.getLong("asset_id"),
            rs.getLong("price"),
            rs.getInt("creation_height"),
            rs.getLong("quantity"),
            askOrderDbKeyFactory.newKey(rs.getLong("id"))
            );
    }
  }

  protected class SqlBid extends Order.Bid {
    private SqlBid(ResultSet rs) throws SQLException {
      super(
            rs.getLong("id"),
            rs.getLong("account_id"),
            rs.getLong("asset_id"),
            rs.getLong("price"),
            rs.getInt("creation_height"),
            rs.getLong("quantity"),
            bidOrderDbKeyFactory.newKey(rs.getLong("id"))
            );
    }


  }

}
