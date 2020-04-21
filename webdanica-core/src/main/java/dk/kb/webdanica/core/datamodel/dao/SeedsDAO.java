package dk.kb.webdanica.core.datamodel.dao;

import java.util.Iterator;
import java.util.List;

import dk.kb.webdanica.core.datamodel.DanicaStatus;
import dk.kb.webdanica.core.datamodel.Seed;
import dk.kb.webdanica.core.datamodel.Status;
import dk.kb.webdanica.core.utils.SkippingIterator;

public interface SeedsDAO {
	
	Seed getSeed(String url) throws DaoException;
	
	boolean insertSeed(Seed singleSeed) throws DaoException;
	
	boolean updateSeed(Seed singleSeed) throws DaoException;
	
	
	
	Long getSeedsCount() throws DaoException;
	
	
	SkippingIterator<Seed> getSeedsForStatus(Status status, long offset, int limit) throws DaoException;
	Long getSeedsCount(Status status) throws DaoException;
	
	SkippingIterator<Seed> getSeedsForDomain(String domain, long offset, int limit) throws DaoException;
	Long getSeedsCount(String domain) throws DaoException;
	
	SkippingIterator<Seed> getSeedsForDomain(String domain, Status status, long offset, int limit) throws DaoException;
	Long getSeedsCount(String domain, Status status) throws DaoException;
	
	SkippingIterator<Seed> getSeedsForDomain(String domain, Status status, DanicaStatus danicaStatus, long offset, int maxfetched) throws DaoException;
	Long getSeedsCount(String domain, Status status, DanicaStatus dstatus) throws DaoException;
	
	
	
	
	
	Long getSeedsDanicaCount(DanicaStatus s) throws DaoException;
	
	SkippingIterator<Seed> getSeedsReadyToExport(boolean includeAlreadyExportedSeeds) throws DaoException;

	boolean existsUrl(String url) throws DaoException;
	
	
	
	
	
	
}
	
	

