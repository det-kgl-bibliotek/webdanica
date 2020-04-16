package dk.kb.webdanica.core.datamodel.dao;

import dk.kb.webdanica.core.tools.SkippingIterator;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.phoenix.jdbc.PhoenixPreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;

public class CursorSkippingIterator<T> implements SkippingIterator<T>, AutoCloseable {
    
    private Logger logger = LoggerFactory.getLogger(CursorSkippingIterator.class);
    private Iterator<T> backing;
    private Connection conn;
    private String cursorName;
    private int batchSize;
    private SQLFunction<ResultSet, List<T>> resultsMaker;
    
    //To handle a bug in phoenix. If you get a result set less than the requested size, it is the last. But
    // if you ask to get the next, it will return the same resultset again and again
    private boolean incomplete = false;
    
    
    public CursorSkippingIterator(PhoenixPreparedStatement select, Connection conn,
                                  SQLFunction<ResultSet, List<T>> resultsMaker, int batchSize) {
        
        this.conn = conn;
        this.batchSize = batchSize;
        this.resultsMaker = resultsMaker;
        
        cursorName = "cursor" + RandomStringUtils.randomNumeric(10);
        try {
            String sql = dumbDown(select);
            select.close();
            
            //I know about SQL Injection. But notice, the only injected things are
            // cursorName, which I control totally, so no risk there
            // batchSize, which is an int, so does not allow injected values
            
            // https://phoenix.apache.org/cursors.html
            try (PreparedStatement cursorCreate = conn.prepareStatement(
                    "DECLARE " + cursorName + " CURSOR FOR " + sql)) {
                logger.info("Declaring '{}' cursor as {}", sql, cursorName);
                logger.debug(cursorCreate.toString());
                cursorCreate.execute();
            }
            
            
            try (PreparedStatement cursorOpen = conn.prepareStatement(
                    "OPEN " + cursorName + "")) {
                logger.info("Opening {}", cursorName);
                logger.debug(cursorOpen.toString());
                
                cursorOpen.execute();
            }
            
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        refresh();
    }
    
    @Override
    public long skip(long recordsToSkip) {
        logger.debug("Asking to skip {} records on curser {}", recordsToSkip, cursorName);
        long skipped = 0;
        while (backing.hasNext() && skipped < recordsToSkip) {
            backing.next();
            skipped += 1;
        }
        logger.debug("Skipped {} records already in iterator on {}", skipped, cursorName);
        
        if (skipped < recordsToSkip) { //Still more to skip
            
            //TODO if skip longer than MAXINT
            //Move the cursor forward the remaining fields
            try (PreparedStatement statement = conn.prepareStatement(
                    "FETCH NEXT " + (recordsToSkip - skipped) + " ROWS FROM " + cursorName + "");) {
                logger.info("Skipping '{}' records on cursor '{}'", (recordsToSkip - skipped), cursorName);
                logger.debug(statement.toString());
                try (ResultSet rset = statement.executeQuery();) {
                    while (rset.next()) {
                        skipped = recordsToSkip;
                    }
                    skipped = recordsToSkip;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        refresh();
        return skipped;
    }
    
    
    @Override
    public boolean hasNext() {
        refresh();
        return backing.hasNext();
    }
    
    private void refresh() {
        if (incomplete) {
            return;
        }
        if (backing != null && backing.hasNext()) {
            return;
        }
        try (PreparedStatement statement = conn.prepareStatement(
                "FETCH NEXT " + batchSize + " ROWS FROM " + cursorName + "");) {
            logger.info("Fetching '{}' records on cursor '{}'", batchSize, cursorName);
            logger.debug(statement.toString());
            ResultSet rset = statement.executeQuery();
            List<T> hits = resultsMaker.apply(rset);
            if (hits.size() < batchSize) {
                //If we get fewer than batchSize results, this is the last iterator.
                //Had to add this due to a bug with distinct domains that caused it to fetch the same 333 hits repeatedly
                incomplete = true;
            }
            backing = hits.iterator();
            
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public T next() {
        refresh();
        return backing.next();
    }
    
    @Override
    public void close() throws Exception {
        try (PreparedStatement statement = conn.prepareStatement("CLOSE " + cursorName + "");) {
            logger.info("Closing cursor '{}'", cursorName);
            logger.debug(statement.toString());
            statement.execute();
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        this.close();
    }
    
    /**
     * THIS IS BRITTLE AND DUMB
     *
     * @param preparedStatement
     * @return
     * @throws SQLException
     */
    protected String dumbDown(PhoenixPreparedStatement preparedStatement) throws SQLException {
        String sql = preparedStatement.toString();
        List<Object> params = preparedStatement.getParameters();
        ParameterMetaData paramMetadata = preparedStatement.getParameterMetaData();
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            String replacement;
            int type = paramMetadata.getParameterType(i + 1);
            switch (type) {
                //TODO the other types should also be formattet.
                case Types.VARCHAR:
                    replacement = "'" + param.toString() + "'";
                    break;
                default:
                    replacement = param.toString();
            }
            sql = sql.replaceFirst("([^a-zA-Z0-9])\\?([^a-zA-Z0-9]|$)", "$1" + replacement + "$2");
            //sql = sql.replace(":"+i, replacement);
        }
        
        logger.info("Converted '{}' to '{}'", preparedStatement, sql);
        return sql;
        
    }
    
    /**
     * Function that throws SQLException
     *
     * @param <T> the input type
     * @param <R> the output type
     */
    @FunctionalInterface
    public static interface SQLFunction<T, R> {
        R apply(T t) throws SQLException;
    }
    
}
