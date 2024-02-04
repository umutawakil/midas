#!/bin/bash

echo "Setting aws credentials"
aws configure set region us-east-1
aws configure set aws_access_key_id $accessKey
aws configure set aws_secret_access_key $secretKey

echo "Getting environment script from s3.."
aws s3 cp s3://$bucket ./ --recursive

echo "Attempting to run application script..."
sh run-app.sh