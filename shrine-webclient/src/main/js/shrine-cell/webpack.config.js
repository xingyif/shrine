"use strict";
const webpack = require('webpack');
const path = require('path');
const loaders = require('./webpack.loaders');
const DIR = '../i2b2/js-i2b2/cells/SHRINE/dist/';

module.exports = {
  entry: [
    './src/index.js'
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