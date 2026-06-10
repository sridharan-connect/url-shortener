# URL Shortener

A Spring Boot based URL Shortener service supporting URL generation, redirection, expiry validation, Redis caching, and asynchronous analytics processing.

## Features

* Create Short URL
* Redirect to Original URL
* Expiry Validation
* Redis Cache Support
* Analytics Tracking
* Background Analytics Processing using BlockingQueue

## Tech Stack

* Java 17
* Spring Boot
* PostgreSQL
* Redis
* Maven

## High Level Flow

Redirect Request

↓ Check Redis Cache

↓ Fetch from Database (Cache Miss)

↓ Validate Expiry

↓ Update Cache

↓ Publish Analytics Event to Queue

↓ Background Worker Processes Analytics

## Project Structure

controller → REST APIs

service → Business Logic

repository → Database Access

dto → Request/Response Objects

model → Entity Models

## Learning Objectives

This project was built to understand caching strategies, asynchronous processing, URL redirection workflows, analytics collection, and scalable backend design patterns.

