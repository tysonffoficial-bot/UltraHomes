# UltraHomes

Plugin de Minecraft (Spigot/Paper) para gestionar múltiples hogares de forma avanzada.

## Características
- Comando único `/home` con subcomandos `set`, `del`, `default` y `list`.
- Alias rápidos `/sethome`, `/delhome` y `/homes` para acciones frecuentes.
- Límite de hogares configurable (`maxHomes`) y ampliable por permisos `ultrahomes.limit.X`.
- Guarda ubicaciones, mundo y fecha de creación en `homes.yml`.
- Teletransportes seguros que validan que el mundo exista antes de mover al jugador.

## Comandos
- `/home` teletransporta al hogar predeterminado.
- `/home set [nombre]` o `/sethome [nombre]` crea un hogar (por defecto "home").
- `/home del <nombre>` o `/delhome <nombre>` elimina un hogar.
- `/home default <nombre>` define el hogar predeterminado.
- `/home list` o `/homes` muestra todos los hogares con mundo y fecha.
- `/home <nombre>` teletransporta a un hogar específico.

## Permisos
- `ultrahomes.limit.X` establece el número máximo de hogares para el jugador. El plugin buscará el valor más alto entre 1 y 100 que el jugador tenga. Por defecto, todos tienen `ultrahomes.limit.3`.

## Construcción

Requiere JDK 17 y Maven.

```bash
mvn package
```

Dependencias en tiempo de ejecución:
- Servidor Spigot/Paper 1.20.1 o superior.
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (opcional, se declara como `softdepend`).

El artefacto listo para usar se encuentra en `target/UltraHomes-1.0.0-shaded.jar`.

## Publicar en GitHub

Si quieres subir el código y el _jar_ generado a un repositorio remoto:

1. Configura el _remote_ (sustituye la URL por la de tu repositorio):
   ```bash
   git remote add origin git@github.com:tu-usuario/UltraHomes.git
   ```
2. Envía la rama de trabajo:
   ```bash
   git push -u origin work
   ```
3. (Opcional) Crea un _release_ en GitHub y adjunta `target/UltraHomes-1.0.0-shaded.jar`.
