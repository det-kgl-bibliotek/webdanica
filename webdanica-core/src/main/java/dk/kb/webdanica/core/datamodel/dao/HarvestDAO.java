package dk.kb.webdanica.core.datamodel.dao;

import java.util.Iterator;
import java.util.List;

import dk.kb.webdanica.core.interfaces.harvesting.SingleSeedHarvest;

public interface HarvestDAO {
	
	boolean insertHarvest(SingleSeedHarvest report) throws Exception;

	SingleSeedHarvest getHarvest(String harvestName) throws Exception;

	Iterator<SingleSeedHarvest> getAll() throws Exception;
	Iterator<String> getAllNames() throws Exception;
	Long getCount() throws Exception;
	
	Iterator<SingleSeedHarvest> getAllWithSeedurl(String seed) throws Exception;
	
	Long getCountWithSeedurl(String url) throws Exception;
	
	Iterator<SingleSeedHarvest> getAllWithSuccessfulstate(boolean b) throws Exception;
	
	boolean exists(String harvestName) throws Exception;
	
	
}
