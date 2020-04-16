package dk.kb.webdanica.core.datamodel.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import dk.kb.webdanica.core.datamodel.AnalysisStatus;
import dk.kb.webdanica.core.datamodel.BlackList;
import dk.kb.webdanica.core.datamodel.criteria.SingleCriteriaResult;
import dk.kb.webdanica.core.interfaces.harvesting.NasReports;
import dk.kb.webdanica.core.interfaces.harvesting.SingleSeedHarvest;
import dk.kb.webdanica.core.tools.AutochainingIterator;
import dk.kb.webdanica.core.utils.CloseUtils;
import dk.kb.webdanica.core.utils.DatabaseUtils;
import dk.kb.webdanica.core.utils.SimpleXml;
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
    public static long LIMIT = 100000L;
    
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
	
	public static final String INSERT_SQL;

	static {
		INSERT_SQL = ""
				+ "UPSERT INTO harvests (harvestname, seedurl, finalState, successful, harvested_time, files, error, " //1-7
				+ "reports, fetched_urls " //8-9
				+ ",analysis_state, analysis_state_reason" //10-11
				+ ") " 
				+ "VALUES (?,?,?,?,?,?,?,?,?, ?, ?) ";
	}
	
	@Override
	public boolean insertHarvest(SingleSeedHarvest report) throws Exception {
		java.sql.Array sqlArr = null;
		PreparedStatement stm = null;
		int res = 0;
		try {
			long harvestedTime = report.getHarvestedTime();
			if (!(harvestedTime > 0)) {
				harvestedTime = System.currentTimeMillis();
				System.err.println("harvestedTime undefined. setting it to  " + harvestedTime);
			} 
			Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
			// Handle the case of getFiles == null
			if (report.getFiles()== null) {
			    String[] emptyStrArr = new String[0];
                sqlArr = conn.createArrayOf("VARCHAR", emptyStrArr);
			} else {
			    List<String> strListFiles = report.getFiles();
			    String[] strArrFiles = new String[strListFiles.size()];
			    strArrFiles = strListFiles.toArray(strArrFiles);
			    sqlArr = conn.createArrayOf("VARCHAR", strArrFiles);
			}
			stm = conn.prepareStatement(INSERT_SQL);
			stm.clearParameters();
			stm.setString(1, report.getHarvestName());
			stm.setString(2, report.getSeed());
			stm.setInt(3, report.getFinalState().ordinal());
			stm.setBoolean(4, report.isSuccessful());
			stm.setLong(5, harvestedTime);
			stm.setArray(6, sqlArr);
			stm.setString(7, report.getErrMsg());
			//reports, fetched_urls, analysis_state, analysis_state_reason
			if (report.getReports()== null) {
				String[] emptyStrArr = new String[0];
				sqlArr = conn.createArrayOf("VARCHAR", emptyStrArr);
				stm.setArray(8, sqlArr);
			} else {
				List<String> strListReports = report.getReports().getReportsAsJsonLists();
				String[] strArrReports = new String[strListReports.size()];
				strArrReports = strListReports.toArray(strArrReports);
				sqlArr = conn.createArrayOf("VARCHAR", strArrReports);
				stm.setArray(8, sqlArr);
			}	
			
			if (report.getFetchedUrls() == null) {
				String[] emptyStrArr = new String[0];
				sqlArr = conn.createArrayOf("VARCHAR", emptyStrArr);
				stm.setArray(9, sqlArr);
			} else {
				List<String> strListUrls = new ArrayList<String>(report.getFetchedUrls());
				String[] strArrUrls = new String[strListUrls.size()];
				strArrUrls = strListUrls.toArray(strArrUrls);
				sqlArr = conn.createArrayOf("VARCHAR", strArrUrls);
				stm.setArray(9, sqlArr);
			}
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
		} finally {
			CloseUtils.freeQuietly(sqlArr);
        	CloseUtils.closeQuietly(stm);
		}
		return res != 0;
	}	

	public static final String SELECT_HARVEST_BY_NAME_SQL = "SELECT * FROM harvests WHERE harvestname=?";

	public static final String SELECT_HARVEST_COUNT_SQL = "SELECT COUNT(*) FROM harvests";
	
	/**
	 * @param harvestName a given harvestname
	 * @return null, if none found with given harvestname
	 */
	@Override
	public SingleSeedHarvest getHarvest(String harvestName) throws Exception {
		SingleSeedHarvest report = null;
		PreparedStatement stm = null;
		ResultSet rs = null;
		try {
			Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
			stm = conn.prepareStatement(SELECT_HARVEST_BY_NAME_SQL);
			stm.clearParameters();
			stm.setString(1, harvestName);
			rs = stm.executeQuery();
			report = getHarvestFromResultSet(rs);
		} finally {
        	CloseUtils.closeQuietly(rs);
        	CloseUtils.closeQuietly(stm);
		}
		return report; 
	}
	
	private String READ_ALL_SQL = "SELECT * FROM harvests";
	
	@Override
	public Iterator<SingleSeedHarvest> getAll() throws Exception {
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		return Utils.getResultIterator(READ_ALL_SQL, conn, rs -> getHarvestsFromResultSet(rs));
	}
	

 	public static final String GET_ALL_WITH_SEEDURL_SQL = "SELECT * FROM harvests WHERE seedurl=?";

 	
	@Override
	public Iterator<SingleSeedHarvest> getAllWithSeedurl(String seedurl) throws Exception {
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		PreparedStatement stm = conn.prepareStatement(GET_ALL_WITH_SEEDURL_SQL);
		stm.clearParameters();
		stm.setString(1, seedurl);
		return Utils.getResultIteratorSQL((PhoenixPreparedStatement) stm,
										  conn,
										  rs -> getHarvestsFromResultSet(rs),
										  1000);
	}

 	public static final String GET_ALL_WITH_SUCCESSFUL_SQL = "SELECT * FROM harvests WHERE successful=?";

	@Override
	public Iterator<SingleSeedHarvest> getAllWithSuccessfulstate(boolean successful) throws Exception {
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		
		PreparedStatement stm = conn.prepareStatement(GET_ALL_WITH_SUCCESSFUL_SQL);
		stm.clearParameters();
		stm.setBoolean(1, successful);
		return Utils.getResultIteratorSQL((PhoenixPreparedStatement) stm,
										  conn,
										  rs -> getHarvestsFromResultSet(rs),
										  1000);
		
	}

    @Override
    public Long getCount() throws Exception {
        PreparedStatement stm = null;
        ResultSet rs = null;
        long res = 0;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            stm = conn.prepareStatement(SELECT_HARVEST_COUNT_SQL);
            stm.clearParameters();
            rs = stm.executeQuery();
            if (rs != null && rs.next()) {
                res = rs.getLong(1);
            }
        } finally {
        	CloseUtils.closeQuietly(rs);
        	CloseUtils.closeQuietly(stm);
        }
        return res;
    }
	
	public static final String GET_ALL_NAMES_LIMIT_SQL = "SELECT harvestname FROM harvests LIMIT ?";
	
	@Override
    public Iterator<String> getAllNames() throws Exception { // Limit currently hardwired to 100K
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		
		PreparedStatement stm = conn.prepareStatement(GET_ALL_NAMES_LIMIT_SQL);
		stm.setLong(1, LIMIT);
		return Utils.getResultIteratorSQL((PhoenixPreparedStatement) stm,
										  conn,
										  rs -> getHarvestNamesFromResultSet(rs),
										  1000);
		
		
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

    public static final String SELECT_EXISTS_SQL = "SELECT count(*) FROM harvests where harvestname=?";
	@Override
    public boolean exists(String harvestName) throws Exception {
		PreparedStatement stm = null;
        ResultSet rs = null;
        long res = 0;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            stm = conn.prepareStatement(SELECT_EXISTS_SQL);
            stm.clearParameters();
            stm.setString(1, harvestName);
            rs = stm.executeQuery();
            if (rs != null && rs.next()) {
                res = rs.getLong(1);
            }
        } finally {
        	CloseUtils.closeQuietly(rs);
        	CloseUtils.closeQuietly(stm);
        }
	    return res != 0;
    }

	public static final String GET_COUNT_WITH_SEEDURL_SQL = "SELECT count(*) FROM harvests WHERE seedurl=?";
	
	@Override
    public Long getCountWithSeedurl(String url) throws Exception {
		PreparedStatement stm = null;
        ResultSet rs = null;
        long res = 0;
        try {
            Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
            stm = conn.prepareStatement(GET_COUNT_WITH_SEEDURL_SQL);
            stm.clearParameters();
            stm.setString(1, url);
            rs = stm.executeQuery();
            if (rs != null && rs.next()) {
                res = rs.getLong(1);
            }
        } finally {
        	CloseUtils.closeQuietly(rs);
        	CloseUtils.closeQuietly(stm);
        }
	    return res;
    }

}
