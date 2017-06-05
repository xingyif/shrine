export class TabsModel{

    constructor() {
        let mode = TabsModel.min;
        TabsModel.prototype.setMax = () => mode = TabsModel.full;
        TabsModel.prototype.setMin = () => mode = TabsModel.min;
        TabsModel.prototype.mode = () => mode;
    }

    static get full() {
        return 'v-full';
    }

    static get min() {
        return 'v-min';
    }
}