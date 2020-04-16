package dk.kb.webdanica.core.datamodel.dao;

import java.util.Iterator;
import java.util.List;

import dk.kb.webdanica.core.datamodel.DanicaStatus;
import dk.kb.webdanica.core.datamodel.Seed;
import dk.kb.webdanica.core.datamodel.Status;

public interface SeedsDAO {

	boolean insertSeed(Seed singleSeed) throws DaoException;
	
	boolean updateSeed(Seed singleSeed) throws DaoException;
	
	Iterator<Seed> getSeeds(Status fromOrdinal, int limit) throws DaoException;
	
	Long getSeedsCount() throws DaoException;
	
	Long getSeedsCount(Status fromOrdinal) throws DaoException;

	Long getSeedsDanicaCount(DanicaStatus s) throws DaoException;
	
	
	
	Iterator<Seed> getSeedsReadyToExport(boolean includeAlreadyExportedSeeds) throws DaoException;

	boolean existsUrl(String url) throws DaoException;
	
	Seed getSeed(String url) throws DaoException;
	
	
	Iterator<Seed> getSeeds(String domain, int limit) throws DaoException;
	
	Long getSeedsCount(String domain) throws DaoException;
	
	
	Iterator<Seed> getSeeds(String domain, Status status, int limit) throws DaoException;
	
	Long getSeedsCount(String domain, Status status) throws DaoException;
	
	
	Iterator<Seed> getSeeds(String domain, Status status, DanicaStatus danicaStatus, int maxfetched) throws DaoException;
	
	Long getSeedsCount(String domain, Status status, DanicaStatus dstatus) throws DaoException;
	
}
	
	

