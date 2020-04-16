package dk.kb.webdanica.core.tools;

import java.util.Iterator;

public class SimpleSkippingIterator<T> implements SkippingIterator<T>{
    private Iterator<T> backing;
    
    public SimpleSkippingIterator(Iterator<T> backing) {
        this.backing = backing;
    }
    
    @Override
    public boolean hasNext() {
        return backing.hasNext();
    }
    
    @Override
    public T next() {
        return backing.next();
    }
    
    
    @Override
    public long skip(long recordsToSkip) {
        long skipped = 0;
        while (hasNext() && skipped < recordsToSkip){
            next();
            skipped++;
        }
        return skipped;
    }
    
    @Override
    public void close() throws Exception {
    
    }
}
