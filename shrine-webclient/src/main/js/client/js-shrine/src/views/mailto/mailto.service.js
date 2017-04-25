import {inject} from 'aurelia-framework';
import {QEPRepository} from 'repository/qep.repository';

@inject(QEPRepository)
export class MailToService {

    constructor(repository) {
        this.repository = repository;
    }

    fetchStewardEmail() {
        return this.repository.fetchStewardEmail();
    }
}