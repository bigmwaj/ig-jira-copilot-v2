# ig-jira-copilot-v2
Jira AI Copilot Integration Gateway v2

## AI Orchestration Service

A production-ready integration application that uses **Apache Camel** to orchestrate **Jira** and **GitHub Copilot** workflows, automating the software development lifecycle.

### Technology Stack

| Component       | Version   |
|-----------------|-----------|
| Java            | 17+       |
| Spring Boot     | 3.4.3     |
| Apache Camel    | 4.10.0    |
| Maven           | 3.9+      |

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                    IG Jira Copilot Service                          │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                   Apache Camel Context                       │   │
│  │                                                              │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │   │
│  │  │   Route 1    │  │   Route 2    │  │    Route 3       │   │   │
│  │  │  Story       │  │  Dev Plan    │  │  Dev Plan        │   │   │
│  │  │  Refinement  │  │  Generation  │  │  Review          │   │   │
│  │  │ AI01→AI03    │  │ AI04→AI06    │  │ AI07→AI09        │   │   │
│  │  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘   │   │
│  │         │                 │                    │             │   │
│  │  ┌──────────────────────────────────────────────────────┐    │   │
│  │  │                    Route 4                           │    │   │
│  │  │              Code Generation AI10→AI12               │    │   │
│  │  └──────────────────────────────────────────────────────┘    │   │
│  │                                                              │   │
│  │  SEDA queues (async):                                        │   │
│  │   seda:refinement-process      seda:devplan-process          │   │
│  │   seda:devplan-review-process  seda:codegen-process          │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                     │
│         │ HTTP                              │ HTTP                  │
│         ▼                                   ▼                       │
│  ┌─────────────┐                   ┌──────────────────┐            │
│  │  Jira REST  │                   │  Copilot API     │            │
│  │  API        │                   │  (OpenAI compat) │            │
│  └─────────────┘                   └──────────────────┘            │
└─────────────────────────────────────────────────────────────────────┘
```

### Integration Workflow

```
Jira Issue (AI-Agent label)
         │
         ▼
┌────────────────────────────────────────────────────────────────────┐
│                    ROUTE 1: User Story Refinement                  │
│                                                                    │
│  Timer ──► Query Jira (AI01) ──► Split Issues ──► SEDA (async)    │
│                                                         │          │
│  AI03 ◄── Update Jira ◄── Parse Response ◄── Copilot API          │
│  (Refined)            (description+state)   (AI02 in progress)    │
└────────────────────────────────────────────────────────────────────┘
         │ Refined Story (AI03) → Manual: set to AI04
         ▼
┌────────────────────────────────────────────────────────────────────┐
│                    ROUTE 2: Development Plan Generation            │
│                                                                    │
│  Timer ──► Query Jira (AI04) ──► Split Issues ──► SEDA (async)    │
│                                                         │          │
│  AI06 ◄── Update Jira ◄── Parse Response ◄── Copilot API          │
│  (Plan)   + Create Task        (devplan)    (AI05 in progress)    │
│           (linked, AI07)                                           │
└────────────────────────────────────────────────────────────────────┘
         │ New Task created with AI07 state
         ▼
┌────────────────────────────────────────────────────────────────────┐
│                    ROUTE 3: Development Plan Review                │
│                                                                    │
│  Timer ──► Query Jira (AI07) ──► Split Tasks ──► SEDA (async)     │
│                                                         │          │
│  AI09 ◄── Update Jira ◄── Parse Response ◄── Copilot API          │
│  (Reviewed)          (description+state)   (AI08 in progress)    │
└────────────────────────────────────────────────────────────────────┘
         │ Reviewed plan (AI09) → Manual: set to AI10
         ▼
┌────────────────────────────────────────────────────────────────────┐
│                    ROUTE 4: Code Generation                        │
│                                                                    │
│  Timer ──► Query Jira (AI10) ──► Split Tasks ──► SEDA (async)     │
│                                                         │          │
│  AI12 ◄── Update Jira ◄── Parse Response ◄── Copilot API          │
│  (Generated)         (description+state)   (AI11 in progress)    │
└────────────────────────────────────────────────────────────────────┘
```

### AI Exchange Tracking States

| State | Meaning                          |
|-------|----------------------------------|
| AI01  | Waiting for refinement           |
| AI02  | Refinement in progress           |
| AI03  | Refinement completed             |
| AI04  | Waiting for development plan     |
| AI05  | Dev plan generation in progress  |
| AI06  | Dev plan generated               |
| AI07  | Waiting for dev plan review      |
| AI08  | Review in progress               |
| AI09  | Review completed                 |
| AI10  | Waiting for code generation      |
| AI11  | Code generation in progress      |
| AI12  | Code generation completed        |

### Project Structure

```
src/
├── main/
│   ├── java/ca/eps_consulting/ig_jira_copilot/
│   │   ├── IgJiraCopilotApplication.java        # Main Spring Boot entry point
│   │   ├── config/
│   │   │   └── AppConfig.java                   # Configuration properties
│   │   ├── dto/
│   │   │   ├── JiraIssueDto.java                # Jira issue/search result DTOs
│   │   │   ├── JiraTaskDto.java                 # Jira task creation DTO
│   │   │   ├── CopilotRequestDto.java           # Copilot API request DTO
│   │   │   └── CopilotResponseDto.java          # Copilot API response DTO
│   │   ├── processor/
│   │   │   ├── JiraProcessor.java               # Jira request/response processing
│   │   │   └── CopilotProcessor.java            # Copilot request/response processing
│   │   └── routes/
│   │       ├── BaseOrchestrationRoute.java      # Shared route utilities
│   │       ├── UserStoryRefinementRoute.java    # Route 1: AI01→AI03
│   │       ├── DevelopmentPlanGenerationRoute.java # Route 2: AI04→AI06
│   │       ├── DevelopmentPlanReviewRoute.java  # Route 3: AI07→AI09
│   │       └── CodeGenerationRoute.java         # Route 4: AI10→AI12
│   └── resources/
│       └── application.yml                      # Application configuration
└── test/
    ├── java/ca/eps_consulting/ig_jira_copilot/routes/
    │   ├── UserStoryRefinementRouteTest.java
    │   ├── DevelopmentPlanGenerationRouteTest.java
    │   └── ReviewAndCodeGenRouteTest.java
    └── resources/
        └── application.yml                      # Test configuration
```

### Configuration

Copy `src/main/resources/application.yml` and configure the following environment variables:

```bash
# Jira Configuration
export JIRA_BASE_URL=https://your-company.atlassian.net
export JIRA_USERNAME=your-email@company.com
export JIRA_API_TOKEN=your-jira-api-token
export JIRA_PROJECT_KEY=MYPROJECT
export JIRA_AI_FIELD_ID=customfield_10100   # Your custom field ID

# GitHub Copilot API
export COPILOT_API_URL=https://api.githubcopilot.com
export COPILOT_API_KEY=your-copilot-api-key

# Optional scheduling (milliseconds)
export ROUTE_REFINEMENT_SCHEDULE=60000
export ROUTE_DEV_PLAN_SCHEDULE=60000
export ROUTE_DEV_PLAN_REVIEW_SCHEDULE=60000
export ROUTE_CODE_GEN_SCHEDULE=60000
```

### Running the Application

```bash
# Build
mvn clean package

# Run
java -jar target/ig-jira-copilot-1.0.0-SNAPSHOT.jar

# Or with Maven
mvn spring-boot:run
```

### Running Tests

```bash
mvn test
```

### Prerequisites

- Java 17+
- Jira instance with:
  - A custom field "AI Exchange Tracking" (note the field ID)
  - Issues labeled with `AI-Agent`
- GitHub Copilot API access or compatible OpenAI API endpoint

