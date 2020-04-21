package dk.kb.webdanica.webapp.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dk.kb.webdanica.core.datamodel.dao.DaoException;
import dk.kb.webdanica.core.utils.SkippingIterator;
import org.slf4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jwat.common.Base64;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.html.HtmlEntity;
import com.antiaction.common.templateengine.Template;
import com.antiaction.common.templateengine.TemplateParts;
import com.antiaction.common.templateengine.TemplatePlaceBase;
import com.antiaction.common.templateengine.TemplatePlaceHolder;
import com.antiaction.common.templateengine.TemplatePlaceTag;

import dk.kb.webdanica.core.datamodel.Cache;
import dk.kb.webdanica.core.datamodel.DanicaStatus;
import dk.kb.webdanica.core.datamodel.Seed;
import dk.kb.webdanica.core.datamodel.Status;
import dk.kb.webdanica.core.datamodel.criteria.CriteriaUtils;
import dk.kb.webdanica.core.datamodel.criteria.SingleCriteriaResult;
import dk.kb.webdanica.core.datamodel.dao.CacheDAO;
import dk.kb.webdanica.core.datamodel.dao.CriteriaResultsDAO;
import dk.kb.webdanica.core.datamodel.dao.DAOFactory;
import dk.kb.webdanica.core.datamodel.dao.HarvestDAO;
import dk.kb.webdanica.core.datamodel.dao.SeedsDAO;
import dk.kb.webdanica.core.interfaces.harvesting.SingleSeedHarvest;
import dk.kb.webdanica.webapp.Configuration;
import dk.kb.webdanica.core.Constants;
import dk.kb.webdanica.webapp.Environment;
import dk.kb.webdanica.webapp.MenuItem;
import dk.kb.webdanica.webapp.Navbar;
import dk.kb.webdanica.webapp.Pagination;
import dk.kb.webdanica.webapp.Servlet;
import dk.kb.webdanica.webapp.User;
import dk.kb.webdanica.webapp.workflow.HarvestWorkThread;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import org.slf4j.LoggerFactory;

import static dk.kb.webdanica.core.utils.CloseUtils.closeQuietly;


public class SeedsResource implements ResourceAbstract {
    
    private static final Logger logger = LoggerFactory.getLogger(SeedsResource.class);
    
    protected static final int A_ACCEPT = 1;
    
    protected static final int A_REJECT = 2;
    
    protected static final int A_DELETE = 3;
    
    private Environment environment;
    
    protected int R_STATUS_LIST = -1;
    
    protected int R_STATUS_LIST_ID = -1;
    
    protected int R_STATUS_LIST_ID_DUMP = -1;
    
    protected int R_URL_WARC_DOWNLOAD = -1;
    protected int R_STATUS_SEED_SHOW = -1;
    
    public static final String SEED_PATH = "/seed/";
    
    public static final String SEEDS_PATH = "/seeds/";
    
    public static final String SEEDS_NUMERIC_PATH = "/seeds/<numeric>/";
    
    public static final String SEEDS_NUMERIC_DUMP_PATH = "/seeds/<numeric>/dump/";
    
    private static final String SEED_SHOW_TEMPLATE = "seed_master.html";
    
    
    @Override
    public void resources_init(Environment environment) {
        this.environment = environment;
    }
    
    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_STATUS_LIST = resourceManager.resource_add(this, SEEDS_PATH,
                                                     environment.getResourcesMap()
                                                                .getResourceByPath(SEEDS_PATH)
                                                                .isSecure());
        
        R_STATUS_LIST_ID = resourceManager.resource_add(this, SEEDS_NUMERIC_PATH,
                                                        environment.getResourcesMap()
                                                                   .getResourceByPath(SEEDS_NUMERIC_PATH)
                                                                   .isSecure());
        
        //R_STATUS_LIST_ID_DUMP = resourceManager.resource_add(this, "/seeds/<numeric>/dump/<numeric>/", true);
        R_STATUS_LIST_ID_DUMP = resourceManager.resource_add(this, SEEDS_NUMERIC_DUMP_PATH,
                                                             environment.getResourcesMap()
                                                                        .getResourceByPath(SEEDS_NUMERIC_DUMP_PATH)
                                                                        .isSecure());
        R_STATUS_SEED_SHOW = resourceManager.resource_add(this, SEED_PATH,
                                                          environment.getResourcesMap()
                                                                     .getResourceByPath(SEED_PATH)
                                                                     .isSecure());
        
        //R_URL_WARC_DOWNLOAD = resourceManager.resource_add(this, "/url/warc/<numeric>/", true);
    }
    
    @Override
    public void resource_service(ServletContext servletContext, User dab_user,
                                 HttpServletRequest req, HttpServletResponse resp,
                                 int resource_id, List<Integer> numerics, String pathInfo) throws IOException {
        
        if (resource_id == R_STATUS_LIST || resource_id == R_STATUS_LIST_ID) {
            SeedsRequest seedsRequest = SeedsRequest.getUrlFromPathinfo(pathInfo, SEEDS_PATH);
            if (seedsRequest.isChangeStateRequest()) {
                DAOFactory dao = environment.getConfig().getDAOFactory();
                changeStateForAll(seedsRequest, dao, resp);
            } else {
                urls_list(dab_user, req, resp, numerics, seedsRequest);
            }
        } else if (resource_id == R_STATUS_SEED_SHOW) {
            SeedRequest seedRequest = SeedRequest.getUrlFromPathinfo(pathInfo, SEED_PATH);
            
            url_show(dab_user, req, resp, seedRequest);
            
        } else if (resource_id == R_STATUS_LIST_ID_DUMP) {
            // Dump all resources in the given state to screen
            // text/plain; charset=utf-8
            urls_list_dump(dab_user, req, resp, numerics);
        }
    }
    
    private void changeStateForAll(SeedsRequest seedsRequest, DAOFactory dao, HttpServletResponse resp)
            throws IOException {
        Status old = Status.fromOrdinal(seedsRequest.getCurrentState());
        HarvestDAO hdao = dao.getHarvestDAO();
        SeedsDAO sdao = dao.getSeedsDAO();
        
        StringBuilder log = new StringBuilder();
        Long items = Constants.MAX_SEEDS_TO_FETCH;
        int succeeded = 0;
        try {
            long count = sdao.getSeedsCount(old);
            if (count < Constants.MAX_SEEDS_TO_FETCH) {
                items = count;
            }
            log.append("Statechange for " + items + " seeds\n");
            Iterator<Seed> seeds = sdao.getSeedsForStatus(old, 0, items.intValue());
            boolean unnormalStateChange = seedsRequest.isRetryAnalysisRequest();
            
            while (seeds.hasNext()) {
                Seed s = seeds.next();
                try {
                    if (!unnormalStateChange) { // i.e not a retryanalysisrequest
                        changeStateInDB(s,
                                        new SeedRequest(s.getUrl(),
                                                        seedsRequest.getNewState(),
                                                        seedsRequest.getPathInfo()),
                                        sdao);
                    } else {
                        retryAnalysis(s, sdao, hdao, environment.getConfig());
                    }
                    succeeded++;
                } catch (Throwable e) {
                    log.append("Unable to change state for the " + items + " seeds in state '" + old
                               + "'. The exception follows:\n");
                    StatusResource.throwable_stacktrace_dump(e, log);
                }
            }
            Cache.updateCache(dao);
        } catch (Throwable e) {
            log.append("Unable to change state for the " + items + " seeds in state '" + old
                       + "'. The exception follows:\n");
            StatusResource.throwable_stacktrace_dump(e, log);
        }
        log.append("The statechange was successful for " + succeeded + " out of " + items + " seeds");
        CommonResource.show_message(log.toString(), resp, environment);
    }
    
    private void changeState(SeedRequest seedRequest, SeedsDAO dao) throws Exception {
        Seed s = dao.getSeed(seedRequest.getUrl());
        changeStateInDB(s, seedRequest, dao);
    }
    
    private void changeStateInDB(Seed s, SeedRequest seedRequest, SeedsDAO dao) throws Exception {
        Status old = s.getStatus();
        if (seedRequest.isAcceptSeedAsDanicaRequest()) {
            Status newStatus = Status.DONE;
            s.setStatus(newStatus);
            s.setStatusReason("Changed from status '" + old + "' to status '" + newStatus
                              + "' because user has accepted this as a danica seed");
            s.setDanicaStatus(DanicaStatus.YES);
            s.setDanicaStatusReason("Accepted as danica by webuser");
        } else if (seedRequest.isRejectSeedAsDanicaRequest()) {
            Status newStatus = Status.REJECTED;
            s.setStatus(newStatus);
            s.setStatusReason("Changed from status '" + old + "' to status '" + newStatus
                              + "' because user has rejected this as a danica seed");
            s.setDanicaStatus(DanicaStatus.NO);
            s.setDanicaStatusReason("Rejected as danica by webuser");
        } else { //isValidNewState
            Status newStatus = Status.fromOrdinal(seedRequest.getNewState());
            s.setStatus(newStatus);
            s.setStatusReason("Changed from status '" + old + "' to status '" + newStatus + "' by user-request ");
        }
        dao.updateSeed(s);
    }
    
    private void url_show(User dab_user, HttpServletRequest req,
                          HttpServletResponse resp, SeedRequest sr) throws IOException {
        SeedsDAO dao = Servlet.environment.getConfig().getDAOFactory().getSeedsDAO();
        HarvestDAO hdao = Servlet.environment.getConfig().getDAOFactory().getHarvestDAO();
        // Retrieving UUID or maybe name from pathinfo instead of String equivalent of numerics
        Seed seedToShow = null;
        try {
            if (!dao.existsUrl(sr.getUrl())) {
                String error = "The given url '" + sr.getUrl() + "' was not found in the database";
                CommonResource.show_error(error, resp, environment);
                return;
            } else {
                if (sr.getNewState() != null) {
                    if (Status.isValidNewState(sr.getNewState()) || sr.isAcceptSeedAsDanicaRequest()
                        || sr.isRejectSeedAsDanicaRequest()) {
                        changeState(sr, dao);
                        resp.sendRedirect(Servlet.environment.getSeedsPath()); // redirect to main seedspage
                        return;
                    } else if (sr.isAcceptSeedAsDanicaRequest()) {
                        changeState(sr, dao);
                        resp.sendRedirect(Servlet.environment.getSeedsPath()); // redirect to main seedspage
                        return;
                    } else if (sr.isRejectSeedAsDanicaRequest()) {
                        changeState(sr, dao);
                        resp.sendRedirect(Servlet.environment.getSeedsPath()); // redirect to main seedspage
                        return;
                    } else if (sr.isRetryAnalysisRequest()) {
                        retryAnalysis(sr.getUrl(), dao, hdao, Servlet.environment.getConfig());
                        resp.sendRedirect(Servlet.environment.getSeedsPath()); // redirect to main seedspage
                        return;
                    } else {
                        String error = "The given statuscode '" + sr.getNewState() + "' is not valid";
                        CommonResource.show_error(error, resp, environment);
                        return;
                    }
                } else {
                    try {
                        seedToShow = dao.getSeed(sr.getUrl());
                    } catch (Exception e) {
                        e.printStackTrace();
                        String error = "The given url '" + sr.getUrl()
                                       + "' could not be retrived from the database due to this error: " + e;
                        CommonResource.show_error(error, resp, environment);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            String errMsg = "Unexpected exception thrown:" + e;
            logger.warn(errMsg, e);
            CommonResource.show_error(errMsg, resp, environment);
            return;
            
        }
        if (seedToShow == null) {
            String errMsg = "Should not happen. No seed found in database";
            logger.warn(errMsg);
            CommonResource.show_error(errMsg, resp, environment);
            return;
        }
        // Set up the page for showing the seed page
        
        ServletOutputStream out = resp.getOutputStream();
        resp.setContentType("text/html; charset=utf-8");
        String errorStr = null;
        String successStr = null;
        Caching.caching_disable_headers(resp);
        String templateName = SEED_SHOW_TEMPLATE;
        Template template = environment.getTemplateMaster().getTemplate(templateName);
        
        TemplatePlaceHolder titlePlace = TemplatePlaceBase.getTemplatePlaceHolder("title");
        TemplatePlaceHolder appnamePlace = TemplatePlaceBase.getTemplatePlaceHolder("appname");
        TemplatePlaceHolder navbarPlace = TemplatePlaceBase.getTemplatePlaceHolder("navbar");
        TemplatePlaceHolder userPlace = TemplatePlaceBase.getTemplatePlaceHolder("user");
        TemplatePlaceHolder menuPlace = TemplatePlaceBase.getTemplatePlaceHolder("menu");
        TemplatePlaceHolder backPlace = TemplatePlaceBase.getTemplatePlaceHolder("back");
        TemplatePlaceHolder headingPlace = TemplatePlaceBase.getTemplatePlaceHolder("heading");
        TemplatePlaceHolder alertPlace = TemplatePlaceBase.getTemplatePlaceHolder("alert");
        TemplatePlaceHolder contentPlace = TemplatePlaceBase.getTemplatePlaceHolder("content");
        
        /*        
      <h4>URL: <placeholder id="url" /><br>
  RedirectedUrl: <placeholder id="redirected_url" /></br>  
  Domain: <placeholder id="domain" /></br>
  Hostname: <placeholder id="host" /></br>
  TLD: <placeholder id="tld" /></br>
  Status: <placeholder id="status" /> </br> 
  StatusReason: <placeholder id="status_reason" /></br>
  DanicaStatus: <placeholder id="danica_status" /></br>
  InsertedDate: <placeholder id="inserted_time" /></br>
  UpdatedTime: <placeholder id="updated_time" /></br>
  Exported: <placeholder id="exported" /></br>
  <br>
  Harvests: <placeholder id="harvests" /></br>
  CriteriaResult: <placeholder id="criteriaresults" /></br>
*/
        TemplatePlaceHolder urlPlace = TemplatePlaceBase.getTemplatePlaceHolder("url");
        TemplatePlaceHolder redirectedUrlPlace = TemplatePlaceBase.getTemplatePlaceHolder("redirected_url");
        TemplatePlaceHolder domainPlace = TemplatePlaceBase.getTemplatePlaceHolder("domain");
        TemplatePlaceHolder hostPlace = TemplatePlaceBase.getTemplatePlaceHolder("host");
        TemplatePlaceHolder tldPlace = TemplatePlaceBase.getTemplatePlaceHolder("tld");
        
        TemplatePlaceHolder statusPlace = TemplatePlaceBase.getTemplatePlaceHolder("status");
        TemplatePlaceHolder statusReasonPlace = TemplatePlaceBase.getTemplatePlaceHolder("status_reason");
        TemplatePlaceHolder danicastatusPlace = TemplatePlaceBase.getTemplatePlaceHolder("danica_status");
        TemplatePlaceHolder danicastatusReasonPlace = TemplatePlaceBase.getTemplatePlaceHolder("danica_status_reason");
        TemplatePlaceHolder insertedTimePlace = TemplatePlaceBase.getTemplatePlaceHolder("inserted_time");
        TemplatePlaceHolder updatedTimePlace = TemplatePlaceBase.getTemplatePlaceHolder("updated_time");
        TemplatePlaceHolder exportedPlace = TemplatePlaceBase.getTemplatePlaceHolder("exported");
        TemplatePlaceHolder harvestsPlace = TemplatePlaceBase.getTemplatePlaceHolder("harvests");
        TemplatePlaceHolder criteriaResultsPlace = TemplatePlaceBase.getTemplatePlaceHolder("criteriaresults");
        TemplatePlaceHolder linksPlace = TemplatePlaceBase.getTemplatePlaceHolder("links");
        
        List<TemplatePlaceBase> placeHolders = new ArrayList<TemplatePlaceBase>();
        placeHolders.add(titlePlace);
        placeHolders.add(appnamePlace);
        placeHolders.add(navbarPlace);
        placeHolders.add(userPlace);
        placeHolders.add(menuPlace);
        placeHolders.add(backPlace);
        placeHolders.add(headingPlace);
        placeHolders.add(alertPlace);
        placeHolders.add(contentPlace);
        // add the new placeholders
        placeHolders.add(urlPlace);
        placeHolders.add(redirectedUrlPlace);
        placeHolders.add(domainPlace);
        placeHolders.add(hostPlace);
        placeHolders.add(tldPlace);
        placeHolders.add(statusPlace);
        placeHolders.add(statusReasonPlace);
        placeHolders.add(danicastatusPlace);
        placeHolders.add(insertedTimePlace);
        placeHolders.add(updatedTimePlace);
        placeHolders.add(exportedPlace);
        placeHolders.add(harvestsPlace);
        placeHolders.add(criteriaResultsPlace);
        placeHolders.add(linksPlace);
        placeHolders.add(danicastatusReasonPlace);
        
        TemplateParts templateParts = template.filterTemplate(placeHolders, resp.getCharacterEncoding());
        
        
        /*
         * Heading.
         */
        String heading = "Details about seed '" + seedToShow.getUrl() + "': ";
        
        /*
         * Places.
         */
        
        if (titlePlace != null) {
            titlePlace.setText(HtmlEntity.encodeHtmlEntities(Constants.WEBAPP_NAME).toString());
        }
        
        if (appnamePlace != null) {
            appnamePlace.setText(HtmlEntity.encodeHtmlEntities(Constants.WEBAPP_NAME
                                                               + Constants.SPACE + environment.getVersion())
                                           .toString());
        }
        
        if (navbarPlace != null) {
            navbarPlace.setText(Navbar.getNavbar(Navbar.N_SEEDS));
        }
        
        if (userPlace != null) {
            userPlace.setText(Navbar.getUserHref(dab_user));
        }
        
        if (backPlace != null) {
            backPlace.setText("<a href=\""
                              + Servlet.environment.getSeedsPath() + seedToShow.getStatus().ordinal() + "/"
                              + "\" class=\"btn btn-primary\"><i class=\"icon-white icon-list\"></i> Tilbage til oversigten</a>");
        } else {
            logger.warn("No back´placeholder found in template '" + templateName + "'");
        }
        
        if (headingPlace != null) {
            headingPlace.setText(heading);
        } else {
            logger.warn("No heading´ placeholder found in template '" + templateName + "'");
        }
        
        String redirectedUrlText = seedToShow.getRedirectedUrl();
        if (redirectedUrlText == null || redirectedUrlText.isEmpty()) {
            redirectedUrlText = "N/A";
        }
        Set<String> linkSet = new HashSet<String>();
        Status state = seedToShow.getStatus();
        if (state.equals(Status.ANALYSIS_FAILURE) || state.equals(Status.HARVESTING_FAILED)
            || state.equals(Status.HARVESTING_IN_PROGRESS) || state.equals(Status.HARVESTING_FINISHED)) {
            String links = environment.getSeedPath() + HTMLUtils.encode(CriteriaUtils.toBase64(seedToShow.getUrl()))
                           + "/" + Status.READY_FOR_HARVESTING.ordinal() + "/";
            String retryHarvestLink = "[<A href=\"" + links + "\"> Retry harvesting</A>]";
            linkSet.add(retryHarvestLink);
        }
        
        if (!state.equals(Status.NEW)) {
            String links = environment.getSeedPath() + HTMLUtils.encode(CriteriaUtils.toBase64(seedToShow.getUrl()))
                           + "/" + Status.NEW.ordinal() + "/";
            String retryLink = "[<A href=\"" + links + "\"> Reset seed to status NEW</A>]";
            linkSet.add(retryLink);
        }
        DanicaStatus dState = seedToShow.getDanicaStatus();
        if (!dState.equals(DanicaStatus.YES)) {
            String links = environment.getSeedPath() + HTMLUtils.encode(CriteriaUtils.toBase64(seedToShow.getUrl()))
                           + "/" + SeedRequest.ACCEPT_AS_DANICA_CODE + "/";
            String retryLink = "[<A href=\"" + links + "\"> Accept seed as Danica</A>]";
            linkSet.add(retryLink);
        }
        if (!dState.equals(DanicaStatus.NO)) {
            String links = environment.getSeedPath() + HTMLUtils.encode(CriteriaUtils.toBase64(seedToShow.getUrl()))
                           + "/" + SeedRequest.REJECT_AS_DANICA_CODE + "/";
            String retryLink = "[<A href=\"" + links + "\"> Reject seed as Danica</A>]";
            linkSet.add(retryLink);
        }
        if (state.equals(Status.ANALYSIS_FAILURE) || state.equals(Status.HARVESTING_FINISHED)) {
            String links = environment.getSeedPath() + HTMLUtils.encode(CriteriaUtils.toBase64(seedToShow.getUrl()))
                           + "/" + SeedRequest.RETRY_ANALYSIS_CODE + "/";
            String retryLink = "[<A href=\"" + links + "\"> Retry Analysis of last harvestdata</A>]";
            linkSet.add(retryLink);
        }
        
        ResourceUtils.insertText(linksPlace, "links", StringUtils.join(linkSet, "&nbsp;&nbsp;"), templateName, logger);
        ResourceUtils.insertText(urlPlace, "url", seedToShow.getUrl(), templateName, logger);
        ResourceUtils.insertText(redirectedUrlPlace, "redirected_url", redirectedUrlText, templateName, logger);
        ResourceUtils.insertText(tldPlace, "tld", seedToShow.getTld() + "", templateName, logger);
        ResourceUtils.insertText(hostPlace, "host", seedToShow.getHostname() + "", templateName, logger);
        ResourceUtils.insertText(domainPlace, "domain", seedToShow.getDomain() + "", templateName, logger);
        ResourceUtils.insertText(statusPlace, "status", seedToShow.getStatus() + "", templateName, logger);
        ResourceUtils.insertText(statusReasonPlace,
                                 "status_reason",
                                 seedToShow.getStatusReason() + "",
                                 templateName,
                                 logger);
        ResourceUtils.insertText(danicastatusPlace,
                                 "danica_status",
                                 seedToShow.getDanicaStatus() + "",
                                 templateName,
                                 logger);
        
        String danicaStatusReasonToShow = seedToShow.getDanicaStatusReason();
        if (danicaStatusReasonToShow == null) {
            danicaStatusReasonToShow = "N/A";
        }
        ResourceUtils.insertText(danicastatusReasonPlace,
                                 "danica_status_reason",
                                 danicaStatusReasonToShow,
                                 templateName,
                                 logger);
        ResourceUtils.insertText(insertedTimePlace,
                                 "inserted_time",
                                 ResourceUtils.printDate(seedToShow.getInsertedTime()),
                                 templateName,
                                 logger);
        ResourceUtils.insertText(updatedTimePlace,
                                 "updated_time",
                                 ResourceUtils.printDate(seedToShow.getUpdatedTime()),
                                 templateName,
                                 logger);
        ResourceUtils.insertText(exportedPlace, "exported", "" + seedToShow.showExportedState(), templateName, logger);
        String harvestsString = "N/A";
        String criteriaString = "N/A";
        StringBuilder sbCriteriaResults = new StringBuilder();
        try {
            long hcount = hdao.getCountWithSeedurl(seedToShow.getUrl());
            if (hcount > 0) {
                String link = environment.getHarvestsPath()
                              + HTMLUtils.encode(CriteriaUtils.toBase64(seedToShow.getUrl())) + "/";
                harvestsString = "<A href=\"" + link + "\">" + hcount + " harvests</A>";
            } else {
                harvestsString = "0 harvests";
            }
            CriteriaResultsDAO cdao = this.environment.getConfig().getDAOFactory().getCriteriaResultsDAO();
            Iterator<SingleCriteriaResult> cresults = cdao.getResultsByUrl(seedToShow.getUrl());
            Set<String> clinks = new HashSet<>();
            long ccount = cdao.getResultsByUrlCount(seedToShow.getUrl());
            sbCriteriaResults.append(ccount + " results");
            
            if (cresults.hasNext()) {
                while (cresults.hasNext()) {
                    SingleCriteriaResult c = cresults.next();
                    String linkText = "result from harvest '" + c.harvestName + "'";
                    clinks.add(CriteriaResultsResource.createLink(environment, c.harvestName, c.url, linkText));
                }
                StringBuilder sb = new StringBuilder();
                sb.append("</br><pre>\r\n");
                for (String listElement : clinks) {
                    sb.append(listElement);
                    sb.append("\r\n");
                }
                sb.append("</pre>\r\n");
                sbCriteriaResults.append(sb);
                criteriaString = sbCriteriaResults.toString();
            }
        } catch (Throwable e) {
            String errMsg = "Unexpected exception thrown:" + e;
            logger.warn(errMsg, e);
            CommonResource.show_error(errMsg, resp, environment);
            return;
        }
        
        ResourceUtils.insertText(harvestsPlace, "harvests", harvestsString, templateName, logger);
        ResourceUtils.insertText(criteriaResultsPlace, "criteriaresults", criteriaString, templateName, logger);
        StringBuilder sb = new StringBuilder();
        ResourceUtils.insertText(contentPlace, "content", sb.toString(), templateName, logger);
        
        CommonResource.insertInAlertPlace(alertPlace, errorStr, successStr, templateName, logger);
        try {
            for (int i = 0; i < templateParts.parts.size(); ++i) {
                out.write(templateParts.parts.get(i).getBytes());
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            logger.warn("IOException thrown, but ignored: " + e);
        }
    }
    
    public static String findHarvestNameInStatusReason(String statusReason) {
        final String SPLITTER = "harvestname";
        final String SPLITTER2 = "harvest";
        String harvestName = null;
        boolean isTypeOne = !statusReason.contains("'") && statusReason.contains(SPLITTER);
        boolean isTypeTwo = statusReason.contains("'") && statusReason.contains(SPLITTER);
        boolean isTypeThree = statusReason.contains("'") && statusReason.contains(SPLITTER2);
        
        if (isTypeOne || isTypeTwo) {
            //System.out.println("Can't find harvestname in field statusreason. marker has changed: '" +  statusReason + "'");
            String[] statusReasonParts = statusReason.split(SPLITTER);
            if (statusReasonParts.length > 1) {
                harvestName = statusReasonParts[1].trim();
                if (!isTypeTwo) {
                    String[] harvestNameparts = harvestName.split(" ");
                    if (harvestNameparts.length == 2) {
                        return harvestNameparts[1];
                    } else {
                        return null;
                    }
                } else {
                    return harvestName.substring(harvestName.indexOf("'") + 1, harvestName.lastIndexOf("'"));
                }
            } else {
                return null;
            }
        } else if (isTypeThree) {
            String[] statusReasonParts = statusReason.split(SPLITTER2);
            if (statusReasonParts.length > 1) {
                harvestName = statusReasonParts[1].trim();
                if (harvestName.indexOf("'") != harvestName.lastIndexOf("'") && harvestName.indexOf("'") != -1) {
                    return harvestName.substring(harvestName.indexOf("'") + 1, harvestName.lastIndexOf("'"));
                }
            }
        }
        return null;
    }
    
    private void retryAnalysis(Seed s, SeedsDAO dao, HarvestDAO hdao, Configuration conf) throws Exception {
        String statusReason = s.getStatusReason();//
        String harvestName = findHarvestNameInStatusReason(statusReason);
        if (harvestName == null) {
            logger.warn("Can't find harvestname in field statusreason. marker has changed: '" + statusReason + "'");
            return;
        }
        ;
        
        SingleSeedHarvest ssh = hdao.getHarvest(harvestName);
        if (ssh == null) {
            logger.warn("Can't find harvest '" + harvestName + "' in harvests table");
            return;
        } else {
            if (!ssh.isSuccessful()) {
                logger.warn("Can't retry analysis of failed harvest '" + harvestName + "'");
                return;
            } else {
                // writeharvest to harvestLogs
                try {
                    List<SingleSeedHarvest> harvests = new ArrayList<SingleSeedHarvest>();
                    harvests.add(ssh);
                    HarvestWorkThread.writeHarvestLog(harvests, conf);
                } catch (Throwable e) {
                    logger.warn("Failed to harvestlog for harvest '" + harvestName + "' to harvestlogsdir", e);
                    return;
                }
                // update status of seed
                s.setStatus(Status.READY_FOR_ANALYSIS);
                s.setStatusReason("Ready for analysis retry of harvest '" + harvestName + "'");
                dao.updateSeed(s);
            }
        }
        
    }
    
    private void retryAnalysis(String url, SeedsDAO dao, HarvestDAO hdao, Configuration conf) throws Exception {
        Seed s = dao.getSeed(url); // we have already checked that it exists
        retryAnalysis(s, dao, hdao, conf);
    }
    
    private void urls_list_dump(User dab_user, HttpServletRequest req,
                                HttpServletResponse resp, List<Integer> numerics) throws IOException {
        SeedsDAO dao = Servlet.environment.getConfig().getDAOFactory().getSeedsDAO();
        
        int status = 0; //Ordinal for Status.NEW
        if (numerics.size() >= 1) {
            status = numerics.get(0);
        }
        
        int online_status = 0; // default = alle (alternatives: only_only, offline-only
        if (numerics.size() == 2) {
            online_status = numerics.get(1);
        }
        
        ServletOutputStream out = resp.getOutputStream();
        resp.setContentType("text/plain; charset=utf-8");
        //resp.setHeader("content-disposition", "attachment; filename=\"url_list_status_"+ status + "_" + online_status_text + ".txt\"");
        resp.setHeader("content-disposition", "attachment; filename=\"url_list_status_" + status + ".txt\"");
        //logger.info("Using encoding in response: " + resp.getCharacterEncoding());
        
        // TODO this does not scale
        // Make an iterator
        Iterator<Seed> urlRecords = null;
        try {
            // This needs to be further limited (e.g. by domain, and tld)
            urlRecords = dao.getSeedsForStatus(Status.fromOrdinal(online_status), 0, 10000);
        } catch (Exception e) {
            String errMsg = "Unexpected exception thrown:" + e;
            logger.warn(errMsg, e);
            CommonResource.show_error(errMsg, resp, environment);
            return;
        }
        
        
        StringBuilder sb = new StringBuilder();
        sb.append("##\r\n");
        //TODO size of seeds
        sb.append("## Liste over alle  seeds i status "
                  + status + "\r\n");
        sb.append("##\r\n");
        
        while (urlRecords.hasNext()) {
            Seed rec = urlRecords.next();
            sb.append(rec.getUrl());
            sb.append("\r\n");
        }
        
        try {
            out.write(sb.toString().getBytes("utf-8"));
            out.flush();
            out.close();
        } catch (IOException e) {
            logger.warn("IOException thrown: " + e);
        }
    }
    
    
    public void urls_list(User dab_user, HttpServletRequest req,
                          HttpServletResponse resp, List<Integer> numerics, SeedsRequest seedsRequest)
            throws IOException {
        
        
        //Parse Parameters
        
        final int status = getStatus(numerics, seedsRequest);
        
       
        
        boolean bShowAll = false;
        
        
        //Template crap
        
        String templateName = "urls_list.html";
        Template template = environment.getTemplateMaster().getTemplate(templateName);
        
        TemplatePlaceHolder titlePlace = TemplatePlaceBase.getTemplatePlaceHolder("title");
        TemplatePlaceHolder appnamePlace = TemplatePlaceBase.getTemplatePlaceHolder("appname");
        TemplatePlaceHolder navbarPlace = TemplatePlaceBase.getTemplatePlaceHolder("navbar");
        TemplatePlaceHolder userPlace = TemplatePlaceBase.getTemplatePlaceHolder("user");
        TemplatePlaceHolder menuPlace = TemplatePlaceBase.getTemplatePlaceHolder("menu");
        TemplatePlaceHolder statemenuPlace = TemplatePlaceBase.getTemplatePlaceHolder("state_menu");
        TemplatePlaceHolder headingPlace = TemplatePlaceBase.getTemplatePlaceHolder("heading");
        TemplatePlaceHolder actionButtonsPlace = TemplatePlaceBase.getTemplatePlaceHolder("action_buttons");
        TemplatePlaceHolder paginationPlace = TemplatePlaceBase.getTemplatePlaceHolder("pagination");
        TemplatePlaceHolder pagination2Place = TemplatePlaceBase.getTemplatePlaceHolder("pagination2");
        TemplatePlaceTag myformTag = TemplatePlaceTag.getInstance("form", "myform");
        TemplatePlaceHolder statusPlace = TemplatePlaceBase.getTemplatePlaceHolder("status");
        TemplatePlaceHolder dumpPlace = TemplatePlaceBase.getTemplatePlaceHolder("dump");
        TemplatePlaceHolder alertPlace = TemplatePlaceBase.getTemplatePlaceHolder("alert");
        
        TemplatePlaceHolder linksPlace = TemplatePlaceBase.getTemplatePlaceHolder("links");
        
        List<TemplatePlaceBase> placeHolders = Arrays.asList((TemplatePlaceBase) titlePlace,
                                                             appnamePlace,
                                                             navbarPlace,
                                                             userPlace,
                                                             menuPlace,
                                                             statemenuPlace,
                                                             headingPlace,
                                                             actionButtonsPlace,
                                                             paginationPlace,
                                                             pagination2Place,
                                                             //myformTag,
                                                             statusPlace,
                                                             dumpPlace,
                                                             alertPlace);
        
        resp.setContentType("text/html; charset=utf-8");
        TemplateParts templateParts = template.filterTemplate(placeHolders, resp.getCharacterEncoding());
        
        
        CacheDAO cdao = Servlet.environment.getConfig().getDAOFactory().getCacheDAO();
        Cache cache = null;
        try {
            cache = cdao.getCache();
        } catch (Throwable e) {
            logger.warn("Exception thrown during call to cdao.getCache(): " + e);
        }
        if (cache == null) {
            logger.warn("No cache available from cdao.getCache(). Using dummy values instead");
            cache = Cache.getDummyCache();
        }
    
        
        headingPlace.setText(getHeading(seedsRequest));
    
    
        StringBuilder statemenuSb;
        try {
             statemenuSb = buildStatemenu(status, cache);
        } catch (Throwable e) {
            String errMsg = "Building statemenu failed: " + ExceptionUtils.getFullStackTrace(e);
            logger.warn(errMsg, e);
            CommonResource.show_error(errMsg, resp, environment);
            return;
        }
        statemenuPlace.setText(statemenuSb.toString());
    
    
        /*
         * Menu.
         */
        menuPlace.setText("");
        
        
        /*
         * Urls.
         */
        
        StringBuilder urlListSb = new StringBuilder();
        
        
        urlListSb.append("<table class=\"table table-striped\">\n");
        urlListSb.append("  <thead>\n");
        urlListSb.append("    <tr>\n");
        
        urlListSb.append("      <th>url</th>\n");
        if (status == Status.REJECTED.ordinal()) {
            urlListSb.append("      <th>grund</th>\n");
        }
        
        urlListSb.append("    </tr>\n");
        urlListSb.append("  </thead>\n");
        urlListSb.append("  <tbody>\n");
        
        
        int maxUrlLengthToShow = environment.getConfig().getMaxUrlLengthToShow();
        
        SeedsDAO sdao = Servlet.environment.getConfig().getDAOFactory().getSeedsDAO();
        long seedsCount;
        try {
            seedsCount = getSeedsCount(seedsRequest, status, sdao);
        } catch (DaoException e) {
            String errMsg = "Exception on retrieving seedcount from '" + seedsRequest
                            + "' : " + ExceptionUtils.getFullStackTrace(e);
            logger.warn(errMsg, e);
            CommonResource.show_error(errMsg, resp, environment);
            return;
        }
    
        final int itemsPerPage = getItemsPerPage(req);
    
        final long pages = Pagination.getPages(seedsCount, itemsPerPage);
    
        logger.debug("Number of pages: {}",pages);
        final long page = getPage(req, pages);
    
        logger.debug("Current page: {}",page);
        
        final long first_item_offset = (page - 1) * itemsPerPage;
        logger.debug("Offset of first item: {}",first_item_offset);
        
        int show = itemsPerPage;
        
        
        SkippingIterator<Seed> urlRecords;
        try {
            urlRecords = getUrlIterator(seedsRequest, status, itemsPerPage, sdao, first_item_offset);
        } catch (DaoException e) {
            String errMsg = "Exception on retrieving seeds from '" + seedsRequest
                            + "' : " + ExceptionUtils.getFullStackTrace(e);
            logger.warn(errMsg, e);
            CommonResource.show_error(errMsg, resp, environment);
            return;
        }
        try {
            while (urlRecords.hasNext() && show > 0) {
                Seed urlRecord = urlRecords.next();
                
                urlListSb.append("<tr>");
                urlListSb.append("<td>");
                urlListSb.append("<a href=\"");
                urlListSb.append(urlRecord.getUrl()); // Point to original url
                urlListSb.append("\">");
                urlListSb.append(makeEllipsis(urlRecord.getUrl(), maxUrlLengthToShow));
                urlListSb.append("</a>");
                // Add encoded link to show details about seed
                String base64Encoded = Base64.encodeString(urlRecord.getUrl());
                if (base64Encoded == null) {
                    logger.warn(
                            "base64 encoding of url '" + urlRecord.getUrl()
                            + "' gives null. Maybe it's already encoded?");
                    base64Encoded = urlRecord.getUrl();
                }
                String linkToShowPage = Servlet.environment.getSeedPath() + HTMLUtils.encode(base64Encoded) + "/\"";
                urlListSb.append("(<a href=\"" + linkToShowPage + ">Show details</a>)");
                urlListSb.append("</td>");
                urlListSb.append("<td>");
                
                if (status == Status.REJECTED.ordinal()) {
                    urlListSb.append("<b>" + urlRecord.getStatusReason() + "</b>");
                }
                urlListSb.append("</td>");
                urlListSb.append("</tr>\n");
                --show;
            }
        } finally {
            closeQuietly(urlRecords);
        }
        urlListSb.append("  </tbody>\n");
        urlListSb.append("</table>\n");
        
        statusPlace.setText(urlListSb.toString());
        
        
        
       
        /*
         * Places.
         */
        titlePlace.setText(HtmlEntity.encodeHtmlEntities(Constants.WEBAPP_NAME).toString());
        
        
        appnamePlace.setText(HtmlEntity.encodeHtmlEntities(
                Constants.WEBAPP_NAME + Constants.SPACE + environment.getVersion()).toString());
        
        
        navbarPlace.setText(Navbar.getNavbar(Navbar.N_SEEDS));
        
        
        userPlace.setText(Navbar.getUserHref(dab_user));
        
        
        
        
/*
            actionButtonsPlace.setText(actionButtonsSb.toString());
        
*/
        paginationPlace.setText(Pagination.getPagination(page, itemsPerPage, pages, bShowAll));
        
        
        pagination2Place.setText(Pagination.getPagination(page, itemsPerPage, pages, bShowAll));
        
        
        dumpPlace.setText("");
        
        
        if (myformTag.htmlItem != null) {
            myformTag.htmlItem.setAttribute("action", "?page=" + page + "&itemsperpage=" + itemsPerPage);
        }
        
        
        /*LinkPlace*/
        String linkprefix = environment.getSeedsPath() + Status.fromOrdinal(status).ordinal() + "/";
        Set<String> linkSet = new HashSet<String>();
        
        String links = linkprefix + Status.READY_FOR_HARVESTING.ordinal() + "/";
        String link = "[<A href=\"" + links + "\"> Retry harvesting</A>]";
        linkSet.add(link);
        
        links = linkprefix + Status.NEW.ordinal() + "/";
        link = "[<A href=\"" + links + "\"> Reset seeds to status NEW</A>]";
        linkSet.add(link);
        
        links = linkprefix + SeedRequest.RETRY_ANALYSIS_CODE + "/";
        link = "[<A href=\"" + links + "\"> Retry Analysis of last harvestdata</A>]";
        linkSet.add(link);
        
        linksPlace.setText(StringUtils.join(linkSet, "&nbsp;&nbsp;"));
        
        
        /*
         * contentPlace.setText( sb.toString() );
         */
        
        String errorStr = null;
        String successStr = null;
        CommonResource.insertInAlertPlace(alertPlace, errorStr, successStr, templateName, logger);
        
        
        //Output the page
        try (ServletOutputStream out = resp.getOutputStream()) {
            Caching.caching_disable_headers(resp);
            for (int i = 0; i < templateParts.parts.size(); ++i) {
                out.write(templateParts.parts.get(i).getBytes());
            }
            out.flush();
            
        } catch (IOException e) {
            logger.warn("Unexpected exception thrown", e);
        }
        
    }
    
    private String getHeading(SeedsRequest seedsRequest) {
        String heading = "";
        if (seedsRequest.isShowSeedsFromDomainRequest()) {
            heading = "Seeds from domain '" + seedsRequest.getDomain() + "'";
        } else if (seedsRequest.isShowSeedsFromDomainWithStateRequest()) {
            heading = "Seeds from domain '" + seedsRequest.getDomain() + "' with state '" + DanicaStatus.fromOrdinal(
                    seedsRequest.getCurrentState());
        }
        return heading;
        
    }
    
    private SkippingIterator<Seed> getUrlIterator(SeedsRequest seedsRequest,
                                                  int status,
                                                  int itemsPerPage,
                                                  SeedsDAO sdao,
                                                  long first_item_offset) throws DaoException {
        SkippingIterator<Seed> urlRecords = null;
        
        if (seedsRequest.isShowSeedsFromDomainRequest()) {
            urlRecords = sdao.getSeedsForDomain(seedsRequest.getDomain(), first_item_offset, itemsPerPage);
        } else if (seedsRequest.isShowSeedsFromDomainWithStateRequest()) {
            urlRecords = sdao.getSeedsForDomain(seedsRequest.getDomain(),
                                                Status.fromOrdinal(status),
                                                first_item_offset,
                                                itemsPerPage);
        } else {
            urlRecords = sdao.getSeedsForStatus(Status.fromOrdinal(status), first_item_offset, itemsPerPage);
        }
        return urlRecords;
    }
    
    private int getStatus(List<Integer> numerics, SeedsRequest seedsRequest) {
        int status = 0; //Default state shown: Status.NEW
        if (numerics.size() == 1) {
            status = numerics.get(0);
        }
        if (seedsRequest.isShowSeedsFromDomainWithStateRequest()) {
            status = seedsRequest.getCurrentState();
        }
        return status;
    }
    
    private int getItemsPerPage(HttpServletRequest req) {
        String itemsperpageStr = req.getParameter("itemsperpage");
        int itemsPerPage = 25;
        if (itemsperpageStr != null && itemsperpageStr.length() > 0) {
            try {
                itemsPerPage = Integer.parseInt(itemsperpageStr);
            } catch (NumberFormatException e) {
                logger.warn("The given value of 'itemsperpage': '" + itemsperpageStr
                            + "' is not a valid integer!. Using the default: 25");
                itemsPerPage = 25;
                itemsperpageStr = "25";
            }
        }
        if (itemsPerPage < 1) {
            int defaultItemsPerPage = environment.getDefaultItemsPerPage();
            logger.warn("Got negative itemsPerPage '" + itemsPerPage + "'. Changing it to itemsPerPage="
                        + defaultItemsPerPage);
            itemsPerPage = defaultItemsPerPage;
        }
        return itemsPerPage;
    }
    
    private long getPage(HttpServletRequest req, long pages) {
        //Paging parsing
        String pageStr = req.getParameter("page");
        long page = 1;
        if (pageStr != null && pageStr.length() > 0) {
            try {
                page = Integer.parseInt(pageStr);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        // Implementing paging with cassandra
        // https://datastax.github.io/java-driver/manual/paging/
        if (page < 1) {
            logger.warn("Got negative pagenr '" + page + "'. Changing it to page=1");
            page = 1;
            
        }
        if (page > pages) {
            logger.warn("Asked for page " + page + ", but we only have " + pages + ". Set page to maxpage");
            page = pages;
        }
        return page;
    }
    
    private long getSeedsCount(SeedsRequest seedsRequest, int status, SeedsDAO sdao) throws DaoException {
        long seedsCount;
        if (seedsRequest.isShowSeedsFromDomainRequest()) {
            seedsCount = sdao.getSeedsCount(seedsRequest.getDomain());
        } else if (seedsRequest.isShowSeedsFromDomainWithStateRequest()) {
            seedsCount = sdao.getSeedsCount(seedsRequest.getDomain(), Status.fromOrdinal(status));
        } else {
            seedsCount = sdao.getSeedsCount(Status.fromOrdinal(status));
        }
        return seedsCount;
    }
    
    /**
     * Truncate a string to max-length plus " .." if string exceeds maxlength.
     *
     * @param orgString A string
     * @param maxLength The max length the string must fill
     * @return the original String if the string is less than maxLength long,
     * otherwise the substring of length maxLength of the original
     * String followed by " .."
     */
    private String makeEllipsis(final String orgString, final int maxLength) {
        String resultString = orgString;
        if (orgString.length() > maxLength) {
            resultString = orgString.substring(0, maxLength - 1) + " ..";
        }
        return resultString;
    }
    
    public static StringBuilder buildStatemenu(int status,
                                               Cache cache) throws Exception {
        /*
         * State menu.
         */
        StringBuilder statemenuSb = new StringBuilder();
    
        List<MenuItem> menuStatesArr = makemenuArrayCache(cache);
        
        String heading = "N/A";
        
        for (MenuItem item : menuStatesArr) {
            // javascript:switchToState(0)
            statemenuSb.append("<li id=\"state_");
            statemenuSb.append(item.getOrdinalState());
            statemenuSb.append("\"");
            if (status == item.getOrdinalState()) {
                heading = item.getLongHeaderDescription();
                statemenuSb.append(" class=\"active\"");
            }
            statemenuSb.append("><a href=\"");
            statemenuSb.append(Servlet.environment.getSeedsPath());
            statemenuSb.append(item.getOrdinalState());
            statemenuSb.append("/\">");
            statemenuSb.append(item.getShortHeaderDescription());
            statemenuSb.append(" (");
            statemenuSb.append(item.getCount());
            statemenuSb.append(")</a></li>");
        }
       
        return statemenuSb;
    }
    
    // make the statemenu using cached values
    private static List<MenuItem> makemenuArrayCache(Cache cache) throws Exception {
        List<MenuItem> result = new ArrayList<MenuItem>();
        I18n i18n = new I18n(dk.kb.webdanica.core.Constants.WEBDANICA_TRANSLATION_BUNDLE);
        Locale locDa = new Locale("da");
        for (int i = 0; i <= Status.getMaxValidOrdinal(); i++) {
            if (!Status.ignoredState(i)) {
                Long count = cache.getSeedStatusCountsMap().get(i);
                result.add(new MenuItem(i, count, locDa, i18n));
            }
        }
        return result;
    }
    
}
