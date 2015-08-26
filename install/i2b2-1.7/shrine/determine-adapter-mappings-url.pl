use strict;

my $shrine_version = $ENV{'SHRINE_VERSION'};

my $ontology_svn_url_base = "$ENV{'SHRINE_ONT_SVN_URL_BASE'}";

my $adapter_mappings_file_url_suffix = "SHRINE_Demo_Downloads/trunk/AdapterMappings_i2b2_DemoData.xml";

my $adapter_mappings_file_url = "$ontology_svn_url_base/$adapter_mappings_file_url_suffix";

print $adapter_mappings_file_url;
