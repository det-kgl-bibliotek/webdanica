package dk.kb.webdanica.webapp.workflow;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import ch.qos.logback.classic.Level;
import org.slf4j.Logger;

import dk.kb.webdanica.core.datamodel.dao.DAOFactory;
import dk.kb.webdanica.core.datamodel.Cache;
import dk.kb.webdanica.webapp.Environment;
import org.slf4j.LoggerFactory;

/**
 * A worker-thread for updating the state cache.
 *
 */
public class StateCacheUpdateWorkThread extends WorkThreadAbstract {
	
	static {
        logger = LoggerFactory.getLogger(StateCacheUpdateWorkThread.class);
    }

	private DAOFactory daofactory;
	private AtomicBoolean updateInProgress = new AtomicBoolean(false);
	
	/**
	 * Constructor for the StateCacheUpdateWorkThread
	 * @param environment The Webdanica webapp environment object
	 * @param threadName The name of this thread
	 */
	public StateCacheUpdateWorkThread(Environment environment, String threadName) {
		this.environment = environment;
		this.threadName = threadName;
		this.daofactory = environment.getConfig().getDAOFactory();
	}

	@Override
	protected void process_init() {	
	}

	@Override
	protected void process_run() {
		// check if not needs to run now
		if (!environment.bScheduleCacheUpdating) {
			return;
		}
		if (updateInProgress.get()) {
			 logger.info(
	                    "State cache update process already in progress at '" + new Date()
	                            + "'. Skipping");
			return;
        } else {
        	updateInProgress.set(Boolean.TRUE);
        	try {
        		Cache.updateCache(daofactory);
        	} catch (Throwable e) {
        		logger.warn( "Failure during updating of cache", e);
        	} finally {
        		updateInProgress.set(Boolean.FALSE);
        	}
        }
		
	}

	@Override
	protected void process_cleanup() {
	}

	@Override
	public int getQueueSize() {
		return 0;
	}

}
