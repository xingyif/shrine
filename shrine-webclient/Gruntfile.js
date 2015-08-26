module.exports = function (grunt) {

    grunt.initConfig({
        copy: {
            build: {
                files: [
                    {expand: true, src: ['src/main/html/**/*'], dest: 'build/'}
                ]
            },
            deploy: {
                files: [
                    {cwd: 'build/src/main/html', src: ['**/*'], dest: '/usr/local/apache-tomcat-7.0.50/webapps/ROOT/shrine-webclient/', expand: true}
                ]
            }
        },

        // Deletes all .js files
        clean: {
            options: {
                force: true
            },
            js: ["build/", '/usr/local/apache-tomcat-7.0.50/webapps/ROOT/shrine-webclient/*']
        }
    });
    grunt.loadNpmTasks('grunt-contrib-stylus');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-clean');
    /*grunt.loadNpmTasks('grunt-karma');
    grunt.registerTask('test', ['karma']);
    grunt.registerTask('build', ['test', 'clean', 'copy:build', 'uglify:build']);
    grunt.registerTask('deploy', ['copy:deploy']);*/
    grunt.registerTask('default', ['clean', 'copy:build', 'copy:deploy']);
};
