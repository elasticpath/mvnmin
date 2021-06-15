# Overview

`mvnmin` speeds up large multi-module maven builds, building only the changed modules. 

`mvnmin` has the following benefits: 

 * Detects and builds only changed modules, thus speeding up builds.
 * Easily includes or excludes modules. 
 * Builds basic dependent modules, building only what you need and restarts your application.
 * Allows finer-grained control over `maven-invoker-based` sub-projects. 
 
# Installation

The following section describes how to install and build `mvnmin`.

## Binary Installation

1. Go to [`mvnmin` releases](https://github.com/elasticpath/mvnmin/releases) and check for currently available releases.
1. Download the required zip file and place it into a folder, such as `~/tools/mvnmin`.
1. Extract the downloaded file:

    ```
    unzip maven-minimal-1.0.0.zip
    ```

1. On a `*nix` running bash, including Mac, create an alias in your terminal. 

    **Note**: This can also be made for permanent use, by adding it to your `~/.bash_profile`.

    ```
    alias mvnmin='java -jar ~/tools/mvnmin/mvnmin-1.0.0-jar-with-dependencies.jar'
    ```

1. Execute `mvnmin --version` and you should see the following output: 
   
    ```
   mvnmin 1.0.0
    ```

## Build from source

1. Clone and build the project source as shown in the following example:

    ```
    cd ~/git
    git clone https://github.com/elasticpath/mvnmin.git
    cd mvnmin
    ./mvnw clean install
    ```

1. On a Linux/Mac running bash, add the following to your `~/.bash_profile`:

    ```
    alias mvnmin='java -jar ~/git/mvnmin/target/mvnmin-1.0.0-SNAPSHOT-jar-with-dependencies.jar'
    ```

# Usage and Examples

The following section describes the different usage and examples for `mvnmin`.

## Command Line Options

```
usage: mvmin [options] [<maven goal(s)>] [<maven phase(s)>] [<maven arg(s)>]

  Project Activation/Deactivation
    --all                      Activate all `pom.xml` files in all sub directories
                               (default max depth: 6)
    --diff[=commit[..commit]]  Activate all projects changed since the specified commit,
                               or range of specified commits.
                               (default: 'master')
    -pl,--projects <arg>       Comma-delimited list of specified reactor projects
                               to build as well as those otherwise activated.
                               A project can be specified by `groupId:artifactId`
                               A project can be deactivated by leading with an
                               exclamation mark or hyphen: `-groupId:artifactId`
    --nbi                      No build-if dependencies are considered, just
                               changed modules.

  Scripting
    -p                         Don't invoke maven, print out activated projects,
                               sorted, newline separated.

  Debug
    -d --dry-run               Don't invoke maven, print out the commands that
                               would have been executed.
       --version               Print the version number of mvnmin and exit.

```

The default mode, when nothing is specified, is to build the projects with dirty files, according to `git status`.

`--all` Activates all projects from the current directory down.  `mvnmin` pretends that all found `pom.xml`
        files are dirty, activating all projects.  Only six levels of directories are considered.
        This can be changed, see the [Environment Variable](#environment-variables) section.  This overrides the default option, as
        all projects are enabled.

`--diff[=commit[..commit]]` Identifies all files changed by specified commit, or between the commit range and 
                            activates all containing projects.  This option works in addition to the default behaviour.
                            Adding the committed changes to the currently dirty files, and activating all affected projects.
                            If no commit is specified, the value `master..` is used.

`-p` Useful for piping into other tools.

`--dry-run` Prints out the list of projects determined to have changed, sorted, newline separated.

`--help` Shows usage information.
             
None of the `mvnmin` options are passed down to maven directly.  Also, `mvnmin`'s options are selected to not clash with any current maven options.

Almost all maven phases, goals and arguments provided are passed through to the underlying maven invocation.

There are a few exceptions:

* `-T` Is the number of threads argument and is passed through to maven, unless the a synthetic reactor has the `single-thread` attribute set to `true`.
* `-pl` Is the project list argument is hijacked by `mvnmin`, and the processed version of `-pl` is passed to maven instead. 
* `-f`/`--files` This option is reserved by `mvnmin` and must not be provided.  `mvnmin` exits with an error message if you provide it.		     

## Environment Variables

The following environment variables alter `mvnmin`s behaviour:

- `DEBUG=true`: Enables debug outupt. This variable is disabled by default.
 
- `MVNMIN_MAXDEPTH=<int>`: Limits the levels of directories considered. The default is 6.

- `MVN_COMMAND=<mvn alternate>`: The `mvn` command `mvnmin` should invoke.

## Basic Examples

- `mvnmin` can print the list of the currently changed modules to the terminal:

  ```
  mvnmin -p
  ``` 

- It can also invoke maven on just those changed modules:

  ```
  mvnmin clean install
  mvnmin pmd:check checkstyle:check
  ```

This can be combined with `-am` and `-amd`, allowing maven to build only those pieces needed, thus saving time:

```
mvnmin clean install -amd -am
``` 

- `mvnmin` can print all modules to the terminal. It does this by looking for all the `pom.xml`
files, whether they are associated with a reactor or not:

  ```
  mvnmin -p --all
  ```

- `mvnmin` can run maven goals against all projects:

  ```
  mvnmin --all pmd:check checkstyle:check
  ```

- Combine `mvnmin`'s ability to list all the modules with the excellent `fzf` command, the command-line fuzzy-finder, provides a simple way to explore and select your modules:

  ```
  mvnmin -p --all | fzf          # single-select mode
  mvnmin -p --all | fzf -m       # select multiple modules
  ```

- Count the number of *-itest modules you have:

  ```
  mvnmin -p --all | grep '\-itest' | wc -l
  ```

- Use `--dry-run` to see what maven command would be executed, without actually invoking maven.  This is very useful when troubleshooting:

  ```
  mvnmin --dry-run clean install pmd:check -T4

  RUN  0 Main reactor : mymvn clean install pmd:check -T4 -f pom.xml --projects com.elasticpath.tools:mvnmin
  ```


## Configuring mvn command mvnmin calls 

`mvnmin` can drive different maven wrapper scripts, allowing it to fit your projects.

`mvnmin` determines which command to issue to invoke a maven build in the following order:

1. The value in the env var `MVN_COMMAND`, if specified.
1. The value specified in the `maven-command` element in `mvnmin.xml`, if specified.
1. The maven wrapper `mvnw` if found in the root project folder.
1. `mvn` if none of the above match.

## Advanced mvnmin configuration

`mvnmin` has an optional configuration file, `mvnmin.xml`, which is in the root module of a multi-module project.

This file allows you to:

* Define which module that should be ignored.
* Define the build-if modules.
* Rewire a larger reactor into smaller ones, with a different build flow.

### `mvnmin.xml` format

```
<?xml version="1.0" encoding="UTF-8" ?>

<mvnmin>

    <!-- Override the maven wrapper script to use. -->
    <maven-command></maven-command>

	<!--
	  These projects will never be included in mvnmin build.
	 -->
	<ignored-modules>
		<module></module>
	</ignored-modules>


	<!--
	  Conditional Build Glue:
	  If a project is activated that matches the module regex, mvnmin will activate the associated projects.
	-->
	<build-ifs>
		<build-if description="">
			<match regex=""/>      <!-- If this matches ... -->
			<module></module>      <!-- ... then build these modules-->
		</build-if>
	</build-ifs>


	<!--
	  Reactor Overrides
	  Define sub-reactors that mvnmin can then target in a finer-grained way, than vanilla maven.

	  The ordering of the reactors is significant, this is the order mvnmin will invoke the reactors.

	  The reactor options are:
	    name=<reactor name>                     - (required) An arbitrary name for a Reactor, used during mvnmin output.
	    pom=<relative path to poml.xml>         - (required) The path the `pom.xml` which the maven build will be started with.
	    pattern                                 - (required, for none primary) A regex to match module names for this reactor (see the following code).
	    primary=<true|false>                    - (optional) If true, this overrides the internal primary reactor's configuration.
	                                              Only one primary is allowed.  The primary reactor's patterns are 
	                                              ignored, as the primary reactor is formed from all unclaimed modules.
	    single-thread=<true|false>              - (optional) Force a reactor to run single-threaded, regardless of command line arguments.
	    skip-if=<cmd line argument regex>       - (optional) A regex that will cause the reactor to be skipped, if present on the command line.
	-->
	<reactors>
		<reactor name="" pom="" primary="" single-thread="" skip-if="">
			<pattern></pattern>
		</reactor>
	<reactors>

</mvnmin>
```


# Contributing to this project

Anyone and everyone is welcome to contribute. Please take a moment to
review the [guidelines for contributing](CONTRIBUTING.md).

* [Bug reports](CONTRIBUTING.md#bug-reports)
* [Feature requests](CONTRIBUTING.md#feature-requests)
* [Pull requests](CONTRIBUTING.md#pull-requests)

# License
[Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

Copyright 2021, Elastic Path Software Inc.
