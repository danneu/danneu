{:title "Setting up Mocha testing with Coffeescript, Node.js, and a Cakefile"
 :permalink "14-setting-up-mocha-testing-with-coffeescript-node-js-and-a-cakefile"}

I'm still in a Javascript (well, Coffeescript) kick. In an attempt to be productive while I train with great discipline, I decided to port Paul Dix's [Domainatrix](https://github.com/pauldix/domainatrix) Ruby gem into a Node module.

I really like the syntax and polish of the [Mocha](http://visionmedia.github.com/mocha/) test framework. It
spoke to me. I also use the [Chai](http://chaijs.com/) assertion module.

## Objectives

* Run all tests with `cake test`
* A `test_helper.coffee` file loaded before my tests to DRY them up
* Hook in to the convetion of also making `npm test` run my tests

## Directory structure

```text
    /
    |-- lib  
    |    +-- (Compiled Javascript)
    |-- src  
    |    +-- (Coffeescript)
    |-- test
    |    |-- test_helper.coffee
    |    +-- (Other tests)
    |-- package.json
    |-- Cakefile
```

Mocha expects a `test/` directory. When you run mocha, it runs all the tests
found there.

## The Cakefile

```javascript
# Cakefile

{exec} = require "child_process"

REPORTER = "min"

task "test", "run tests", ->
  exec "NODE_ENV=test 
    ./node_modules/.bin/mocha 
    --compilers coffee:coffee-script
    --reporter #{REPORTER}
    --require coffee-script 
    --require test/test_helper.coffee
    --colors
  ", (err, output) ->
    throw err if err
    console.log output
```

The breakdown:

* `NODE_ENV=test` forces the test environment. Among other things, it lets us reset assured that tests will use the test database (which will be trashed)
* `--compilers coffee:coffeescript` lets Mocha know it'll be compiling and running .coffee tests
* `--reports #{REPORTER}` sets the test output mode. I like "min" because it clears the screen between tests
* `--require coffee-script` lets us write our test_helper with Coffeescript
* `--require test/test_helper.coffee` loads our test_helper before our tests are run
* `--colors` forces output colors since Mocha disables them when it doesn't think it's printing to a terminal

I tried to add a `test-w` test that would include Mocha's `--watch` and
`--growl` options, but I couldn't get the watcher to actually work.

## The Test Helper

```javascript
# test/test_helper.coffee

global.assert = require("chai").assert
```

Rather than having to declare `assert = require("chai").assert` in every test, I
attach the assert function to the global variable that persists across my tests.

## package.json

Nodejitsu has a helpful [package.json cheatsheet](http://package.json.nodejitsu.com/).

```javascript
{
  ...
  "scripts": {
    "test": "cake test"
  },
  "devDependencies": {
    "chai": "~1.0.3",
    "mocha": "~1.1.0"
  },
  ...
}
```

Note: `npm install --save chai mocha` will automatically add them to the
dependencies hash along with their version number. From there you can move them
into devDependencies.

The `scripts` hash tells Node what commands to run. For example, now `npm test`
simply calls our Cakefile task. It seems to be the convention along with
creating an `npm server` command. Basically, developers that clone/install your
project shouldn't have to hunt for the magical command that happens to run your
test suite and start your server.

But `npm test` adds Node's error output after your test output so I don't use it.

## That's it

I hope to get Mocha's watcher working soon so I don't have to manually run my
tests. I'll update this post if I figure it out.
