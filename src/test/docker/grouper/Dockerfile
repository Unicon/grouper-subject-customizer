FROM unicon/grouper-demo:2.3.0

MAINTAINER jgasper@unicon.net

COPY /container-scripts /opt/container-scripts/
RUN chmod +x /opt/container-scripts/run-grouper.sh

COPY /seed-data/provisioningTargetAttributes.gsh /

RUN set -x; \
    rm -rf /opt/apache-tomcat-6.0.44/work/ \
    && cd /opt/grouper.apiBinary-2.3.0/ \
    && service mysql start \
    && service slapd start \
    && bin/gsh /provisioningTargetAttributes.gsh

COPY conf/ /opt/grouper.apiBinary-2.3.0/conf/
COPY lib/ /opt/grouper.apiBinary-2.3.0/lib/custom/

COPY lib/ /opt/apache-tomcat-6.0.44/webapps/grouper/WEB-INF/lib/
COPY conf/ /opt/apache-tomcat-6.0.44/webapps/grouper/WEB-INF/classes/

CMD ["run-grouper.sh"]




