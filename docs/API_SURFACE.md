# Social Publish — UI Routes & API Surface Specification

This document details all user-facing web routes (Thymeleaf pages) and REST API endpoints provided by the **Social Publish** platform.

---

## 1. User Interface (UI) Routes

All UI routes are server-rendered via Thymeleaf with responsive dark/light mode support, real-time STOMP WebSocket connectivity, and client-side form validation.

| Path | Controller | Method | Access | Description |
|:---|:---|:---:|:---:|:---|
| `/` | `HomeController` | `GET` | Public | Landing page with platform features, live preview showcase, and authentication links. |
| `/login` | `AuthController` | `GET` | Public | User login page with credentials form and Google OAuth2 SSO button. |
| `/register` | `RegisterController` | `GET`, `POST` | Public | User registration page with email confirmation and validation. |
| `/dashboard` | `HomeController` | `GET` | Authenticated | Main analytics dashboard showing publishing stats, 14-day timeline charts, and recent activity. |
| `/posts/new` | `PostController` | `GET` | Authenticated | Multi-platform post composer with Groq AI Assistant, file uploader, and adaptive live previews. |
| `/posts` | `PostController` | `POST` | Authenticated | Submit new post for immediate publishing, scheduling, or saving as a draft. |
| `/posts/{id}/edit` | `PostController` | `GET` | Authenticated | Edit existing draft or reschedule a pending post. |
| `/queue` | `QueueController` | `GET` | Authenticated | Content queue view with filtering by status (*Draft, Scheduled, Published, Failed*), platform, and bulk actions. |
| `/calendar` | `CalendarPageController` | `GET` | Authenticated | Visual FullCalendar view with interactive drag-and-drop rescheduling. |
| `/templates` | `TemplatesPageController` | `GET` | Authenticated | Saved post templates library with platform tags and one-click composer loading. |
| `/accounts` | `AccountsPageController` | `GET` | Authenticated | Integrations Hub showing active connection status across all 6 social platforms. |
| `/accounts/telegram` | `TelegramAccountsController`| `GET`, `POST` | Authenticated | Telegram Bot token, chat/channel ID, and multi-account configuration. |
| `/accounts/discord` | `DiscordController` | `GET`, `POST` | Authenticated | Discord Webhook URL and embed configuration. |
| `/accounts/slack` | `SlackController` | `GET`, `POST` | Authenticated | Slack Webhook URL and channel routing settings. |
| `/accounts/notion` | `NotionController` | `GET`, `POST` | Authenticated | Notion integration token, Database ID, and page property mapping. |
| `/accounts/linkedin` | `LinkedInController` | `GET`, `POST` | Authenticated | LinkedIn OAuth2 authorization code flow & account connection. |
| `/accounts/reddit` | `RedditAccountsController` | `GET`, `POST` | Authenticated | Reddit OAuth2 authorization code flow & subreddit preferences. |
| `/settings` | `SettingsController` | `GET`, `POST` | Authenticated | Profile settings, password update, Groq AI model configuration, and account deletion. |

---

## 2. REST API Endpoints

All `/api/*` endpoints require an active authenticated session (or appropriate security context) and exchange JSON payloads.

### 2.1 Content Calendar API — `/api/calendar`

#### `GET /api/calendar/events`
Fetch all scheduled posts formatted as FullCalendar event objects for a user.
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "title": "Product Launch Announcement",
      "start": "2026-09-20T14:30:00Z",
      "platforms": ["TELEGRAM", "DISCORD", "LINKEDIN"],
      "status": "SCHEDULED"
    }
  ]
  ```

#### `PATCH /api/calendar/events/{id}/reschedule`
Update the scheduled execution timestamp of a pending post via drag-and-drop.
- **Request Body**:
  ```json
  {
    "scheduledAt": "2026-09-21T16:00:00Z"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "status": "ok"
  }
  ```

#### `DELETE /api/calendar/events/{id}`
Cancel and delete a scheduled calendar event.
- **Response**: `200 OK`
  ```json
  {
    "status": "deleted"
  }
  ```

---

### 2.2 Post Templates API — `/api/templates`

#### `GET /api/templates`
List all reusable post templates created by the authenticated user.
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "e2c3e1e4-3982-4522-8356-9e1faee98db1",
      "name": "Weekly Tech Roundup",
      "content": "Here is our weekly summary of top engineering articles...",
      "platforms": ["TELEGRAM", "DISCORD", "SLACK", "LINKEDIN"],
      "tags": ["tech", "roundup"]
    }
  ]
  ```

#### `POST /api/templates`
Create a new reusable post template.
- **Request Body**:
  ```json
  {
    "name": "Product Release Note",
    "content": "🚀 New feature released: ...",
    "platforms": ["TELEGRAM", "TWITTER", "DISCORD"]
  }
  ```
- **Response**: `200 OK` with created template DTO.

#### `DELETE /api/templates/{id}`
Delete a template by ID.
- **Response**: `200 OK`

---

### 2.3 AI Assistant API — `/api/ai-assistant`

#### `POST /api/ai-assistant/chat`
Send conversational prompts to the Groq LLaMA 3.3 70B AI Assistant for post creation, rewriting, tone transformation, or translation.
- **Request Body**:
  ```json
  {
    "message": "Rewrite this announcement to sound exciting and add 3 hashtags for LinkedIn and Telegram",
    "targetPlatforms": ["LINKEDIN", "TELEGRAM"],
    "currentContent": "Our new v2.0 update is out now.",
    "tone": "VIRAL"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "reply": "🔥 Huge news! We just dropped Social Publish v2.0 — packed with multi-platform scheduling and AI-assisted creation! 🚀 Check it out now.\n\n#TechNews #ProductLaunch #SocialMediaAutomation",
    "extractedHashtags": ["#TechNews", "#ProductLaunch", "#SocialMediaAutomation"],
    "suggestedPlatforms": ["LINKEDIN", "TELEGRAM"]
  }
  ```

---

### 2.4 Notifications API — `/api/notifications`

#### `GET /api/notifications`
Fetch the user's recent in-app notification feed.
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
      "type": "PUBLISH_SUCCESS",
      "title": "Post Published Successfully",
      "message": "Your post was published to Telegram and Discord.",
      "createdAt": "2026-09-16T16:15:00Z",
      "read": false
    }
  ]
  ```

#### `POST /api/notifications/read`
Mark all unread notifications as read.
- **Response**: `200 OK` (`{"status": "ok"}`)

#### `DELETE /api/notifications`
Clear all user notifications.
- **Response**: `200 OK` (`{"status": "cleared"}`)

---

## 3. Real-Time WebSocket (STOMP) Surface

- **Endpoint**: `/ws` (with SockJS fallback fallback)
- **Application Destination Prefix**: `/app`
- **Broker Destination Prefix**: `/topic`, `/queue`
- **User Destination**: `/topic/notifications/{userId}` (delivers real-time publishing status toasts and live badge counter updates)
