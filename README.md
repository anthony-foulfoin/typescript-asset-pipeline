# Typescript Asset Pipeline

Overview
-----------
The `Typescript-Asset-Pipeline` module provides Typescript compilation support for the jvm based asset-pipeline. Simply add this file to your buildscript classpath or development environment and they are automatically processed.

For more information on how to use asset-pipeline, visit [here](http://www.github.com/bertramdev/asset-pipeline).

Prerequisite
-----------
You need to install [Typescript](http://www.typescriptlang.org) at first. 

The plugin will look for a Typescript executable in these paths : 
* `./node_modules/.bin/tsc` if Linux or OSX
* `./node_modules/.bin/tsc.cmd` if Windows
* The default system `tsc` command if the npm one cannot be found

Note that `./` designates the root path of your build.gradle file

Installation
------------
Add this plugin to your buildscript and dependencies list :

```gradle
//Example build.gradle file
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.bertramlabs.plugins:asset-pipeline-gradle:2.6.9'
        classpath 'anthofo.plugins:typescript-asset-pipeline:1.0'
    }
}
dependencies {
    compile 'com.bertramlabs.plugins:asset-pipeline-spring-boot:2.6.4' // Here I'm using asset pipeline with spring boot
    compile 'anthofo.plugins:typescript-asset-pipeline:1.0'
}
```

Usage
-----

Create files in your standard `assets` folder with extension `.ts`. You can put them in subfolders, for instance `assets/ts/`

```ts
// assets/ts/Student.ts
class Student {
    fullname : string;
    constructor(public firstname, public middleinitial, public lastname) {
        this.fullname = firstname + " " + middleinitial + " " + lastname;
    }
}
```

You can then include or "require" your ts files or folders in your standard `assets/javascripts`. See [asset-pipeline](http://www.github.com/bertramdev/asset-pipeline) documentation.

```js
// assets/javascripts/main.js
/*
*= require ts/Student
*= require_full_tree ts/other/
*/
```

The `main.js` will be the file you will include in your html. See [asset-pipeline](http://www.github.com/bertramdev/asset-pipeline) documentation.

Production
----------
During war build your Typescript files are compiled into js files. You can of course still use the excludes asset-pipeline option if you don't want to compile and include some .ts files in your war

Sample Gradle Config:
```gradle
  assets {
      excludes = ['**/*.d.ts']
  }
```
