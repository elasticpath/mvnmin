# Overview

Mvnminimal speeds up large multi-module maven builds by only building what's changed. 

# Setup

You can either install from a `homebrew` tap, a zip binary, or from source.

## Install via `homebrew`

We have a private `homebrew` tap that allows internal EP developers to install and upgrade mvnmin simply.
This also allows us to practise the release process for a public launch.

1. Create a Personal Access Token  
   Our internal Github requires `homebrew` to provide a Personal Access Token so it can download the `mvnmin` formula.  
   Thankfully this is temporary, until we make `mvnmin` public.  
   1. This guide shows how to create a token: https://docs.github.com/en/github/authenticating-to-github/creating-a-personal-access-token
   1. Grant the `public_repo` permission to the token. 
   1. Record the value of this token in a `~/.netrc` file
      ```
      machine github.elasticpath.net
      login <usename>
      password <token>  
      ```

2. Tap the formula  
The config from the previous step will allow you now to tap the formula
   ```
   brew tap ijensen/homebrew-mvnmin https://github.elasticpath.net/ijensen/homebrew-mvnmin
   ```

3. Install  
   Including the value of the token from the previous step allows us to actually install `mvnmin`.
   ```
   HOMEBREW_GITHUB_API_TOKEN=<token> brew install mvnmin
   ``` 

   If all goes well you should then be able to run `mvnmin`
   ```
   mvnmin --help
   ```

4. To upgrade in the future  
   You will need to provide the token again.
   ```
   HOMEBREW_GITHUB_API_TOKEN=<token> brew upgrade mvnmin 
   ```

## Download a binary of mvnmin

Check for a binary release here: https://github.elasticpath.net/commerce/mvnmin/releases

Download a `mvnmin-x.x.x.zip` archive and place it into a folder, e.g. `~/tools/mvnmin`

On a *nix running bash (including Mac), add the following to your `~/.bash_profile`:

```
alias mvnmin='java -jar ~/tools/mvnmin/maven-minimal-0.0.5-jar-with-dependencies.jar'
```


## Build Maven Minimal from source

Clone and build the project source as follows:

```
cd ~/git
git clone git@github.elasticpath.net:commerce/mvnmin.git
cd mvnmin
./mvnw clean install
```

On a Linux/Mac running bash, add the following to your `~/.bash_profile`:
```
alias mvnmin='java -jar ~/git/mavenminimal/target/maven-minimal-0.0.5-SNAPSHOT-jar-with-dependencies.jar'
```

# Running mvnmin

## Command Line Options

usage: mvmin [options] [<maven goal(s)>] [<maven phase(s)>] [<maven arg(s)>]

  Project Activation/Deactivation
    --all                      activate all pom.xml file in all sub directories
                               (default max depth: 6)
    --diff[=commit[..commit]]  activate all projects changed since the specified commit,
                               or range of specified commits.
                               (default: 'master')
    -pl,--projects <arg>       Comma-delimited list of specified reactor projects
                               to build as well as those otherwise activated.
                               A project can be specified by groupId:artifactId
                               A project can be deactivated by leading with an
                               exclamation mark or hyphen: -groupId:artifactId
    --nbi                      No build-if dependencies are considered, just
                               changed modules

  Scripting
    -p                         don't invoke maven, print out activated projects,
                               sorted, newline separated.

  Debug
    -d --dry-run               don't invoke maven, print out the commands that
                               would have been executed
       --version               print the version number of mvnmin and exit


The default mode (when nothing is specified) is to build the projects with dirty files.  According to `git status`.

`--all` activates all projects from the current directory down.  `mvnmin` pretends that all found pom.xml
        files are dirty, activating all projects.  Only six levels of directories are considered.
        This can be changed, see the Environment Variable section below.  This overrides the default option, as
        all projects are enabled.

`--diff[=commit[..commit]]` identifies all files changed by specified commit, or between the commit range and 
                            activates all containing projects.  This option works in addition to the default behaviour.
                            Adding the committed changes to the currently dirty files, and activating all affected projects.
                            If no commit is specified, the value `master..` is used.

`-p` is useful for piping into other tools

`--dry-run` prints out the list of projects determined to have changed, sorted, newline separated.

`--help` show usage information.
             
None of the `mvnmin` options are passed down to maven directly.  Also, mvnmin's options have been selected so as not to clash with any current maven options.

Almost all maven phases, goals and args provided are passed through to the underlying maven invocation.

There a a few exceptions:
* `-T` the number of threads argument, is passed through to maven, unless the a synthetic reactor has the `single-thread` attribute set to `true`
* `-pl` the project list argument is hijacked by `mvnmin`, and the processed version of `-pl` is passed to maven instead. 
		     
## Environment Variables

The following environment variables alter `mvnmin`s behaviour.

 `DEBUG=true` enables debug outupt. Disabled by default
 
 `MVNMIN_MAXDEPTH=<int>` limits the levels of directories considered.  Default is 6.

## Configuring mvnmin

mvnmin has an optional configuration file, `mvnmin.xml`.
This file allows you to:
* define module that should be ignored
* define build-if modules
* rewire a larger reactor into smaller ones, with a different build flow

### mvnmin.xml format

```
<?xml version="1.0" encoding="UTF-8" ?>

<mvnmin>

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
	    name=<reactor name>                     - an arbitrary name for a Reactor, used during mvnmin output
	    pom=<relative path to poml.xml>         - path the pom.xml which the maven build will be started with
	    single-thread=<true|false>              - force a reactor to run single-threaded, regardless of command line arguments
	    skip-if=<cmd line argument regex>       - a regex that will cause the reactor to be skipped, if present on the command line
	    pattern                                 - a regex to match module names for this reactor (see below)
	-->
	<reactors>
		<reactor name="" pom="" single-thread="" skip-if="">
			<pattern></pattern>
		</reactor>
	<reactors>

</mvnmin>
```
