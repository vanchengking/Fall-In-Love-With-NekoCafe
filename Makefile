.PHONY: up down test
up:    ; docker compose up -d --build
down:  ; docker compose down -v
test:  ; docker compose exec app pytest
