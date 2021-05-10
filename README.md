atlas-version
============

[![Gradle Plugin ID](https://img.shields.io/badge/gradle-net.wooga.github-brightgreen.svg?style=flat-square)](https://plugins.gradle.org/plugin/net.wooga.version)
[![Build Status](https://img.shields.io/travis/wooga/atlas-version/master.svg?style=flat-square)](https://travis-ci.org/wooga/atlas-version)
[![Coveralls Status](https://img.shields.io/coveralls/wooga/atlas-version/master.svg?style=flat-square)](https://coveralls.io/github/wooga/atlas-version?branch=master)
[![Apache 2.0](https://img.shields.io/badge/license-Apache%202-blue.svg?style=flat-square)](https://raw.githubusercontent.com/wooga/atlas-version/master/LICENSE)
[![GitHub tag](https://img.shields.io/github/tag/wooga/atlas-version.svg?style=flat-square)]()
[![GitHub release](https://img.shields.io/github/release/wooga/atlas-version.svg?style=flat-square)]()

A [gradle] plugin to generate semver version from the current git repository.

# Applying the plugin

**build.gradle**
```groovy
plugins {
    id 'net.wooga.version' version '0.1.0'
}
```

Documentation
=============

- [API docs](https://wooga.github.io/atlas-version/docs/api/)

Gradle and Java Compatibility
=============================

Built with OpenJDK8

| Gradle Version  | Works  |
| :-------------: | :----: |
| < 5.0           | ![no]  |
| 5.0             | ![no]  |
| 5.1             | ![yes] |
| 5.2             | ![yes] |
| 5.3             | ![yes] |
| 5.4             | ![yes] |
| 5.5             | ![yes] |
| 5.6             | ![yes] |
| 5.6             | ![yes] |
| 6.0             | ![yes] |
| 6.1             | ![yes] |
| 6.2             | ![yes] |
| 6.3             | ![yes] |
| 6.4             | ![yes] |
| 6.5             | ![yes] |
| 6.6             | ![yes] |
| 6.6             | ![yes] |
| 6.7             | ![yes] |
| 6.8             | ![yes] |
| 7.0             | ![yes] |

Development
===========

[Code of Conduct](docs/Code-of-conduct.md)

LICENSE
=======

Copyright 2020 Wooga GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

<!-- Links -->
[gradle]:               https://gradle.org/ "Gradle"

[yes]:                  https://resources.atlas.wooga.com/icons/icon_check.svg "yes"
[no]:                   https://resources.atlas.wooga.com/icons/icon_uncheck.svg "no"
