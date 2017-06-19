System.register(['aurelia-framework', './i2b2.service', './tabs.model'], function (_export, _context) {
    "use strict";

    var inject, I2B2Service, TabsModel, _dec, _class, I2B2PubSub;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaFramework) {
            inject = _aureliaFramework.inject;
        }, function (_i2b2Service) {
            I2B2Service = _i2b2Service.I2B2Service;
        }, function (_tabsModel) {
            TabsModel = _tabsModel.TabsModel;
        }],
        execute: function () {
            _export('I2B2PubSub', I2B2PubSub = (_dec = inject(I2B2Service, TabsModel), _dec(_class = function I2B2PubSub(i2b2Svc, tabs) {
                _classCallCheck(this, I2B2PubSub);

                this.listen = function () {
                    var setVertStyle = function setVertStyle(a, b) {
                        return b.find(function (e) {
                            return e.action === 'ADD';
                        }) ? tabs.setMax() : tabs.setMin();
                    };
                    i2b2Svc.onResize(setVertStyle);
                };
            }) || _class));

            _export('I2B2PubSub', I2B2PubSub);
        }
    };
});
//# sourceMappingURL=i2b2.pub-sub.js.map
