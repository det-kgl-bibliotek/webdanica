
-- Load the metadata from the parsed data, which is JSON strings stored in a gzip-file
captures = LOAD '$input' USING org.archive.bacon.io.SequenceFileLoader()
             AS ( key:chararray, value:chararray );


-- Convert the JSON Strings into Pig Map Objects.

captures = FOREACH captures GENERATE key, FROMJSON( value ) AS m:[];

captures = FILTER captures BY m#'errorMessage' is null;
captures = FILTER captures BY ISNT_ROBOTSTXT(m#'url');  
captures = FILTER captures BY FILENOTFOUND(m#'code');

--captures = FOREACH captures GENERATE m#'url' as url:chararray, 
-- m#'outlinks' as links:chararray, LOWER(CONCATTEXT(m)) AS text:chararray, m#'date' AS date:chararray, HOST(m#'url') AS hostname:chararray;

captures = FOREACH captures GENERATE m#'url' as url:chararray,
 m#'outlinks' as links:chararray, CONCATTEXT(m) AS text:chararray, m#'date' AS date:chararray, HOST(m#'url') AS hostname:chararray;


--captures = FOREACH captures GENERATE CombinedCombo(url, date, text, links, hostname);
--captures = FOREACH captures GENERATE CombinedComboJson(url, date, text, links, hostname, true);
captures = FOREACH captures GENERATE CombinedCombo(url, date, text, links, hostname, true);
 
STORE captures INTO '$output' USING PigStorage();
