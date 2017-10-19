/*
 * Created on 12/04/2013
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

package dk.kb.webdanica.webapp;

import java.util.Map;
import java.util.TreeMap;

public class Navbar {

    public static final int N_INDEX = 0;

    public static final int N_SEEDS = 1;

    public static final int N_BLACKLISTS = 2;

    public static final int N_HARVESTS = 3;

    public static final int N_CRITERIA_RESULTS = 4;

    public static final int N_DOMAINS = 5;

    public static final int N_INGESTLOGS = 6;

    public static final int N_STATUS = 7;

    private static Map<Integer, String> navbarMap = new TreeMap<Integer, String>();

    private static String[][] items  = new String[][] {
    			//{ Servlet.environment.contextPath + "/ingest/", "Ingest" },
                { Servlet.environment.getContextPath() + "/seeds/", "Seeds" }, 
                { Servlet.environment.getContextPath() + "/blacklists/", "Blacklists" },
                { Servlet.environment.getContextPath() + "/harvests/", "Høstninger" },
                { Servlet.environment.getContextPath() + "/criteriaresults/", "Kriterie resultater" },
                { Servlet.environment.getContextPath() + "/domains/", "Domæner" },
                { Servlet.environment.getContextPath() + "/ingestlogs/", "Ingestlogs" },
                { Servlet.environment.getContextPath() + "/status/", "Status" }
    };

    public static synchronized String getNavbar(int menu) {
    	String str = navbarMap.get(menu);
        if (str == null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < items.length; ++i) {
                sb.append("<li");
                if (i == (menu - 1)) {
                    sb.append(" class=\"active\"");
                }
                sb.append(">");
                sb.append("<a href=\"");
                sb.append(items[i][0]);
                sb.append("\">");
                sb.append(items[i][1]);
                sb.append("</a></li>");
                sb.append("\n");
            }
            str = sb.toString();
            navbarMap.put(menu, str);
        }
        return str;
    }

    public static String getUserHref(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("<a href=\"");
        sb.append(Servlet.environment.getContextPath());
        sb.append("/user/");
        sb.append(user== null?"NO-ID":user.id); //FIXME a hack to avoid a NPE
        sb.append("/\" class=\"navbar-link\">");
        sb.append(user== null?"NO-NAME":user.username);//FIXME a hack to avoid a NPE
        sb.append("</a>");
        return sb.toString();
    }

}
