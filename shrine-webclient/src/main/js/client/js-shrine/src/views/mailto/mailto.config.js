export const MailConfig = {
    mailto: 'mailto:',
    subject: encodeURIComponent('Question from a SHRINE User'),
    body : encodeURIComponent(`Please enter the suggested information and your question. Your data steward will reply to this email.
        \n\n***Never send patient information, passwords, or other sensitive information by email****
        \nName:
        \nTitle:
        \nUser name (to log into SHRINE):
        \nTelephone Number (optional):
        \nPreferred email address (optional):
        \n\nQuestion or Comment:`)
};