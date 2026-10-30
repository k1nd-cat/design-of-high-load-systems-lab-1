FROM eclipse-temurin:21-jre-noble

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fL --show-error --connect-timeout 20 --max-time 900 --retry 3 \
    https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.tgz \
    -o /tmp/jmeter.tgz \
    && tar -xzf /tmp/jmeter.tgz -C /opt \
    && rm /tmp/jmeter.tgz

WORKDIR /work

COPY load.jmx /work/load.jmx

ENTRYPOINT ["/opt/apache-jmeter-5.6.3/bin/jmeter"]
