package dk.kb.webdanica.core.datamodel.dao;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dk.kb.webdanica.core.datamodel.AnalysisStatus;
import dk.kb.webdanica.core.interfaces.harvesting.NasReports;
import dk.kb.webdanica.core.interfaces.harvesting.SingleSeedHarvest;
import dk.kb.webdanica.core.utils.CloseUtils;
import dk.kb.webdanica.core.utils.DatabaseUtils;
import dk.netarkivet.harvester.datamodel.JobStatus;
import org.apache.phoenix.jdbc.PhoenixPreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
<pre>
------------+--------------+-------------+------------------------+------------+----------------+--------------+----------------+-----------------+-----------------+-----------+----------+------------+
| TABLE_CAT  | TABLE_SCHEM  | TABLE_NAME  |      COLUMN_NAME       | DATA_TYPE  |   TYPE_NAME    | COLUMN_SIZE  | BUFFER_LENGTH  | DECIMAL_DIGITS  | NUM_PREC_RADIX  | NULLABLE  | REMARKS  | COLUMN_DEF |
+------------+--------------+-------------+------------------------+------------+----------------+--------------+----------------+-----------------+-----------------+-----------+----------+------------+
|            |              | HARVESTS    | HARVESTNAME            | 12         | VARCHAR        | null         | null           | null            | null            | 0         |          |            |
|            |              | HARVESTS    | SEEDURL                | 12         | VARCHAR        | null         | null           | null            | null            | 1         |          |            |
|            |              | HARVESTS    | ERROR                  | 12         | VARCHAR        | null         | null           | null            | null            | 1         |          |            |
|            |              | HARVESTS    | SUCCESSFUL             | 16         | BOOLEAN        | null         | null           | null            | null            | 1         |          |            |
|            |              | HARVESTS    | FINALSTATE             | 4          | INTEGER        | null         | null           | null            | null            | 1         |          |            |
|            |              | HARVESTS    | HARVESTED_TIME         | -5         | BIGINT         | null         | null           | null            | null            | 1         |          |            |
|            |              | HARVESTS    | FILES                  | 2003       | VARCHAR ARRAY  | null         | null           | null            | null            | 1         |          |            |
|            |              | HARVESTS    | FETCHED_URLS           | 2003       | VARCHAR ARRAY  | null         | null           | null            | null            | 1         |          |            |
|            |              | HARVESTS    | ANALYSIS_STATE         | 4          | INTEGER        | null         | null           | null            | null            | 1         |          |            |
|            |              | HARVESTS    | ANALYSIS_STATE_REASON  | 12         | VARCHAR        | null         | null           | null            | null            | 1         |          |            |
|            |              | HARVESTS    | REPORTS                | 2003  
</pre>
* Should be identical with 
* <a href="https://raw.githubusercontent.com/netarchivesuite/webdanica/master/scripts/hbase-phoenix/create_harvests.sql">create-script for table harvests</a
*/
public class HBasePhoenixHarvestDAO implements HarvestDAO {

	private static final Logger logger = LoggerFactory.getLogger(HBasePhoenixHarvestDAO.class);
 
	public static final long LIMIT = 100000L;
    
	public SingleSeedHarvest getHarvestFromResultSet(ResultSet rs) throws Exception {
		SingleSeedHarvest report = null;
		if (rs != null) {
			if (rs.next()) {
				report = getSingleSeed(rs);
			}
		}
		return report; 
	}

	public List<SingleSeedHarvest> getHarvestsFromResultSet(ResultSet rs) throws SQLException {
		List<SingleSeedHarvest> harvestsFound = new ArrayList<>();
		if (rs != null) {
			while (rs.next()) {
				harvestsFound.add(getSingleSeed(rs));
			}
		}
		return harvestsFound;
	}
	
	private SingleSeedHarvest getSingleSeed(ResultSet rs) throws SQLException {
		return new SingleSeedHarvest(
				rs.getString("harvestname"),
				rs.getString("seedurl"),
				rs.getBoolean("successful"),
				DatabaseUtils.sqlArrayToArrayList(rs.getArray("files")),
				rs.getString("error"),
				JobStatus.fromOrdinal(rs.getInt("finalState")),
				rs.getLong("harvested_time"),
				NasReports.makeNasReportsFromJson(
						DatabaseUtils.sqlArrayToArrayList(rs.getArray("reports"))),
				DatabaseUtils.sqlArrayToArrayList(rs.getArray("fetched_urls")),		
				AnalysisStatus.fromOrdinal(rs.getInt("analysis_state")),
				rs.getString("analysis_state_reason")
				);
	}
	
	
	@Override
	public boolean insertHarvest(SingleSeedHarvest report) throws Exception {
		
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		
		java.sql.Array sqlArrFiles = null;
		java.sql.Array sqlArrReports = null;
		java.sql.Array sqlArrUrls = null;
		
		int res;
		try {
			long harvestedTime = report.getHarvestedTime();
		
			if (!(harvestedTime > 0)) {
				harvestedTime = System.currentTimeMillis();
				logger.warn("harvestedTime undefined. setting it to  " + harvestedTime);
			} 

			
			try (PreparedStatement stm = conn.prepareStatement(
					"UPSERT INTO harvests (harvestname, seedurl, finalState, successful, harvested_time, files, error, "
//1-7
					+ "reports, fetched_urls " //8-9
					+ ",analysis_state, analysis_state_reason" //10-11
					+ ") "
					+ "VALUES (?,?,?,?,?,?,?,?,?,?,?) ");) {
				stm.setString(1, report.getHarvestName());
				stm.setString(2, report.getSeed());
				stm.setInt(3, report.getFinalState().ordinal());
				stm.setBoolean(4, report.isSuccessful());
				stm.setLong(5, harvestedTime);

				sqlArrFiles =  createSQLArray(conn, report.getFiles());
				stm.setArray(6, sqlArrFiles);
				
				stm.setString(7, report.getErrMsg());
				
				
				//reports, fetched_urls, analysis_state, analysis_state_reason
				
				sqlArrReports = createSQLArray(conn, report.getReports().getReportsAsJsonLists());
				stm.setArray(8, sqlArrReports);
				
				sqlArrUrls = createSQLArray(conn, report.getFetchedUrls());
				stm.setArray(9, sqlArrUrls);
				
				// analysis_state
				AnalysisStatus as = report.getAnalysisState();
				if (as == null) {
					as = AnalysisStatus.UNKNOWN_STATUS;
				}
				stm.setInt(10, as.ordinal());
				
				// analysis_state_reason
				stm.setString(11, report.getAnalysisStateReason());
				
				res = stm.executeUpdate();
				conn.commit();
			}
		} finally {
			CloseUtils.freeQuietly(sqlArrFiles);
			CloseUtils.freeQuietly(sqlArrReports);
			CloseUtils.freeQuietly(sqlArrUrls);
		}
		return res != 0;
	}
	
	private Array createSQLArray(Connection conn, List<String> fetchedUrls) throws SQLException {
		String[] strArrUrls;
		if (fetchedUrls == null) {
			strArrUrls = new String[0];
		} else {
			strArrUrls = fetchedUrls.toArray(new String[0]);
		}
		return conn.createArrayOf("VARCHAR", strArrUrls);
	}
	
	/**
	 * @param harvestName a given harvestname
	 * @return null, if none found with given harvestname
	 */
	@Override
	public SingleSeedHarvest getHarvest(String harvestName) throws Exception {
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		try (PreparedStatement stm = conn.prepareStatement("SELECT * FROM harvests WHERE harvestname=?")) {
			stm.setString(1, harvestName);
			try (ResultSet rs = stm.executeQuery()) {
				return getHarvestFromResultSet(rs);
			}
		}
	}
	
	@Override
	public Iterator<SingleSeedHarvest> getAll() throws Exception {
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		try (PreparedStatement stm = conn.prepareStatement("SELECT * FROM harvests");) {
			return new CursorSkippingIterator<>((PhoenixPreparedStatement) stm,
												conn,
												rs -> getHarvestsFromResultSet(rs),
												1000);
		}
	}
	
	
	@Override
	public Iterator<SingleSeedHarvest> getAllWithSeedurl(String seedurl) throws Exception {
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		try (PreparedStatement stm = conn.prepareStatement("SELECT * FROM harvests WHERE seedurl=?");) {
			stm.setString(1, seedurl);
			return new CursorSkippingIterator<>((PhoenixPreparedStatement) stm,
												conn,
												rs -> getHarvestsFromResultSet(rs),
												1000);
		}
	}
	
	@Override
	public Iterator<SingleSeedHarvest> getAllWithSuccessfulstate(boolean successful) throws Exception {
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		
		try (PreparedStatement stm = conn.prepareStatement("SELECT * FROM harvests WHERE successful=?");) {
			stm.setBoolean(1, successful);
			return new CursorSkippingIterator<>((PhoenixPreparedStatement) stm,
												conn,
												rs -> getHarvestsFromResultSet(rs),
												1000);
		}
		
	}
	
	@Override
	public Long getCount() throws Exception {
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		
		try (PreparedStatement stm = conn.prepareStatement("SELECT COUNT(*) FROM harvests");) {
			try (ResultSet rs = stm.executeQuery();) {
				if (rs != null && rs.next()) {
					return rs.getLong(1);
				}
			}
		}
		return 0l;
	}
	
	@Override
    public Iterator<String> getAllNames() throws Exception { // Limit currently hardwired to 100K
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		
		try (PreparedStatement stm = conn.prepareStatement("SELECT harvestname FROM harvests LIMIT ?");) {
			stm.setLong(1, LIMIT);
			return new CursorSkippingIterator<>((PhoenixPreparedStatement) stm,
												conn,
												rs -> getHarvestNamesFromResultSet(rs),
												1000);
		}
		
		
    }

    private List<String> getHarvestNamesFromResultSet(ResultSet rs) throws SQLException {
        List<String> harvests = new ArrayList<>();
        if (rs != null) {
            while (rs.next()) {
				String name = rs.getString("harvestname");
                harvests.add(name);
            }
        }
        return harvests;
    }
	
	@Override
	public boolean exists(String harvestName) throws Exception {
		long res = 0;
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		try (PreparedStatement stm = conn.prepareStatement("SELECT count(*) FROM harvests where harvestname=?");) {
			stm.setString(1, harvestName);
			try (ResultSet rs = stm.executeQuery();) {
				if (rs != null && rs.next()) {
					res = rs.getLong(1);
				}
			}
		}
		
		return res != 0;
    }
	
	@Override
	public Long getCountWithSeedurl(String url) throws Exception {
		
		long res = 0;
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		try (PreparedStatement stm = conn.prepareStatement("SELECT count(*) FROM harvests WHERE seedurl=?");) {
			stm.setString(1, url);
			try (ResultSet rs = stm.executeQuery();) {
				if (rs != null && rs.next()) {
					res = rs.getLong(1);
				}
			}
		}
		return res;
	}

}
