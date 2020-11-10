FROM maven:3.6.0-jdk-8-alpine AS MAVEN_TOOL_CHAIN
#Copy over the project files and run maven package
WORKDIR /tmp
COPY pom.xml /tmp/
COPY src /tmp/src/
RUN mvn package

#########################

FROM openjdk:8-jdk-alpine as layerbuilder
WORKDIR /user/app
#Copy over the fat-jar file into the /user/app directory
COPY --from=MAVEN_TOOL_CHAIN /tmp/target/*-executable.jar app.jar
#ARG JAR_FILE=target/*-executable.jar
#COPY ${JAR_FILE} app.jar
#And extract the layers
RUN java -Djarmode=layertools -jar app.jar extract

#########################

FROM openjdk:8-jdk-alpine
WORKDIR /user/app
#Copy over the extracted layers from layer-builder step
COPY --from=layerbuilder /user/app/application/ ./
COPY --from=layerbuilder /user/app/dependencies/ ./
COPY --from=layerbuilder /user/app/snapshot-dependencies/ ./
COPY --from=layerbuilder /user/app/spring-boot-loader/ ./
#Allow debuggin
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
#Expose the ports for REST and DEBUG
EXPOSE 8080
EXPOSE 5005
#Labels
LABEL component="FIX-CLIENT"
#LABEL artifact="FIX-CLIENT"
#LABEL version="FIX-CLIENT"
#the application entry point
ENTRYPOINT ["java","org.springframework.boot.loader.JarLauncher"]

#Health check by pinging the actuator health url
HEALTHCHECK  --start-period=20s CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1