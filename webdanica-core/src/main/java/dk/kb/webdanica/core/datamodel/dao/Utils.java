package dk.kb.webdanica.core.datamodel.dao;

import dk.kb.webdanica.core.tools.AutochainingIterator;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class Utils {
    
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    
    public static <T> long count(Iterator<T> iterator){
        long count = 0;
        while (iterator.hasNext()) {
            T next = iterator.next();
            count++;
        }
        return count;
    }
    
    public static String dumbDown(PhoenixPreparedStatement preparedStatement) throws SQLException {
        String sql = preparedStatement.toString();
        List<Object> params = preparedStatement.getParameters();
        ParameterMetaData paramMetadata = preparedStatement.getParameterMetaData();
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            String replacement;
            int type = paramMetadata.getParameterType(i+1);
            switch (type){
                case Types.VARCHAR:
                    replacement = "'"+param.toString()+"'";
                    break;
                default:
                    replacement = param.toString();
            }
            sql = sql.replaceFirst("([^a-zA-Z0-9])\\?([^a-zA-Z0-9])", "$1"+replacement+"$2");
            //sql = sql.replace(":"+i, replacement);
        }
        
        logger.info("Converted '{}' to '{}'",preparedStatement, sql);
        return sql;
        
    }
    
    public static <T> Iterator<T> getResultIteratorSQL(PhoenixPreparedStatement select,
                                                       Connection conn,
                                                       SQLFunction<ResultSet, List<T>> resultsMaker,
                                                       int batchSize) throws
            SQLException {
        String cursorName = "cursor" + RandomStringUtils.randomNumeric(10);
    
        String sql = dumbDown(select);
        select.close();
    
        //I know about SQL Injection. But notice, the only injected things are
        // cursorName, which I control totally, so no risk there
        // batchSize, which is an int, so does not allow injected values
        
        // https://phoenix.apache.org/cursors.html
        try (PreparedStatement cursorCreate = conn.prepareStatement(
                "DECLARE "+cursorName+" CURSOR FOR " + sql)) {
            logger.info("Declaring '{}' cursor as {}", sql, cursorName);
            logger.debug(cursorCreate.toString());
            cursorCreate.execute();
        }
        
        
        try (PreparedStatement cursorOpen = conn.prepareStatement(
                "OPEN "+cursorName+"")) {
            logger.info("Opening {}", cursorName);
            logger.debug(cursorOpen.toString());
            
            cursorOpen.execute();
        }
        
        //on-element array to cheat effectively final vars in lambda
        final boolean[] incomplete = {false};
        
        Function<Integer, Iterator<T>> getNextFunction = i -> {
            if (incomplete[0]){ //last iterator was the last, so give him no further
                return Collections.emptyIterator();
            }
            try (PreparedStatement statement = conn.prepareStatement("FETCH NEXT "+batchSize+" ROWS FROM "+cursorName+"");) {
                logger.debug(statement.toString());
                ResultSet rset = statement.executeQuery();
                List<T> hits = resultsMaker.apply(rset);
                logger.info("Fetched {} hits on {}", hits.size(), cursorName);
                if (hits.size() < batchSize){
                    //If we get fewer than batchSize results, this is the last iterator.
                    //Had to add this due to a bug with distinct domains that caused it to fetch the same 333 hits repeatedly
                    incomplete[0] = true;
                }
                return hits.iterator();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
        Consumer<Integer> closeFunction = i -> {
            logger.info("Closing {} cursor", cursorName);
            try (PreparedStatement statement = conn.prepareStatement("CLOSE "+cursorName+"");) {
                logger.debug(statement.toString());
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
        AutochainingIterator<T> iterator = new AutochainingIterator<>(
                getNextFunction,
                closeFunction);
        
        
        return iterator;
    }
    
}

/**
 * Function that throws SQLException
 * @param <T> the input type
 * @param <R> the output type
 */
@FunctionalInterface
interface SQLFunction<T, R> {
    R apply(T t) throws SQLException;
}