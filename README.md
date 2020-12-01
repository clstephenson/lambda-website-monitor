# lambda-website-monitor

AWS Lambda function to check if a webserver is reachable. 

## Purpose
I created this small project to try and retrieve a test page from a webserver, and notify me if that page is not available.

## Description
This is deployed as an AWS Lambda function. It runs on a schedule (e.g. every 15 minutes) and attempts to perform a GET request for a specific test URL. 

It publishes a custom metric to CloudWatch to track the website status over time. A CloudWatch alarm is configured to publish a message to an Simple Notification Service topic if the website can't be reached (based on the latest value of the metric). 

Subscribers receive an email and text message if the alarm is triggered.


![System Architecture Diagram](https://clstephenson.s3-us-west-2.amazonaws.com/apps/lambda-website-monitor/WebsiteMonitorDiagram.png "System Architecture Diagram")
