# Wormholes pour Vaelor

Version de [Wormholes](https://github.com/VolmitSoftware/Wormholes) (VolmitSoftware, GPL-3.0) qui tourne sur les serveurs Vaelor en Paper 26.3, et mod Fabric pour les instances 26.3 du modpack.

Base : tag `2.2.0-26.1.2-26.3` (`694186b`) du dépôt d'origine. Les modifications de Vaelor sont dans les commits qui suivent, un par sujet.

## Modifications

1. **Mod, biome inconnu du client.** Depuis un serveur sans Terralith, la vue vers un serveur qui l'a restait vide, et le chemin des shaders Iris échouait toutes les 5 secondes. `ClientMeshWorld` et `PortalIrisBiomes` remplacent un biome que le client ne connaît pas par `minecraft:plains`.
2. **Plugin, choix des entités montrées.** Une vue n'affiche que `max-spoofed-entities` entités, mais elles étaient prises dans un ordre quelconque : un joueur de l'autre côté pouvait disparaître. `EntityPriority` les trie (joueurs d'abord, puis distance au portail) avant la coupe, dans `RemoteProjectionView`, `ProjectedEntityRenderer`, `EntityRenderCaches` et `RegionSnapshotWorldViewProvider`.
3. **Plugin, `/wormholes admin adoptrunes`.** Wormholes ne reconnaît que les runes posées à la main : un bloc tracé à Axiom, à WorldEdit ou collé n'envoie pas d'évènement de pose. La commande prend le bloc de prismarine (ou de prismarine sombre) visé et enregistre comme runes tous les blocs de même matière reliés par leurs faces dans le plan de la face visée, 4 096 au plus. Permission `wormholes.admin.items`. Textes dans les 17 langues.
4. **Vues des mondes de plus de 512 blocs de haut.** Un morceau de vue (`ViewSlice`, une colonne de chunk) était refusé au-delà de 16 × 512 × 16 cellules. Gigantica (Spawn) donne des colonnes de 561 blocs quand la distance de rendu du joueur est grande : les serveurs qui regardaient Spawn rejetaient sa vue (« Invalid view slice cell count ») et le portail restait vide. La limite suit maintenant la hauteur maximale d'une dimension (4 096).
5. **Plafond de la vue native.** La profondeur de la vue native (mod) suivait la distance de rendu du client, jusqu'à 32 chunks : un joueur réglé sur 32 faisait demander près de 4 600 colonnes de chunk au serveur d'en face, ce qui a saturé la mémoire de Hub puis bloqué son fil principal (2026-10-11). `[client-view] mesh-max-distance-chunks` (8 par défaut, 2 à 32) plafonne cette profondeur ; garder la même valeur sur les serveurs reliés.
6. **Mod, nettoyage de la déconnexion sur le fil de rendu.** Fabric lance `ClientPlayConnectionEvents.DISCONNECT` depuis le fil réseau (Netty) quand la connexion se ferme, et `WormholesClient.disconnected()` y libérait les tampons graphiques et les pipelines Iris des portails. Le plus souvent `RenderSystem` refusait l'appel (« Rendersystem called from wrong thread » au journal) ; parfois un appel OpenGL non gardé passait, et LWJGL arrêtait la JVM sur le coup, sans rapport de crash : le jeu disparaissait à la fermeture, à la déconnexion ou au redémarrage du serveur (9 sessions sur 55 du 15 au 28 octobre 2026). `WormholesFabricClient` confie maintenant `disconnected()` au fil de rendu par `minecraft.execute`.

## Compiler

- JDK 25, avec `JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"` (un test échoue en locale française).
- VolmLib au commit `f983dd1`, clonée dans un dossier `VolmLib` voisin de celui-ci : `settings.gradle` la trouve seule. Les compilations JitPack de VolmLib échouent depuis le 2026-10-05.
- Sous Windows, un chemin court (les chemins longs cassent la compilation).

Plugin :

```bash
./gradlew buildBukkit -PvolmitPackaging=dev -PpluginOutDirectory=../out --init-script vaelor/sans-mavenlocal-init.gradle
```

Mod :

```bash
./gradlew buildFabric -PvolmitPackaging=dev -PpluginOutDirectory=../out
```

`volmitPackaging=dev` saute ProGuard, qui plante dans l'outil de packaging de VolmLib. `vaelor/sans-mavenlocal-init.gradle` écarte le dépôt Maven local : une copie de PacketEvents qui embarque Adventure s'y trouvait et rendait le plugin inchargeable sur Paper.

Après compilation, vérifier qu'aucune classe `art/arcane/wormholes/libs/kyori/` n'est présente dans `META-INF/volmit/runtime.jar.xz`.

## En production

Depuis le 2026-10-11 : plugin `Wormholes-2.2.0.jar` (SHA-256 `b8d387e2…`) sur Spawn, Minage, Habitable, Donjon, Nether & End et Hub ; mod `Wormholes-2.2.0-fabric-26.3.jar` (`11fe70ea…`, depuis le 2026-10-28, avec la sixième modification ; la cinquième n'y change rien, elle ne concerne que les serveurs) dans les instances « Vaelor 26.3 ». Mod précédent : `9c3443b0…`.
