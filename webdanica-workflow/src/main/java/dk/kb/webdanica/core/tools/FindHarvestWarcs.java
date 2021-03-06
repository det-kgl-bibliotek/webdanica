package dk.kb.webdanica.core.tools;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dk.kb.webdanica.core.interfaces.harvesting.HarvestLog;
import dk.kb.webdanica.core.interfaces.harvesting.SingleSeedHarvest;

public class FindHarvestWarcs {

	/**
	 * Find out all heritrix warcs generated by the harvests mentioned in a specific HarvestLog Report
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length !=2) {
			System.err.println("Missing arguments: harvestLog filedir");
			System.exit(1);
		}
		File harvestLog = new File(args[0]);
		if (!harvestLog.exists()) {
			System.err.println("HarvestLog '" + harvestLog.getAbsolutePath() + "' does not exist");
			System.exit(1);
		}
		File filedir = new File(args[1]);
		if (!filedir.isDirectory()) {
			System.err.println("Filedir '" + filedir.getAbsolutePath() + "' does not exist or is not a directory");
			System.exit(1);
		}
		Set<String> filenames = getFilenames(harvestLog);
		String prefix = filedir.getCanonicalPath();
		for (String filename: filenames) {
			System.out.print(prefix + "/" + filename + " ");
		}
		
	}
	
	public static Set<String> getFilenames(File harvestLog) throws IOException {
		List<SingleSeedHarvest> results = HarvestLog.readHarvestLog(harvestLog);
		Set<String> filenames = new HashSet<String>();
		for (SingleSeedHarvest h: results) {
			if (h.getHeritrixWarcs().size() > 0) {
				filenames.addAll(h.getHeritrixWarcs());
			} else {
				// Log this in a logfile
				//System.err.println("No heritrix warcs found for harvest '" +  h.harvestName + "' with seed '" 
				//		+ h.seed + "'");
			}
		}
		return filenames;
	}
}


