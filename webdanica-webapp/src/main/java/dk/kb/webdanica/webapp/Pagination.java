/*
 * Created on 07/05/2013
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

package dk.kb.webdanica.webapp;

/**
 * Builds a pagination HTML text block using twitter-bootstrap styles.
 *
 * <div class="pagination pull-right">
 *   <ul>
 *     <li class="disabled"><span>Forrige</span></li>
 *     <li class="active"><span>1</span></li>
 *     <li><a href="#">2</a></li>
 *     <li><a href="#">3</a></li>
 *     <li class="disabled"><span>...</span></li>
 *     <li><a href="#">8</a></li>
 *     <li><a href="#">9</a></li>
 *     <li><a href="#">10</a></li>
 *     <li><a href="#">NÃ¦ste</a></li>
 *   </ul>
 * </div>
 */
public class Pagination {

    /**
     * Calculate the total number of pages.
     * @param items total number of items
     * @param itemsPerPage items displayed per page
     * @return the total number of pages
     */
    public static long getPages(long items, int itemsPerPage) {
        long pages = (items + itemsPerPage - 1) / itemsPerPage;
        if (pages == 0) {
            pages = 1;
        }
        return pages;
    }

    /**
     * Builds a pagination HTML text block.
     * 
     * @param page current page
     * @param itemsPerPage items displayed per page
     * @param pages total number of pages
     * @return HTML text block
     */
    public static String getPagination(long page, int itemsPerPage, long pages, boolean bShowAll) {
        if (page < 1) {
            page = 1;
        }
        if (pages == 0) {
            pages = 1;
        }
        if (page > pages) {
            page = pages;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"pagination pull-right\">\n");
        sb.append("<ul>\n");
        // Previous
        if (page > 1) {
            sb.append("<li><a href=\"?page=" + (page - 1) + "&itemsperpage="
                    + itemsPerPage + "\">Forrige</a></li>");
        } else {
            sb.append("<li class=\"disabled\"><span>Forrige</span></li>");
        }
        // First.
        if (page == 1) {
            sb.append("<li class=\"active\"><span>" + 1 + "</span></li>");
        } else {
            sb.append("<li><a href=\"?page=" + 1 + "&itemsperpage="
                    + itemsPerPage + "\">" + 1 + "</a></li>");
        }
        // List.
        long tmpPage = page - 3;
        if (tmpPage > pages - 7) {
            tmpPage = pages - 7;
        }
        if (tmpPage > 2) {
            sb.append("<li class=\"disabled\"><span>...</span></li>");
        } else {
            tmpPage = 2;
        }
        int show = 8;
        while (show > 1 && tmpPage <= pages) {
            if (tmpPage == page) {
                sb.append("<li class=\"active\"><span>" + tmpPage
                        + "</span></li>");
            } else {
                sb.append("<li><a href=\"?page=" + tmpPage + "&itemsperpage="
                        + itemsPerPage + "\">" + tmpPage + "</a></li>");
            }
            --show;
            tmpPage++;
        }
        // Last
        if (tmpPage <= pages) {
            if (tmpPage < pages) {
                sb.append("<li class=\"disabled\"><span>...</span></li>");
            }
            if (tmpPage == page) {
                sb.append("<li class=\"active\"><span>" + pages
                        + "</span></li>");
            } else {
                sb.append("<li><a href=\"?page=" + pages + "&itemsperpage="
                        + itemsPerPage + "\">" + pages + "</a></li>");
            }
        }
        // Next.
        if (page < pages) {
            sb.append("<li><a href=\"?page=" + (page + 1) + "&itemsperpage="
                    + itemsPerPage + "\">Næste</a></li>");
        } else {
            sb.append("<li class=\"disabled\"><span>Næste</span></li>");
        }
        // Items per page.
        sb.append("<li>");
        String[][] options = new String[][] {{"10", "10"}, {"25", "25"}, {"50", "50"}, {"100", "100"}, {"all", "Vis alle"}};
        int selected;
        if (bShowAll) {
        	selected = options.length - 1;
        } else {
        	switch (itemsPerPage) {
        	case 10:
        		selected = 0;
        		break;
        	default:
        	case 25:
        		selected = 1;
        		break;
        	case 50:
        		selected = 2;
        		break;
        	case 100:
        		selected = 3;
        		break;
        	}
        }
        //sb.append("<select name=\"organization\" class=\"input-mini\" onchange=\"submitForm('myform');\">");
        sb.append("<select name=\"itemsperpage\" class=\"input-mini\" onchange=\"this.form.submit();\">");
        for (int i=0; i<options.length; ++i) {
        	sb.append("<option value=\"");
        	sb.append(options[i][0]);
        	sb.append("\"");
        	if (i == selected) {
        		sb.append(" selected=\"1\"");
        	}
        	sb.append(">");
        	sb.append(options[i][1]);
        	sb.append("</option>");
        }
        sb.append("</select>");
        sb.append("</li>");
        sb.append("</ul>\n");
        sb.append("</div>\n");
        return sb.toString();
    }

}
