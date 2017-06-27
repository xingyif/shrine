(function () {
	'use strict';
	// -- public -- //
	i2b2.SHRINE.RequestTopic = requestTopic;
	i2b2.SHRINE.TopicInfo = showTopicInfo;
	i2b2.SHRINE.view.modal.topicInfoDialog = getTopicInfoDialog();

	// -- events -- //
	i2b2.events.afterLogin.subscribe(loginSuccessHandler);

	// -- @todo: boostrap the Webclient plugin tabs here. -- //
	function loginSuccessHandler(type, args) {
		if (i2b2.hive.cfg.lstCells.SHRINE.serverLoaded) {
			i2b2.PM.model.shrine_domain = true;
		}

		if (i2b2.h.isSHRINE()) {
			loadTopics(type, args);
			renderTopics();
			bootstrap();
		}
	}

	function loadTopics(type, args) {
		var msg = i2b2.SHRINE.ajax.readApprovedEntries("SHRINE");
		msg.parse();
		if (msg.error) {
			console.error("Could not get approved topic list from SHRINE");
			console.dir(msg);
			alert('Could not get approved topics list from SHRINE.');
		}
		else {
			i2b2.SHRINE.model.topics = {};
			var l = msg.model.length;
			for (var i = 0; i < l; i++) {
				var rec = msg.model[i];
				if (rec.TopicID != undefined) {
					i2b2.SHRINE.model.topics[rec.TopicID] = rec;
				}
			}
		}
	}


	function renderTopics() {
		var dropdown = $('queryTopicSelect');
		while (dropdown.hasChildNodes()) { dropdown.removeChild(dropdown.firstChild); }
		// create the "Select Topic" option
		var sno = document.createElement('OPTION');
		sno.setAttribute('value', null);
		var snt = document.createTextNode(" ------ Select an Approved Query Topic ------ ");
		sno.appendChild(snt);
		dropdown.appendChild(sno);
		// populate with topics
		for (var i in i2b2.SHRINE.model.topics) {
			var rec = i2b2.SHRINE.model.topics[i];
			if (rec.TopicID != undefined && rec.approval == "Approved") {
				// ONT options dropdown
				var sno = document.createElement('OPTION');
				sno.setAttribute('value', rec.TopicID);
				var snt = document.createTextNode(rec.Name);
				sno.appendChild(snt);
				dropdown.appendChild(sno);
			}
		}

		$$('#crcDlgResultOutputPRC input')[0].disabled = true;
		$('crcDlgResultOutputPRS').hide();
	}

	/*
	* Change this value in the config file [\i2b2\cells\SHRINE\cell_config_data.js]
	*/
	function requestTopic() {
		window.open(i2b2.SHRINE.cfg.config.newTopicURL, 'RequestTopic', 'toolbar=1,scrollbars=1,location=1,statusbar=1,menubar=1,resizable=1,width=800,height=600');
	}


	function showTopicInfo() {
		var s = $('queryTopicSelect');
		if (s.selectedIndex == null || s.selectedIndex == 0) {
			return true;
		}
		var topicID = s.options[s.selectedIndex].value;
		if (topicID == "") { return; }
		i2b2.SHRINE.view.modal.topicInfoDialog.showInfo(topicID);
	}


	function getTopicInfoDialog() {
		return {
			showInfo: function (id) {
				var thisRef = i2b2.SHRINE.view.modal.topicInfoDialog;
				if (!thisRef.yuiDialog) {
					thisRef.yuiDialog = new YAHOO.widget.SimpleDialog("SHRINE-info-panel", {
						zindex: 700,
						width: "400px",
						fixedcenter: true,
						constraintoviewport: true
					});
					thisRef.yuiDialog.render(document.body);
					// show the form
					thisRef.yuiDialog.show();
				}
				// show the form
				$('SHRINE-info-panel').show();
				thisRef.yuiDialog.show();
				thisRef.yuiDialog.center();
				// display the topic info
				var rec = i2b2.SHRINE.model.topics[id];
				if (undefined == rec) { thisRef.yuiDialog.hide(); }	// bad id == bail out here
				$('SHRINE-info-title').innerHTML = rec.Name;
				$('SHRINE-info-body').innerHTML = rec.Intent;
			}
		};
	}

	//@todo: cleanup
	function bootstrap() {
		var config = i2b2.SHRINE.cfg.config;
		jQuery('#' + i2b2.SHRINE.plugin.viewName).load(config.wrapperHtmlFile, function (response, status, xhr) { });

		//i2b2 overrides
		i2b2.events.afterQueryInit = new YAHOO.util.CustomEvent("afterQueryInit", i2b2);
		var _queryRun = i2b2.CRC.ctrlr.QT._queryRun;
		i2b2.CRC.ctrlr.QT._queryRun = function (name, options) {
			i2b2.events.afterQueryInit.fire({ name: name, data: options });
			return _queryRun.apply(i2b2.CRC.ctrlr.QT, [name, options]);
		}

		var statusDisplay = i2b2.CRC.view.status.showDisplay;
		i2b2.CRC.view.status.showDisplay = function () {
			if (i2b2.CRC.view.status.currentTab === 'status' || !jQuery('.query-viewer.active').length) {
				return statusDisplay.apply(i2b2.CRC.view.status.showDisplay, []);
			}
		}
	}
})();

