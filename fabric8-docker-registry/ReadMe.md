# Private Docker Registry

Hosted private [docker registry](https://github.com/docker/distribution)

This project will create a private docker registry and service.  When run on OpenShift with fabric8 a route will be created note the port is required in the image name.

# Usage

```
docker build --rm -t fabric8-docker-registry.vagrant.f8:80/myorg/example-image .

docker push fabric8-docker-registry.vagrant.f8:80/myorg/example-image
```
