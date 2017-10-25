/**
 * This module is the psuedo-refactored legacy (SHRINE_cell_ctrlr.js?) logic into ES2015+.
 */
import I2B2Decorator from './common/i2b2.decorator';
import bootstrapper from './shrine.bootstrapper';
class ShrineController extends I2B2Decorator {
  constructor() {
    super();
  }

  decorate() {
    this.shrine.RequestTopic = bind.RequestTopic(this);
    this.shrine.TopicInfo = bind.TopicInfo(this);
    this.shrine.view.modal.topicInfoDialog = bind.topicInfoDialog(this);
    this.i2b2.events.afterLogin.subscribe(bind.afterLogin(this))
  }

  startBootstrapper() {
    bootstrapper.bootstrap();
  }

  loadTopics(type, args) {
    var msg = this.shrine.ajax.readApprovedEntries("SHRINE");
    msg.parse();
    if (msg.error) {
      console.error("Could not get approved topic list from SHRINE");
      console.dir(msg);
      alert('Could not get approved topics list from SHRINE.');
    }
    else {
      this.shrine.model.topics = {};
      var l = msg.model.length;
      for (var i = 0; i < l; i++) {
        var rec = msg.model[i];
        if (rec.TopicID != undefined) {
          this.shrine.model.topics[rec.TopicID] = rec;
        }
      }
    }
  }

  renderTopics() {
    var dropdown = this.prototype$('queryTopicSelect');

    while (dropdown.hasChildNodes()) { dropdown.removeChild(dropdown.firstChild); }
    // create the "Select Topic" option
    var sno = document.createElement('OPTION');
    sno.setAttribute('value', null);
    var snt = document.createTextNode(" ------ Select an Approved Query Topic ------ ");
    sno.appendChild(snt);
    dropdown.appendChild(sno);
    // populate with topics
    for (var i in this.shrine.model.topics) {
      var rec = this.shrine.model.topics[i];
      if (rec.TopicID != undefined && rec.approval == "Approved") {
        // ONT options dropdown
        var sno = document.createElement('OPTION');
        sno.setAttribute('value', rec.TopicID);
        var snt = document.createTextNode(rec.Name);
        sno.appendChild(snt);
        dropdown.appendChild(sno);
      }
    }
    this.prototype$$('#crcDlgResultOutputPRC input')[0].disabled = true;
    this.prototype$('crcDlgResultOutputPRS').hide();
  }
}

//TODO: refactor this to use function.bind if possible.
// -- these closure bind have their scope altered by i2b2 -- //
const bind = {
  RequestTopic: (context) => () => {
    context.global.open(
      context.shrine.cfg.config.newTopicURL,
      'RequestTopic', 'toolbar=1,scrollbars=1,location=1,statusbar=1,menubar=1,resizable=1,width=800,height=600'
    );
  },

  TopicInfo: (context) => () => {
    var s = context.prototype$('queryTopicSelect');
    if (s.selectedIndex == null || s.selectedIndex == 0) {
      return true;
    }
    var topicID = s.options[s.selectedIndex].value;
    if (topicID == "") { return; }
    context.shrine.view.modal.topicInfoDialog.showInfo(topicID);
  },

  topicInfoDialog: (context) => () => ({
      showInfo: function (id) {
        const thisRef = context.shrine.view.modal.topicInfoDialog;
        //TODO: encapsulate this reference better.
        const SimpleDialog = context.YAHOO.widget.SimpleDialog;
        if (!thisRef.yuiDialog) {
          thisRef.yuiDialog = new SimpleDialog("SHRINE-info-panel", {
            zindex: 700,
            width: "400px",
            fixedcenter: true,
            constraintoviewport: true
          });
          thisRef.yuiDialog.render(context.global.document.body);
          // show the form
          thisRef.yuiDialog.show();
        }
        // show the form
        context.prototype$('SHRINE-info-panel').show();
        thisRef.yuiDialog.show();
        thisRef.yuiDialog.center();
        // display the topic info
        var rec = i2b2.SHRINE.model.topics[id];
        if (undefined == rec) { thisRef.yuiDialog.hide(); }	// bad id == bail out here
        context.prototype$('SHRINE-info-title').innerHTML = rec.Name;
        context.prototype$('SHRINE-info-body').innerHTML = rec.Intent;
      }
    }),
  
  afterLogin: (context) => (type, args) => {
    if (context.i2b2.hive.cfg.lstCells.SHRINE.serverLoaded) {
      context.i2b2.PM.model.shrine_domain = true;
    }

    if (context.i2b2.h.isSHRINE()) {
      context.loadTopics(type, args);
      context.renderTopics();
      bootstrapper.bootstrap();
    }
  }
}

// -- singleton -- //
export default new ShrineController();

