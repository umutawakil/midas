#!/bin/bash

#This script pushes the needed application artifacts to the given environment. That includes static assets like js
#the application itself, a redirect node app, configuration files, and a script that sets environment variables
#for the application. By the time you read this a lot of that may change so use this file to work backwards
# and update this documentation to be up to date.

set -e

environment=$1
localProjectFolder=$2

deploymentBucket="midas-deployment-${environment}"
preDeploymentBucket="midas-pre-deployment-${environment}"

#Move static secrets and environment config files to deployment bucket. The preDeploymentBucket is populated manually.
# And its contents are not meant to change across deployments, such as password files and configurations not under source control.
aws s3 cp s3://${preDeploymentBucket} s3://${deploymentBucket} --recursive

#build application
${localProjectFolder}/gradlew build -x test

#push the key deployment files to the deployment bucket. The EC2 instance the app will run on will initialize itself
# by pulling the artifacts below onto itself, moving them to the propert locations, and running them as needed.
aws s3 cp ${localProjectFolder}/build/libs/*SNAPSHOT.jar s3://${deploymentBucket}/app.jar
#aws s3 cp ${localProjectFolder}/src/main/resources/redirect/index.js s3://${deploymentBucket}/index.js
aws s3 cp ${localProjectFolder}/src/main/resources/app.conf s3://${deploymentBucket}/app.conf
aws s3 cp ${localProjectFolder}/src/main/resources/app.service s3://${deploymentBucket}/app.service

echo "Artifacts deployed. Ready for application stack create/update command"