package dk.kb.webdanica.core.datamodel.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import dk.kb.webdanica.core.datamodel.IngestLog;
import dk.kb.webdanica.core.datamodel.Seed;
import dk.kb.webdanica.core.utils.CloseUtils;
import dk.kb.webdanica.core.utils.DatabaseUtils;
import org.apache.phoenix.jdbc.PhoenixPreparedStatement;

/**
 * 
 * <a href="https://raw.githubusercontent.com/netarchivesuite/webdanica/master/scripts/hbase-phoenix/create_ingestlog.sql">create-script for table ingestlog</a
 *
 */
public class HBasePhoenixIngestLogDAO implements IngestLogDAO {

	private static final String INSERT_SQL;

	static {
		INSERT_SQL = ""
				+ "UPSERT INTO ingestLog (logLines, filename, inserted_date, linecount, insertedcount, rejectedcount, duplicatecount, errorcount) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ";
	}

	@Override
	public boolean insertLog(IngestLog log) throws Exception {
		java.sql.Array sqlArr = null;
		PreparedStatement stm = null;
		int res = 0;
		try {
			Long insertedDate = System.currentTimeMillis();
			if (log.getDate() != null) {
				insertedDate = log.getDate().getTime();
			}
			List<String> strList = log.getLogEntries();
			String[] strArr = new String[strList.size()];
			strArr = strList.toArray(strArr);
			Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
			sqlArr = conn.createArrayOf("VARCHAR", strArr);
			stm = conn.prepareStatement(INSERT_SQL);
			stm.clearParameters();
			stm.setArray(1, sqlArr);
			stm.setString(2, log.getFilename());
			stm.setLong(3, insertedDate);
			stm.setLong(4, log.getLinecount());
			stm.setLong(5, log.getInsertedcount());
			stm.setLong(6, log.getRejectedcount());
			stm.setLong(7, log.getDuplicatecount());
			stm.setLong(8, log.getErrorcount());
			res = stm.executeUpdate();
			conn.commit();
		} finally {
			CloseUtils.freeQuietly(sqlArr);
        	CloseUtils.closeQuietly(stm);
		}
		return res != 0;
	}

	private static final String GET_INGEST_DATES_SQL;

	static {
		GET_INGEST_DATES_SQL = ""
				+ "SELECT inserted_date "
				+ "FROM ingestLog"
				+ " ORDER BY inserted_date DESC";
	}

	@Override
	public Iterator<Long> getIngestDates() throws Exception { // as represented as millis from epoch
		PreparedStatement stm = null;
		try {
			Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
			stm = conn.prepareStatement(GET_INGEST_DATES_SQL);
			stm.clearParameters();
			return Utils.getResultIteratorSQL((PhoenixPreparedStatement)stm, conn, rs -> {
				List<Long> ingestDates = new ArrayList<>();
				while (rs.next()) {
					ingestDates.add(rs.getLong("inserted_date"));
				}
				return ingestDates;
			}, 1000);
		
		} finally {
        	CloseUtils.closeQuietly(stm);
		}
	}

	private static final String GET_INGEST_BY_DATE_SQL;

	static {
		GET_INGEST_BY_DATE_SQL = ""
				+ "SELECT * "
				+ "FROM ingestLog "
				+ "WHERE inserted_date=?";
	}

	@Override
	public IngestLog readIngestLog(Long timestamp) throws Exception {
		IngestLog retrievedLog = null;
		PreparedStatement stm = null;
		ResultSet rs = null;
		try {
			Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
			stm = conn.prepareStatement(GET_INGEST_BY_DATE_SQL);
			stm.clearParameters();
			stm.setLong(1, timestamp);
			rs = stm.executeQuery();
			if (rs != null) {
				while (rs.next()) {
					retrievedLog = new IngestLog(
							DatabaseUtils.sqlArrayToArrayList(rs.getArray("logLines")),
							rs.getString("filename"),
							new Date(rs.getLong("inserted_date")),
							rs.getLong("linecount"),
							rs.getLong("insertedcount"),
							rs.getLong("rejectedcount"),
							rs.getLong("duplicatecount"),
							rs.getLong("errorcount")
					);
				}
			}
		} finally {
        	CloseUtils.closeQuietly(rs);
        	CloseUtils.closeQuietly(stm);
		}
		return retrievedLog;
	}

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }
	
}
