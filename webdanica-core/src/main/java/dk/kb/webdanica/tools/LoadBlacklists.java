package dk.kb.webdanica.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import dk.kb.webdanica.datamodel.BlackList;
import dk.kb.webdanica.datamodel.BlackListDAO;

/**
 * Program to load blacklists into the webdanica database.
 *  
 *  Remember to call program with -Dwebdanica.settings.file=$LOADSEEDS_HOME/webdanica_settings_file
 *  and -Ddk.netarkivet.settings.file=$LOADSEEDS_HOME/settings_NAS_Webdanica.xml
 *  and -Dlogback.configurationFile=$LOADSEEDS_HOME/silent_logback.xml (to avoid logs to STDOUT)
 *  TESTED on files
 *   webdanica-core/src/test/resources/special_traps.txt
 *   webdanica-core/src/test/resources/standard_netarkivet_traps.txt
 *
 *  
 *  Usage LoadBlackLists <file>
 *  TODO add a description to the arguments  
 */  
public class LoadBlacklists {

	private File blacklistfile;
	
	public LoadBlacklists(File blacklistfile) {
	   this.blacklistfile = blacklistfile;
    }

	public static void main(String[] args) {
		
		if (args.length != 1) {
			System.err.println("Need blacklist file as argument");
			System.exit(1);
		}
		File seedsfile = new File(args[0]);
		if (!seedsfile.isFile()){
			System.err.println("The blacklist file located '" + seedsfile.getAbsolutePath() + "' does not exist or is not a proper file");
			System.exit(1);
		}
		
		System.out.println("Inserting blacklists from file '" + seedsfile.getAbsolutePath() + "'");

		LoadBlacklists loadseeds = new LoadBlacklists(seedsfile);
		loadseeds.insertList();
		
		BlackListDAO dao = BlackListDAO.getInstance();
		System.out.println("Showing all existing blacklists:");
		List<BlackList> allLists = dao.getLists(false);
		for (BlackList b: allLists) {
			System.out.println(b);
		}
		System.out.println("Showing all existing active blacklists:");
		List<BlackList> activeListOnly = dao.getLists(true);
		for (BlackList b: activeListOnly) {
			System.out.println(b);
		}
		dao.close();
	}
	
	/**
	 * 
	 */
	public void insertList() {
		BlackListDAO dao = BlackListDAO.getInstance();
		String line;
        long linecount=0L;
        long insertedcount=0L;
        long rejectedcount=0L;
        long duplicatecount=0L;
        String trimmedLine = null;
        BufferedReader fr = null;
        List<String> logentries = new ArrayList<String>();
        String name = blacklistfile.getName();
        List<String> blackListRegexp = new ArrayList<String>();
        try {
        	fr = new BufferedReader(new FileReader(blacklistfile));
	        while ((line = fr.readLine()) != null) {
	            trimmedLine = line.trim();
	            linecount++;
	            blackListRegexp.add(trimmedLine);
	        }
	      BlackList b = new BlackList(name, "", blackListRegexp, true);  
	      dao.insertList(b);
	      System.out.println("Created blacklist with name name=" + name);
        } catch (Throwable e) {
	        e.printStackTrace();
        } finally {
        	IOUtils.closeQuietly(fr);
        }
       
}
}