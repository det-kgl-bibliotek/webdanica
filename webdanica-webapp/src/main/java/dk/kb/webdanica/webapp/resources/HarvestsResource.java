package dk.kb.webdanica.webapp.resources;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.html.HtmlEntity;
import com.antiaction.common.templateengine.Template;
import com.antiaction.common.templateengine.TemplatePartBase;
import com.antiaction.common.templateengine.TemplateParts;
import com.antiaction.common.templateengine.TemplatePlaceBase;
import com.antiaction.common.templateengine.TemplatePlaceHolder;
import dk.kb.webdanica.core.Constants;
import dk.kb.webdanica.core.datamodel.dao.HarvestDAO;
import dk.kb.webdanica.core.interfaces.harvesting.SingleSeedHarvest;
import dk.kb.webdanica.webapp.Environment;
import dk.kb.webdanica.webapp.Navbar;
import dk.kb.webdanica.webapp.Pagination;
import dk.kb.webdanica.webapp.Servlet;
import dk.kb.webdanica.webapp.User;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class HarvestsResource implements ResourceAbstract {
    
    private static final Logger logger = LoggerFactory.getLogger(HarvestsResource.class);
    
    private Environment environment;
    
    /*
    protected static final int[] USER_ADD_PERMISSIONS = {Permission.P_USER_ADMIN, Permission.P_USER_ADD};
    */
    protected int R_HARVEST_LIST = -1;
    
    //protected int R_BLACKLIST_ADD = -1;
    
    public static final String HARVESTS_PATH = "/harvests/";
    
    @Override
    public void resources_init(Environment environment) {
        this.environment = environment;
        
    }
    
    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_HARVEST_LIST = resourceManager.resource_add(this, HARVESTS_PATH,
                                                      environment.getResourcesMap()
                                                                 .getResourceByPath(HARVESTS_PATH)
                                                                 .isSecure());
    }
    
    @Override
    public void resource_service(ServletContext servletContext, User dab_user,
                                 HttpServletRequest req, HttpServletResponse resp,
                                 int resource_id, List<Integer> numerics, String pathInfo) throws IOException {
        
        if (resource_id == R_HARVEST_LIST) {
            harvests_list(dab_user, req, resp, pathInfo);
        }
    }
    
    public void harvests_list(User dab_user, HttpServletRequest req,
                              HttpServletResponse resp, String pathInfo) throws IOException {
        
        HarvestRequest hr = HarvestRequest.getRequest(HarvestsResource.HARVESTS_PATH, pathInfo);
        
        
        
        TemplatePlaceHolder titlePlace = TemplatePlaceBase.getTemplatePlaceHolder("title");
        TemplatePlaceHolder appnamePlace = TemplatePlaceBase.getTemplatePlaceHolder("appname");
        TemplatePlaceHolder navbarPlace = TemplatePlaceBase.getTemplatePlaceHolder("navbar");
        TemplatePlaceHolder userPlace = TemplatePlaceBase.getTemplatePlaceHolder("user");
        TemplatePlaceHolder menuPlace = TemplatePlaceBase.getTemplatePlaceHolder("menu");
        TemplatePlaceHolder headingPlace = TemplatePlaceBase.getTemplatePlaceHolder("heading");
        TemplatePlaceHolder contentPlace = TemplatePlaceBase.getTemplatePlaceHolder("content");
        TemplatePlaceHolder usersPlace = TemplatePlaceBase.getTemplatePlaceHolder("users");
        
        List<TemplatePlaceBase> placeHolders = new ArrayList<>();
        placeHolders.add(titlePlace);
        placeHolders.add(appnamePlace);
        placeHolders.add(navbarPlace);
        placeHolders.add(userPlace);
        placeHolders.add(menuPlace);
        placeHolders.add(headingPlace);
        placeHolders.add(contentPlace);
        placeHolders.add(usersPlace);
    
        Template template = environment.getTemplateMaster().getTemplate("harvests_list.html");
        //This magic must happen before you start to fill content into the placeholders, as below
        TemplateParts templateParts = template.filterTemplate(placeHolders, resp.getCharacterEncoding());
    
    /*
    
        String pageStr = req.getParameter("page");
        long page = 1;
        if (pageStr != null && pageStr.length() > 0) {
            try {
                page = Integer.parseInt(pageStr);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        String itemsperpageStr = req.getParameter("itemsperpage");
        boolean bShowAll = false;
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
    
        TemplatePlaceHolder paginationPlace = TemplatePlaceBase.getTemplatePlaceHolder("pagination");
        TemplatePlaceHolder pagination2Place = TemplatePlaceBase.getTemplatePlaceHolder("pagination2");
    
    
    
        // Implementing paging with cassandra
        // https://datastax.github.io/java-driver/manual/paging/
        if (page < 1) {
            logger.warn("Got negative pagenr '" + page + "'. Changing it to page=1");
            page = 1;
        
        }
        if (itemsPerPage < 1) {
            int defaultItemsPerPage = environment.getDefaultItemsPerPage();
            logger.warn("Got negative itemsPerPage '" + itemsPerPage + "'. Changing it to itemsPerPage="
                        + defaultItemsPerPage);
            itemsPerPage = defaultItemsPerPage;
        
        }
    
        long items = Math.min(maxUrlsToFetch, seedsCount);
        long pages = Pagination.getPages(items, itemsPerPage);
        if (page > pages) {
            logger.warn("Asked for page " + page + ", but we only have " + pages + ". Set page to maxpage");
            page = pages;
        }
        long fItem = (page - 1) * itemsPerPage;
        int show = itemsPerPage;
    
    
    
    
    
        if (paginationPlace != null) {
            paginationPlace.setText(Pagination.getPagination(page, itemsPerPage, pages, bShowAll));
        }
    
        if (pagination2Place != null) {
            pagination2Place.setText(Pagination.getPagination(page, itemsPerPage, pages, bShowAll));
        }
    */
    
        HarvestDAO harvestDAO = environment.getConfig().getDAOFactory().getHarvestDAO();
        Iterator<SingleSeedHarvest> harvestList;
        Long harvestCount;
        try {
            if (hr.viewAll()) {
                harvestList = harvestDAO.getAll();
                harvestCount = harvestDAO.getCount();
            } else {
                harvestList = harvestDAO.getAllWithSeedurl(hr.getSeedUrl());
                harvestCount = harvestDAO.getCountWithSeedurl(hr.getSeedUrl());
            }
        } catch (Exception e) {
            // Create error-page
            String errMsg = "Unexpected exception thrown:" + e;
            logger.warn(errMsg, e);
            CommonResource.show_error(errMsg, resp, environment);
            return;
        }
        
        
        titlePlace.setText(HtmlEntity.encodeHtmlEntities(Constants.WEBAPP_NAME).toString());
    
    
        appnamePlace.setText(HtmlEntity.encodeHtmlEntities(
                Constants.WEBAPP_NAME + Constants.SPACE + environment.getVersion()).toString());
    
    
        navbarPlace.setText(Navbar.getNavbar(Navbar.N_HARVESTS));
    
    
        userPlace.setText(Navbar.getUserHref(dab_user));
        
        StringBuilder sb = showHarvestList(harvestList);
        //logger.trace("This is the harvest list '{}'",sb.toString());
        usersPlace.setText(sb.toString());
        contentPlace.setText(sb.toString());
    
        StringBuilder menuSb = showMenu();
        menuPlace.setText(menuSb.toString());
    
        String heading = showHeading(hr, harvestCount);
        headingPlace.setText(heading);
    
        
        resp.setContentType("text/html; charset=utf-8");
        Caching.caching_disable_headers(resp);
        // Write out the page requested by the client browser
        try (ServletOutputStream out = resp.getOutputStream()) {
            for (int i = 0; i < templateParts.parts.size(); ++i) {
                TemplatePartBase templatePartBase = templateParts.parts.get(i);
                //logger.trace("Writing out '{}",templatePartBase.getText());
                out.write(templatePartBase.getBytes());
            }
            out.flush();
        } catch (IOException e) {
            logger.error("Caught Exception", e);
            throw new RuntimeException(e);
        }
    }
    
    @NotNull
    private String showHeading(HarvestRequest hr, Long harvestCount) {
        /*
         * Heading.
         */
        
        String heading = "Liste over alle ("+harvestCount+") høstninger i systemet";
        if (!hr.viewAll()) {
            heading = "Liste over høstninger i systemet af seedurl '" + hr.getSeedUrl() + "' ("+harvestCount+")";
        }
        return heading;
    }
    
    @NotNull
    private StringBuilder showMenu() {
        /*
         * Menu.
         */
        
        StringBuilder menuSb = new StringBuilder();
        
        menuSb.append("<li id=\"state_0\"");
        menuSb.append(" class=\"active\"");
        menuSb.append("><a href=\"");
        menuSb.append(Servlet.environment.getHarvestsPath());
        menuSb.append("\">");
        menuSb.append("Liste over harvests");
        menuSb.append("</a></li>\n");
        
         /*
	        if (dab_user.hasAnyPermission(USER_ADD_PERMISSIONS)) {
	            menuSb.append("<li id=\"state_1\"");
	            menuSb.append("><a href=\"");
	            menuSb.append(DABServlet.environment.usersPath);
	            menuSb.append("add/\">");
	            menuSb.append("Opret bruger");
	            menuSb.append("</a></li>\n");
	        }
*/
        return menuSb;
    }
    
    private StringBuilder showHarvestList(Iterator<SingleSeedHarvest> harvestList) {
        // Primary textarea
        StringBuilder sb = new StringBuilder();
    
        long count = 0;
        while (harvestList.hasNext()) {
            SingleSeedHarvest harvest = harvestList.next();
            count++;
            
            sb.append("<tr>");
            sb.append("<td>");
            sb.append("<a href=\"");
            sb.append(Servlet.environment.getHarvestPath());
            sb.append(harvest.getHarvestName());
            sb.append("/\">");
            sb.append(harvest.getHarvestName());
            sb.append("</a>");
            sb.append("</td>");
            sb.append("<td>");
            sb.append(harvest.getSeed());
            sb.append("</td>");
            sb.append("<td>");
            sb.append(new Date(harvest.getHarvestedTime()));
            sb.append("</td>");
            sb.append("</tr>\n");
        }
        return sb;
    }
}



