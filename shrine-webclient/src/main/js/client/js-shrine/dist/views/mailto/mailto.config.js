System.register([], function (_export, _context) {
    "use strict";

    var MailConfig;
    return {
        setters: [],
        execute: function () {
            _export('MailConfig', MailConfig = {
                mailto: 'mailto:',
                subject: encodeURIComponent('Question from a SHRINE User'),
                body: encodeURIComponent('Please enter the suggested information and your question. Your data steward will reply to this email.\n        \n\n***Never send patient information, passwords, or other sensitive information by email****\n        \nName:\n        \nTitle:\n        \nUser name (to log into SHRINE):\n        \nTelephone Number (optional):\n        \nPreferred email address (optional):\n        \n\nQuestion or Comment:')
            });

            _export('MailConfig', MailConfig);
        }
    };
});
//# sourceMappingURL=mailto.config.js.map
