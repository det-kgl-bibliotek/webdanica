package dk.kb.webdanica.core.datamodel.dao;

import java.util.Iterator;
import java.util.List;

import dk.kb.webdanica.core.datamodel.criteria.SingleCriteriaResult;

public interface CriteriaResultsDAO {

	boolean insertRecord(SingleCriteriaResult s) throws Exception;

	SingleCriteriaResult getSingleResult(String url, String harvest) throws Exception;

	Iterator<SingleCriteriaResult> getResultsByHarvestname(String string) throws Exception;

	Iterator<SingleCriteriaResult> getResultsByUrl(String url) throws Exception;
	
	long getResultsByUrlCount(String url) throws Exception;

	Iterator<SingleCriteriaResult> getResultsBySeedurl(String string) throws Exception;

	Iterator<SingleCriteriaResult> getResults() throws Exception;

	Iterator<String> getHarvestedUrls(String harvestname) throws Exception;

	long getCountByHarvest(String harvestName) throws Exception;

	void deleteRecordsByHarvestname(String string) throws Exception;

	boolean updateRecord(SingleCriteriaResult singleAnalysis) throws Exception;

}
