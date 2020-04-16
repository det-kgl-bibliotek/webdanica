package dk.kb.webdanica.core.datamodel.dao;

import dk.kb.webdanica.core.datamodel.BlackList;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;

public interface BlackListDAO {

	boolean insertList(BlackList aBlackList) throws SQLException;

	BlackList readBlackList(UUID fromString) throws SQLException;

	Iterator<BlackList> getLists(boolean b) throws SQLException;
}
