## System Tests

This runs the system tests for Fabric8 DevOps in a new kubernetes namespace.

### Requirements

This test case requires the following binaries to be installed on your **PATH**

* [gofabric8 installer](https://github.com/fabric8io/gofabric8/) 
* [oc](https://github.com/openshift/origin/releases) command line tool for OpenShift
* [chrome driver](https://code.google.com/p/selenium/wiki/ChromeDriver)

and [Google Chrome](https://www.google.com/chrome/browser/desktop/) browser to be installed

### How to run

To run:

    mvn clean install
    
Or to use a defined namespace name use:
    
    mvn clean install -DuseExistingNamespace=mysystest
    