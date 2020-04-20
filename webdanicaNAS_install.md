# Installation and configuration of the NetarchiveSuite used by webdanica

The webdanica system uses a quickstart based Netarchivesuite 5.4 instance running on the same machine as the webapp.

The recipe used is as written on the page <https://sbforge.org/display/NASDOC54/Installation+of+the+Quickstart+system>

By default, this uses derby as the backend database system. See [this page](webdanica_with_postgresql.md) for how to use Postgresql instead.

Only exception is the use of [install/deploy_webdanica_netarchivesuite.xml](install-deprecated/deploy_webdanica_netarchivesuite.xml) instead of the 'deploy_standalone_example.xml'.
If using Postgresql, use instead [install/deploy_webdanica_netarchivesuite_pgsql.xml](install-deprecated/deploy_webdanica_netarchivesuite_pgsql.xml). 

Before deploying netarchivesuite with the RunNetarchivesuite.sh, you need to change the following

 * The `/home/test/ARKIV` should be replaced with the correct path (this variable must be the same as the WEBDATADIR in the `automatic-workflow/setenv.sh`)
 * The deployInstallDir (default = `/home/test`) must be adapted to your environment
 * The deployMachineUserName (default = test) must be adapted to your environment
 * The receiver and sender (default = test@localhost)  must be adapted to your environment
 * The mail.server setting (default = post.kb.dk)  must be adapted to your environment

The differences from the quickstart are as follows: 
 * The netarchivesuite writes its harvestdata to $HOME/ARKIV (e.g. `/home/test/ARKIV` in our staging setup) using a localarcrepositoryclient in order for the data to easily accessable outside the netarchivesuite system folder
 * No bitpreservation is thus attempted, therefore the netarchive installation has no ArcrepositoryApplication, BitarchiveMonitorApplications, and BitarchiveApplications
 * IndexingApplication is not required, as we assume that the deduplication is disabled by removing the deduplication bean from the template used by Webdanica.

Note: Currently, we have no ViewerProxyApplication and IndexServerApplication as well, but they could be turned on, if viewerproxying the metadata files is required by the curators

A sample Heritrix3 template without the deduplication beans can be found here: [install/templates/default_orderxml.xml](`install/templates/default_orderxml.xml`).

What you call this template is up to you, but the name must be same as the value of setting 'settings.harvesting.template' in your `webdanica_settings.xml` file.

The same goes for the schedule used by the automatic harvesting workflow which is defined by the setting 'settings.harvesting.schedule' in your `webdanica_settings.xml` file.

You need to use a schedule that only runs once.
You create this in the NetarchiveSuite GUI by 
 * going to Definitions->Schedules 
 * select "Create new schedule"
 * Choose name = Once
 * Choose the second Until option (under the Continue subheading)
 * Write 1 (thus it reads until 1 harvests have been done)
 * Save 

