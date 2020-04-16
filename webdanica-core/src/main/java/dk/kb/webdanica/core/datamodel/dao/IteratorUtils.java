package dk.kb.webdanica.core.datamodel.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Iterator;

public class IteratorUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(IteratorUtils.class);
    
 
    public static <T> long count(Iterator<T> iterator){
        long count = 0;
        while (iterator.hasNext()) {
            T next = iterator.next();
            count++;
        }
        return count;
    }
    
    
}



