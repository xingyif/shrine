//https://www.w3schools.com/js/js_function_closures.asp
export class QueryViewerModel{
    constructor() {
        this.isLoaded = false;
        this.isFetching = false;
        this.loadedCount = 0;
        this.totalQueries = 0;
        this.screens = [];
    }
    get moreToLoad() {
        console.log(`loaded count ${this.loadedCount} total queris: ${this.totalQueries}`);
        return this.loadedCount < this.totalQueries;
    }
}