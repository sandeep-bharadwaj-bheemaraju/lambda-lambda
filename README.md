
# lambda-lambda  
  
*lambda-lambda* is a lambda piece of work which helps in data migration from On-Prem to RDS and provides an easy-and-fastway to migrate your data to AWS Cloud applications.  
  
Currently, the focus is primarily on migrating data using CSV or Delimited file.  
  
# Overview  
  
*lambda-lambda* is a part of Lambda-Glue Architecture for data migration solution  
  
![lambda-lambda](https://github.com/sandeep-bharadwaj-bheemaraju/lambda-lambda/blob/master/dashboard/web/img/lambda-lambda.png)  
  
# Solution Design  
  
To migrate the data from legacy system to AWS database, *lambda-lambda* inspects if new csv files are available for processing and triggers glue job if required. This lambda does below task.  
  
* **lambda-lambda shall read the configuration from Dynamo DB and moves the files from ready directory to in-process directory.**  
* **Triggers AWS Glue Job.**  
* **Persists the received job id, file names and job status to Dynamo DB.**
  
## Requirements  
  
* `java`/`javac` (Java 8 runtime environment and compiler)  
* `gradle` (The build system for Java)  
  
Step by Step installation procedure is clearly explained in the **Lambda Glue Architecture for Batch Processing.pdf** file in the repository.