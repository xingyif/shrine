export class ResultValueConverter {
    toView(value) {
        if (!value) {
            return 'not available';
        }

        if (value.status !== "FINISHED") {
            return value.status;
        }
        return value.count < 0? '<10' : value.count;
    }
}