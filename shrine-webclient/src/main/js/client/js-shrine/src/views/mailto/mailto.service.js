import {inject} from 'aurelia-framework';
import {HttpClient} from 'aurelia-fetch-client';
import 'fetch';

@inject(HttpClient)
export class MailToService {

    constructor(http) {
        http.configure(config => {
            config
                .useStandardConfiguration()
                .withBaseUrl(this.url)
        });

        this.http = http;
    }

    fetchStewardEmail() {
        return this.http.fetch('data?key=stewardEmail')
            .then(response => response.json())
            .then(address => {
                return (address.indexOf('\"') > 0)? 
                    address.split('\"')[1] : address;
            })
            .catch(() => '');
    }

    
     get url() {
        const port = '6443';
        const url = document.URL;
        const service = '/shrine-metadata/'; //data?key=stewardEmail';
        return url.substring(0, url.indexOf(port) + port.length) + service;
    }
}