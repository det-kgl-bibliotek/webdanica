package dk.kb.webdanica.core.datamodel.criteria;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
    @deprecated Use the dk.netarkivet.common.utils.archive.ArchiveDateConverter from NetarchiveSuite 5.2.2 instead
*/
public class DateConverter {

/** ARC date format string as specified in the ARC documentation (14 digits). */
public static final String ARC_DATE_FORMAT = "yyyyMMddHHmmss";

/** WARC date format string as specified by the WARC ISO standard. */
public static final String WARC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

/** date format string used by Heritrix with 17 digits.  */
public static final String HERITRIX_DATE_FORMAT = "yyyyMMddHHmmssSSS";

/** ARC <code>DateFormat</code> as specified in the ARC documentation. */
private final DateFormat arcDateFormat;

/** WARC <code>DateFormat</code> as specified in the WARC ISO standard. */
private final DateFormat warcDateFormat;

/** <code>DateFormat</code> as used by Heritrix. */
private final DateFormat d17DateFormat;


/**
 * Creates a new <code>ArchiveDate</code>.
 */
    private DateConverter() {
        arcDateFormat = new SimpleDateFormat(ARC_DATE_FORMAT);
        arcDateFormat.setLenient(false);
        arcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        warcDateFormat = new SimpleDateFormat(WARC_DATE_FORMAT);
        warcDateFormat.setLenient(false);
        warcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        d17DateFormat = new SimpleDateFormat(HERITRIX_DATE_FORMAT);
        d17DateFormat.setLenient(false);
        d17DateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * <code>DateFormat</code> is not thread safe, so we wrap its construction
     * inside a <code>ThreadLocal</code> object.
     */
    private static final ThreadLocal<DateConverter> DateParserTL = new ThreadLocal<DateConverter>() {
        @Override
        public DateConverter initialValue() {
            return new DateConverter();
        }
    };

/**
 * Returns a <code>DateFormat</code> object for ARC date conversion.
 *
 * @return a <code>DateFormat</code> object for ARC date conversion
 */
public static DateFormat getArcDateFormat() {
    return DateParserTL.get().arcDateFormat;
}

/**
 * Returns a <code>DateFormat</code> object for WARC date conversion.
 *
 * @return a <code>DateFormat</code> object for WARC date conversion
 */
public static DateFormat getWarcDateFormat() {
    return DateParserTL.get().warcDateFormat;
}

/**
 * Returns a <code>DateFormat</code> object for Heritrix 17-digit date conversion
 *
 * @return a <code>DateFormat</code> object for WARC date conversion
 */
public static DateFormat getHeritrixDateFormat() {
    return DateParserTL.get().d17DateFormat;
}

}
