FROM amazon/aws-cli

#FROM amazoncorretto:17

WORKDIR /

#Add the application
COPY build/libs/midas-1.0-SNAPSHOT.jar app.jar

#Add script that will be run every time container runs
COPY infrastructure/docker/entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

#Install Java
RUN yum install -y java-17-amazon-corretto
#COPY /Users/umutawakil/Documents/midas-externalized/deps/jdk-21_linux-x64_bin.tar.gz jdk-21_linux-x64_bin.tar.gz
#RUN tar -xvzf jdk-21_linux-x64_bin.tar.gz
#RUN ln -s /jdk-21.0.2/bin/java java

#This script will run on every launch of the container and is the only command at the time of this writing that will
ENTRYPOINT sh entrypoint.sh
