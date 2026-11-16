# Mesa Arcana (mod de cliente, Fabric, Minecraft 26.1.2)

Efectos magicos alrededor de la mesa de encantamiento, solo en tu cliente:

- Anillos de runas girando alrededor del libro (el exterior y uno interior en sentido contrario)
- Runas sueltas que suben
- Haz de luz vertical sobre el libro
- Runas brillantes en el suelo alrededor de la mesa
- Mas estanterias alrededor = mas runas, mas luz y giro mas rapido

Funciona solo o junto al resource pack MesaArcana.zip.

## Compilar con GitHub
1. Sube todo este proyecto a un repo de GitHub.
2. Pestana Actions -> "build" -> Run workflow (o se lanza solo al hacer push).
3. Descarga el artifact "MesaArcana": ahi esta el .jar (no uses el -sources.jar).
4. Copia el .jar a la carpeta mods junto con Fabric API.

## Ajustes
Se crea config/mesaarcana.json la primera vez. Si lo editas y guardas, se recarga solo
(en 2 segundos) sin cerrar el juego: efectos on/off, density, ringHeight, maxDistance y colores.
