//https://www.w3schools.com/js/js_function_closures.asp
export class QueryViewerModel{
    constructor() {
        this.processing = false;
        this.loadedCount = 0;
        this.totalQueries = 0;
        this.screens = [];
    }
    get moreToLoad() {
        return this.loadedCount < this.totalQueries;
    }

    get hasData() {
        return this.screens.length > 0;
    }
}