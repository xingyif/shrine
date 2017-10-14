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
}

export default ShrineSnippets;