import I2B2Decorator from './common/i2b2.decorator';
import APPROVED_ENTRY_XML from './shrine-xml';
class ShrineMessenger extends I2B2Decorator {

	constructor() {
		super();
	}

	decorate() {
		this.shrine.ajax = this.i2b2.hive.communicatorFactory("SHRINE");
		this.shrine.cfg.msgs = { readApprovedEntries: APPROVED_ENTRY_XML };
		let readApprovedEntries = bind.readApprovedEntries(this);
		this.shrine.cfg.parsers = { readApprovedEntries };
		this.shrine.ajax._addFunctionCall(
			"readApprovedEntries",
			this.shrine.cfg.config.readApprovedURL,
			this.shrine.cfg.msgs.readApprovedEntries,
			null,
			this.shrine.cfg.parsers.readApprovedEntries
		);
	}
}

// -- this is bound to context somewhere in i2b2.
const bind = {
	readApprovedEntries: (context) =>
		function () {
			if (!this.error) {
				this.model = [];
				var qm = this.refXML.getElementsByTagName('sheriffEntry');
				for (var i = 0; i < 1 * qm.length; i++) {
					var o = new Object;
					o.xmlOrig = qm[i];
					o.approval = context.i2b2.h.getXNodeVal(qm[i], 'approval');
					o.TopicID = context.i2b2.h.getXNodeVal(qm[i], 'queryTopicID');
					o.Name = context.i2b2.h.getXNodeVal(qm[i], 'queryName');
					this.model.push(o);
				}
			} else {
				this.model = false;
				console.error("[readApprovedEntries] Could not parse() data!");
			}
			return this;
		}
}

// -- singleton --//
export default new ShrineMessenger();