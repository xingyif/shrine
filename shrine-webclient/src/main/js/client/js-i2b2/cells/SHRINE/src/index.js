import messenger from './shrine.messenger';
import controller from './shrine.controller';
import shrinePlugin from './shrine.plugin';
messenger.decorate();
shrinePlugin.decorate();
controller.decorate();