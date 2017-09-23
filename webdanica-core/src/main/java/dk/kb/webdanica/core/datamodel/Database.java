package dk.kb.webdanica.core.datamodel;

import com.datastax.driver.core.Session;

/**
 * @deprecated Only used by Cassandra
 *
 */
public interface Database {
    boolean isClosed();
    void close();
    // TODO find out how to support both hbase and cassandra with the same Database interface
    // probably getSession should be replaced something else
    Session getSession();  
}
