# Section B - Architecture Diagram and Documentation

![Architecture Diagram](Architecture%20Diagram.png "Architecture Diagram")

Please refer to the [Architecture Diagram](./Architecture%20Diagram.png).

### 1. Network Topology (VPC)

- **Component:** Amazon Virtual Private Cloud (VPC)
- **Design:**
  - A single VPC (e.g., `10.0.0.0/16`) spanning multiple Availability Zones (e.g., us-east-1a, us-east-1b, us-east-1c) for high availability.
  - **Public Subnets:**
    - Host internet-facing resources: Public Application Load Balancer (ALB) nodes, NAT Gateways, Bastion Host (not shown for brevity but recommended).
    - Route table associated with an Internet Gateway (IGW) for direct internet access.
  - **Private Subnets (per AZ, per Tier):**
    - **Web Tier Private Subnets:** Host EC2 instances for the front-end web servers. No direct internet access. Outbound traffic routed via NAT Gateways in public subnets for patching, external API calls.
    - **App Tier Private Subnets:** Host EC2 instances for the back-end application servers. No direct internet access. Outbound traffic via NAT Gateways.
    - **Data Tier Private Subnets:** Host RDS instances and ElastiCache clusters. No internet access at all (route table has no IGW or NAT Gateway route for general internet).
- **Justification:**
  - **Security:** Isolates application components. Private subnets prevent direct internet exposure to backend and data layers.
  - **High Availability:** Spanning AZs mitigates an AZ failure. NAT Gateways are deployed per AZ for HA.
  - **Control:** Granular control over routing and network access.

### 2. Edge Layer (CDN & WAF)

- **Components:** Amazon CloudFront, AWS WAF, AWS Shield
- **Design:**
  - **CloudFront:**
    - Serves static assets (JS, CSS, images, videos) directly from an S3 bucket origin.
    - Proxies dynamic requests to the public Application Load Balancer (ALB).
    - Configured with custom SSL certificate (via AWS Certificate Manager - ACM).
    - Utilizes caching policies to reduce load on origins.
  - **AWS WAF:**
    - Integrated with CloudFront (and/or ALB).
    - Rules to block common web exploits (SQLi, XSS), rate-based rules, Geo-blocking.
  - **AWS Shield:**
    - Standard (free) provides DDoS protection.
    - Shield Advanced (paid) offers enhanced DDoS protection, 24/7 DDoS response team, and cost protection against DDoS-related spikes.
- **Justification:**
  - **Performance:** CDN caches content closer to users, reducing latency.
  - **Scalability:** Offloads traffic from origin servers.
  - **Security:** WAF protects against common attacks. Shield mitigates DDoS.
  - **Cost:** Reduces data transfer out costs from origin.

### 3. Front-End Tier (Web Servers)

- **Components:** Public Application Load Balancer (ALB), EC2 Instances in an Auto Scaling Group (ASG).
- **Design:**
  - **Public ALB:**
    - Internet-facing, deployed across public subnets in multiple AZs.
    - Listens on HTTPS (port 443), terminates SSL using ACM certificate.
    - Routes traffic to Web Tier EC2 instances.
    - **Load Balancing Algorithm:** `Least Outstanding Requests`. Good for varying request completion times, ensuring new requests go to less busy instances. `Round Robin` is also a good default.
  - **EC2 Instances (ASG):**
    - Run web server software (e.g., Nginx, Apache) serving front-end application logic (e.g., rendering HTML, handling initial user interactions).
    - Deployed in private "Web Tier" subnets across multiple AZs.
    - **Auto Scaling Policy:**
      - **Target Tracking:** Based on average CPU Utilization (e.g., scale out if CPU > 60%, scale in if CPU < 40%). For compute-bound apps.
      - **Scheduled Scaling:** To handle predictable peaks (e.g., scale up before business hours).
- **Justification:**
  - **Scalability & Elasticity:** ASG automatically adjusts capacity based on demand.
  - **High Availability:** ALB and ASG distribute load and instances across AZs. ALB health checks automatically route traffic away from unhealthy instances.
  - **Security:** Instances are in private subnets, only accessible via ALB. SSL termination at ALB simplifies certificate management on instances.

### 4. Back-End Tier (Application Servers)

- **Components:** Internal Application Load Balancer (ALB), EC2 Instances in an Auto Scaling Group (ASG).
- **Design:**
  - **Internal ALB:**
    - Not internet-facing. Accessible only from within the VPC (specifically from the Web Tier).
    - Deployed across private "App Tier" subnets in multiple AZs.
    - Listens on HTTP/HTTPS (e.g., port 8080) for requests from the Web Tier.
    - **Load Balancing Algorithm:** `Least Outstanding Requests` or `Round Robin`.
  - **EC2 Instances (ASG):**
    - Run application logic (e.g., Java, Python, Node.js microservices).
    - Deployed in private "App Tier" subnets across multiple AZs.
    - **Auto Scaling Policy:** Similar to Web Tier (CPU utilization, potentially custom metrics like SQS queue length if using asynchronous processing).
- **Justification:**
  - **Scalability & Elasticity:** ASG for dynamic capacity adjustment.
  - **High Availability:** Internal ALB and ASG across AZs.
  - **Security:** Internal ALB and instances are in private subnets, only accessible by the Web Tier, further isolating core business logic.
  - **Decoupling:** Separates presentation logic from business logic, allowing independent scaling and development.

### 5. Data Layer

- **Components:** Amazon RDS (e.g., PostgreSQL, MySQL, Aurora), Amazon ElastiCache (e.g., Redis, Memcached).
- **Design:**
  - **Amazon RDS:**
    - **Instance Type:** Chosen based on performance needs.
    - **Multi-AZ Deployment:** Enabled for automatic failover to a standby replica in a different AZ.
    - **Read Replicas:** Deployed in one or more AZs to offload read traffic from the primary database, improving read scalability.
    - Deployed in private "Data Tier" subnets.
    - **Backup Policy:** Automated daily snapshots with transaction log backups for Point-In-Time Recovery (PITR). Retain backups for a defined period (e.g., 7-35 days). Regular manual snapshots for long-term archival or pre-major-change backups.
  - **Amazon ElastiCache:**
    - Used for caching frequently accessed data (e.g., session state, query results, user profiles) to reduce latency and database load.
    - **Redis:** If advanced data structures or persistence is needed. Cluster mode for scalability and HA.
    - **Memcached:** If simpler key-value caching is sufficient.
    - Deployed in private "Data Tier" subnets, accessible by the App Tier. Multi-AZ deployment for HA.
- **Justification:**
  - **Managed Service:** RDS and ElastiCache reduce operational overhead (patching, backups, scaling).
  - **High Availability:** RDS Multi-AZ provides synchronous replication and automatic failover. ElastiCache cluster mode provides sharding and replication.
  - **Scalability:** RDS Read Replicas scale read operations. ElastiCache reduces DB load, allowing the primary DB to handle more write traffic.
  - **Performance:** Caching significantly improves response times for frequently accessed data.
  - **Security:** Both are in private subnets, accessible only by the App Tier through security groups.

### 6. Security Controls

- **Network Access Control Lists (NACLs):**
  - Stateless firewalls at the subnet level.
  - **Public Subnets:** Allow inbound HTTP/S from anywhere (0.0.0.0/0). Allow outbound traffic. Allow ephemeral ports for return traffic.
  - **Private Subnets (Web, App, Data):** More restrictive. For example, Web Tier subnet NACL allows inbound from ALB IPs on web ports, outbound to App Tier IPs on app ports, and to NAT Gateway. Data Tier subnet NACL allows inbound from App Tier IPs on DB/cache ports. Deny all other traffic explicitly.
  - **Justification:** First line of defense, coarser-grained than Security Groups.
- **Security Groups (SGs):**
  - Stateful firewalls at the resource level (EC2, RDS, ELB, ElastiCache).
  - **ALB-Public-SG:** Allows inbound HTTPS (443) from 0.0.0.0/0 (or CloudFront IPs if stricter).
  - **Web-Tier-EC2-SG:** Allows inbound HTTP/S (e.g., 80/443) ONLY from ALB-Public-SG. Allows outbound to App-Tier-Internal-ALB-SG on app port (e.g., 8080). Allows SSH from Bastion Host SG.
  - **ALB-Internal-SG:** Allows inbound (e.g., 8080) ONLY from Web-Tier-EC2-SG.
  - **App-Tier-EC2-SG:** Allows inbound (e.g., 8080) ONLY from ALB-Internal-SG. Allows outbound to Data-Tier-RDS-SG on DB port (e.g., 5432) and Data-Tier-Cache-SG on cache port (e.g., 6379). Allows SSH from Bastion Host SG.
  - **Data-Tier-RDS-SG:** Allows inbound (e.g., 5432) ONLY from App-Tier-EC2-SG.
  - **Data-Tier-Cache-SG:** Allows inbound (e.g., 6379) ONLY from App-Tier-EC2-SG.
  - **Justification:** Fine-grained, stateful control. Principle of least privilege.
- **AWS WAF & Shield:** (As described in Edge Layer).
- **AWS Identity and Access Management (IAM):**
  - **IAM Roles:** For EC2 instances, Lambda functions, etc., granting least privilege permissions to access other AWS services (e.g., S3, SQS, CloudWatch) without embedding credentials.
  - **IAM Users & Groups:** For human users, with MFA enforced. Granular policies attached to groups.
  - **Justification:** Securely manage access to AWS resources.
- **AWS Key Management Service (KMS):**
  - Used for creating and managing encryption keys.
  - **Encryption Algorithms:** Typically AES-256 for data at rest.
  - Encrypts EBS volumes, S3 objects (Server-Side Encryption with KMS - SSE-KMS), RDS instances, ElastiCache (if supported, e.g. Redis AUTH + TLS).
  - **Justification:** Protects data at rest. Centralized key management and audit.
- **AWS Secrets Manager / Parameter Store:**
  - Securely store and manage secrets like database credentials, API keys.
  - Integrates with IAM for access control. Automatic rotation for RDS credentials.
  - **Justification:** Avoids hardcoding secrets in code or configuration files.
- **Encryption in Transit:**
  - TLS/SSL for all external connections (CloudFront, Public ALB).
  - TLS/SSL can also be used for internal communication between tiers (Web -> App ALB, App ALB -> App Instances), using ACM Private CA or self-signed certs (managed).
  - **Encryption Algorithms:** Modern TLS protocols (TLS 1.2, TLS 1.3) with strong cipher suites.
  - **Justification:** Protects data from eavesdropping during transmission.

### 7. Logging, Metrics, Alerts

- **AWS CloudWatch:**
  - **Metrics:** Collects default metrics (CPU, Network, Disk, ELB request counts, latency, RDS metrics, etc.). Custom metrics can be published by applications (e.g., business KPIs, queue depths).
  - **Logs:**
    - **CloudWatch Logs Agent:** Installed on EC2 instances to collect application logs, system logs.
    - **VPC Flow Logs:** Capture IP traffic information for VPC, subnets, ENIs. Stored in CloudWatch Logs or S3.
    - **ALB Access Logs:** Stored in S3.
    - **CloudTrail Logs:** Audit API calls made to AWS services. Stored in S3.
    - **RDS Logs:** Database error logs, slow query logs, audit logs published to CloudWatch Logs.
  - **Alarms:**
    - Set on CloudWatch metrics (e.g., high CPU, low disk space, high error rates on ALB, unhealthy host count, high DB connections, high latency).
    - **Actions:** Trigger Auto Scaling, send notifications via Amazon Simple Notification Service (SNS) to email, SMS, Slack (via Lambda integration), or PagerDuty.
  - **Dashboards:** Create custom dashboards to visualize key metrics and log insights for operational visibility.
- **Justification:** Essential for monitoring system health, performance, troubleshooting issues, security analysis, and triggering automated responses.

### 8. Backup Policies

- **Amazon RDS:**
  - Automated daily snapshots, configurable retention (e.g., 7-35 days).
  - Transaction log backups for Point-In-Time Recovery (PITR) up to the last 5 minutes.
  - Manual snapshots for long-term archival or pre-deployment backups.
  - Consider cross-region snapshot copies for DR.
- **Amazon EBS (for EC2 instance root/data volumes):**
  - AWS Backup or Amazon Data Lifecycle Manager to automate snapshot creation, retention, and deletion schedules.
  - Store snapshots for recovery or new instance launches.
- **Amazon S3 (for static assets, logs, backups):**
  - Versioning enabled on buckets to recover from accidental deletions or overwrites.
  - Lifecycle policies to transition older versions/data to cheaper storage classes (e.g., S3 Glacier) or delete them.
  - Cross-Region Replication (CRR) for critical S3 buckets (e.g., backups, CloudTrail logs) for DR.
- **Infrastructure as Code (IaC):**
  - CloudFormation or Terraform templates act as a "backup" of the infrastructure configuration, allowing for quick redeployment.
- **Justification:** Protects against data loss, corruption, and enables recovery from failures. Compliance requirements often dictate backup policies.

### 9. Failover Mechanisms

- **ELB Health Checks:** ALBs constantly monitor the health of registered EC2 instances. If an instance fails a health check, the ALB stops sending traffic to it.
- **Auto Scaling Group (ASG):**
  - Monitors instance health (EC2 status checks, ELB health checks).
  - If an instance is marked unhealthy, ASG terminates it and launches a new one to maintain desired capacity.
  - Distributes instances across multiple AZs. If one AZ fails, ASG can launch new instances in other AZs (if capacity settings allow and subnets are configured).
- **RDS Multi-AZ:**
  - Synchronous replication to a standby instance in a different AZ.
  - Automatic failover to the standby (typically <1-2 minutes, DNS propagation might take longer) in case of primary instance failure or AZ outage.
- **ElastiCache (Redis):**
  - Cluster mode with replicas in different AZs. Automatic failover of primary nodes.
- **Route 53 DNS Failover:**
  - Can be configured with health checks on CloudFront distribution or ALB endpoint.
  - If primary endpoint becomes unhealthy, Route 53 can failover traffic to a secondary endpoint (e.g., a static S3 error page, a scaled-down stack in another region for DR).
- **S3 Cross-Region Replication:** For DR, critical data can be replicated to another region.
- **NAT Gateway HA:** Deploy NAT Gateways in each AZ with routes configured so that if one AZ's NAT Gateway fails, traffic from private subnets in that AZ can still route out via NAT Gateways in other AZs (requires careful route table configuration or use of Transit Gateway). A simpler approach is one NAT GW per AZ, serving private subnets in that AZ.
- **Justification:** Ensures high availability and resilience against component failures or AZ-level disruptions, minimizing downtime.

### 10. CI/CD (Continuous Integration/Continuous Deployment)

- **Source Control:** AWS CodeCommit (or GitHub, Bitbucket).
- **Build:** AWS CodeBuild
  - Compiles code, runs unit/integration tests, creates build artifacts (e.g., JARs, WARs, Docker images if using containers).
  - Artifacts stored in S3 or Amazon ECR (for Docker images).
- **Deploy:** AWS CodeDeploy
  - Automates deployments to EC2 instances in Auto Scaling Groups.
  - Supports deployment strategies: In-place, Blue/Green.
  - Blue/Green is preferred for minimizing downtime and easy rollback. CodeDeploy can manage traffic shifting with ELB.
- **Pipeline Orchestration:** AWS CodePipeline
  - Defines and automates the stages of the release process (Source -> Build -> Test -> Deploy Staging -> Deploy Production).
  - Integrates with CodeCommit, CodeBuild, CodeDeploy, and can have manual approval gates.
- **Infrastructure as Code (IaC):**
  - AWS CloudFormation or HashiCorp Terraform for provisioning and managing infrastructure.
  - Templates version-controlled in CodeCommit.
  - Changes to infrastructure are also deployed via a pipeline (e.g., CodePipeline invoking CloudFormation stack updates).
- **Justification:**
  - **Automation:** Reduces manual effort and human error in deployment.
  - **Speed & Frequency:** Enables faster and more frequent releases.
  - **Consistency:** Ensures deployments are repeatable and reliable.
  - **Rollback:** Facilitates quick rollback in case of deployment issues (especially with Blue/Green).
  - **Quality:** Integration of automated testing improves code quality.

### 11. Other Infrastructure Features

- **Amazon S3 for Static Assets:**
  - Highly durable, scalable, and cost-effective storage for website's static content (HTML, CSS, JS, images).
  - Versioned and backed up. Served via CloudFront.
- **AWS Certificate Manager (ACM):**
  - Provides and manages SSL/TLS certificates for CloudFront and ALBs.
  - Automatic certificate renewal.
- **Bastion Host (Optional but Recommended):**
  - A hardened EC2 instance in a public subnet used for secure SSH access to EC2 instances in private subnets.
  - Strict Security Group rules (allow SSH only from specific corporate IPs).
  - Regularly patched and monitored. Consider using SSM Session Manager as an alternative to a Bastion Host for improved security and auditability (no open SSH ports needed).

## Conclusion

This 3-tier web application design on AWS provides a robust foundation that is scalable, highly available, secure, and production-ready. By leveraging managed services where appropriate, it reduces operational overhead. The design emphasizes defense-in-depth security, automated scaling, comprehensive monitoring, and automated deployment pipelines. Continuous review and optimization of costs, performance, and security posture will be necessary as the application evolves.
