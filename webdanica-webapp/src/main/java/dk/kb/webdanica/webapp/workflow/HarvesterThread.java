package dk.kb.webdanica.webapp.workflow;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;

import dk.kb.webdanica.core.interfaces.harvesting.SingleSeedHarvest;

public class HarvesterThread implements Runnable {

	/** The log. */
	private static final Logger logger = Logger.getLogger(HarvestWorkThread.class.getName());
	private String url;
	private String eventHarvestName;
	private String scheduleName;
	private String templateName;
	private long harvestMaxBytes;
	private int harvestMaxObjects;
	private SingleSeedHarvest harvest;
	private boolean harvestSuccess = false;
	
	/**
	 * 
	 * @param url
	 * @param eventHarvestName
	 * @param scheduleName
	 * @param templateName
	 * @param harvestMaxBytes
	 * @param harvestMaxObjects
	 */
	public HarvesterThread(String url, String eventHarvestName, String scheduleName, String templateName, long harvestMaxBytes, int harvestMaxObjects) {
		this.url = url;
		this.eventHarvestName=eventHarvestName;
		this.scheduleName=scheduleName;
		this.templateName=templateName;
		this.harvestMaxBytes=harvestMaxBytes;
		this.harvestMaxObjects=harvestMaxObjects;
	}

	@Override
	public void run() {
		harvest = new SingleSeedHarvest(url, eventHarvestName, scheduleName, templateName, harvestMaxBytes, harvestMaxObjects);
		try {
			harvestSuccess = harvest.finishHarvest(false);
		} catch (Throwable e) {
			logger.log(Level.WARNING, "Exception during harvesting:" + ExceptionUtils.getFullStackTrace(e), e);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean getHarvestSuccess() {
		return harvestSuccess;
	}
	
	/**
	 * 
	 * @return
	 */
	public SingleSeedHarvest getHarvestResult() {
		return harvest;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean constructionOK() {
	    return harvest.getConstructionOK();
	}
}

	
