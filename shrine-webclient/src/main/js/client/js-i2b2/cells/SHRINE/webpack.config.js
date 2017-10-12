"use strict";
const webpack = require('webpack');
const path = require('path');
const loaders = require('./webpack.loaders');
const DIR = './'

module.exports = {
  entry: [
    './src/shrine.plugin.js',
    './src/shrine.controller.js', // your app's entry point
  ],
  devtool: process.env.WEBPACK_DEVTOOL || 'eval-source-map',
  output: {
    publicPath: '/',
    path: path.join(__dirname, DIR),
    filename: 'shrine.bundle.js'
  },
  resolve: {
    extensions: ['.js', '.jsx']
  },
  module: {
    loaders
  },
  plugins: [
    new webpack.NoEmitOnErrorsPlugin(),
    new webpack.NamedModulesPlugin()
  ]
};