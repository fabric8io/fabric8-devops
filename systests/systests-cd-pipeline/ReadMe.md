## System Tests

This runs the system tests for Fabric8 DevOps in a new kubernetes namespace.

### Requirements

This test case requires a recent installation of [gofabric8 installer](https://github.com/fabric8io/gofabric8/) to be on your **PATH** environment variable.

### How to run

To run:

    mvn clean install
    
Or to use a defined namespace name use:
    
    mvn clean install -DuseExistingNamespace=mysystest
    