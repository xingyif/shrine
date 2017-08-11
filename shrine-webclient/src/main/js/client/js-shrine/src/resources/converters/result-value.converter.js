export class ResultValueConverter {
    toView(value) {
        
        // -- @todo: switch statement?
        if (!value) {
            return 'not available';
        }

        if (value.status !== "FINISHED") {
            return '';
        }
        
        return value.count < 0? '<=10' : value.count;
    }
}