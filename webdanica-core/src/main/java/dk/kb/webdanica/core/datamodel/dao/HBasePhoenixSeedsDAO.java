package dk.kb.webdanica.core.datamodel.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import dk.kb.webdanica.core.tools.SimpleSkippingIterator;
import dk.kb.webdanica.core.tools.SkippingIterator;
import org.apache.phoenix.jdbc.PhoenixPreparedStatement;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import dk.kb.webdanica.core.datamodel.DanicaStatus;
import dk.kb.webdanica.core.datamodel.Seed;
import dk.kb.webdanica.core.datamodel.Status;
import dk.kb.webdanica.core.utils.CloseUtils;
import org.slf4j.LoggerFactory;

/**
 * See
 * <a href="https://raw.githubusercontent.com/netarchivesuite/webdanica/master/scripts/hbase-phoenix/create_seeds.sql">create-script for table seeds</a>
 */
public class HBasePhoenixSeedsDAO implements SeedsDAO {
    
    
    private static final Logger logger = LoggerFactory.getLogger(HBasePhoenixSeedsDAO.class);
    
    private static final String UPSERT_SQL =
            "UPSERT INTO seeds (url, redirected_url, host, domain, tld, inserted_time, updated_time, danica, status, status_reason, exported, exported_time, danica_reason) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ";
    
    private static final String EXISTS_SQL = "SELECT count(*) "
                                             + "FROM seeds "
                                             + "WHERE url=? ";
    
    
    @Override
    public boolean insertSeed(Seed singleSeed) throws DaoException {
        if (existsUrl(singleSeed.getUrl())) {
            return false;
        }
        return upsertSeed(singleSeed, true);
        
    }
    
    private boolean upsertSeed(Seed singleSeed, boolean isInsert) throws DaoException {
        PreparedStatement stm = null;
        int res = 0;
        Long now = System.currentTimeMillis();
        Long updatedTime = now;
        Long insertedTime;
        Long exportedTime = null;
        if (singleSeed.getExportedState() && singleSeed.getExportedTime() == null) {
            exportedTime = now;
        }
        Timestamp exportedTimeAsTimestamp = null;
        if (exportedTime != null) {
            exportedTimeAsTimestamp = new Timestamp(exportedTime);
        }
        if (isInsert) {
            insertedTime = now;
        } else {
            insertedTime = singleSeed.getInsertedTime();
            if (insertedTime == null) {
                logger.warn("InsertedTime shouldn't be null for updates, but was for seed w/url '" + singleSeed.getUrl()
                            + "'. Setting insertedTime for current time");
                insertedTime = now;
            }
        }
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            stm = conn.prepareStatement(UPSERT_SQL);
            stm.clearParameters();
            stm.setString(1, singleSeed.getUrl());
            stm.setString(2, singleSeed.getRedirectedUrl());
            stm.setString(3, singleSeed.getHostname());
            stm.setString(4, singleSeed.getDomain());
            stm.setString(5, singleSeed.getTld());
            stm.setTimestamp(6, new Timestamp(insertedTime));
            stm.setTimestamp(7, new Timestamp(updatedTime));
            stm.setInt(8, singleSeed.getDanicaStatus().ordinal());
            stm.setInt(9, singleSeed.getStatus().ordinal());
            stm.setString(10, singleSeed.getStatusReason());
            stm.setBoolean(11, singleSeed.getExportedState());
            stm.setTimestamp(12, exportedTimeAsTimestamp);
            stm.setString(13, singleSeed.getDanicaStatusReason());
            res = stm.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            CloseUtils.closeQuietly(stm);
        }
        return res != 0;
    }
    
    
    @Override
    public boolean existsUrl(String url) throws DaoException {
        PreparedStatement stm = null;
        ResultSet rs = null;
        long res = 0;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            stm = conn.prepareStatement(EXISTS_SQL);
            stm.clearParameters();
            stm.setString(1, url);
            rs = stm.executeQuery();
            if (rs != null && rs.next()) {
                res = rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            CloseUtils.closeQuietly(rs);
            CloseUtils.closeQuietly(stm);
        }
        return res != 0L;
    }
    
    
    @Override
    public boolean updateSeed(Seed singleSeed) throws DaoException {
        return upsertSeed(singleSeed, false);
    }
    
    
    @Override
    public Long getSeedsCount(Status status) throws DaoException {
        PreparedStatement stm = null;
        ResultSet rs = null;
        long res = 0;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            if (status != null) {
                stm = conn.prepareStatement("SELECT count(*) "
                                                        + "FROM seeds "
                                                        + "WHERE status=? ");
                stm.clearParameters();
                stm.setInt(1, status.ordinal());
            } else {
                stm = conn.prepareStatement("SELECT count(*) "
                                                        + "FROM seeds ");
                stm.clearParameters();
            }
            rs = stm.executeQuery();
            if (rs != null && rs.next()) {
                res = rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            CloseUtils.closeQuietly(stm);
            CloseUtils.closeQuietly(rs);
        }
        return res;
    }
    
    @Override
    public SkippingIterator<Seed> getSeedsForDomain(String domain, Status status, long offset, int limit) throws DaoException {
        return getSeedsForDomain(domain, status, null, offset, limit);
    }
    
    @Override
    public Long getSeedsCount(String domain, Status status) throws DaoException {
        return getSeedsCount(domain, status, null);
    }
    
    @Override
    public SkippingIterator<Seed> getSeedsForStatus(Status status, long offset, int limit) throws DaoException {
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            try (PreparedStatement stm = conn.prepareStatement("SELECT * "
                                                          + " FROM seeds "
                                                          + " WHERE status=? "
                                                          + " ORDER BY inserted_time "
                                                          + " LIMIT ? "
                                                          + " OFFSET ? ")) {
                stm.setInt(1, status.ordinal());
                stm.setInt(2, limit);
                stm.setLong(3, offset);
    
                CursorSkippingIterator.SQLFunction<ResultSet, List<Seed>> resultMaker = getSeedResultSetParser();
    
                if (limit > 1000) {
                    return new CursorSkippingIterator<>((PhoenixPreparedStatement) stm, conn, resultMaker,
                                                        1000);
                } else {
                    try (ResultSet rs = stm.executeQuery()) {
                        return new SimpleSkippingIterator<>(resultMaker.apply(rs).iterator());
                    }
                }
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        }
    }
    
    @NotNull
    private CursorSkippingIterator.SQLFunction<ResultSet, List<Seed>> getSeedResultSetParser() {
        return rs -> {
            List<Seed> seedList = new ArrayList<>();
            while (rs.next()) {
                seedList.add(getSeedFromResultSet(rs));
            }
            return seedList;
        };
    }
    
    @Override
    public Long getSeedsCount() throws DaoException {
        return getSeedsCount((Status) null);
    }
    
    @Override
    public SkippingIterator<Seed> getSeedsForDomain(String domain, Status status, DanicaStatus dstatus, long offset, int limit)
            throws DaoException {
        PreparedStatement stm = null;
        if (domain == null || domain.isEmpty()) {
            return null;
        }
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            if (status != null && dstatus == null) {
                stm = conn.prepareStatement("SELECT * "
                                            + " FROM seeds "
                                            + " WHERE domain=? AND status=? "
                                            + " ORDER BY inserted_time"
                                            + " LIMIT ? "
                                            + " OFFSET ? "
                );
                stm.clearParameters();
                stm.setString(1, domain);
                stm.setInt(2, status.ordinal());
                stm.setInt(3, limit);
                stm.setLong(4, offset);
            } else if (status == null && dstatus != null) {
                stm = conn.prepareStatement("SELECT * "
                                            + " FROM seeds "
                                            + " WHERE domain=? AND danica=? "
                                            + " ORDER BY inserted_time "
                                            + " LIMIT ? "
                                            + " OFFSET ? "
                );
                stm.clearParameters();
                stm.setString(1, domain);
                stm.setInt(2, dstatus.ordinal());
                stm.setInt(3, limit);
                stm.setLong(4, offset);
            } else if (status != null && dstatus != null) {
                stm = conn.prepareStatement("SELECT * "
                                            + " FROM seeds "
                                            + " WHERE domain=? AND status=? AND danica=? "
                                            + " ORDER BY inserted_time "
                                            + " LIMIT ? "
                                            + " OFFSET ? "
                );
                stm.clearParameters();
                stm.setString(1, domain);
                stm.setInt(2, status.ordinal());
                stm.setInt(3, dstatus.ordinal());
                stm.setInt(4, limit);
                stm.setLong(5, offset);
            } else {
                stm = conn.prepareStatement("SELECT * "
                                            + " FROM seeds "
                                            + " WHERE domain=? "
                                            + " ORDER BY inserted_time "
                                            + " LIMIT ? "
                                            + " OFFSET ? "
                );
                stm.clearParameters();
                stm.setString(1, domain);
                stm.setInt(2, limit);
                stm.setLong(3, offset);
            }
            CursorSkippingIterator.SQLFunction<ResultSet, List<Seed>> resultMaker = getSeedResultSetParser();
    
            if (limit > 1000) {
                return new CursorSkippingIterator<>((PhoenixPreparedStatement) stm, conn, resultMaker,
                                                    1000);
            } else {
                try (ResultSet rs = stm.executeQuery()){
                    return new SimpleSkippingIterator<>(resultMaker.apply(rs).iterator());
                } finally {
                    stm.close();
                }
            }
            
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            CloseUtils.closeQuietly(stm);
        }
    }
    
    @Override
    public SkippingIterator<Seed> getSeedsForDomain(String domain, long offset, int limit) throws DaoException {
        return getSeedsForDomain(domain, null, null, offset, limit);
    }
    
    private Seed getSeedFromResultSet(ResultSet rs) throws SQLException {
        Timestamp t = rs.getTimestamp("exported_time");
        Long exportedTime = null;
        if (t != null) {
            exportedTime = t.getTime();
        }
        return new Seed(
                rs.getString("url"),
                rs.getString("redirected_url"),
                rs.getString("host"),
                rs.getString("domain"),
                rs.getString("tld"),
                rs.getTimestamp("inserted_time").getTime(),
                rs.getTimestamp("updated_time").getTime(),
                DanicaStatus.fromOrdinal(rs.getInt("danica")),
                Status.fromOrdinal(rs.getInt("status")),
                rs.getString("status_reason"),
                rs.getBoolean("exported"),
                exportedTime,
                rs.getString("danica_reason")
        );
    }
    
    
    @Override
    public Long getSeedsDanicaCount(DanicaStatus s) throws DaoException {
        if (s == null) {
            return 0L;
        }
        long res = 0;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            try (PreparedStatement stm = conn.prepareStatement("SELECT COUNT(*) FROM seeds WHERE danica=?")){
                stm.setInt(1, s.ordinal());
                try (ResultSet rs = stm.executeQuery();) {
                    if (rs != null && rs.next()) {
                        res = rs.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        }
        return res;
    }
    
    
    @Override
    public SkippingIterator<Seed> getSeedsReadyToExport(boolean includeAlreadyExported) throws DaoException {
        //DanicaStatus==YES && exported==false && status==DONE  // Seed kan også have DanicaStatus=YES, men have Status REJECTED, hvis domænet allerede er danica
        PreparedStatement stm = null;
        DanicaStatus yes = DanicaStatus.YES;
        Status done = Status.DONE;
        boolean exportedValue = false; // Don't export seeds more than once
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            if (!includeAlreadyExported) {
                stm = conn.prepareStatement("SELECT * "
                                                        + "FROM seeds "
                                                        + "WHERE status=? and danica=? and exported=?");
                stm.setInt(1, done.ordinal());
                stm.setInt(2, yes.ordinal());
                stm.setBoolean(3, exportedValue);
            } else {
                stm = conn.prepareStatement("SELECT * "
                                                        + "FROM seeds "
                                                        + "WHERE status=? and danica=?");
                stm.setInt(1, done.ordinal());
                stm.setInt(2, yes.ordinal());
            }
            return new CursorSkippingIterator<>((PhoenixPreparedStatement) stm, conn, getSeedResultSetParser(), 1000);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            CloseUtils.closeQuietly(stm);
        }
    }
    
    
    @Override
    public Seed getSeed(String url) throws DaoException {
       
        if (!existsUrl(url)) {
            return null;
        }
        Seed result = null;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            try (PreparedStatement stm = conn.prepareStatement("SELECT * "
                                                + "FROM seeds "
                                                + "WHERE url=?");) {
                stm.setString(1, url);
                try (ResultSet rs = stm.executeQuery();) {
                    if (rs != null && rs.next()) {
                        result = getSeedFromResultSet(rs);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        }
        return result;
    }
    
    @Override
    public Long getSeedsCount(String domain) throws DaoException {
        return getSeedsCount(domain, null, null);
    }
    
    @Override
    public Long getSeedsCount(String domain, Status status, DanicaStatus dstatus) throws DaoException {
        if (domain == null || domain.isEmpty()) {
            return 0L;
        }
        PreparedStatement stm = null;
        long res = 0;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            if (status != null && dstatus == null) {
                stm = conn.prepareStatement("SELECT count(*) "
                                                        + "FROM seeds "
                                                        + "WHERE domain=? AND status=?");
                stm.setString(1, domain);
                stm.setInt(2, status.ordinal());
            } else if (status == null && dstatus != null) {
                stm = conn.prepareStatement("SELECT count(*) "
                                                        + "FROM seeds "
                                                        + "WHERE domain=? AND danica=?");
                stm.setString(1, domain);
                stm.setInt(2, dstatus.ordinal());
            } else if (status != null && dstatus != null) {
                stm = conn.prepareStatement("SELECT count(*) "
                                                        + "FROM seeds "
                                                        + "WHERE domain=? AND status=? AND danica=?");
                stm.setString(1, domain);
                stm.setInt(2, status.ordinal());
                stm.setInt(3, dstatus.ordinal());
            } else {
                stm = conn.prepareStatement("SELECT count(*) "
                                                        + "FROM seeds "
                                                        + "WHERE domain=? ");
                stm.setString(1, domain);
            }
            try (ResultSet rs = stm.executeQuery();) {
                if (rs != null && rs.next()) {
                    res = rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            CloseUtils.closeQuietly(stm);
        }
        return res;
    }
    
}
