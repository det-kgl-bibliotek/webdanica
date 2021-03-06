package dk.kb.webdanica.webapp.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import dk.kb.webdanica.core.Constants;
import org.slf4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.html.HtmlEntity;
import com.antiaction.common.templateengine.Template;
import com.antiaction.common.templateengine.TemplateParts;
import com.antiaction.common.templateengine.TemplatePlaceBase;
import com.antiaction.common.templateengine.TemplatePlaceHolder;

import dk.kb.webdanica.core.datamodel.Domain;
import dk.kb.webdanica.core.datamodel.Seed;
import dk.kb.webdanica.core.datamodel.criteria.CriteriaUtils;
import dk.kb.webdanica.core.datamodel.dao.DAOFactory;
import dk.kb.webdanica.core.datamodel.dao.DaoException;
import dk.kb.webdanica.core.datamodel.dao.DomainsDAO;
import dk.kb.webdanica.webapp.Environment;
import dk.kb.webdanica.webapp.Navbar;
import dk.kb.webdanica.webapp.Servlet;
import dk.kb.webdanica.webapp.User;
import org.slf4j.LoggerFactory;

public class DomainResource implements ResourceAbstract {

	    private static final Logger logger = LoggerFactory.getLogger(DomainResource.class);

	    private Environment environment;

	    protected int R_DOMAINS_LIST = -1;
	    
	    protected int R_DOMAIN_SHOW = -1;
	    
	    protected int R_DOMAIN_SEEDS_SHOW = -1;
	    
	    private String TLD_LIST_TEMPLATE = "tld_list.html";
	    
		private String DOMAIN_SHOW_TEMPLATE = "domain_show.html";
		
		private String DOMAIN_LIST_TEMPLATE = "domain_list.html";
		
		private String SEEDS_LIST_TEMPLATE = "domain_seeds_list.html";

		private DAOFactory daofactory;

		public static final String DOMAIN_LIST_PATH = "/domains/";

		public static final String DOMAIN_PATH = "/domain/";
		
		// usage: /domainseeds/$domain/ or /domainseeds/$domain/$danicastatus/
		public static final String DOMAIN_SEEDS_PATH = "/domainseeds/"; 
		
	    @Override
	    public void resources_init(Environment environment) {
	        this.environment = environment;
	        this.daofactory = environment.getConfig().getDAOFactory();
	    }

	    @Override
	    public void resources_add(ResourceManagerAbstract resourceManager) {
	        R_DOMAINS_LIST = resourceManager.resource_add(this, DOMAIN_LIST_PATH, 
	        		   		environment.getResourcesMap().getResourceByPath(DOMAIN_LIST_PATH).isSecure());
	        R_DOMAIN_SHOW = resourceManager.resource_add(this, DOMAIN_PATH, 
    		   		environment.getResourcesMap().getResourceByPath(DOMAIN_PATH).isSecure());
	        R_DOMAIN_SEEDS_SHOW = resourceManager.resource_add(this, DOMAIN_SEEDS_PATH, 
                    environment.getResourcesMap().getResourceByPath(DOMAIN_SEEDS_PATH).isSecure());
	    }

	    @Override
	    public void resource_service(ServletContext servletContext, User dab_user,
	    		HttpServletRequest req, HttpServletResponse resp,
	    		int resource_id, List<Integer> numerics, String pathInfo) throws IOException {
	        
	        if (resource_id == R_DOMAINS_LIST) {
	            executeDomainsListRequest(pathInfo, dab_user, req, resp);
	        } else if (resource_id == R_DOMAIN_SHOW) {
	            executeDomainShowRequest(pathInfo, dab_user, req, resp);
	        } else if (resource_id == R_DOMAIN_SEEDS_SHOW) {
                executeDomainSeedsShowRequest(pathInfo, dab_user, req, resp);
	        } else {
	        	String error = "No resource matching pathinfo'" +  pathInfo + "' in resource '" +  this.getClass().getName() + "'";
	        	CommonResource.show_error(error, resp, environment);
	        	return;
	        }
	    }
	    
	    private void executeDomainSeedsShowRequest(String pathInfo,
	            User dab_user, HttpServletRequest req, HttpServletResponse resp) throws IOException {
	        DomainSeedsRequest dsr = DomainSeedsRequest.getDomainSeedsRequest(pathInfo);
	        if (dsr.getValid()) {
	            String domain = dsr.getDomain();
	            int maxfetched = environment.getConfig().getMaxUrlsToFetch();
	            Iterator<Seed> seeds;
                try {
                    seeds = daofactory.getSeedsDAO().getSeedsForDomain(domain, dsr.getStatus(), dsr.getDanicaStatus(), 0, maxfetched);
                    domainSeedsShow(pathInfo, dab_user, req, resp, seeds, dsr);
                } catch (DaoException e) {
                    CommonResource.show_error("Error showing seeds for domain = " +  dsr.getDomain() + ", danicastate=" +  dsr.getDanicaStatus(), resp, environment);
                    return;
                }
	        } else {
	            CommonResource.show_error("Invalid request, domain = " +  dsr.getDomain() + ", danicastate=" +  dsr.getDanicaStatus(), resp, environment);
	            return;
	        }
	    }
	    
	    /**
	     * This method  handles the requests
	     * /domainseeds/$domain/ or /domainseeds/$domain/$danicastatus/
	     * 
	     * @throws IOException
	     */
	    private void domainSeedsShow(String pathInfo, User dab_user,
	            HttpServletRequest req, HttpServletResponse resp, Iterator<Seed> seeds, DomainSeedsRequest dsr) throws IOException {
	        ServletOutputStream out = resp.getOutputStream();
	        resp.setContentType("text/html; charset=utf-8");
	        Caching.caching_disable_headers(resp);
	        String templateName = SEEDS_LIST_TEMPLATE;
	        Template template = environment.getTemplateMaster().getTemplate(templateName);

	        TemplatePlaceHolder titlePlace = TemplatePlaceBase.getTemplatePlaceHolder("title");
	        TemplatePlaceHolder appnamePlace = TemplatePlaceBase.getTemplatePlaceHolder("appname");
	        TemplatePlaceHolder navbarPlace = TemplatePlaceBase.getTemplatePlaceHolder("navbar");
	        TemplatePlaceHolder userPlace = TemplatePlaceBase.getTemplatePlaceHolder("user");
	        TemplatePlaceHolder menuPlace = TemplatePlaceBase.getTemplatePlaceHolder("menu");
	        TemplatePlaceHolder headingPlace = TemplatePlaceBase.getTemplatePlaceHolder("heading");
	        TemplatePlaceHolder contentPlace = TemplatePlaceBase.getTemplatePlaceHolder("content");
	        TemplatePlaceHolder usersPlace = TemplatePlaceBase.getTemplatePlaceHolder("users");
	        TemplatePlaceHolder backPlace = TemplatePlaceBase.getTemplatePlaceHolder("back");

	        List<TemplatePlaceBase> placeHolders = new ArrayList<TemplatePlaceBase>();
	        placeHolders.add(titlePlace);
	        placeHolders.add(appnamePlace);
	        placeHolders.add(navbarPlace);
	        placeHolders.add(userPlace);
	        placeHolders.add(menuPlace);
	        placeHolders.add(headingPlace);
	        placeHolders.add(contentPlace);
	        placeHolders.add(usersPlace);

	        TemplateParts templateParts = template.filterTemplate(placeHolders, resp.getCharacterEncoding());

	        // Primary textarea
	        StringBuffer sb = new StringBuffer();


	        long seedCount = 0;
			while (seeds.hasNext()) {
				Seed s = seeds.next();
	            seedCount++;
	            String showDetails = "<A href=\"" + Servlet.environment.getSeedPath() + "/" + CriteriaUtils.toBase64(s.getUrl()) + "\"> Show details </A>";
	            sb.append("<tr>");
	            sb.append("<td>");    
	            sb.append("<a href=\"");
	            sb.append(s.getUrl());
	            sb.append("\">");
	            sb.append(s.getUrl());
	            sb.append("</a>");
	            sb.append("( " + showDetails + ")");
	            sb.append("</td>");
	            sb.append("<td>");
	            sb.append(s.getDomain());
	            sb.append("</td>");
	            sb.append("<td>");
	            sb.append(s.getDanicaStatus());
	            sb.append("</td>");
	            sb.append("<td>");
	            sb.append(s.getStatus()); 
	            sb.append("</td>");
	            sb.append("</tr>\n");
	        }
	        
	        setDomainsNavigationPlaces(titlePlace, appnamePlace, navbarPlace, userPlace, backPlace, dab_user, templateName);
	        
	        if (usersPlace != null) {
                usersPlace.setText(sb.toString());
            }
		
	        
			String all = " ";
			if (dsr.getDanicaStatus() == null && dsr.getStatus() == null) {
				all= "all ";
			}
			String heading = "Showing " + all + seedCount + " seeds from domain '" + dsr.getDomain() + "'";
			if (dsr.getStatus() != null) {
				heading += ", with state '" + dsr.getStatus() + "'";
			}
			if (dsr.getDanicaStatus() != null) {
				heading += ", with danica-state '" + dsr.getDanicaStatus() + "'";
			}
		
			if (headingPlace != null) {
				headingPlace.setText(heading);
			} else {
				logger.warn("No heading´ placeholder found in template '" + templateName + "'" );
			}
	        
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
        

        /**
	     * Show the seeds from a given domain with possibly a given DanicaStatus and State 
	     * @param pathInfo
	     * @param dab_user
	     * @param req
	     * @param resp
	     * @throws IOException
	     */
        private void executeDomainShowRequest(String pathInfo, User dab_user, HttpServletRequest req, HttpServletResponse resp) throws IOException{
	        DomainRequest dr = DomainRequest.getDomainRequest(pathInfo);
            if (dr.getValid()) {
                Domain domain = null;
                try {
                    domain = getDomainFromPathinfo(pathInfo, DOMAIN_PATH);
                } catch (DaoException e)  {
                    String error = "Impossible to retrieve domain from database in resource '" +  this.getClass().getName() + "': " +  ExceptionUtils.getFullStackTrace(e);
                    CommonResource.show_error(error, resp, environment);
                    return;
                }
                if (domain != null) {
                    Long seedscount = 0L;
                    try {
                        seedscount = daofactory.getSeedsDAO().getSeedsCount(domain.getDomain());
                    } catch (DaoException e) {
                        String error = "Impossible to extract seedscount for domain '" +  domain.getDomain() + "': " + ExceptionUtils.getFullStackTrace(e);
                        CommonResource.show_error(error, resp, environment);
                        return;
                    }
                    domain_show(dab_user, req, resp, domain, seedscount);
                } else {
                    String error = "Impossible to find valid domain from pathinfo'" +  pathInfo + "' in resource '" +  this.getClass().getName() + "'";
                    CommonResource.show_error(error, resp, environment);
                    return;
                } 
            } else {
                String error = "Invalid request: " + pathInfo;
                CommonResource.show_error(error, resp, environment);
                return;
            }
            
        }

        private void executeDomainsListRequest(String pathInfo, User dab_user, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            DomainsRequest dr = DomainsRequest.getDomainsRequest(pathInfo);
            if (dr.getValid()) {
                domains_list(dab_user, req, resp, dr);
            } else {
                String error = "Invalid request: " + pathInfo;
                CommonResource.show_error(error, resp, environment);
                return;
            }
            
        }

        private Domain getDomainFromPathinfo(String pathInfo, String domainPath) throws DaoException {
	        Domain domain = null;
	    	DomainsDAO ddao = daofactory.getDomainsDAO();
	        String[] pathParts = pathInfo.split(domainPath);
	        if (pathParts.length == 2) {
	            domain = ddao.getDomain(pathParts[1].substring(0, pathParts[1].length()-1));
	        } 
	        return domain;
        }

	    /**
	     * Show the following information about any domain in the domains table
	     * using the template webdanica-webapp/src/main/webapp/domain_show.html.
	     * 
	     * Danica Status: <placeholder id="danicaState" /><br>
           Danica Status Reason: <placeholder id="danicaStateReason" /><br>
           SeedCount: <placeholder id="seedCount" /><br>
           DanicaParts: <placeholder id="danicaParts" /><br>
           UpdatedTime: <placeholder id="domainUpdatedTime" /></br>
           
           Notes: <placeholder id="domainNotes" /><br>
	     * 
	     */
		private void domain_show(User dab_user, HttpServletRequest req,
                HttpServletResponse resp, Domain d, Long seedsCount) throws IOException {
	    	ServletOutputStream out = resp.getOutputStream();
	        resp.setContentType("text/html; charset=utf-8");
	        // TODO error text
	        String errorStr = null;
	        String successStr = null;
	        Caching.caching_disable_headers(resp);
	        String templateName = DOMAIN_SHOW_TEMPLATE;
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
	        
	        TemplatePlaceHolder danicaStatePlace = TemplatePlaceBase.getTemplatePlaceHolder("danicaState");
	        TemplatePlaceHolder danicaStateReasonPlace = TemplatePlaceBase.getTemplatePlaceHolder("danicaStateReason");
	        // links here to the seeds in the database from that domain
	        TemplatePlaceHolder seedCountPlace = TemplatePlaceBase.getTemplatePlaceHolder("seedCount");
	        
	        TemplatePlaceHolder danicaPartsPlace = TemplatePlaceBase.getTemplatePlaceHolder("danicaParts");
	        TemplatePlaceHolder domainNotesPlace = TemplatePlaceBase.getTemplatePlaceHolder("domainNotes");
	        TemplatePlaceHolder domainUpdatedTimePlace = TemplatePlaceBase.getTemplatePlaceHolder("domainUpdatedTime");
	        
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
	        placeHolders.add(danicaStatePlace);
	        placeHolders.add(seedCountPlace);
	        placeHolders.add(danicaPartsPlace);
	        placeHolders.add(domainNotesPlace);
	        placeHolders.add(domainUpdatedTimePlace);
	            
	        TemplateParts templateParts = template.filterTemplate(placeHolders, resp.getCharacterEncoding());
	        
	        /*
	         * Heading.
	         */
	        String heading = "Information about domain '" + d.getDomain() + "' from tld " +  d.getTld() + " :";  
	        
	        /*
	         * Places.
	         */
	        setDomainsNavigationPlaces(titlePlace, appnamePlace, navbarPlace, userPlace, backPlace, dab_user, templateName);
	       
	        if (headingPlace != null) {
	            headingPlace.setText(heading);
	        } else {
	        	logger.warn("No heading´ placeholder found in template '" + templateName + "'" );
	        }

	        ResourceUtils.insertText(danicaStatePlace, "danicaState",  d.getDanicaStatus().toString(), DOMAIN_SHOW_TEMPLATE, logger);
	        String reason = d.getDanicaStatusReason();
	        if (reason.isEmpty()) {
	            reason = "None";
	        }
	        ResourceUtils.insertText(danicaStateReasonPlace, "danicaStateReason",  reason, DOMAIN_SHOW_TEMPLATE, logger);
	        String seedCountText = "<A href=\"" + environment.getDomainSeedsPath() + d.getDomain() + "/\">" + seedsCount + "</A>"; 
	        ResourceUtils.insertText(seedCountPlace, "seedCount", seedCountText, DOMAIN_SHOW_TEMPLATE, logger);
	        
	        String danicaParts = "N/A";
	        if (d.getDanicaParts() != null) {
	            danicaParts = StringUtils.join(d.getDanicaParts(), ",");
	        }
	        ResourceUtils.insertText(danicaPartsPlace, "danicaParts",  danicaParts, DOMAIN_SHOW_TEMPLATE, logger);
	        String notes = "";
	        if (d.getNotes() != null) {
	            notes = d.getNotes();
	        }
	        ResourceUtils.insertText(domainNotesPlace, "domainNotes", notes , DOMAIN_SHOW_TEMPLATE, logger);
	        
	        Long updatedTime = d.getUpdatedTime();
	        String updatedTimeString = "No updatedTime value stored in database"; 
	        if (updatedTime != null) {
	            updatedTimeString = new Date(updatedTime).toString();
	        }
	       
	        ResourceUtils.insertText(domainUpdatedTimePlace, "domainUpdatedTime", updatedTimeString, DOMAIN_SHOW_TEMPLATE, logger);
	        
	        /*
	        StringBuilder sb = new StringBuilder();
	        
	        sb.append("<pre>\r\n");
	    	for (String listElement: blacklist) {
	    		sb.append(listElement);
	    		sb.append("\r\n");
	    	}	
	    	
	    	ResourceUtils.insertText(contentPlace, "content",  sb.toString(), BLACKLIST_SHOW_TEMPLATE, logger);
	        */
	        
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

		/**
	     * List all the domains matching the given DomainsRequest
	     * using templates 
	     *  webdanica-webapp/src/main/webapp/tld_list.html
	     *  webdanica-webapp/src/main/webapp/domain_list.html
	     * 
	     * The first template is used for presenting all known tlds 
	     * The second template is used for presenting all known domains for a specific tld
	     */
		public void domains_list(User dab_user, HttpServletRequest req,
	            HttpServletResponse resp, DomainsRequest dr) throws IOException {
	        ServletOutputStream out = resp.getOutputStream();
	        resp.setContentType("text/html; charset=utf-8");

	        Caching.caching_disable_headers(resp);
	        String templatename = TLD_LIST_TEMPLATE;
	        boolean showDomainsForTld = dr.getTld() != null;
	        String tld = dr.getTld();
	        if (showDomainsForTld) {
	            templatename = DOMAIN_LIST_TEMPLATE;
	        }
	        Template template = environment.getTemplateMaster().getTemplate(templatename);

	        TemplatePlaceHolder titlePlace = TemplatePlaceBase.getTemplatePlaceHolder("title");
	        TemplatePlaceHolder appnamePlace = TemplatePlaceBase.getTemplatePlaceHolder("appname");
	        TemplatePlaceHolder navbarPlace = TemplatePlaceBase.getTemplatePlaceHolder("navbar");
	        TemplatePlaceHolder userPlace = TemplatePlaceBase.getTemplatePlaceHolder("user");
	        TemplatePlaceHolder menuPlace = TemplatePlaceBase.getTemplatePlaceHolder("menu");
	        TemplatePlaceHolder headingPlace = TemplatePlaceBase.getTemplatePlaceHolder("heading");
	        TemplatePlaceHolder contentPlace = TemplatePlaceBase.getTemplatePlaceHolder("content");
	        TemplatePlaceHolder usersPlace = TemplatePlaceBase.getTemplatePlaceHolder("users");
	        TemplatePlaceHolder backPlace = TemplatePlaceBase.getTemplatePlaceHolder("back");

	        List<TemplatePlaceBase> placeHolders = new ArrayList<TemplatePlaceBase>();
	        placeHolders.add(titlePlace);
	        placeHolders.add(appnamePlace);
	        placeHolders.add(navbarPlace);
	        placeHolders.add(userPlace);
	        placeHolders.add(menuPlace);
	        placeHolders.add(headingPlace);
	        placeHolders.add(contentPlace);
	        placeHolders.add(usersPlace);
	        placeHolders.add(backPlace);

	        TemplateParts templateParts = template.filterTemplate(placeHolders, resp.getCharacterEncoding());
	        
	        // Primary textarea
	        StringBuffer sb = new StringBuffer();
	        
	        Iterator<String> tldList;
	        String header = "";
	        if (!showDomainsForTld) {
	            try {
	                tldList = daofactory.getDomainsDAO().getTlds();
	                header = "Listing all top level domains:";
	            } catch (Exception e) {
	                String errMsg = "System-error: Exception thrown";
	                logger.warn( errMsg, e);
	                CommonResource.show_error(errMsg, resp, environment);
	                return;
	            }
				while (tldList.hasNext()) {
					String t = tldList.next();
                    sb.append("<tr>");
                    sb.append("<td>");    
                    sb.append("<a href=\"");
                    sb.append(Servlet.environment.getDomainsPath());
                    sb.append(t);
                    sb.append("/\">");
                    sb.append(t);
                    sb.append("</a>");
                    sb.append("</td>");
                    sb.append("<td>");
                    sb.append("&nbsp;");
                    sb.append("</td>");
                    sb.append("<td>");
                    sb.append("&nbsp;");
                    sb.append("</td>");
                    sb.append("</tr>\n");
                }
	        } else { 
	            Long domainsCount = 0L;
	            Iterator<Domain> domains;
	            try {
	                domainsCount = daofactory.getDomainsDAO().getDomainsCount(null, tld);
                    domains = daofactory.getDomainsDAO().getDomains(null, tld, environment.getConfig().getMaxUrlsToFetch());
                } catch (DaoException e) {
                    String errMsg = "System-error: Exception thrown: " + ExceptionUtils.getFullStackTrace(e);
                    logger.warn( errMsg, e);
                    CommonResource.show_error(errMsg, resp, environment);
                    return;
                   
                }
	            header = "Listing all " + domainsCount + " domains in top level domain '" +  tld + "':";
	            String domainLinkPrefix = "<A href=\"" + environment.getDomainSeedsPath() + "/";
				while (domains.hasNext()) {
					Domain d = domains.next();
	                Long seedscount = 0L;
                    try {
                        seedscount = daofactory.getSeedsDAO().getSeedsCount(d.getDomain());
                    } catch (DaoException e) {
                        logger.warn( "Error while retrieving seedscount for domain '" + d.getDomain() + "':", e);
                    }
	                sb.append("<tr>");
	                sb.append("<td>");    
	                sb.append("<a href=\"");
	                sb.append(Servlet.environment.getDomainPath());
	                sb.append(d.getDomain());
	                sb.append("/\">");
	                sb.append(d.getDomain());
	                sb.append("</a>");
	                sb.append("</td>");
	                sb.append("<td>");
	                sb.append(d.getTld());
	                sb.append("</td>");
	                sb.append("<td>");
	                sb.append(d.getDanicaStatus());
	                sb.append("</td>");
	                sb.append("<td>");
	                sb.append(domainLinkPrefix + d.getDomain() + "/\">" + seedscount + "</A>"); 
	                sb.append("</td>");
	                sb.append("</tr>\n");
	            }
	        }
	        
            
	        /*
	         * Menu.
	         */

	        StringBuilder menuSb = new StringBuilder();

	        menuSb.append("<li id=\"state_0\"");
	        menuSb.append(" class=\"active\"");
	        menuSb.append("><a href=\"");
	        menuSb.append(Servlet.environment.getDomainsPath());
	        menuSb.append("\">");
	        menuSb.append(header);
	        menuSb.append("</a></li>\n");
	        
	        /*
	         * Heading.
	         */

	        String heading = header;

	        /*
	         * Places.
	         */
	        setDomainsNavigationPlaces(titlePlace, appnamePlace, navbarPlace, userPlace, backPlace, dab_user, templatename); 
	        
	        if (menuPlace != null) {
	            menuPlace.setText(menuSb.toString());
	        }

	        if (headingPlace != null) {
	            headingPlace.setText(heading);
	        }

	        /*
	         * if ( contentPlace != null ) { contentPlace.setText( sb.toString() );
	         * }
	         */

	        if (usersPlace != null) {
	            usersPlace.setText(sb.toString());
	        }
	        
	        // Write out the page requested by the client browser
	        try {
	            for (int i = 0; i < templateParts.parts.size(); ++i) {
	                out.write(templateParts.parts.get(i).getBytes());
	            }
	            out.flush();
	            out.close();
	        } catch (IOException e) {
	        	
	        }
	    }
		
		private void setDomainsNavigationPlaces(
		        TemplatePlaceHolder titlePlace,
		        TemplatePlaceHolder appnamePlace,
		        TemplatePlaceHolder navbarPlace,
		        TemplatePlaceHolder userPlace, 
		        TemplatePlaceHolder backPlace,
		        User dab_user,
		        String templateName) {
		    if (titlePlace != null) {
		        titlePlace.setText(HtmlEntity.encodeHtmlEntities(Constants.WEBAPP_NAME).toString());
		    }

		    if (appnamePlace != null) {
		        appnamePlace.setText(HtmlEntity.encodeHtmlEntities(Constants.WEBAPP_NAME + Constants.SPACE + environment.getVersion()).toString());
		    }

		    if (navbarPlace != null) {
		        navbarPlace.setText(Navbar.getNavbar(Navbar.N_DOMAINS));
		    }

		    if (userPlace != null) {
		        userPlace.setText(Navbar.getUserHref(dab_user));
		    } 

		    if (backPlace != null) {
		        backPlace.setText("<a href=\"" 
		                + Servlet.environment.getDomainsPath() 
		                + "\" class=\"btn btn-primary\"><i class=\"icon-white icon-list\"></i> Tilbage til oversigten</a>");
		    } else {
		        logger.warn("No back´placeholder found in template '" + templateName + "'" );
		    }
		}
		
		
	}
