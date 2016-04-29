Maven Shell is an app that contains java build tools and allows development inside kubernetes

You can ssh into the pod using..

    kubectl exec -ti $(kubectl get pods | grep maven-shell | cut -f 1 -d ' ') bash


You might also want to pin the pod to a single node so that the mounted workspace host volume is reused if the pod is recreated.

To do this add a label to you nodes and edit the resource to include a node selector..


    kubectl label nodes kubernetes-node-5 type=developer
    kubectl edit rc maven-shell


add node selector to spec..


    nodeSelector:
      type: developer
