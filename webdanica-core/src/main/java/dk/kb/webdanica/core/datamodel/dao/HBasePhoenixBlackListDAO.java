package dk.kb.webdanica.core.datamodel.dao;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import dk.kb.webdanica.core.datamodel.BlackList;
import dk.kb.webdanica.core.utils.CloseUtils;
import dk.kb.webdanica.core.utils.DatabaseUtils;
import org.apache.phoenix.jdbc.PhoenixPreparedStatement;

/**
 * 
 * See 
 * <a href="https://raw.githubusercontent.com/netarchivesuite/webdanica/master/scripts/hbase-phoenix/create_blacklists.sql">create-script for table blacklists</a>
 *   
 */
public class HBasePhoenixBlackListDAO implements BlackListDAO {

	private static final String INSERT_SQL;

	static {
		INSERT_SQL = ""
				+ "UPSERT INTO blacklists (uid, name, description, blacklist, last_update, is_active) "
				+ "VALUES (?, ?, ?, ?, ?, ?) ";
	}

	@Override
	public boolean insertList(BlackList aBlackList) throws SQLException {
		java.sql.Array sqlArr = null;
		int res = 0;
		try {
			Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
			
			String uuid = UUID.randomUUID().toString();
			long updated_time = System.currentTimeMillis();
			
			sqlArr = toSQLArray(conn, aBlackList.getList());
			
			try (PreparedStatement stm = conn.prepareStatement(INSERT_SQL)) {
				stm.clearParameters();
				stm.setString(1, uuid);
				stm.setString(2, aBlackList.getName());
				stm.setString(3, aBlackList.getDescription());
				stm.setArray(4, sqlArr);
				stm.setLong(5, updated_time);
				stm.setBoolean(6, aBlackList.isActive());
				res = stm.executeUpdate();
				conn.commit();
			}
		} finally {
			CloseUtils.freeQuietly(sqlArr);
		}
		return res != 0;
	}
	
	private Array toSQLArray(Connection conn, List<String> strList) throws SQLException {
		String[] strArr = new String[strList.size()];
		strArr = strList.toArray(strArr);
		Array sqlArr = conn.createArrayOf("VARCHAR", strArr);
		return sqlArr;
	}
	
	private static final String GET_BLACKLIST_SQL = "SELECT * FROM blacklists WHERE uid=? ";


	@Override
	public BlackList readBlackList(UUID uid) throws SQLException {
		BlackList retrievedBlacklist = null;
		PreparedStatement stm = null;
		ResultSet rs = null;
		try {
			Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
			stm = conn.prepareStatement(GET_BLACKLIST_SQL);
			stm.clearParameters();
			stm.setString(1, uid.toString());
			rs = stm.executeQuery();
			if (rs != null) {
				if (rs.next()) {
					retrievedBlacklist = new BlackList(
							uid,
							rs.getString("name"),
							rs.getString("description"),
							DatabaseUtils.sqlArrayToArrayList(rs.getArray("blacklist")),
							rs.getLong("last_update"),
							rs.getBoolean("is_active")
					);
				}
			}
		} finally {
        	CloseUtils.closeQuietly(rs);
        	CloseUtils.closeQuietly(stm);
		}
		return retrievedBlacklist;
	}

	private static final String GET_ACTIVE_SQL = "SELECT * "
												 + " FROM blacklists "
												 + " WHERE is_active=true ";

	private static final String GET_ALL_SQL = "SELECT * "
											  + " FROM blacklists ";

	
	@Override
	public Iterator<BlackList> getLists(boolean activeOnly) throws SQLException {
		
		Connection conn = HBasePhoenixConnectionManager.getThreadLocalConnection();
		String SQL = GET_ALL_SQL;
		if (activeOnly) {
			SQL = GET_ACTIVE_SQL;
		}
		PreparedStatement stm = conn.prepareStatement(SQL);
		
		return new CursorSkippingIterator<>((PhoenixPreparedStatement)stm, conn, rs -> {
			List<BlackList> blacklistList = new ArrayList<>();
			while (rs.next()) {
				BlackList blacklist = new BlackList(
						UUID.fromString(rs.getString("uid")),
						rs.getString("name"),
						rs.getString("description"),
						DatabaseUtils.sqlArrayToArrayList(rs.getArray("blacklist")),
						rs.getLong("last_update"),
						rs.getBoolean("is_active")
				);
				blacklistList.add(blacklist);
			}
			return blacklistList;
		},
											   1000);
	}
	
}
