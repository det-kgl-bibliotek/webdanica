package dk.kb.webdanica.core.seeds.filtering;

import java.io.File;

import dk.kb.webdanica.core.WebdanicaSettings;
import dk.kb.webdanica.core.utils.Settings;
import dk.kb.webdanica.core.utils.UnitTestUtils;

public class IgnoredSuffixes {

    private static String[] ignoredExts = Settings
            .getAll(WebdanicaSettings.IGNORED_SUFFIXES);

    /**
     * Test program to test with your own settings. If not a path to a valid
     * settingsfile is given, the file
     * webdanica-core/src/test/resources/settings.xml will be used
     * 
     * @param args
     *            zero or one (an optional
     *            /full/path/to/a/webdanicaSettingsfiles
     */
    public static void main(String[] args) {
        if (args.length == 0) { // use default
            File good_webdanicaSettingsFile = UnitTestUtils
                    .getTestResourceFile("settings.xml");
            System.setProperty(Settings.SETTINGS_FILE_PROPERTY,
                    good_webdanicaSettingsFile.getAbsolutePath());
            Settings.reload();
        } else {
            File some_webdanicaSettingsFile = new File(args[1]);
            System.setProperty(Settings.SETTINGS_FILE_PROPERTY,
                    some_webdanicaSettingsFile.getAbsolutePath());
            Settings.reload();
        }
        String[] ignoredSuffixes = Settings
                .getAll(WebdanicaSettings.IGNORED_SUFFIXES);
        for (String suffix : ignoredSuffixes) {
            System.out.println(suffix);
        }
    }

    /**
     * Test whether a seed has a suffix that matches the list of ignored
     * suffixes.
     * @param seed a given seed
     * @return the matches suffix, if it matches an ignored suffix, null
     *         otherwise
     */
    public static String matchesIgnoredExtension(String seed) {
        String low = seed.toLowerCase();
        if (low.contains("?")) { // Ignore everything from the ? and beyond
            low = low.substring(0, low.indexOf('?'));
        }
        for (String ign : ignoredExts) {
            if (low.endsWith(ign)) {
                return ign;
            }
        }
        return null;
    }

    /**
     * @return the list read from settings-file of the ignored Suffixes
     */
    public static String[] getIgnoredSuffixes() {
        return ignoredExts;
    }

}
