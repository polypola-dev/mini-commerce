# Mini Commerce

Separated frontend/backend implementation baseline for the mini-commerce architecture.

## Structure

- `frontend/` - Next.js storefront and order UI.
- `backend/` - Spring Boot API with catalog, order, and Redis Lua inventory reservation.
- `docker-compose.yml` - PostgreSQL and Redis for local development.

## Local Development

Start infrastructure:

```bash
docker compose up -d postgres redis
```

Backend:

```bash
docker compose up backend
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

The frontend expects the backend at `http://localhost:8080` by default. Override with `NEXT_PUBLIC_API_BASE_URL`.
