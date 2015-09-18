## System Tests

This runs the system tests for Fabric8 DevOps in a new kubernetes namespace.

To be able to run the system tests you need to setup a namespace first using the [gofabric8 installer](https://github.com/fabric8io/gofabric8/):

    oc new-project myitest
    oc project myitests
    gofabric8 deploy -y --console=false --templates=false

To run:

    mvn clean install -DuseExistingNamespace=myitest
