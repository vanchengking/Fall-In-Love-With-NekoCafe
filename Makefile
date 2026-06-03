.PHONY: install up down logs migrate seed dev-backend dev-frontend test perf

install:
	npm --prefix backend install
	npm --prefix frontend install

up:
	docker compose up -d --build

down:
	docker compose down -v

logs:
	docker compose logs -f

migrate:
	npm --prefix backend run migrate

seed:
	npm --prefix backend run seed

dev-backend:
	npm --prefix backend run dev

dev-frontend:
	npm --prefix frontend run dev -- --host 127.0.0.1

test:
	npm --prefix backend test

perf:
	k6 run tests/perf/reservations.k6.js
