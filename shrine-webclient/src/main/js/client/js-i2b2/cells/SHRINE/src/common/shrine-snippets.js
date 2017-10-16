class ShrineSnippets {
  static shrineCSVExport = () => `
    <a id="shrineCSVExport" style="cursor: pointer; text-decoration:underline; color: blue;">
      Export to CSV
    </a>`;

  static shrineTab = () => `
      <div class="tabBox query-viewer active">
        <div>Query Viewer</div>
      </div>
    `;

  static shrinePanel = () => `
      <div id="shrinePlugin" class="shrinePluginContent" oncontextmenu="return false" style="padding: 0;">
      </div>
    `;


  static dialogHTML = (i2b2, data) => {
    const wikiBaseUrl = (i2b2.hive.cfg.wikiBaseUrl || 'https://open.med.harvard.edu/wiki/display/SHRINE/');
    const codec = data.problemDigest && data.problemDigest.codec ? data.problemDigest.codec : '';

    return `
      <div id="pluginErrorDetail" style="display:none;"> 
        <div class="hd" style="background:#6677AA;">
          Query Error Detail
        </div> 
        <div class="bd"> 
          <br /> 
          <div style="border: 1px solid #C0C0C0; max-height: 450px; background-color: #FFFFFF; overflow: scroll; word-wrap: break-word; padding: 10px 5px;" class="StatusBoxText"> 
            <div>
              <b>Summary:</b>
            </div> 
            <div>  
              ${data.status}
            </div>
            <br/> 
            <div>
              <b>Description:</b>
            </div> 
            <div>
              <p>  ${data.statusMessage}  </p>
            </div>
            <br/> 
            <span id="pluginMoreDetail"> 
              <div>
                <b>Codec:</b>
              </div> 
              <div>  
                ${data.problemDigest && data.problemDigest.codec ? data.problemDigest.codec : 'not available'}
              </div> 
              <div>
                <i>For information on troubleshooting and resolution, check <a href="${wikiBaseUrl}${codec}" target="_blank">the SHRINE Error Codex</a>.</i> 
              </div>
              <br/> 
              <div>
                <b>Stamp:</b>
              </div> 
              <div>  
                ${data.problemDigest && data.problemDigest.stampText ? data.problemDigest.stampText : 'not available'}  
              </div>
              <br/> 
              <div>
                <b>Stack Trace Name:</b>
              </div> 
              <div>  
                ${data.problemDigest && data.problemDigest.codec ? data.problemDigest.codec : 'not available'}  
              </div>
              <br/> 
              <div>
                <b>Stack Trace Message:</b>
              </div> 
              <div>  
                ${data.problemDigest && data.problemDigest.description ? data.problemDigest.description : 'not available'}  
              </div>
              <br/> 
              <div>
                <b>Stack Trace Details:</b>
              </div> 
              <div>  
                ${data.problemDigest && data.problemDigest.detailsString ? data.problemDigest.detailsString.split(',').join('<br/>') : 'not available'}  
              </div>
              <br/> 
          </span>
        </div> 
      </div> 
    </div>`;
  }
}

export default ShrineSnippets;