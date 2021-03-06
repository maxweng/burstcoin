package brs.db.firebird;

import brs.Burst;
import brs.Order;
import brs.db.sql.SqlOrderStore;
import brs.db.sql.DbUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class FirebirdOrderStore extends SqlOrderStore {
  @Override
  protected void saveOrder(Connection con, String table, Order order) throws SQLException {
    try (PreparedStatement pstmt = con.prepareStatement("UPDATE OR INSERT INTO "
                                                        + DbUtils.quoteTableName(table)
                                                        + " (id, account_id, asset_id, "
                                                        + "price, quantity, creation_height, height, latest) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE) MATCHING (id, height)")) {
      int i = 0;
      pstmt.setLong(++i, order.getId());
      pstmt.setLong(++i, order.getAccountId());
      pstmt.setLong(++i, order.getAssetId());
      pstmt.setLong(++i, order.getPriceNQT());
      pstmt.setLong(++i, order.getQuantityQNT());
      pstmt.setInt(++i, order.getHeight());
      pstmt.setInt(++i, Burst.getBlockchain().getHeight());
      pstmt.executeUpdate();
    }
  }
}
