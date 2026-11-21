# Tareas de Chat SimpApp en el Worker compartido `rj2-cron`

`rj2-cron` despacha las tareas programadas de todas las apps de RJ2 (la cuenta solo tiene 5 Cron Triggers).
Chat SimpApp **no crea Cron Triggers propios**: reutiliza el cron diario que ya existía (`17 4 * * *`, 04:17 UTC).

Desplegado el 21-nov-2026 con estos cambios (si alguien vuelve a desplegar `rj2-cron` desde otra copia del
código, **debe conservarlos**, o las tareas de Chat SimpApp dejarán de correr):

```ts
// en JOBS:
{ cron: '17 4 * * *', app: 'CHAT_SIMPAPP', path: '/cron/cleanup-alerts' },
{ cron: '17 4 * * *', app: 'CHAT_SIMPAPP', path: '/cron/cleanup-db' },
```

```toml
# en wrangler.toml de rj2-cron:
[[services]]
binding = "CHAT_SIMPAPP"
service = "chat-simpapp-api"
```

Secreto de `rj2-cron`: `CHAT_SIMPAPP_CRON_SECRET` (mismo valor que `CRON_SECRET` de `chat-simpapp-api`; ya cargado).

| Tarea | Qué hace |
|---|---|
| `/cron/cleanup-alerts` | Borra de Firestore las alertas del barrio vencidas (la app ya las oculta al vencer). |
| `/cron/cleanup-db` | Limpia D1: envíos registrados de más de 2 días, tokens sin uso de 6 meses, caché vencido. |
