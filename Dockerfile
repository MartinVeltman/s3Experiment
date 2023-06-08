FROM openjdk:21-slim
ARG JAR_FILE=ObjectStorage/target/*.jar
COPY ${JAR_FILE} /opt/service.jar
COPY run.sh /opt/run.sh
RUN chmod +x /opt/run.sh

# to start a docker container, use docker run /opt/run.sh <port>
USER 1000:1000
CMD /opt/run.sh