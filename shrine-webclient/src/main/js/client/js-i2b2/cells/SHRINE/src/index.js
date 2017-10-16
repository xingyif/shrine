import messenger from './shrine.messenger';
import controller from './shrine.controller';
import shrinePlugin from './shrine.plugin';
messenger.decorate();
controller.decorate();
shrinePlugin.decorate();