import './index.css';
import messenger from './shrine.messenger';
import controller from './shrine.controller';
import shrinePlugin from './shrine.plugin';
const GLOBAL = window || {}; //TODO: mock window for testing.
messenger.decorate(GLOBAL);
shrinePlugin.decorate(GLOBAL);
controller.decorate(GLOBAL);