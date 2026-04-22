### IncidentBrain API Documentation
**Version:** 1.1.0  
**Authentication:** JWT Bearer Token required for all non-auth endpoints.

---

## 1. Authentication
Endpoints for user access control. These are the only public endpoints.

### Register User
* **Method:** `POST /auth/register`
* **Request Body:**
    ```json
    {
      "username": "admin",
      "password": "securePassword123"
    }
    ```
* **Response:** `201 Created` | "User registered successfully"

### Login User
* **Method:** `POST /auth/login`
* **Request Body:**
    ```json
    {
      "username": "admin",
      "password": "securePassword123"
    }
    ```
* **Response:** `200 OK`
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiJ..."
    }
    ```

---

## 2. Metrics Ingestion & Monitoring
All endpoints in this section require an `Authorization: Bearer <token>` header.

### Subscribe to Service Monitoring
* **Method:** `POST /ingest/subscribe`
* **Query Parameters:** `url`, `name`, `threshold`
* **Response:** `200 OK` | "alice is now monitoring testing-service"

### Live Monitoring Status
* **Method:** `GET /ingest/status`
* **Response:** `200 OK`
    ```json
    {
      "http://localhost:8084/actuator/prometheus": {
        "name": "testing-service",
        "up": true,
        "latency": 10.51,
        "dbUsagePercent": 0.0,
        "lastUpdate": "2026-04-23T12:00:00Z"
      }
    }
    ```

### Unsubscribe Monitoring
* **Method:** `POST /ingest/unsubscribe`
* **Query Parameters:** `url`
* **Response:** `200 OK` | "alice removed http://localhost:8084/actuator/prometheus"

---

## 3. Context Dashboard
All endpoints in this section require an `Authorization: Bearer <token>` header.

### Active Incidents
* **Method:** `GET /context/dashboard/active`
* **Response:** `200 OK`
    ```json
    [
      {
        "incidentId": "uuid",
        "service": "testing-service",
        "status": "OPEN",
        "severity": "MEDIUM",
        "alertIds": ["uuid"],
        "logs": ["log lines..."],
        "metrics": {
          "systemCpuUsage": 0.33,
          "jvmMemoryUsed": 150000000,
          "httpRequests": 71,
          "healthStatus": "UP",
          "fetchedAt": "timestamp",
          "details": {
            "jvm.threads.live": 25
          }
        },
        "incidentStartedAt": "timestamp",
        "enrichedAt": "timestamp"
      }
    ]
    ```

### Incident History
* **Method:** `GET /context/dashboard/history/{serviceName}`
* **Response:** `200 OK` (Array of objects following the Incident schema above)

### Deep Incident Analysis
* **Method:** `GET /context/dashboard/analysis/{incidentId}`
* **Response:** `200 OK`
    ```json
    {
      "incidentId": "uuid",
      "rootCause": "Description of the issue",
      "impactScore": 75,
      "suggestedFix": "Steps to resolve",
      "confidenceLevel": "90%"
    }
    ```

---

## 4. Jira Integration
All endpoints in this section require an `Authorization: Bearer <token>` header.

### Add Jira Credentials
* **Method:** `POST /api/jira/credentials`
* **Request Body:**
    ```json
    {
      "name": "primary-jira",
      "baseUrl": "https://incidentbrain.atlassian.net",
      "userEmail": "user@gmail.com",
      "apiToken": "your-token",
      "projectKey": "KAN",
      "active": true
    }
    ```
* **Response:** `200 OK` (Returns the saved object including generated `id`)

### Jira Metrics
* **Method:** `GET /api/jira/credentials/metrics`
* **Response:** `200 OK`
    ```json
    {
      "totalTicketsCreated": 5,
      "activeCredentialSets": 1,
      "totalCredentialSets": 1
    }
    ```

---

## 5. Configuration
All endpoints in this section require an `Authorization: Bearer <token>` header.

### Update Infrastructure Endpoints
* **Method:** `POST /api/config/endpoints`
* **Request Body:**
    ```json
    {
      "esUrl": "http://localhost:9200/incidentbrain-logs-*/_search",
      "metricsTemplate": "http://localhost:8084/actuator"
    }
    ```
* **Response:** `200 OK` | "Infrastructure ports injected."

### Get Current Configuration
* **Method:** `GET /api/config/current`
* **Response:** `200 OK`
    ```json
    {
      "elasticsearchUrl": "...",
      "actuatorTemplate": "...",
      "configured": true
    }
    ```