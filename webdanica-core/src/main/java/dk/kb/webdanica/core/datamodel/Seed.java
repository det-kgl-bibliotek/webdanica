package dk.kb.webdanica.core.datamodel;

import java.util.Date;

import dk.kb.webdanica.core.utils.UrlUtils;
import dk.kb.webdanica.core.utils.UrlInfo;

public class Seed {
	
/*
	CREATE TABLE seeds (
		    url VARCHAR PRIMARY KEY,
		    redirected_url VARCHAR,
		    host VARCHAR(256),
		    domain VARCHAR(256),
		    tld VARCHAR(64) // Top level domain for this seed
		    inserted_time TIMESTAMP,
		    updated_time TIMESTAMP,
   		    danica INTEGER, // see dk.kb.webdanica.datamodel.DanicaStatus enum class
		    status INTEGER, // see dk.kb.webdanica.datamodel.Status enum class
		    status_reason VARCHAR, // textual explanation behind its state
		);
*/
	private final String url;
	private Status status;
	private String statusReason;
	private String redirectedUrl;
	private String hostname;
	private DanicaStatus danicaStatus;
	private String domain;
	private String tld;
	private Long insertedTime;
	private Long updatedTime;
	private boolean exported;
	private Long exportedTime;
	private String danicaStatusReason;
	public static final String NOT_YET_EXPORTED_STRING = "Not yet exported";
	public static final String EXPORTED_BUT_EXPORTEDTIME_UNKNOWN_STRING 
	    = "The seed was exported but we don't know the exportedTime";
		
	public Seed(String url) {
		this.url = url;
		this.redirectedUrl = null;
		UrlInfo info = UrlUtils.getInfo(url);
		this.hostname = info.getHostname();
		this.domain = info.getDomain();
		this.tld = info.getTld();
		this.insertedTime = null;
		this.updatedTime = null;
		this.danicaStatus = DanicaStatus.UNDECIDED;
		this.danicaStatusReason = "";
		this.status = Status.NEW;
		this.statusReason = "";
		this.exported = false;
		this.exportedTime = null;
	}
	
	public Seed(String url, String redirectedUrl, String hostname, String domain, String tld, Long insertedTime, Long updatedTime, DanicaStatus danicastate, Status state, String stateReason,
			boolean exported, Long exportedTime, String danicaStatusReason) {
		this.url = url;
		setRedirectedUrl(redirectedUrl);
		this.hostname = hostname;
		this.domain = domain;
		this.tld = tld;
		this.insertedTime = insertedTime;
		this.updatedTime = updatedTime;
		setDanicaStatus(danicastate);
		setStatus(state);
		setStatusReason(stateReason);
		this.exported = exported;
		this.exportedTime = exportedTime;
		this.danicaStatusReason = danicaStatusReason;
	}
	

	public void setDanicaStatus(DanicaStatus danicastate) {
	    this.danicaStatus = danicastate;
    }
	
	public void setDanicaStatusReason(String newDanicaStatusReasons) {
	    this.danicaStatusReason = newDanicaStatusReasons;
    }


	public String getUrl() {
	    return url;
    }

	public Status getStatus() {
	    return status;
    }
	
	public String getRedirectedUrl() {
	    return redirectedUrl;
    }

	public String getHostname() {
	    return hostname;
    }

	public String getDomain() {
	    return domain;
    }

	public String getTld() {
	    return tld;
	}
	
	public String getStatusReason() {
	    return statusReason;
    }

	public DanicaStatus getDanicaStatus() {
	    return danicaStatus;
    }

	public String getDanicaStatusReason() {
	    return danicaStatusReason;
    }
	
	public String toString() {
		return "Seed '" + url + "' with status " +  status;
	}

	public void setStatus(Status newStatus) {
	    this.status = newStatus;
    }

	public void setStatusReason(String newReason) {
	    this.statusReason = newReason;
    }

	public void setRedirectedUrl(String redirectedUrl) {
	    this.redirectedUrl = redirectedUrl;
    }
	
	public Long getUpdatedTime() {
		return this.updatedTime;
	}
	
	public Long getInsertedTime() {
		return this.insertedTime;
	}
	
	public Long getExportedTime() {
		return this.exportedTime;
	}
	
	public boolean getExportedState() {
		return this.exported;
	}

	public void setExportedTime(long time) {
	    this.exportedTime = time;
    }

	public void setExportedState(boolean b) {
	    this.exported = b;
    }

	public String showExportedState() {
	    if (exported == false) {
	    	return NOT_YET_EXPORTED_STRING;
	    } else {
	        if (exportedTime != null) {
	            return "The seed was exported at: " +  new Date(exportedTime);
	        } else {
	            return "The seed was exported but we don't know the exportedTime";
	        }
	    }
	    
    }
}