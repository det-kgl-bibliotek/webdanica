/*
 * Created on 26/03/2013
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

package dk.kb.webdanica.webapp.resources;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dk.kb.webdanica.webapp.Environment;
import dk.kb.webdanica.webapp.User;

public interface ResourceAbstract {
	/**
	 * 
	 * @param environment
	 */
    public void resources_init(Environment environment);
    /**
     * 
     * @param resourceManager
     */
    public void resources_add(ResourceManagerAbstract resourceManager);
   
    /**
     * 
     * @param servletContext
     * @param dab_user
     * @param req
     * @param resp
     * @param resource_id
     * @param numerics
     * @param pathInfo
     * @throws IOException
     */
    public void resource_service(ServletContext servletContext, User dab_user, HttpServletRequest req, HttpServletResponse resp, int resource_id, List<Integer> numerics, String pathInfo)
            throws IOException, SQLException;

}
