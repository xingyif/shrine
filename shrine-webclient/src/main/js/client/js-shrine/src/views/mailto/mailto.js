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
                window.top.location = this.getComposition(email);
            });
    }


    getComposition(address) {
        return this.config.mailto + address + '?' + this.config.subject + '&' + this.config.body;
    }
}


