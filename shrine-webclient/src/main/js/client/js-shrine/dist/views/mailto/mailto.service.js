System.register(['repository/qep.repository'], function (_export, _context) {
    "use strict";

    var QEPRepository, _class, _temp, MailToService;

    function _classCallCheck(instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError("Cannot call a class as a function");
        }
    }

    return {
        setters: [function (_repositoryQepRepository) {
            QEPRepository = _repositoryQepRepository.QEPRepository;
        }],
        execute: function () {
            _export('MailToService', MailToService = (_temp = _class = function () {
                function MailToService(repository) {
                    _classCallCheck(this, MailToService);

                    this.repository = repository;
                }

                MailToService.prototype.fetchStewardEmail = function fetchStewardEmail() {
                    return this.repository.fetchStewardEmail();
                };

                return MailToService;
            }(), _class.inject = [QEPRepository], _temp));

            _export('MailToService', MailToService);
        }
    };
});
//# sourceMappingURL=mailto.service.js.map
