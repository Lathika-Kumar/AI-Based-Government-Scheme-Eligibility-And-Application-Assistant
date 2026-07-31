# SchemeBridge Completion Roadmap

## Project Goal
Finish the SchemeBridge project end-to-end by moving from UI/mock-based flow to a working backend-integrated product.

## Current Status
- Frontend public, citizen, and admin flows are largely completed.
- Service layer and API structure are already prepared.
- Backend integration is the main remaining milestone.

- AI/OCR and deployment are secondary improvements after core functionality works.

## Priority 1 — Backend Core APIs
Implement the essential REST APIs first.

### Authentication
- POST /api/v1/auth/login
- POST /api/v1/auth/register
- POST /api/v1/auth/logout
- POST /api/v1/auth/refresh
- POST /api/v1/auth/forgot-password
- POST /api/v1/auth/reset-password

### Profile
- GET /api/v1/profile
- PUT /api/v1/profile
- GET /api/v1/profile/completion
- PATCH /api/v1/profile/preferences

### Schemes
- GET /api/v1/schemes
- GET /api/v1/schemes/:id
- POST /api/v1/schemes/recommendations
- GET /api/v1/schemes/categories
- GET /api/v1/schemes/search
- POST /api/v1/schemes/:id/eligibility

### Documents
- GET /api/v1/documents
- POST /api/v1/documents
- DELETE /api/v1/documents/:id
- POST /api/v1/documents/:id/verify
- POST /api/v1/documents/digilocker/sync
- GET /api/v1/documents/:id/download
- GET /api/v1/documents/vault-score

### Applications
- GET /api/v1/applications
- POST /api/v1/applications
- GET /api/v1/applications/:id
- POST /api/v1/applications/:id/withdraw
- GET /api/v1/applications/saved
- POST /api/v1/applications/saved/:schemeId
- DELETE /api/v1/applications/saved/:schemeId
- GET /api/v1/applications/:id/timeline

### Notifications
- GET /api/v1/notifications
- GET /api/v1/notifications/unread-count
- POST /api/v1/notifications/:id/read
- POST /api/v1/notifications/read-all
- DELETE /api/v1/notifications/:id
- DELETE /api/v1/notifications
- PUT /api/v1/notifications/preferences

## Priority 2 — Frontend Integration
Connect the existing frontend services to the backend.

### Tasks
- Replace mock fallback usage with real API calls
- Configure environment variables for API base URL
- Test login, signup, profile, schemes, documents, and applications end-to-end
- Fix CORS and authentication issues

## Priority 3 — Admin Features
Complete admin workflows.

### Tasks
- GET /api/v1/admin/stats
- GET /api/v1/admin/users
- PUT /api/v1/admin/users/:id
- GET /api/v1/admin/grievances
- POST /api/v1/admin/grievances/:id/resolve
- POST /api/v1/admin/schemes
- PUT /api/v1/admin/schemes/:id
- DELETE /api/v1/admin/schemes/:id
- POST /api/v1/admin/schemes/:id/publish
- POST /api/v1/admin/applications/:id/review

## Priority 4 — AI and OCR
Add real intelligence features after core flows are working.

### Tasks
- Integrate AI chat service
- Connect document suggestions
- Add OCR-based document extraction
- Connect DigiLocker OAuth flow

## Priority 5 — Deployment
Prepare the project for production.

### Tasks
- Set up backend hosting
- Set up database
- Configure environment variables securely
- Deploy frontend and backend
- Add CI/CD pipeline
- Configure domain and HTTPS

## Suggested Execution Order
1. Build backend skeleton
2. Implement auth and profile APIs
3. Implement schemes and documents APIs
4. Implement applications and notifications APIs
5. Connect frontend to backend
6. Implement admin APIs
7. Add AI/OCR integration
8. Deploy and test

## Completion Checklist
- [ ] Backend server runs locally
- [ ] Auth flow works
- [ ] User profile works
- [ ] Scheme browsing works
- [ ] Document upload and verify works
- [ ] Application submission works
- [ ] Notifications work
- [ ] Admin console works
- [ ] Frontend is connected to live API
- [ ] App is deployed successfully
