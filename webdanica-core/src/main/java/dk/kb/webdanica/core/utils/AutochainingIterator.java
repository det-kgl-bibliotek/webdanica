package dk.kb.webdanica.core.utils;


import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class to automatically chain generated iterators
 *
 * Use it like
 * {@code
 * Iterator combined = new AutochainingIterator(i -> getIterator(i))
 * }
 *
 * I.e. feed it a function (Integer -> Iterator<T>) to generate the next iterator
 * in the set.
 *
 * The Integer input to the function is the offset, i.e. the count of objects seen so far.
 *
 * Whenever the current iterator is exhausted, it generates the next one and continues from there.
 *
 * This continues until the generating function returns null or an iterator with no next element;
 *
 * @param <T> the type of object iterated over
 */
public class AutochainingIterator<T> implements Iterator<T> {
    
    private final Consumer<Integer> finalCloser;
    //Offset into the overall stream
    private int offset = 0;

    //The current iterator
    private SkippingIterator<T> current;
    
    /**
     * The function to generate iterators
     * Takes an integer as input, which is the offset
     * @see #offset
      */
    private Function<Integer, SkippingIterator<T>> iteratorGenerator;
   
    /**
     * The next item to return
     */
    private T currentItem;
    
    /**
     * Turn a iterator-generator into an iterator.
     * When the previous iterator runs out, it automatically requests the next one
     *
     * This continues until either the generator returns an empty iterator or null
     *
     *
     * @param iteratorGenerator the function to generate the next iterator. Takes the overall offset as input
     */
    public AutochainingIterator(Function<Integer, SkippingIterator<T>> iteratorGenerator) {
        this.iteratorGenerator = iteratorGenerator;
        this.finalCloser = null;
        init();
    }
    
    public AutochainingIterator(Function<Integer, SkippingIterator<T>> iteratorGenerator, Consumer<Integer> finalCloser) {
        this.iteratorGenerator = iteratorGenerator;
        this.finalCloser = finalCloser;
        init();
    }
    
    
    private void init() {
        current = nextIterator(0);
        
        if (current.hasNext()) {
            currentItem = current.next();
        } else {
            currentItem = null;
        }
    }
    
    @Override
    public boolean hasNext() {
        if (currentItem == null){
            close();
        }
        return currentItem != null;
        
    }
    
    @Override
    public T next() {
        try {
            if (currentItem == null){
                close();
                throw new NoSuchElementException("No next");
            }
            T result = currentItem;

            if (current.hasNext()) {
                //prepare next item and return the current
                currentItem = current.next();
                return result;
            } else {
                //get next iterator
                current = nextIterator(offset +1 );
                
                if (current != null && current.hasNext()) {
                    currentItem =  current.next();
                } else {
                    currentItem = null;
                }
                return result;
            }
        } finally {
            offset += 1;
        }
    }
    
    public long skip(long recordsToSkip){
        long totalSkipped = 0;
        while (hasNext() && totalSkipped < recordsToSkip){
            totalSkipped += current.skip(recordsToSkip-totalSkipped);
            next();
        }
        return totalSkipped;
    }
    
    private SkippingIterator<T> nextIterator(int offset) {
        return iteratorGenerator.apply(offset);
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
    }
    
    private void close() {
        if (finalCloser != null){
            finalCloser.accept(offset);
        }
    }
}

