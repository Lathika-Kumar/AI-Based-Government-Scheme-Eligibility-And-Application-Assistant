# Antigravity Work Plan for SchemeBridge

This file is for handing off the remaining work to Antigravity in a compact and clear way.

## Goal
Finish the SchemeBridge project by implementing the missing backend APIs and connecting them to the existing frontend structure.

## What is already done
- Frontend UI for public, citizen, and admin portals is mostly complete.
- Frontend services and API config are already prepared.
- API endpoint structure is already defined in the frontend.
- A backend starter folder has been created.

## What still needs to be done

### 1. Backend API implementation
Implement the following modules in Spring Boot:
- Auth APIs
- Profile APIs
- Scheme APIs
- Document APIs
- Application APIs
- Notification APIs
- Admin APIs
- Config/lookup APIs

### 2. Backend structure
Create the following backend components:
- Controllers
- Services
- DTOs / request models
- Response models
- Exception handling
- Validation
- Security / JWT setup

### 3. Database integration
Connect the backend to a database.
Recommended options:
- MongoDB for a document-style app
- PostgreSQL if relational modeling is preferred

### 4. Frontend integration
Connect the frontend services to the real backend.
- Replace mock fallback behavior with real API calls.
- Configure environment variables for API base URL.
- Fix CORS/auth issues.

### 5. Testing
Add basic verification for:
- login/register
- profile operations
- schemes listing and search
- document upload
- application submission
- admin review flow

### 6. Deployment
Prepare the project for deployment:
- backend hosting
- frontend hosting
- environment variables
- CI/CD if possible

## Recommended implementation order
1. Auth + profile
2. Schemes
3. Documents
4. Applications
5. Notifications
6. Admin endpoints
7. Frontend integration
8. Testing
9. Deployment

## Important note for Antigravity
Please do not start with unnecessary UI redesign.
Focus on:
- backend API implementation
- correct endpoint naming
- frontend-backend integration
- working end-to-end flows

## Expected result
A working, deployable version of SchemeBridge where the frontend can use real backend APIs instead of mock data.
