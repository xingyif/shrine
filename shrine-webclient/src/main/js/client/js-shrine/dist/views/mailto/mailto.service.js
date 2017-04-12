System.register(['aurelia-framework', 'repository/qep.repository'], function (_export, _context) {
    "use strict";

    var inject, QEPRepository, _dec, _class, MailToService;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_aureliaFramework) {
            inject = _aureliaFramework.inject;
        }, function (_repositoryQepRepository) {
            QEPRepository = _repositoryQepRepository.QEPRepository;
        }],
        execute: function () {
            _export('MailToService', MailToService = (_dec = inject(QEPRepository), _dec(_class = function () {
                function MailToService(repository) {
                    _classCallCheck(this, MailToService);

                    this.repository = repository;
                }

                MailToService.prototype.fetchStewardEmail = function fetchStewardEmail() {
                    return this.repository.fetchStewardEmail();
                };

                return MailToService;
            }()) || _class));

            _export('MailToService', MailToService);
        }
    };
});
//# sourceMappingURL=mailto.service.js.map
