package dk.kb.webdanica.core.utils;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import java.util.zip.GZIPInputStream;


import dk.kb.webdanica.core.Constants;
import org.slf4j.LoggerFactory;


public class StreamUtils {
	
	/** Logging mechanism. */
    private static final Logger LOG = LoggerFactory.getLogger(StreamUtils.class);
    
	public static void writeline(FileOutputStream ftest, String txt) throws FileNotFoundException, IOException {
		byte[] contentInBytes = txt.getBytes();
		ftest.write(contentInBytes);
		ftest.write("\n".getBytes());
		ftest.flush();
	}
	
	public static BufferedReader getBufferedReader(File ingestFile) throws IOException {
		BufferedReader br = null;
		if (isGzippedFile(ingestFile)) {
			br = new BufferedReader(new InputStreamReader(
			        new GZIPInputStream(new FileInputStream(ingestFile))));
		} else {
			 br = new BufferedReader(new FileReader(ingestFile)); 
		}
		
	    return br;
    }

	private static boolean isGzippedFile(File ingestFile) {
	    return ingestFile.getName().endsWith(".gz"); 
    }
	
	
	
	 /** Constant for UTF-8. */
    private static final String UTF8_CHARSET = "UTF-8";

    public static synchronized String getInputStreamAsString(InputStream in) throws IOException {
	//StringBuilder res = new StringBuilder(); // Thought this could be the problem, but no   
    	StringBuffer res = new StringBuffer();
    	if (in == null){
    		LOG.warn("NULL inputstream to method getInputStreamAsString");
    		return "";
    	}

    	byte[] buf = new byte[Constants.IO_BUFFER_SIZE];
    	int read = 0;
    	try {
    		while ((read = in.read(buf)) != -1) {
    			res.append(new String(buf, UTF8_CHARSET), 0, read);
    		}
    	} finally {
    		//in.close();
    	}
    	return res.toString();
    }

	
}
