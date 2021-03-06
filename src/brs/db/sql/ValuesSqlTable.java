package brs.db.sql;

import brs.db.BurstKey;
import brs.db.ValuesTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jooq.impl.TableImpl;

public abstract class ValuesSqlTable<T,V> extends DerivedSqlTable implements ValuesTable<T, V> {

  private final boolean multiversion;
  protected final DbKey.Factory<T> dbKeyFactory;

  protected ValuesSqlTable(String table, TableImpl<?> tableClass, DbKey.Factory<T> dbKeyFactory) {
    this(table, tableClass, dbKeyFactory, false);
  }

  ValuesSqlTable(String table, TableImpl<?> tableClass, DbKey.Factory<T> dbKeyFactory, boolean multiversion) {
    super(table, tableClass);
    this.dbKeyFactory = dbKeyFactory;
    this.multiversion = multiversion;
  }

  protected abstract V load(Connection con, ResultSet rs) throws SQLException;

  protected abstract void save(Connection con, T t, V v) throws SQLException;

  @Override
  public final List<V> get(BurstKey nxtKey) {
    DbKey dbKey = (DbKey) nxtKey;
    List<V> values;
    if (Db.isInTransaction()) {
      values = (List<V>)Db.getCache(table).get(dbKey);
      if (values != null) {
        return values;
      }
    }
    try (Connection con = Db.getConnection();
         PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + dbKeyFactory.getPKClause()
                                                        + (multiversion ? " AND latest = TRUE" : "") + " ORDER BY db_id DESC")) {
      dbKey.setPK(pstmt);
      values = get(con, pstmt);
      if (Db.isInTransaction()) {
        Db.getCache(table).put(dbKey, values);
      }
      return values;
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  private List<V> get(Connection con, PreparedStatement pstmt) {
    try {
      List<V> result = new ArrayList<>();
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          result.add(load(con, rs));
        }
      }
      return result;
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public final void insert(T t, List<V> values) {
    if (!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }
    DbKey dbKey = (DbKey)dbKeyFactory.newKey(t);
    Db.getCache(table).put(dbKey, values);
    try (Connection con = Db.getConnection()) {
      if (multiversion) {
        try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                                                            + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE")) {
          dbKey.setPK(pstmt);
          pstmt.executeUpdate();
        }
      }
      for (V v : values) {
        save(con, t, v);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public void rollback(int height) {
    super.rollback(height);
    Db.getCache(table).clear();
  }

  @Override
  public final void truncate() {
    super.truncate();
    Db.getCache(table).clear();
  }

}
