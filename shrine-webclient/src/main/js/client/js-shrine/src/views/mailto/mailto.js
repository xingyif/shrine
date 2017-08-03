import {MailToService} from 'views/mailto/mailto.service';
import {MailConfig} from 'views/mailto/mailto.config';
export class MailTo {
    static inject = [MailToService, MailConfig];
    constructor(service, config) {
        this.service = service;
        this.config = config;
    }

    openEmail () {
        this.service.fetchStewardEmail()
            .then(email => {
                window.top.location = `mailto:${email}?subject=${this.config.subject}&body=${this.config.body}`;
            });
    }
}



