Java development

You can ssh into the pod using..

    kubectl exec -ti $(kubectl get pods | grep java-developer | cut -f 1 -d ' ') bash

You might also want to pin the pod to a single node so that the mounted workspace host volume is reused if the pod is recreated.  See the [readme.md](https://github.com/fabric8io/fabric8-devops/blob/master/java-developer/ReadMe.md) for help doing this.
