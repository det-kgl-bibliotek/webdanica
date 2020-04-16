package dk.kb.webdanica.core.datamodel.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.phoenix.jdbc.PhoenixPreparedStatement;
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
    
    private static final String UPSERT_SQL = "UPSERT INTO seeds (url, redirected_url, host, domain, tld, inserted_time, updated_time, danica, status, status_reason, exported, exported_time, danica_reason) "
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
    
    private static final String SEEDS_COUNT_BY_STATUS_SQL = "SELECT count(*) "
                                                            + "FROM seeds "
                                                            + "WHERE status=? ";
    
    private static final String SEEDS_COUNT_ALL_SQL = "SELECT count(*) "
                                                      + "FROM seeds ";
    
    private static final String SEEDS_COUNT_BY_DOMAIN_SQL = "SELECT count(*) "
                                                            + "FROM seeds "
                                                            + "WHERE domain=? ";
    
    private static final String SEEDS_COUNT_BY_DOMAIN_AND_STATE_SQL = "SELECT count(*) "
                                                                      + "FROM seeds "
                                                                      + "WHERE domain=? AND status=?";
    
    private static final String SEEDS_COUNT_BY_DOMAIN_AND_DANICASTATE_SQL = "SELECT count(*) "
                                                                            + "FROM seeds "
                                                                            + "WHERE domain=? AND danica=?";
    
    private static final String SEEDS_COUNT_BY_DOMAIN_AND_STATE_AND_DANICASTATE_SQL = "SELECT count(*) "
                                                                                      + "FROM seeds "
                                                                                      + "WHERE domain=? AND status=? AND danica=?";
    
    
    @Override
    public Long getSeedsCount(Status status) throws DaoException {
        PreparedStatement stm = null;
        ResultSet rs = null;
        long res = 0;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            if (status != null) {
                stm = conn.prepareStatement(SEEDS_COUNT_BY_STATUS_SQL);
                stm.clearParameters();
                stm.setInt(1, status.ordinal());
            } else {
                stm = conn.prepareStatement(SEEDS_COUNT_ALL_SQL);
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
    
    private static final String SEEDS_BY_STATUS_SQL = "SELECT * "
                                                      + "FROM seeds "
                                                      + "WHERE status=? LIMIT ?";
    private static final String SEEDS_BY_DOMAIN_SQL = "SELECT * "
                                                      + "FROM seeds "
                                                      + "WHERE domain=? LIMIT ?";
    private static final String SEEDS_BY_DOMAIN_AND_STATE_SQL = "SELECT * "
                                                                + "FROM seeds "
                                                                + "WHERE domain=? AND status=? LIMIT ?";
    private static final String SEEDS_BY_DOMAIN_AND_DANICASTATE_SQL = "SELECT * "
                                                                      + "FROM seeds "
                                                                      + "WHERE domain=? AND danica=? LIMIT ?";
    private static final String SEEDS_BY_DOMAIN_AND_STATE_AND_DANICASTATE_SQL = "SELECT * "
                                                                                + "FROM seeds "
                                                                                + "WHERE domain=? AND status=? AND danica=? LIMIT ?";
    
    @Override
    public Iterator<Seed> getSeeds(String domain, Status status, int limit) throws DaoException {
        return getSeeds(domain, status, null, limit);
    }
    
    @Override
    public Long getSeedsCount(String domain, Status status) throws DaoException {
        return getSeedsCount(domain, status, null);
    }
    
    @Override
    public Iterator<Seed> getSeeds(Status status, int limit) throws DaoException {
        PreparedStatement stm = null;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            stm = conn.prepareStatement(SEEDS_BY_STATUS_SQL);
            stm.clearParameters();
            stm.setInt(1, status.ordinal());
            stm.setInt(2, limit);
            return Utils.getResultIteratorSQL((PhoenixPreparedStatement) stm, conn, rs -> {
                List<Seed> seedList = new ArrayList<>();
                while (rs.next()) {
                    seedList.add(getSeedFromResultSet(rs));
                }
                return seedList;
            }, 1000);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            CloseUtils.closeQuietly(stm);
        }
    }
    
    @Override
    public Long getSeedsCount() throws DaoException {
        return getSeedsCount((Status)null);
    }
    
    @Override
    public Iterator<Seed> getSeeds(String domain, Status status, DanicaStatus dstatus, int limit) throws DaoException {
        PreparedStatement stm = null;
        if (domain == null || domain.isEmpty()) {
            return null;
        }
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            if (status != null && dstatus == null) {
                stm = conn.prepareStatement(SEEDS_BY_DOMAIN_AND_STATE_SQL);
                stm.clearParameters();
                stm.setString(1, domain);
                stm.setInt(2, status.ordinal());
                stm.setInt(3, limit);
            } else if (status == null && dstatus != null) {
                stm = conn.prepareStatement(SEEDS_BY_DOMAIN_AND_DANICASTATE_SQL);
                stm.clearParameters();
                stm.setString(1, domain);
                stm.setInt(2, dstatus.ordinal());
                stm.setInt(3, limit);
            } else if (status != null && dstatus != null) {
                stm = conn.prepareStatement(SEEDS_BY_DOMAIN_AND_STATE_AND_DANICASTATE_SQL);
                stm.clearParameters();
                stm.setString(1, domain);
                stm.setInt(2, status.ordinal());
                stm.setInt(3, dstatus.ordinal());
                stm.setInt(4, limit);
            } else {
                stm = conn.prepareStatement(SEEDS_BY_DOMAIN_SQL);
                stm.clearParameters();
                stm.setString(1, domain);
                stm.setInt(2, limit);
            }
            return Utils.getResultIteratorSQL((PhoenixPreparedStatement) stm, conn, rs -> {
                List<Seed> seedList = new ArrayList<>();
                while (rs.next()) {
                    seedList.add(getSeedFromResultSet(rs));
                }
                return seedList;
            }, 1000);
            
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            CloseUtils.closeQuietly(stm);
        }
    }
    
    @Override
    public Iterator<Seed> getSeeds(String domain, int limit) throws DaoException {
        return getSeeds(domain, null, null, limit);
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
    
    
    private static final String SELECT_COUNT_DANICA_SQL = ""
                                                          + "SELECT COUNT(*) FROM seeds WHERE danica=?";
    
    
    @Override
    public Long getSeedsDanicaCount(DanicaStatus s) throws DaoException {
        PreparedStatement stm = null;
        ResultSet rs = null;
        if (s == null) {
            return 0L;
        }
        long res = 0;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            stm = conn.prepareStatement(SELECT_COUNT_DANICA_SQL);
            stm.clearParameters();
            stm.setInt(1, s.ordinal());
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
        return res;
    }
    
    private static final String SEEDS_READY_TO_EXPORT_SQL = "SELECT * "
                                                            + "FROM seeds "
                                                            + "WHERE status=? and danica=? and exported=?";
    private static final String SEEDS_DANICA_SQL = "SELECT * "
                                                   + "FROM seeds "
                                                   + "WHERE status=? and danica=?";
    
    
    @Override
    public Iterator<Seed> getSeedsReadyToExport(boolean includeAlreadyExported) throws DaoException {
        //DanicaStatus==YES && exported==false && status==DONE  // Seed kan også have DanicaStatus=YES, men have Status REJECTED, hvis domænet allerede er danica
        PreparedStatement stm = null;
        DanicaStatus yes = DanicaStatus.YES;
        Status done = Status.DONE;
        boolean exportedValue = false; // Don't export seeds more than once
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            if (!includeAlreadyExported) {
                stm = conn.prepareStatement(SEEDS_READY_TO_EXPORT_SQL);
                stm.clearParameters();
                stm.setInt(1, done.ordinal());
                stm.setInt(2, yes.ordinal());
                stm.setBoolean(3, exportedValue);
            } else {
                stm = conn.prepareStatement(SEEDS_DANICA_SQL);
                stm.clearParameters();
                stm.setInt(1, done.ordinal());
                stm.setInt(2, yes.ordinal());
            }
            return Utils.getResultIteratorSQL((PhoenixPreparedStatement) stm, conn, rs -> {
                List<Seed> seedList = new ArrayList<>();
                while (rs.next()) {
                    seedList.add(getSeedFromResultSet(rs));
                }
                return seedList;
            }, 1000);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            CloseUtils.closeQuietly(stm);
        }
    }
    
    
    private static final String SEED_SELECT_SQL = "SELECT * "
                                                  + "FROM seeds "
                                                  + "WHERE url=?";
    
    
    @Override
    public Seed getSeed(String url) throws DaoException {
        PreparedStatement stm = null;
        ResultSet rs = null;
        if (!existsUrl(url)) {
            return null;
        }
        Seed result = null;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            stm = conn.prepareStatement(SEED_SELECT_SQL);
            stm.clearParameters();
            stm.setString(1, url);
            rs = stm.executeQuery();
            if (rs != null && rs.next()) {
                result = getSeedFromResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            CloseUtils.closeQuietly(rs);
            CloseUtils.closeQuietly(stm);
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
        ResultSet rs = null;
        long res = 0;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            if (status != null && dstatus == null) {
                stm = conn.prepareStatement(SEEDS_COUNT_BY_DOMAIN_AND_STATE_SQL);
                stm.clearParameters();
                stm.setString(1, domain);
                stm.setInt(2, status.ordinal());
            } else if (status == null && dstatus != null) {
                stm = conn.prepareStatement(SEEDS_COUNT_BY_DOMAIN_AND_DANICASTATE_SQL);
                stm.clearParameters();
                stm.setString(1, domain);
                stm.setInt(2, dstatus.ordinal());
            } else if (status != null && dstatus != null) {
                stm = conn.prepareStatement(SEEDS_COUNT_BY_DOMAIN_AND_STATE_AND_DANICASTATE_SQL);
                stm.clearParameters();
                stm.clearParameters();
                stm.setString(1, domain);
                stm.setInt(2, status.ordinal());
                stm.setInt(3, dstatus.ordinal());
            } else {
                stm = conn.prepareStatement(SEEDS_COUNT_BY_DOMAIN_SQL);
                stm.clearParameters();
                stm.setString(1, domain);
            }
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
        return res;
    }
    
}
