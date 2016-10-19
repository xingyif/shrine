
module.exports = function () {
    // -- dependencies -- //
    var wiredep = require('wiredep');

    var bowerJS = wiredep({ devDependencies: true })['js'];//grab .js dev depenencies.
    var bowerCSS = wiredep({ devDependencies: true })['css'];
    var bowerFnt = [
            './bower_components/font-awesome/fonts/**/*', 
            './bower_components/bootstrap/dist/**/*'
        ];

    // -- directories -- //
    var src = './app/';
    var clientDir = src + 'client/';
    var assets = src + 'assets/';
    var build = '../resources/client/';
    var lintFiles = clientDir + '**/*.js';
    var srcFiles = clientDir + '**/!(*.spec)+(.js)';
    var moduleFiles = clientDir + '**/*.module.js';
    var specFiles = clientDir + '**/*.spec.js';
    var sassFiles = src + 'sass/**/*.scss';
    var cssDir = assets + 'css/';

    var config = {
        index: 'index.html',
        buildDir: build,
        lintFiles: lintFiles,
        jsFiles: [
            clientDir + '**/*.module.js',
            lintFiles,
            '!' + clientDir + '**/*.spec.js'
        ],
        tplFiles: [
            clientDir + '**/*.tpl.html'
        ],
        configFiles: [
            src + '/config/**/*'
        ],
        cssFiles: [
            assets + '**/shrine.css'
        ],
        assetFiles: [assets + '**/*', src + 'config/**/*'],
        clientDir: clientDir,
        sassFiles: sassFiles,
        cssDir: cssDir,
        bower: {
            json: require('./bower.json'),
            directory: './bower_components/',
            ignorePath: '../..',
            jsFiles: bowerJS,
            cssFiles: bowerCSS,
            fontFiles: bowerFnt
        }
    };

    config.bower.font = config.bower.directory + 'font-awesome/fonts/*';

    config.getWiredepDefaultOptions = function () {
        var options = {
            bowerJson: config.bower.json,
            directory: config.bower.directory,
            ignorePath: config.bower.ignorePath
        };
    };

    config.watchFiles = config.assetFiles.concat(config.jsFiles).concat(config.tplFiles).concat(config.index);

    config.karma = getKarmaOptions();

    return config;

    function getKarmaOptions() {
        var options = {
            files: config.bower.jsFiles.concat([moduleFiles, srcFiles, specFiles]),
            exclude: [],
            coverage: {
                reporters: [
                    { type: 'html', subdir: 'report-html' },
                    { type: 'text-summary' } //outputs to console.
                ]
            },
            frameworks: ['jasmine', 'browserify'],
            preprocessors: {
                './app/client/**/*.spec.js': ['browserify']
            }
        };
        return options;
    }

};
