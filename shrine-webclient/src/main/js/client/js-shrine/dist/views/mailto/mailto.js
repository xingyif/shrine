System.register(['aurelia-framework', 'views/mailto/mailto.service', 'views/mailto/mailto.config'], function (_export, _context) {
    "use strict";

    var inject, MailToService, MailConfig, _dec, _class, MailTo;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaFramework) {
            inject = _aureliaFramework.inject;
        }, function (_viewsMailtoMailtoService) {
            MailToService = _viewsMailtoMailtoService.MailToService;
        }, function (_viewsMailtoMailtoConfig) {
            MailConfig = _viewsMailtoMailtoConfig.MailConfig;
        }],
        execute: function () {
            _export('MailTo', MailTo = (_dec = inject(MailToService, MailConfig), _dec(_class = function () {
                function MailTo(service, config) {
                    _classCallCheck(this, MailTo);

                    this.service = service;
                    this.config = config;
                }

                MailTo.prototype.openEmail = function openEmail() {
                    var _this = this;

                    this.service.fetchStewardEmail().then(function (email) {
                        window.top.location = _this.getComposition(email);
                    });
                };

                MailTo.prototype.getComposition = function getComposition(address) {
                    return this.config.mailto + address + '?' + this.config.subject + '&' + this.config.body;
                };

                return MailTo;
            }()) || _class));

            _export('MailTo', MailTo);
        }
    };
});
//# sourceMappingURL=mailto.js.map
