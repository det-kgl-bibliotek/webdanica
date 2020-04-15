package dk.kb.webdanica.core;

public class Constants {
	public static final String WEBDANICA_TRANSLATION_BUNDLE = "dk.kb.webdanica.Translations";
	public static final String CASSANDRA = "cassandra";
	public static final String HBASE_PHOENIX = "hbase-phoenix";
	public static final String DEFAULT_DATABASE_SYSTEM = HBASE_PHOENIX;
	public static final String DUMMY_HARVESTNAME = "DUMMY-HARVESTNAME";
	public static final String DEFAULT_HBASE_CONNECTON = "jdbc:phoenix:localhost:2181:/hbase";

	public static final String NODATA = "nodata";
	public static final String EMPTYLIST = "emptylist";
	/**
	 * How big a buffer we use for read()/write() operations on InputStream/ OutputStream.
	 */
	public static final int IO_BUFFER_SIZE = 4096;
	public static final int DEFAULT_MAIL_PORT = 25;
	public static final String DEFAULT_MAIL_HOST = "localhost";
	public static final String DEFAULT_MAIL_ADMIN = "test@localhost";
	public static final String WEBDANICA_SEEDS_NAME = "webdanicaseeds";
    public static final boolean DEFAULT_CONSIDER_SEED_NOT_DANICA_IF_NOT_EXPLICITLY_DANICA = false;
	
	
	
	public static final String WEBAPP_NAME = "WEBDANICA";
	public static final String SPACE = " ";
	public static final String CRLF = "\r\n";
	public static final String DEFAULT_HARVESTLOGDIR = "/home/harvestlogs/";
	public static final String DEFAULT_HARVESTLOG_PREFIX= "harvestLog-";
	public static final String DEFAULT_HARVESTLOG_READY_SUFFIX = ".txt";
	public static final String DEFAULT_HARVESTLOG_NOTREADY_SUFFIX = ".txt.open";
	
	public static final int DEFAULT_MAX_HARVESTS = 10;
	
	public static final boolean DEFAULT_REJECT_DK_URLS_VALUE = false;
	/**  Default filtering schedule: every 10 minutes. */
	public static final String DEFAULT_FILTERING_CRONTAB = 	"*/10 * * * *";
	/**  Default harvesting schedule: every 60 minutes. */
	public static final String DEFAULT_HARVESTING_CRONTAB = "0 * * * *";
	/**  Default statecaching schedule: every 15 minutes. */
	public static final String DEFAULT_STATECACHING_CRONTAB = "0,15,30,45 * * * *";
	
	public static final long MAX_SEEDS_TO_FETCH = 10000;
	public static final boolean DEFAULT_WEBAPP_DEFAULT_SECURED_SETTING = false;
	public static final int DEFAULT_MAX_FILTERING_RECORDS_PER_RUN = 1000;
	public static final long DEFAULT_HARVEST_MAX_TIMEINMILLIS = 60 * 60 * 1000L;
	public static final int DEFAULT_MAX_URL_LENGTH_TO_SHOW = 40;
	public static final int DEFAULT_MAX_URLS_TO_FETCH = 2000;
	/** Only process 15 harvestlogs at a time in the automatic workflow. */
	public static final int DEFAULT_MAX_HARVESTLOGS_PROCESSED = 15;
} 
