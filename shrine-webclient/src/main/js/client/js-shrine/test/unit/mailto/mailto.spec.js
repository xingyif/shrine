import { MailTo } from '../../src/app/mailto';



describe('the MailTo module', () => {

    beforeEach(() => {
        let mailto = new MailTo();
    });

    it('mailto should be an empty string', () => {
        expect(mailto.email).toBe('');
    });

    it('mailto subject is correct', () => {
        expect(mailto.subject).toBe(encodeURIComponent('Question from a SHRINE User'));
    });

    it('body should be correct', () => {
        expect(mailto.subject).toBe(encodeURIComponent('Please enter the suggested information and your question. Your data steward will reply to this email.' +
        '\n\n***Never send patient information, passwords, or other sensitive information by email****' +
        '\nName:' +
        '\nTitle:' +
        '\nUser name (to log into SHRINE):' +
        '\nTelephone Number (optional):' +
        '\nPreferred email address (optional):' +
        '\n\nQuestion or Comment:'));
    });

    

});