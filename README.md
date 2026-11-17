# Mesa Arcana (mod de cliente, Fabric, Minecraft 26.1.2)

Dibuja efectos con texturas propias alrededor de la mesa de encantamiento (la mesa y el libro
siguen siendo los de vanilla):

- Dos anillos de runas brillantes que flotan y giran alrededor del libro
- Un circulo magico en el suelo (con un anillo de runas que gira)
- Runas sueltas que suben y siempre miran a la camara
- Un haz de luz sobre el libro
- Mas estanterias alrededor = mas brillo, mas runas y giro mas rapido

## Compilar con GitHub
1. Sube todo este proyecto a un repo.
2. Actions -> build -> Run workflow.
3. Baja el artifact "MesaArcana" y usa el .jar que no termine en -sources.
4. Ponlo en mods con Fabric API.

## Ajustes
config/mesaarcana.json (se recarga solo): enabled, ring, floor, glyphs, beam, intensity, speed.

## Cambiar los dibujos
Las texturas estan en assets/mesaarcana/textures/effect/ (PNG con transparencia).
Puedes reemplazarlas con un resource pack con la misma ruta.
