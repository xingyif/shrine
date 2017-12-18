export const MixinNoSynonymTermInfo = context => (synonymValue = 'false') => {
  context.i2b2.ONT.cfg.msgs.GetTermInfo =
    `<ns3:request xmlns:ns3="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns4="http://www.i2b2.org/xsd/cell/ont/1.1/" xmlns:ns2="http://www.i2b2.org/xsd/hive/plugin/">\n
    <message_header>\n
      {{{proxy_info}}}\n
      <i2b2_version_compatible>1.1</i2b2_version_compatible>\n
      <hl7_version_compatible>2.4</hl7_version_compatible>\n
      <sending_application>\n
        <application_name>i2b2 Ontology</application_name>\n
        <application_version>${context.i2b2.ClientVersion}</application_version>\n
      </sending_application>\n
      <sending_facility>\n
        <facility_name>i2b2 Hive</facility_name>\n
      </sending_facility>\n
      <receiving_application>\n
        <application_name>Ontology Cell</application_name>\n
        <application_version>${context.i2b2.ClientVersion}</application_version>\n
      </receiving_application>\n
      <receiving_facility>\n
        <facility_name>i2b2 Hive</facility_name>\n
      </receiving_facility>\n
      <datetime_of_message>{{{header_msg_datetime}}}</datetime_of_message>\n
      <security>\n
        <domain>{{{sec_domain}}}</domain>\n
        <username>{{{sec_user}}}</username>\n
        {{{sec_pass_node}}}\n
      </security>\n
      <message_control_id>\n
        <message_num>{{{header_msg_id}}}</message_num>\n
        <instance_num>0</instance_num>\n
      </message_control_id>\n
      <processing_id>\n
        <processing_id>P</processing_id>\n
        <processing_mode>I</processing_mode>\n
      </processing_id>\n
      <accept_acknowledgement_type>AL</accept_acknowledgement_type>\n
      <application_acknowledgement_type>AL</application_acknowledgement_type>\n
      <country_code>US</country_code>\n
      <project_id>{{{sec_project}}}</project_id>\n
    </message_header>\n
    <request_header>\n
      <result_waittime_ms>{{{result_wait_time}}}000</result_waittime_ms>\n
    </request_header>\n
    <message_body>\n
      <ns4:get_term_info blob="true" type="core" {{{ont_max_records}}} synonyms="${synonymValue}" hiddens="{{{ont_hidden_records}}}">\n
        <self>{{{concept_key_value}}}</self>\n
      </ns4:get_term_info>\n
    </message_body>\n
  </ns3:request>`;

  //will override GetTermInfo Object key
  context.i2b2.ONT.ajax._addFunctionCall('GetTermInfo',
    '{{{URL}}}getTermInfo',
    context.i2b2.ONT.cfg.msgs.GetTermInfo,
    null,
    context.i2b2.ONT.cfg.parsers.ExtractConcepts);
};