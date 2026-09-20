# SchemeBridge Production Deployment Manual

This guide provides step-by-step instructions for deploying SchemeBridge to **Cloud VMs / VPS (AWS, DigitalOcean, Azure, GCP, Linode)** using Docker Compose, as well as a standalone local setup guide.

---

## Architecture Overview

```
                                Client Browser
                                      │
                                      │ Port 80 (or 443 with SSL)
                                      ▼
                       ┌─────────────────────────────┐
                       │     Frontend Container      │
                       │        (Nginx:alpine)       │
                       │  • Serves React / Vite SPA  │
                       │  • Built-in Reverse Proxy   │
                       └──────────────┬──────────────┘
                                      │
                  ┌───────────────────┴───────────────────┐
                  │ (Isolated Docker Network: schemebridge)
                  ▼                                       ▼
    /api/auth/* & /api/admin/users/*               All other /api/*
   ┌─────────────────────────────────┐   ┌─────────────────────────────────┐
   │      Auth Service Container     │   │     Scheme Service Container    │
   │      (Spring Boot, JRE 17)      │   │      (Spring Boot, JRE 17)      │
   │      Port: 8080 (Internal)      │   │      Port: 8081 (Internal)      │
   └────────────────┬────────────────┘   └────────────────┬────────────────┘
                    │                                     │
                    ▼                                     ▼
      ┌──────────────────────────┐          ┌───────────────────────────┐
      │  Oracle Database (Cloud) │          │     MongoDB Container     │
      │       Port: 1521         │          │     (mongo:7.0, Vol: db)  │
      └──────────────────────────┘          └───────────────────────────┘
```

### Why this Architecture?
- **Zero CORS Configuration**: Both the single-page application and the backend API endpoints are routed through the same public port (`80`) via the front-facing Nginx reverse proxy.
- **Resource Isolated**: Services communicate on a private internal bridge network (`schemebridge_network`).
- **Production Performance**: Vite production bundle is pre-compressed and served with aggressive HTTP caching and gzip.

---

## Option 1: VPS / Cloud VM Deployment (Docker Compose)

### 1. Provision a Server
- **OS**: Ubuntu 22.04 LTS / 24.04 LTS or Debian 12
- **Recommended Specs**: 2 vCPU, 4GB RAM (or minimum 2GB RAM with a 2GB swap file)
- **Firewall (Security Groups)**: Allow inbound traffic on ports `80` (HTTP) and `443` (HTTPS).

### 2. Install Docker & Docker Compose
On your Ubuntu/Debian server:
```bash
# Update packages
sudo apt update && sudo apt upgrade -y

# Install Docker & Docker Compose plugin
curl -fsSL https://get.docker.com | sudo sh

# Allow non-root docker execution (optional)
sudo usermod -aG docker $USER
newgrp docker
```

### 3. Clone Repository
```bash
git clone https://github.com/Lathika-Kumar/AI-Based-Government-Scheme-Eligibility-And-Application-Assistant.git schemebridge
cd schemebridge
```

### 4. Configure Environment Variables
Copy the template configuration:
```bash
cp .env.docker .env
nano .env
```
Update any necessary variables (e.g., your Oracle Database connection string, JWT secret):
```dotenv
PORT=80
JWT_SECRET=your-production-jwt-secret-min-32-chars
ORACLE_URL=jdbc:oracle:thin:@your-oracle-host:1521/XEPDB1
ORACLE_USERNAME=SYSTEM
ORACLE_PASSWORD=your_secure_password
```

### 5. Launch the Application
Run the one-click deployment script or use Docker Compose directly:
```bash
chmod +x deploy.sh
./deploy.sh
```
Or manually:
```bash
docker compose up --build -d
```

### 6. Verify Deployment
Check the status of all containers:
```bash
docker compose ps
```
Output:
```
NAME                          STATUS          PORTS
schemebridge-mongodb          Up (healthy)    0.0.0.0:27017->27017/tcp
schemebridge-scheme-service   Up (healthy)    0.0.0.0:8081->8081/tcp
schemebridge-auth-service     Up (healthy)    0.0.0.0:8080->8080/tcp
schemebridge-frontend         Up (healthy)    0.0.0.0:80->80/tcp
```

Visit `http://<your-server-ip>` in your web browser.

---

## Setting Up SSL/TLS (HTTPS) with Certbot

To secure your production domain with free Let's Encrypt certificates:

1. Install Certbot on the host:
   ```bash
   sudo apt install certbot -y
   ```
2. Request an SSL certificate:
   ```bash
   sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com
   ```
3. Mount the certificates into Nginx in `docker-compose.yml`:
   ```yaml
   frontend:
     ports:
       - "80:80"
       - "443:443"
     volumes:
       - /etc/letsencrypt:/etc/letsencrypt:ro
   ```

---

## Option 2: Local Windows Run (Without Docker)

If you are developing locally on Windows and do not have Docker installed:

### 1. Start MongoDB
Run local MongoDB with auth:
```powershell
mongod --config "e:\SCHEMEBRIDGE\mongod_local.cfg"
```

### 2. Start Auth Service (Port 8080)
```powershell
cd e:\SCHEMEBRIDGE\schemebridge-auth-service
$env:SERVER_PORT="8080"
$env:JWT_SECRET="LRCXhnioMtYNNZMrgfKfabTER44FEVNKiB9RVMuO1tE="
mvn spring-boot:run
```

### 3. Start Scheme Service (Port 8081)
```powershell
cd e:\SCHEMEBRIDGE\schemebridge-scheme-service
$env:SERVER_PORT="8081"
$env:JWT_SECRET="LRCXhnioMtYNNZMrgfKfabTER44FEVNKiB9RVMuO1tE="
$env:MONGODB_URI="mongodb://schemebridge_user:31Rfp1OuAafIzBhm1Xu6G5p7@localhost:27017/schemebridge_scheme_db?authSource=admin"
mvn spring-boot:run
```

### 4. Start Frontend
```powershell
cd e:\SCHEMEBRIDGE\schemebridge-frontend\schemeBridge-frontend
npm run dev
```
Access at `http://localhost:5173`.

---

## Environment Variables Reference

| Variable | Service | Description | Default / Example |
| :--- | :--- | :--- | :--- |
| `PORT` | Frontend | Public host HTTP port | `80` |
| `JWT_SECRET` | Auth & Scheme | Symmetric signing key for JWT tokens | `LRCXhnioMtYNNZMrgfKfabTER44...` |
| `JWT_EXPIRATION` | Auth | Access token validity in milliseconds | `3600000` (1 hour) |
| `MONGODB_URI` | Scheme | MongoDB connection string | `mongodb://user:pass@mongodb:27017/db` |
| `ORACLE_URL` | Auth | JDBC URL for Oracle Database | `jdbc:oracle:thin:@host:1521/XEPDB1` |
| `ORACLE_USERNAME` | Auth | Database username | `SYSTEM` |
| `ORACLE_PASSWORD` | Auth | Database password | `system` |
| `FRONTEND_URL` | Scheme | Public origin for CORS/links | `http://localhost` |
| `SPRING_MAIL_HOST`| Auth | SMTP host for email notifications | `smtp.gmail.com` |

---

## Management & Troubleshooting Commands

```bash
# View live logs of all services
docker compose logs -f

# View live logs of a specific service
docker compose logs -f scheme-service
docker compose logs -f auth-service
docker compose logs -f frontend

# Restart a specific service
docker compose restart scheme-service

# Stop all services (retaining data)
docker compose down

# Stop all services and wipe volumes (Caution!)
docker compose down -v
```
