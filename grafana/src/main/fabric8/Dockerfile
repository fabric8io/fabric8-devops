FROM debian:jessie

ARG GRAFANA_VERSION

RUN apt-get update && \
    apt-get -y --no-install-recommends install libfontconfig curl ca-certificates && \
    apt-get clean && \
    curl https://grafanarel.s3.amazonaws.com/builds/grafana_${GRAFANA_VERSION}_amd64.deb > /tmp/grafana.deb && \
    dpkg -i /tmp/grafana.deb && \
    rm /tmp/grafana.deb && \
    apt-get remove -y curl && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*

ENV GF_AUTH_ANONYMOUS_ENABLED=true \
    GF_DASHBOARDS_JSON_PATH=/dashboards \
    GF_PATHS_DATA=/var/lib/grafana \
    PROMETHEUS_ADDRESS=http://prometheus \
    GF_LOG_MODE=console \
    GF_AUTH_ANONYMOUS_ORG_ROLE=Admin

COPY maven /

RUN chmod 777 /etc/grafana /var/lib/grafana /var/log/grafana /run.sh && \
    chmod 666 /etc/grafana/*

VOLUME ["/var/lib/grafana", "/var/lib/grafana/plugins", "/var/log/grafana", "/etc/grafana"]

EXPOSE 3000

ENTRYPOINT ["/run.sh"]
