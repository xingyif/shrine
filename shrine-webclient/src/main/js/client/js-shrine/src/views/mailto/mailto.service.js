import {QEPRepository} from 'repository/qep.repository';
export class MailToService {
    static inject = [QEPRepository];
    constructor(repository) {
        this.repository = repository;
    }

    fetchStewardEmail() {
        return this.repository.fetchStewardEmail();
    }
}