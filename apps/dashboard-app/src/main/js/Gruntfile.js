module.exports = function (grunt) {

    grunt.initConfig({
        stylus: {
            compile: {
                options: {compress: false},
                files: {
                    'src/assets/css/app.css': 'src/assets/styl/app.styl'
                }
            },
            connect: {
                livereload: {
                    options: {
                        port: 9000,
                        hostname: 'localhost',
                        middleware: function (connect) {
                            return [
                                function (req, res, next) {
                                    res.setHeader('Access-Control-Allow-Origin', '*');
                                    res.setHeader('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE');
                                    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
                                    return next();
                                }
                            ];
                        }
                    }
                }
            }
        },
        watch: {
            stylus: {
                files:['src/assets/styl/**.styl'],
                tasks:['stylus:compile']
            },
            css: {
                options: {livereload: true},
                files: ['src/assets/css/**.css']
            },
            html: {
                options: {livereload: true},
                files: ['*.html', 'src/app/**/*.html']
            },
            script: {
                options: {livereload: true},
                files: ['src/app/**/*.js']
            }
        },
        uglify: {
            options: {
                mangle: false
            },
            build: {
                files: [{
                    expand: true,
                    src: ['src/app/**/*.js', '!src/app/**/*.spec.js'],
                    dest: 'build/'
                }]
            }
        },
        copy: {
            build: {
                files: [
                    {expand: true, src: ['index.html', 'index-1.19.html'], dest: 'build/'},
                    {expand: true, src: ['config/*'], dest: 'build/'},
                    {expand: true, src: ['src/app/**/*', "src/assets/**/*", "src/vendor/**/*", "!src/app/**/*.spec.js"], dest: 'build/'},
                    {expand: true, src: ['happy/**/*'], dest: 'build/'}
                ]
            },
            deploy: {
                files: [
                    {cwd: 'build/', src: ['**/*'], dest: '../resources/client/', expand: true}
                ]
            }
        },

        // Deletes all .js files
        clean: {
            options: {
                force: true
            },
            js: ["build/", '../resources/client/*']
        },
        karma: {
            options: {
                // point all tasks to karma config file
                configFile: 'karma.conf.js'
            },
            unit: {
                // run tests once instead of continuously
                singleRun: true
            }
        }
    });
    grunt.loadNpmTasks('grunt-contrib-stylus');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-karma');
    grunt.registerTask('test', ['karma']);
    grunt.registerTask('build', ['test', 'clean', 'copy:build', 'uglify:build']);
    grunt.registerTask('deploy', ['copy:deploy']);
    grunt.registerTask('default', ['clean', 'copy:build', /*'uglify:build',*/ 'copy:deploy']);
};
