# Section A - Design Brief - Medical Register Application

## 1. Introduction

This document outlines the assumptions, integration points, and future considerations for the "Medical Register" application. The primary purpose of this application is to provide a simple, secure platform for performing CRUD (Create, Read, Update, Delete) operations on medical records.

The backend is developed using Spring Boot 3, leveraging its robust features for web development, data persistence, and security. The frontend is rendered using XHTML, presumably with Thymeleaf for server-side templating, providing a straightforward user interface. User authentication is a critical aspect, handled by integration with Auth0, which is assumed to be connected to an LDAP directory for identity management.

---

## 2. Core Functionality & Features

The application delivers the following core functionalities:

- **Medical Record Management (CRUD):**
  - **Create:** Allows authenticated users to add new medical records with fields for Name, Age, and Medical History.
  - **Read:** Allows authenticated users to view lists of medical records and details of individual records.
  - **Update:** Allows authenticated users to modify existing medical records.
  - **Delete:** Allows authenticated users to remove medical records.
- **User Authentication (Auth0 with LDAP backend):**
  - Secure login mechanism provided by Auth0, which is integrated with an LDAP directory.
  - Session management handled by Spring Security.
  - Secure logout, including RP-Initiated Logout to terminate the Auth0 session.
- **Frontend:**
  - An XHTML frontend rendered server-side using Spring Boot with Thymeleaf.
- **Logging:**
  - Standard application logging is implemented using SLF4J and Logback (provided by Spring Boot), with configurable log levels for different environments (e.g., DEBUG for development, INFO/WARN for production).
- **Automated Testing Suite:**
  - Incorporates a testing suite leveraging Spring Boot Test, JUnit, and Mockito for unit/integration tests.
  - Selenium with WebDriverManager is included for end-to-end testing capabilities.
  - Maven Surefire plugin is configured to execute tests during the build lifecycle.
- **CI/CD Pipeline:**
  - An automated CI/CD pipeline is implemented using AWS CodePipeline, AWS CodeBuild, and deployment to Amazon ECS.

---

## 3. Assumptions Made During Development

Several assumptions were made to guide the development process:

- **Data Model Simplicity:** The medical record entity is assumed to have a basic structure (Name, Age, Medical History, plus audit fields like Created At, Updated At, Created By, and Updated By). Specific data validation rules (e.g., age >= 0) are implemented, but complex medical ontologies or relationships are out of scope for this initial version.
- **User Roles & Permissions:** It's assumed that any user successfully authenticated via Auth0 has full CRUD permissions on all medical records. A more granular role-based access control (RBAC) system is not implemented in this initial version.
- **Auth0 and LDAP Integration:** The core assumption is that Auth0 is the primary identity provider for the application. Auth0, in turn, is assumed to be configured to integrate with an existing LDAP directory (e.g., using an Auth0 LDAP connector or federated identity). The `medical-register` application interacts only with Auth0 via OAuth 2.0/OIDC and does not have direct knowledge of or connectivity to the LDAP server. User attributes from LDAP are expected to be available in the Auth0 user profile.
- **Frontend Technology:** The requirement for an "xhtml frontend" implies server-side rendering. Spring Boot with Thymeleaf is the natural fit for this, generating XHTML compliant markup. The UI is assumed to be functional and straightforward, prioritizing core CRUD operations over complex user experience patterns.
- **Database Technology:**
  - For local development and initial testing (including local Kubernetes), H2 file-based database is used.
  - It's explicitly understood that this H2 setup is **not suitable for persistent production data** in containerized environments like ECS or Kubernetes due to the ephemeral nature of container storage. A migration to a managed relational database (e.g., PostgreSQL on RDS) is a key consideration for a true production deployment.
- **Deployment Environment & CI/CD:** The application is designed to be containerized using Docker and deployed via a CI/CD pipeline (AWS CodePipeline) to AWS services (initially ECS, potentially EKS later). Local Kubernetes deployment is for testing and development.
- **Security Focus:** The primary security focus is on user authentication through Auth0. Spring Security provides foundational web security features (like CSRF protection). Data-level security beyond basic authentication is not deeply implemented.
- **Error Handling:** Standard Spring Boot error handling mechanisms are assumed to be in place (e.g., default error pages, potential for custom error controllers).
- **Logging:** Basic logging is implemented via SLF4J (implicitly through Spring Boot starters). Audit logging is an optional enhancement.
- **API Design:** The application is primarily a server-side rendered web application. While Spring MVC controllers exist, a formal, versioned REST API for external consumption is not an initial requirement.
- **Stateless Application Design:** The application is designed to be stateless where possible to facilitate horizontal scaling in containerized environments. Session state necessary for the OAuth2 flow is managed by Spring Security and relies on mechanisms like sticky sessions at the load balancer level when multiple instances are run.

---

## 4. Integration Considerations

- **Auth0 Integration:**
  - **Protocol:** OAuth 2.0 Authorization Code Grant flow and OpenID Connect (OIDC) for identity information, facilitated by `spring-boot-starter-oauth2-client`.
  - **Configuration:**
    - Client ID, Client Secret, and Issuer URI are managed externally through environment variables.
    - For local development, these are provided via the uncommitted `application-local.properties`.
    - For Kubernetes, these are injected as environment variables in the deployment manifest.
  - **Callback URLs:** The `redirect-uri` is dynamically constructed using `{baseUrl}`. The `APP_BASE_URL` environment variable must be correctly set for each deployment environment (local, CloudFront URL for AWS) and these full callback URLs must be whitelisted in the Auth0 application settings.
  - **Logout:** RP-Initiated Logout is implemented to ensure the user's session is terminated both in the application and in Auth0.
  - **Scopes:** `openid`, `profile`, `email` scopes are requested to retrieve basic user information.
- **Database Integration:**
  - **ORM:** Spring Data JPA with Hibernate.
  - **Connection Pooling:** HikariCP is used by default with Spring Boot.
  - **Schema Management:** `spring.jpa.hibernate.ddl-auto` is set to `update`. This is acceptable for H2 during development but should be changed to `validate` or `none` for a production database, with schema migrations handled by tools like Flyway or Liquibase.
- **Frontend-Backend (Server-Side Rendering):**
  - Thymeleaf templates are used to render XHTML pages.
  - Spring MVC controllers handle web requests and populate models for Thymeleaf.
  - Form submissions are used for CRUD operations.
- **CI/CD Pipeline (AWS CodePipeline):**
  - **Source:** GitHub, with webhooks triggering the pipeline.
  - **Build:** AWS CodeBuild executes commands defined in `buildspec.yml`. This includes:
    - Maven build (`mvn clean verify`).
    - JaCoCo code coverage report generation.
    - Docker image construction using `Dockerfile`.
    - Pushing the Docker image to Amazon ECR.
    - Generating `imagedefinitions.json` for ECS deployment.
  - **Deployment:** AWS CodePipeline deploys to Amazon ECS using the `imagedefinitions.json`. For Kubernetes (EKS), the `appspec-prod.yaml` or `appspec-staging.yaml` would be used with CodeDeploy for Kubernetes.
- **Containerization (Docker):**
  - A multi-stage `Dockerfile` is used to create an optimized runtime image.
  - The `SPRING_PROFILES_ACTIVE=prod` environment variable is set in the Dockerfile.
- **Local Kubernetes (Docker Desktop):**
  - `deployment.yaml` defines how the application runs (replicas, image, environment variables, probes, resource requests/limits).
  - `service.yaml` exposes the application via a `LoadBalancer` service, making it accessible on `localhost`.
- **Environment Configuration Management:**
  - Environment-specific configurations are managed through environment variables injected into containers. Spring Profiles (`local`, `prod`) activate `application.properties` and `application-prod.properties` property sets respectively.
- **Security Group Configuration:**
  - Properly configured security groups are essential for controlling traffic between CloudFront, ALB, ECS tasks, and any future database instances (e.g., RDS).

---

## 5. Future Considerations for Expansion

**5.1. Application Enhancement**

- **Enhanced User Roles and Permissions:**
  - Implement distinct roles (e.g., "PATIENT", "DOCTOR", "ADMINISTRATOR").
  - Use Spring Security's method security or fine-grained request matchers to control access to specific CRUD operations or data based on roles.
  - Potentially integrate Auth0 roles and permissions.
- **Production-Grade Database & Schema Migration:**
  - Migrate from H2 to a managed relational database service (e.g., Amazon RDS PostgreSQL or MySQL).
  - Implement robust schema migration tools like Flyway or Liquibase instead of relying on `hibernate.ddl-auto=update`.
- **Advanced Audit Logging:**
  - Implement a dedicated audit log service or table.
  - Log all data modifications (who, what, when), significant read access, and administrative actions.
  - Ensure audit logs are tamper-proof and retained according to policy.
- **Data Security and Compliance:**
  - **Encryption at Rest:** Ensure database encryption is enabled (standard with most RDS configurations).
  - **Encryption in Transit:** Enforce HTTPS for all external communication (CloudFront, ALB).
  - If handling highly sensitive data, consider field-level encryption for specific medical history details.
  - Address specific healthcare compliance standards (e.g., HIPAA, GDPR) if the application's scope expands to require it. This would involve significant architectural and procedural changes.
- **RESTful API Development:**
  - Expose a versioned REST API for the medical register.
  - Secure the API using OAuth 2.0 (e.g., JWT Bearer Token validation, with Auth0 as the authorization server). This would enable integration with Single Page Applications (SPAs), mobile apps, or other backend services.
- **Scalability and High Availability:**
  - Optimize database queries and indexing.
  - Implement caching strategies for frequently accessed, less volatile data (e.g., using Redis or Memcached).
  - Ensure the application remains stateless to fully leverage horizontal scaling in ECS/Kubernetes.
  - For databases, consider read replicas and multi-AZ deployments.
- **User Interface/User Experience (UI/UX) Enhancements:**
  - Transition from XHTML/Thymeleaf to a modern JavaScript framework (e.g., React, Angular, Vue.js) for a richer, more interactive user experience, consuming the aforementioned REST API.
- **File Attachments:**
  - Allow users to attach relevant medical documents (scans, lab results) to records.
  - Store these files securely in a service like Amazon S3, with appropriate access controls.
- **Advanced Search and Reporting:**
  - Implement more sophisticated search capabilities (e.g., full-text search on medical history).
  - Develop reporting features for data analysis.
- **Asynchronous Operations & Messaging:**
  - For tasks like report generation or notifications, use a message queue (e.g., Amazon SQS, RabbitMQ) and background workers to improve responsiveness.
- **Internationalization (i18n) and Localization (l10n):**
  - Add support for multiple languages and regional data formats if the user base expands.
- **Enhanced Monitoring and Alerting:**
  - Integrate with comprehensive monitoring solutions (e.g., Prometheus/Grafana, Datadog, CloudWatch custom metrics).
  - Set up more granular alerting for application errors, performance degradation, and security events.
- **Robust Secrets Management for Kubernetes:**
  - For production Kubernetes deployments, integrate with solutions like HashiCorp Vault or cloud provider-specific secret stores (e.g., AWS Secrets Manager with the CSI driver for EKS) instead of relying solely on basic Kubernetes Secrets for sensitive data like `AUTH0_CLIENT_SECRET`.

**5.2. CI/CD Pipeline Enhancement**

- **Traffic Failover:**
  - **Amazon Route 53:**
    - Use Route 53 health checks to monitor the health of your primary region's ALB.
    - Configure DNS failover policies (e.g., active-passive failover) to route traffic to an ALB in the DR region if the primary region becomes unhealthy.
  - **CloudFront Origin Failover:** Configure CloudFront with an origin group that includes your primary ALB and a secondary ALB (in the DR region) for failover.
- **Amazon CloudFront Optimization:**
  - **Fine-tune Cache Policy (CP1):** Ensure `CloudFrontCachePolicy` is optimally configured for your dynamic and static content. Consider separate cache behaviors for different path patterns.
  - **Origin Shield:** For very high request volumes, consider CloudFront Origin Shield to further reduce load on your ALB.
- **Comprehensive CloudWatch Alarms:**
  - Set up alarms for key metrics: ALB (latency, HTTP 5xx errors, healthy host count), ECS (CPU/Memory utilization, running task count), Database (CPU, connections, read/write latency), SQS (queue depth).
  - Alarm on critical application-specific metrics.
- **Centralized Logging and Analysis:**
  - You're using CloudWatch Logs. Explore CloudWatch Logs Insights for powerful querying and analysis.
  - Consider sending logs to a dedicated logging solution (e.g., ELK stack, Splunk) if you have advanced requirements.
- **CloudWatch Dashboards:**
  - Create dashboards to visualize key metrics and the overall health of your application.
- **Advanced Deployment Strategies:**
  - Use AWS CodeDeploy with ECS to implement blue/green or canary deployments. This minimizes the impact of faulty deployments and allows for quick rollbacks.
- **Pipeline for Infrastructure Changes:**
  - Manage your CloudFormation templates in source control and use a pipeline to deploy infrastructure changes, ensuring consistency and auditability.
- **ECR Image Scanning:** Enable vulnerability scanning for your container images in ECR.
- **IAM Least Privilege:** Continuously review and refine IAM roles and policies to adhere to the principle of least privilege.

By systematically implementing these enhancements, you can significantly improve the overall robustness, fault tolerance, scalability, and recoverability of your application. Prioritize based on your specific business requirements, RTO/RPO objectives, and budget.

---

## Appendix A. App Tech Stack

**A.1. Backend Development & Frameworks:**

- **Java 17:** The core programming language version.
- **Spring Boot 3.4.5:** The primary application framework, providing auto-configuration, embedded servers, and a wide range of production-ready features.
  - **Spring MVC:** For building the web application layer and handling HTTP requests.
  - **Spring Data JPA:** For data persistence, simplifying database interactions using the Java Persistence API (JPA).
  - **Spring Security:** For handling authentication and authorization.
    - **Spring Security OAuth2 Client:** Specifically used for integrating with Auth0 for user authentication.
  - **Spring Boot Actuator:** Provides production-ready features like health checks (`/actuator/health`), metrics, and application info.
- **Hibernate:** The default JPA implementation used by Spring Data JPA for object-relational mapping (ORM).
- **H2 Database:** An in-memory/file-based SQL database. File-based for local and prod, in-memory-based for testing.
- **Lombok:** A Java library to reduce boilerplate code (e.g., getters, setters, constructors) via annotations.
- **Jakarta Validation API & Hibernate Validator:** For implementing bean validation (e.g., `@NotNull`, `@Min`).

**A.2. Frontend Development (Server-Side Rendered):**

- **XHTML:** The markup language for the frontend, as per project requirements.
- **Thymeleaf:** A server-side Java template engine used with Spring Boot to render XHTML pages.
  - **Thymeleaf Extras Spring Security 6:** Integration library to use Spring Security features within Thymeleaf templates.
- **WebJars (Bootstrap & jQuery):** Client-side libraries for UI styling and JavaScript interactions, managed via WebJars.

**A.3. Authentication & Authorization:**

- **Auth0:** External identity provider used for user authentication and assumed to be integrated with an LDAP backend.
- **OAuth 2.0 / OpenID Connect (OIDC):** Protocols used for the authentication flow with Auth0.

**A.4. Build & Dependency Management:**

- **Apache Maven:** The build automation and dependency management tool.

**A.5. Testing:**

- **JUnit 5:** The primary testing framework for Java.
- **Spring Boot Test:** Provides utilities for testing Spring Boot applications.
- **Mockito:** A mocking framework for creating test doubles.
- **Spring Security Test:** Utilities for testing Spring Security configurations.
- **Selenium WebDriver:** For browser automation and end-to-end (E2E) testing.
- **WebDriverManager (io.github.bonigarcia):** Manages browser driver binaries for Selenium.
- **Maven Surefire Plugin:** Executes unit tests during the Maven build lifecycle.
- **JaCoCo Maven Plugin:** Generates code coverage reports.

**A.6. Code Quality & Analysis:**

- **SonarQube (via sonar-maven-plugin):** Configured for static code analysis and code quality tracking (though actual execution depends on a running SonarQube server and token).

**A.7. Containerization:**

- **Docker:** Used to containerize the application.
  - **Multi-stage Docker builds:** Used to create optimized, smaller runtime images. Base images `maven:3.9.6-eclipse-temurin-17` for building and `eclipse-temurin:17-jre-jammy` for runtime.

**A.8. CI/CD & Deployment:**

- **AWS CodePipeline:** Orchestrates the continuous integration and deployment pipeline.
- **AWS CodeBuild:** Executes the build process (Maven, Docker build, etc.).
- **Amazon ECR (Elastic Container Registry):** Used to store the Docker images built by CodeBuild.
- **Amazon ECS (Elastic Container Service):** The target deployment platform for running the containerized application on AWS.
  - **AWS Fargate:** Used as the serverless compute engine for ECS tasks.
- **Application Load Balancer (ALB):** Distributes incoming traffic to the ECS tasks.
- **Amazon CloudFront:** Content Delivery Network (CDN) used in front of the ALB for performance, availability, and caching.
- **AWS WAF (Web Application Firewall):** Provides security protection at the application layer, integrated with CloudFront.
- **AWS IAM (Identity and Access Management):** Manages permissions for AWS services (e.g., CodePipeline role, CodeBuild role, ECS task roles).
- **AWS VPC (Virtual Private Cloud):** Provides network isolation for AWS resources.

**A.9. Local Kubernetes Development/Testing:**

- **Kubernetes (via Docker Desktop):** Local Kubernetes environment for testing deployments.
- **kubectl:** Command-line tool for interacting with Kubernetes clusters.
- **Kubernetes Manifests (YAML):**
  - `deployment.yaml`: Defines the desired state for the application deployment.
  - `service.yaml`: Exposes the application running in pods as a network service.
  - `appspec-prod.yaml` & `appspec-staging.yaml`: AppSpec files for AWS CodeDeploy for Kubernetes (if deploying to EKS via CodeDeploy).

---

## Appendix B: CI/CD Architecture

The following diagram captures the main components of the CI/CD pipeline and their interactions. The CI/CD pipeline (GitHub -> CodeStar -> CodePipeline -> CodeBuild -> ECR -> ECS) is a key part, and the application serving path (Internet -> CloudFront -> WAF -> ALB -> ECS Tasks) is the other.

The diagram was derived from the IaC template [medical-register-aws-cloudformation-template.yaml](../medical-register-aws-cloudformation-template.yaml).

                               +-----------------+
                               |  Internet User  |
                               +-----------------+
                                       |
                                       v
                  +------------------------------------------+
                  | [CloudFront Distribution (CFD)]          |
                  | - Aliases: None                          |
                  | - WebACLId: WAF1                         |
                  | - CachePolicy: CP1                       |
                  | - Origin: ALB1                           |
                  +------------------------------------------+
                                       |
                                       v (HTTPS/HTTP traffic)
                  +-----------------------------------------------------+
                  | [WAFv2 WebACL (WAF1)]                               |
                  | - Scope: CLOUDFRONT                                 |
                  | - Rules: AWSManagedRules (IpRep, Common, BadInputs) |
                  +-----------------------------------------------------+
                                       |
                                       v (HTTP traffic to ALB)
+------------------------------------------------------------------------------------------------------+
| AWS Cloud Environment (Region: us-east-1, Account: 851725251295)                                     |
|                                                                                                      |
|   +--------------------------------------------------------------------------------------------------+
|   | VPC (VPC1: EC2VPC - 10.0.0.0/16)                                                                 |
|   |   - DNS Support & Hostnames Enabled                                                              |
|   |   - DHCPOptions: DHCPO1 (EC2DHCPOptions)                                                         |
|   |   - Network ACL: NACL1 (EC2NetworkAcl) - Associated with all Subnets                             |
|   |                                                                                                  |
|   |   +---------------------------- Internet Gateway (IGW1: EC2InternetGateway) -------------------+ |
|   |   |                                                                                            | |
|   |   +---------- Public Subnets (associated with Public Route Table RTB-PUB: EC2RouteTableBK) ----+ |
|   |   |   |                                                                                        | |
|   |   |   | - SubnetXn (10.0.0.0/20, AZ: use1-az4)                                                 | |
|   |   |   | - Subnet (10.0.16.0/20, AZ: use1-az6)                                                  | |
|   |   |   |                                                                                        | |
|   |   |   |   +----------------------------------------------------------------------------+       | |
|   |   |   |   | [Application Load Balancer (ALB1: ElasticLoadBalancingV2LoadBalancer...)]  |       | |
|   |   |   |   | - Scheme: internet-facing                                                  |       | |
|   |   |   |   | - Security Group: SG-ALB (EC2SecurityGroup) (allows HTTP 80 from 0.0.0.0/0)|       | |
|   |   |   |   | - Listener (L1: ...Listenerappmedicalregisteralb...): HTTP:80 --> TG1      |       | |
|   |   |   |   | - Subnets: SubnetXn, Subnet                                                |       | |
|   |   |   |   | - ENIs: EC2NetworkInterface, EC2NetworkInterfaceAf                         |       | |
|   |   |   |   +----------------------------------------------------------------------------+       | |
|   |   |   |                                       | (HTTP:8080 to tasks)                           | |
|   |   |   |                                       v                                                | |
|   |   |   +---- [NAT Gateway (NGW1: EC2NatGateway)] (in SubnetXn) --- [EIP1 (EC2EIP526121149)]     | |
|   |   |   |     - ENI: EC2NetworkInterfaceEW                                                       | |
|   |   |   +---- [NAT Gateway (NGW2: EC2NatGatewayVU)] (in Subnet) --- [EIP2 (EC2EIP1002980173)]    | |
|   |   |         - ENI: EC2NetworkInterfaceDf                                                       | |
|   |   |                                                                                            | |
|   |   +---------- Private Subnets -----------------------------------------------------------------+ |
|   |   |                                                                                            | |
|   |   | - SubnetHj (10.0.128.0/20, AZ: use1-az4) -> Route Table RTB-PRV1 (EC2RouteTableSY)         | |
|   |   |   (RTB-PRV1 routes 0.0.0.0/0 to NGW1, local VPC CIDR, S3 VPCe)                             | |
|   |   |                                                                                            | |
|   |   | - SubnetUb (10.0.144.0/20, AZ: use1-az6) -> Route Table RTB-PRV2 (EC2RouteTableNc)         | |
|   |   |   (RTB-PRV2 routes 0.0.0.0/0 to NGW2, local VPC CIDR, S3 VPCe)                             | |
|   |   |                                                                                            | |
|   |   |   +------------------------------------------------------------------------------+         | |
|   |   |   | [ECS Service (AppSvc* - Implied by TaskDef & ALB TG)]                        |         | |
|   |   |   | - Runs in Private Subnets (SubnetHj, SubnetUb)                               |         | |
|   |   |   | - Task Definition: TD1 (ECSTaskDefinitionTaskdefinitionmedicalregisterapp9)  |         | |
|   |   |   |   - Fargate (CPU:512, Mem:1024)                                              |         | |
|   |   |   |   - Execution Role: IAM-ECS-Exec (IAMRoleEcsTaskExecutionRole)               |         | |
|   |   |   |   - Container (medical-register-container):                                  |         | |
|   |   |   |     - Image: ECR1 (medical-register:e8488ea)                                 |         | |
|   |   |   |     - Port: 8080                                                             |         | |
|   |   |   |     - Logs: CWL-ECS (LogsLogGroupecsmedicalregisterapp)                      |         | |
|   |   |   |     - Env Vars (AUTH0_CLIENT_ID, etc.)                                       |         | |
|   |   |   | - NetworkMode: awsvpc                                                        |         | |
|   |   |   | - Security Group: SG-Task (EC2SecurityGroupAp) (allows 8080 from SG-ALB)     |         | |
|   |   |   | - Service Role: IAM-ECS-SvcRole (IAMRoleAWSServiceRoleForECS)                |         | |
|   |   |   | - ENIs: EC2NetworkInterfaceEG, EC2NetworkInterfaceAv                         |         | |
|   |   |   |   (Tasks get IPs from private subnets, outbound via NGW1/NGW2)               |         | |
|   |   |   +------------------------------------------------------------------------------+         | |
|   |   |                                                                                            | |
|   |   +--------------------------------------------------------------------------------------------+ |
|   |                                                                                                  |
|   |   [Target Group (TG1: ElasticLoadBalancingV2TargetGroup...)]                                     |
|   |   - Protocol: HTTP, Port: 8080, TargetType: ip                                                   |
|   |   - HealthCheck: /actuator/health                                                                |
|   |   - Targets: IPs of ECS Tasks (e.g., 10.0.130.81, 10.0.22.98)                                    |
|   |                                                                                                  |
|   +--------------------------------------------------------------------------------------------------+
|                                                                                                      |
|   +------------------------------------+     +----------------------------------------------------+  |
|   | [ECR Repository (ECR1:             | <-- | [CodeBuild Project (CB1*: medical-register-build)] |  |
|   |  ECRRepositoryMedicalregister)]    |     | - Service Role: IAM-CB-Role                        |  |
|   | - Name: medical-register           |     |   (IAMRoleCodebuildmedicalregisterbuildservicerole)|  |
|   +------------------------------------+     |   (Policy: IAM-CB-BasePol)                         |  |
|                 ^                            |   (IAMManagedPolicyPolicyserviceroleCodeBuild...)  |  |
|                 | (Pushes image)             | - Reads Secrets: SM1                               |  |
|                 |                            | - Logs: CWL-CB (LogsLogGroupawscodebuild...)       |  |
|                 +----------------------------+ - Artifacts: S3-CP1                                |  |
|                                              +----------------------------------------------------+  |
|                                                                 ^                                    |
|   +------------------------------------+                        | (Triggers build)                   |
|   | [SecretsManager Secret (SM1:       |                        |                                    |
|   |  SecretsManagerSecret)]            |     +---------------------------------------------------+   |
|   | - Name: dockerhub/.../credentials  |     | [CodePipeline (CPPL1*: MedicalRegisterPipeline)]  |   |
|   +------------------------------------+     | - Service Role: IAM-CP-Role                       |   |
|                                              |   (IAMRoleAWSCodePipelineServiceRole...)          |   |
|   +------------------------------------+     |   (Policies: IAM-CP-SvcRolePol, IAM-CP-CBPol,     |   |
|   | [S3 Bucket (S3-CP1:                | <-- |              IAM-CP-ConnPol)                      |   |
|   |  S3BucketCodepipeline...)]         |     |   (ManagedPolicies for S3, CodeBuild, Connections)|   |
|   | - Name: codepipeline-us-east-1...  |     | - Source: GitHub via CodeStar Connection (CSC1)   |   |
|   | - Policy: S3-CP-Pol (DenyUnEnc...) |     | - Logs: CWL-CP (LogsLogGroupawscodepipeline...)   |   |
|   +------------------------------------+     +---------------------------------------------------+   |
|                                                                 ^                                    |
|                                                                 | (Uses connection)                  |
|                                              +---------------------------------------------------+   |
|                                              | [CodeStar Connection (CSC1:                       |   |
|                                              |  CodeStarConnectionsConnectionConnection...)]     |   |
|                                              | - Provider: GitHub                                |   |
|                                              | - Name: medical-register-github                   |   |
|                                              +---------------------------------------------------+   |
|                                                                 ^                                    |
|                                                                 | (Webhook/Poll)                     |
+------------------------------------------------------------------------------------------------------+
                                                                  |
                                                       +--------------------+
                                                       | GitHub Repository  |
                                                       | (medical-register) |
                                                       +--------------------+
                                                                  ^
                                                                  | (Code Push)
                                                       +--------------------+
                                                       | Developer          |
                                                       +--------------------+

Other IAM Roles/Policies:

- IAMRoleAWSServiceRoleForElasticLoadBalancing: For ELB service.
- IAMManagedPolicyPolicyRegionRestrict: General S3 region restriction policy for "CostTest_UserGroup".

Notes:

1.  **VPC Endpoints (S3):** Routes `EC2RouteNN` and `EC2RouteKZ` point to a VPC Endpoint (`vpce-04de2e936c5bfd7ed`) for S3 (`pl-63a5400a` is the prefix list for S3). This allows instances in private subnets to access S3 without going over the internet via NAT Gateways.
2.  **ECS Cluster:** The `ECSClusterCapacityProviderAssociations` implies an ECS Cluster named "medical-register-cluster" using Fargate and Fargate Spot.
3.  **CodePipeline/CodeBuild:** While the pipeline and build project themselves are not defined in this template, their supporting IAM roles, S3 bucket, ECR repository, and log groups are, strongly indicating their existence and use.
4.  **Network Interfaces:** Many `EC2NetworkInterface` and `EC2NetworkInterfaceAttachment` resources are for the ALB, NAT Gateways, and ECS tasks. These are generally managed by AWS for those services.
5.  **Route Tables:**
    - `EC2RouteTableBK` (RTB-PUB): For public subnets, routes 0.0.0.0/0 to IGW1, and local VPC CIDR.
    - `EC2RouteTableSY` (RTB-PRV1): For private subnet SubnetHj, routes 0.0.0.0/0 to NGW1, local VPC CIDR, S3 via VPCe.
    - `EC2RouteTableNc` (RTB-PRV2): For private subnet SubnetUb, routes 0.0.0.0/0 to NGW2, local VPC CIDR, S3 via VPCe.
    - `EC2RouteTable` (Default): Likely the main route table initially, but specific associations are made.
6.  **CloudFront Cache Policy (CP1):** `CloudFrontCachePolicy` is configured to use origin cache control headers and forward all query strings.
