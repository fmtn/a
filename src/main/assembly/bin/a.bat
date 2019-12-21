@echo off
java -Dnashorn.args=--no-deprecation-warning -jar %~dp0${project.build.finalName}-jar-with-dependencies.jar %*