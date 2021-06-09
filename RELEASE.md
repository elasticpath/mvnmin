# Creating a mvnmin release
This section provides instruction on how to create and new `mvnmin` release.

NOTE: This guide assumes you are somewhat familiar with the maven-release-plugin and its typical flow: https://maven.apache.org/guides/mini/guide-releasing.html

## Pre-conditions:
The following must be true before starting this process:
* You have no outstanding changes in your local repository
* All changes have been pushed remotely
* You are authenticated with the remote git repository and able to push changes

## Process
1. Invoke the command below, and answer the questions regarding versions, typically the defaults can be accepted.This will update the pom with a new version number and create a tag in git.
    ````
    mvn release:prepare  
    ````

1. Now create the release version.  We skip deployment as we don't have a place to automatically deploy binaries, yet.
    ````
    mvn release:perform -Darguments="-Dmaven.deploy.skip=true  -Dmaven.site.skip=true -Dmaven.site.deploy.skip=true"
    ````

1. Visit the github releases page :  
    1. Follow this link https://github.elasticpath.net/commerce/mvnmin/releases
    1. Press the `Draft a new release` button
    1. Select the tag you just created above for the `Tag version`
    1. Use the tag name for the `Release title` field
    1. Add release notes as needed
    1. Upload the zip binary from the previous step.  You will find the jar in the project's `target/checkout/target/` folder.
    1. Tick the `This is a pre-release` checkbox (until we hit version 1.0.0)
    1. Check your work, and then hit `Publish release`
    
    note: it is possible to edit a release after publishing.

