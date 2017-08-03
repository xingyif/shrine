System.register(['views/mailto/mailto.service', 'views/mailto/mailto.config'], function (_export, _context) {
    "use strict";

    var MailToService, MailConfig, _class, _temp, MailTo;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_viewsMailtoMailtoService) {
            MailToService = _viewsMailtoMailtoService.MailToService;
        }, function (_viewsMailtoMailtoConfig) {
            MailConfig = _viewsMailtoMailtoConfig.MailConfig;
        }],
        execute: function () {
            _export('MailTo', MailTo = (_temp = _class = function () {
                function MailTo(service, config) {
                    _classCallCheck(this, MailTo);

                    this.service = service;
                    this.config = config;
                }

                MailTo.prototype.openEmail = function openEmail() {
                    var _this = this;

                    this.service.fetchStewardEmail().then(function (email) {
                        window.top.location = 'mailto:' + email + '?subject=' + _this.config.subject + '&body=' + _this.config.body;
                    });
                };

                return MailTo;
            }(), _class.inject = [MailToService, MailConfig], _temp));

            _export('MailTo', MailTo);
        }
    };
});
//# sourceMappingURL=mailto.js.map
