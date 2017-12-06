export class TruncateValueConverter {
    toView(value) {
        const max = 20;
        return value.length > max? `${value.substring(0, max)}...` : value; 
    }
}