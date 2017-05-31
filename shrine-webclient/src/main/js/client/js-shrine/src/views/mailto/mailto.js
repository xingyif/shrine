import {inject} from 'aurelia-framework';
import {MailToService} from 'views/mailto/mailto.service';
import {MailConfig} from 'views/mailto/mailto.config';

@inject(MailToService, MailConfig)
export class MailTo {
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


