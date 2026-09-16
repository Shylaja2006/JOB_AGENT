# Job Agent

## Local development

Frontend:

```text
npm install
copy .env.example .env
npm run dev
```

Set `VITE_API_URL` in `.env` to the local backend URL, such as `http://localhost:8080`.

Backend:

```text
.\mvnw.cmd spring-boot:run
```

Set the variables listed in the backend `.env.example` in the local process environment before starting Spring Boot. Use a managed or local MySQL database supplied through `DB_URL`; the application keeps the existing JPA schema-update behavior.

## Production deployment

### MySQL

Create a managed MySQL database and provide its connection URL, username, and password to the backend as `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Do not commit these values. Back up the database and review provider network and SSL requirements before deployment.

### Backend on Render

Create a Render web service for the backend directory. Use:

```text
Build command: ./mvnw clean package -DskipTests
Start command: java -jar target/job-agent-backend-0.0.1-SNAPSHOT.jar
```

Configure these Render environment variables:

```text
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
ADZUNA_APP_ID=
ADZUNA_APP_KEY=
FRONTEND_URL=https://your-vercel-domain.example
```

`JWT_SECRET` must be a private value at least 32 characters long. `ADZUNA_APP_ID` and `ADZUNA_APP_KEY` are supplied by Adzuna. Set `FRONTEND_URL` to the exact deployed Vercel origin, without a trailing path.

### Frontend on Vercel

Import the frontend directory into Vercel. Use the defaults for a Vite project:

```text
Build command: npm run build
Output directory: dist
```

Set the Vercel environment variable:

```text
VITE_API_URL=https://your-render-service.example
```

The deployed frontend uses `VITE_API_URL` for backend requests. Do not commit `.env` files or production credentials.

## Verification commands

```text
npm run lint
npm run build
.\mvnw.cmd clean compile -DskipTests
```
