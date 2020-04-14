package dk.kb.webdanica.webapp.workflow;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import ch.qos.logback.classic.Level;
import org.slf4j.Logger;

import dk.kb.webdanica.core.WebdanicaSettings;
import dk.kb.webdanica.core.datamodel.BlackList;
import dk.kb.webdanica.core.datamodel.Cache;
import dk.kb.webdanica.core.datamodel.Seed;
import dk.kb.webdanica.core.datamodel.Status;
import dk.kb.webdanica.core.datamodel.dao.BlackListDAO;
import dk.kb.webdanica.core.datamodel.dao.CacheDAO;
import dk.kb.webdanica.core.datamodel.dao.DomainsDAO;
import dk.kb.webdanica.core.datamodel.dao.SeedsDAO;
import dk.kb.webdanica.core.seeds.filtering.FilterUtils;
import dk.kb.webdanica.core.seeds.filtering.ResolveRedirects;
import dk.kb.webdanica.core.utils.SettingsUtilities;
import dk.kb.webdanica.webapp.Configuration;
import dk.kb.webdanica.webapp.Constants;
import dk.kb.webdanica.webapp.Environment;
import org.slf4j.LoggerFactory;

/**
 * Seeds filter work-thread. A worker thread to reject seeds matching ignored
 * suffixes or matching regexps in our active blacklists or matching
 * blacklisted/rejected domains in our domain table.
 */
public class FilterWorkThread extends WorkThreadAbstract {

    static {
        logger = LoggerFactory.getLogger(FilterWorkThread.class);
    }

    private List<Seed> queueList = new LinkedList<Seed>();

    private List<Seed> workList = new LinkedList<Seed>();

    private SeedsDAO seeddao;
    private BlackListDAO blacklistDao;

    private ResolveRedirects resolveRedirects;

    private Configuration configuration;

    private boolean rejectDKUrls;

    private DomainsDAO domainDAO;
    
    private int maxRecordsProcessedInEachRun;
    
    private AtomicBoolean filteringInProgress = new AtomicBoolean(false);

    /**
     * Constructor for the Filter thread worker object.
     * 
     * @param environment The Webdanica webapp environment object
     * @param threadName The name of the thread
     */
    public FilterWorkThread(Environment environment, String threadName) {
        this.environment = environment;
        this.threadName = threadName;
    }

    public void enqueue(Seed urlRecord) {
        synchronized (queueList) {
            queueList.add(urlRecord);
        }
    }

    public void enqueue(List<Seed> urlRecords) {
        synchronized (queueList) {
            queueList.addAll(urlRecords);
        }
    }

    @Override
    public int getQueueSize() {
        int queueSize = 0;
        synchronized (queueList) {
            queueSize = queueList.size();
        }
        return queueSize;
    }

    @Override
    protected void process_init() {
        configuration = Configuration.getInstance();
        seeddao = configuration.getDAOFactory().getSeedsDAO();
        blacklistDao = configuration.getDAOFactory().getBlackListDAO();
        domainDAO = configuration.getDAOFactory().getDomainsDAO();
        
        resolveRedirects = new ResolveRedirects(configuration.getWgetSettings());
        rejectDKUrls = SettingsUtilities.getBooleanSetting(
                WebdanicaSettings.REJECT_DK_URLS,
                Constants.DEFAULT_REJECT_DK_URLS_VALUE);
        maxRecordsProcessedInEachRun = SettingsUtilities.getIntegerSetting(
                WebdanicaSettings.WEBAPP_MAX_FILTERING_RECORDS_PER_RUN,
                Constants.DEFAULT_MAX_FILTERING_RECORDS_PER_RUN);
        
    }

    @Override
    protected void process_run() {
    	if (!environment.bScheduleFiltering) {
			return;
		}
    	// ensure that only filtering process is run at a time
    	if (filteringInProgress.get()) {
            logger.info(
                    "Filtering process already in progress at '" + new Date()
                            + "'. Skipping");
            return;
        } else {
            filteringInProgress.set(Boolean.TRUE);
        }
        try {
            logger.info( "Starting process_run of thread '" + threadName
                    + "' at '" + new Date() + "'");
            List<Seed> seedsNeedFiltering = seeddao.getSeeds(Status.NEW, maxRecordsProcessedInEachRun); 
            enqueue(seedsNeedFiltering);
            if (seedsNeedFiltering.size() > 0) {
                logger.info( "Found '" + seedsNeedFiltering.size()
                        + "' seeds ready for filtering");
            }
            synchronized (queueList) {
                for (int i = 0; i < queueList.size(); ++i) {
                    Seed urlRecord = queueList.get(i);
                    workList.add(urlRecord);
                }
                queueList.clear();
            }
            if (workList.size() > 0) {
                logger.info( "Filter queue: " + workList.size());
                lastWorkRun = System.currentTimeMillis();
                filter(workList);
                startProgress(workList.size());

                stopProgress();
                workList.clear();
            }
            // Update cache
            Cache.updateCache(configuration.getDAOFactory());
        } catch (Throwable e) {
            logger.error( e.toString(), e);
        } finally {
        	logger.info("Finished process_run of thread '" + threadName
                    + "' at '" + new Date() + "'");
        	filteringInProgress.set(false);	
        }
    }

    /**
     * Do filtering on the given list of Seed objects.
     * @param workList a List of Seed objects.
     * @throws Exception
     */
    private void filter(List<Seed> workList) throws Exception {
    	// only use the active blacklists for filtering
        List<BlackList> activeBlackLists = blacklistDao.getLists(true);
        for (Seed s : workList) {
            String url = s.getUrl();
            if (ResolveRedirects.isPossibleUrlredirect(url)) {
                logger.info("Identified possible redirect url '" + url
                        + "'. Trying to resolve it");
                String redirectedUrl = resolveRedirects
                        .resolveRedirectedUrl(url);
                if (redirectedUrl != null && !redirectedUrl.isEmpty()) {
                    s.setRedirectedUrl(redirectedUrl);
                    logger.info("Identified '" + url + "' as redirecting to '"
                            + redirectedUrl + "'");
                    seeddao.updateSeed(s);
                }
            }
            boolean rejected = FilterUtils.doFilteringOnSeed(s, activeBlackLists,
                    rejectDKUrls, domainDAO);
            if (!rejected) { // set status to READY_FOR_HARVESTING and
                             // status_reason to Ready for harvesting
                s.setStatus(Status.READY_FOR_HARVESTING);
                s.setStatusReason("Ready for harvesting");
            }
            seeddao.updateSeed(s);
        }
    }

    
    @Override
    protected void process_cleanup() {
    }

}
