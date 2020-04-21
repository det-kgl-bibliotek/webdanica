package dk.kb.webdanica.core.utils;

import java.util.Iterator;

public interface SkippingIterator<T> extends Iterator<T>, AutoCloseable {
    
    public long skip(long recordsToSkip);
    
}

