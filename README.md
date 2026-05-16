# Social Publish

Social Publish is a professional, scalable multi‑platform social media management and automation platform. It allows users to create, schedule, and analyze content across multiple platforms simultaneously, leveraging AI capabilities and flexible queue management tools. The platform is built on Spring Boot with a focus on reliability, asynchronous processing, and a modern UI.

### Overview

Social Publish provides a full content lifecycle through a unified web interface:

- **Multi-platform Publishing:** Post to Telegram, Discord, Slack, LinkedIn, Notion, and Reddit.
- **Smart Scheduling:** Complex publication scenarios (one-time, recurring) via an integrated calendar.
- **AI Integration:** Content generation and improvement via a built-in AI assistant.
- **Visual Control:** Adaptive previews that show exactly how posts will look on each platform.
- **Media Management:** Optimized storage for photos and videos with cloud content delivery.

## Technologies

- **Language**: Java 21
- **Framework**: Spring Boot 3.2+ (Web, Security, Validation, Mail)
- **Persistence**: Spring Data JPA
- **Database**: PostgreSQL
- **Messaging**: RabbitMQ (asynchronous publishing queue)
- **Scheduling**: Quartz Scheduler (complex post scheduling)
- **Frontend**: Thymeleaf, Vanilla JS, CSS3 (with Dark Mode support)
- **Object Mapping**: MapStruct
- **Build Tool**: Maven
- **Cloud Storage**: Cloudinary (media hosting)
- **Security**: Spring Security (OAuth2, Session-based)
- **Testing**: JUnit 5, Mockito, Testcontainers

### Performance & Architecture

- **Async Publishing**: RabbitMQ ensures that social media API latencies do not affect the user experience.
- **Quartz Scheduling**: Robust processing of thousands of scheduled posts with clustering support.
- **Modular Integration Design**: The architecture allows adding new social platforms without modifying the core system.
- **Draft Autosave**: Local and server-side draft persistence to prevent data loss.
- **Dark/Light Mode**: Fully responsive design with advanced dark theme support.
- **Transactional Integrity**: Ensures posts are either published everywhere or errors are handled with retry capabilities.

### Core Features

#### Post Creator & AI
- **Multi-platform Composer**: Create a single post for all networks at once.
- **AI Assistant**: Integrated chatbot for writing posts, changing tone, or fixing grammar.
- **Rich Previews**: Realistic post rendering for each platform (Telegram bubbles, Discord embeds, etc.).
- **Advanced Settings**: Platform-specific options (polls, inline buttons, silent notifications).

#### Scheduling & Calendar
- **Once & Recurring**: Schedule posts for a specific date or with repetition (weekly, monthly).
- **Content Calendar**: Visual calendar with Drag-and-drop support for quick rescheduling.
- **Queue Management**: Powerful queue with filtering by status, platform, and content type.

#### Media Management
- **Universal Uploader**: Photo and video uploads via Drag-and-drop.
- **Cloudinary Integration**: Automatic scaling and optimization of images.
- **Media Sorting**: Ability to reorder media files within a post before publishing.

#### Integrations & Accounts
- **Multi-account support**: Connect multiple accounts for a single platform.
- **Token Management**: Secure storage of access keys and automatic session refresh.
- **Detailed Logs**: Comprehensive publication history with API response logs.

### Repository Structure

- **main** – Stable, production-ready branch.
- **dev** – Active development branch with latest features and UI improvements.

Source layout:
- `src/main/java/com/socialpublish/posts` – Management of posts, queue, and templates.
- `src/main/java/com/socialpublish/integrations` – Modules for each social platform.
- `src/main/java/com/socialpublish/publishing` – Core publishing logic and async workers.
- `src/main/java/com/socialpublish/scheduling` – Quartz configuration and scheduled jobs.
- `src/main/resources/templates` – UI components and pages (Thymeleaf).

### Getting Started

#### Prerequisites
- **Java** 21+
- **Maven** 3.9+
- **PostgreSQL** 15+
- **RabbitMQ** (via Docker recommended)

#### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/polchduikt/social-publish.git
   cd social-publish
   ```

2. **Configure environment**
   Create `src/main/resources/application-secret.properties` or set environment variables:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/social_publish
   spring.datasource.username=postgres
   spring.datasource.password=your_password
   
   # Cloudinary
   cloudinary.cloud_name=your_name
   cloudinary.api_key=your_key
   cloudinary.api_secret=your_secret
   
   # RabbitMQ
   spring.rabbitmq.host=localhost
   ```

3. **Build and Run**
   ```bash
   mvn spring-boot:run
   ```

### API & UI Highlights

The platform is available at `http://localhost:8080`. Key sections include:

- `/queue` – Queue management and filtering.
- `/posts/create` – Post composer.
- `/calendar` – Content calendar.
- `/integrations` – Platform account settings.

### License

This project is licensed under the **MIT License**.
