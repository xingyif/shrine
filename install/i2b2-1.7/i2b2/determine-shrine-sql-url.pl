use strict;

my $shrine_version = $ENV{'SHRINE_VERSION'};

my $ontology_svn_url_base = "$ENV{'SHRINE_ONT_SVN_URL_BASE'}";

my $shrine_sql_file_url_suffix = "SHRINE_Demo_Downloads/trunk/ShrineDemo.sql";

my $shrine_sql_file_url = "$ontology_svn_url_base/$shrine_sql_file_url_suffix";

print $shrine_sql_file_url;
