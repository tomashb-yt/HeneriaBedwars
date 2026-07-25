# Changelog

Toutes les Ã©volutions notables de HeneriaZombie sont consignÃ©es ici.

## 0.4.0-SNAPSHOT â€” Framework GUI configurable

### AjoutÃ©

- moteur central et registres extensibles d'Ã©crans et d'actions ;
- sessions isolÃ©es, retour/accueil, recherche privÃ©e et pagination sans plafond ;
- thÃ¨mes, menus, matÃ©riaux, textes, slots, permissions, sons et actions dans `guis.yml` ;
- validation agrÃ©gÃ©e et activation atomique asynchrone ;
- menus joueur, administration, maps, instances, diagnostics et confirmation ;
- aperÃ§u/crÃ©ation de map, entrÃ©e d'instance et arrÃªt confirmÃ© ;
- expiration et rafraÃ®chissement pÃ©riodique partagÃ© ;
- tests de pagination, permissions, historique, confirmation, saisie et YAML invalide.

### SÃ©curitÃ© et limites

- les interactions d'inventaire ne permettent pas de rÃ©cupÃ©rer les items ;
- joueur et jeton de vue sont vÃ©rifiÃ©s avant chaque callback ;
- Ã©diteurs mÃ©tier, profils, groupes, statistiques, export et suppression restent indisponibles ;
- la validation visuelle multi-clients reste manuelle.

## 0.3.0-SNAPSHOT â€” Import simple et aperÃ§u des maps

### AjoutÃ©

- dÃ©tection automatique d'un monde grÃ¢ce Ã  son seul `level.dat`, sans YAML obligatoire ;
- lecture bornÃ©e et asynchrone du spawn vanilla NBT ;
- `/zombie map list`, `/zombie map preview <mapId>` et `/zombie map leave` ;
- copies d'aperÃ§u isolÃ©es du registre des parties et nettoyage forcÃ© aprÃ¨s dÃ©chargement ;
- capacitÃ© par dÃ©faut configurable pour les maps sans manifeste.

### SÃ©curitÃ©

- le monde modÃ¨le n'est jamais chargÃ© ni modifiÃ© directement ;
- un seul aperÃ§u par administrateur et transition concurrente refusÃ©e ;
- dÃ©connexion, retour lobby et changement de contexte libÃ¨rent la copie.

## 0.2.3-SNAPSHOT â€” Repli sÃ»r du lobby

### CorrigÃ©

- un refus de crÃ©ation de `zombie_lobby` par Paper ne dÃ©sactive plus le plugin lorsque le monde de
  repli configurÃ© est disponible ;
- le monde de lobby rÃ©ellement utilisÃ© est diagnostiquÃ© clairement au dÃ©marrage.

## 0.2.2-SNAPSHOT â€” CompatibilitÃ© Paper 1.21

### CorrigÃ©

- rÃ©solution compatible de l'attribut de vie maximale renommÃ© entre les premiÃ¨res versions Paper
  1.21 et les versions de maintenance rÃ©centes ;
- retour lobby et initialisation joueur fonctionnels sur Paper 1.21 build 130 ;
- compilation de la frontiÃ¨re Paper contre l'API minimale `1.21-R0.1-SNAPSHOT`.

## 0.2.1-SNAPSHOT â€” Correctif Ticket 002

### CorrigÃ©

- arguments de commandes comme `<id>` affichÃ©s littÃ©ralement sans Ãªtre interprÃ©tÃ©s par
  MiniMessage ;
- message d'usage invalide ne provoquant plus d'exception de commande ;
- crÃ©ation garantie de `zombie_templates` lors de la premiÃ¨re recherche ;
- vóİz¶‰ËkºwµçHŠKÑš[J
JNÃBˆX[[ÛÛ™šYİ\˜][ÛˆY\ÜØYÙ\ÈCBˆX[[ÛÛ™šYİ\˜][Û‹›ØYÛÛ™šYİ\˜][ÛŠ]Q\™XİÜKœ™\ÛÛ™J›Y\ÜØYÙ\Ë[[ŠKÑš[J
JNÃBˆ›ÛXšYTÙ][™ÜÈÙ][™ÜÈCBˆ™]È›ÛXšYTÙ][™ÜÊBˆÛÛ™šYË™Ù][
˜ÛÛ™šYË]™\œÚ[Ûˆ‹JKBˆ™]ÈYÚ[“Ü[ÛœÊBˆÛÛ™šYË™Ù]İš[™ÊœYÚ[‹›[™İXYÙH‹ˆŠKBˆÛÛ™šYË™Ù]›ÛÛX[ŠœYÚ[‹™XYÈ‹˜[ÙJKBˆÛÛ™šYË™Ù]›ÛÛX[ŠœYÚ[‹˜ÚXÚËY›Ü‹]\]\È‹˜[ÙJJKBˆ™]ÈÙ\™\“Ü[ÛœÊBˆÛÛ™šYË™Ù]İš[™ÊœÙ\™\‹›Ø˜K]ÛÜ›‹ˆŠKBˆÛÛ™šYË™Ù]İš[™ÊœÙ\™\‹™˜[˜XÚË]ÛÜ›‹ˆŠJKBˆ™]ÈİÜ˜YÙSÜ[ÛœÊBˆÛÛ™šYË™Ù]İš[™ÊœİÜ˜YÙK\H‹ˆŠKÛÛ™šYË™Ù]İš[™ÊœİÜ˜YÙKœÜ[]KYš[H‹ˆŠJKBˆ™]ÈØ˜SÜ[ÛœÊBˆÛÛ™šYË™Ù]İš[™Ê›Ø˜KÛÜ›‹ÛÛ™šYË™Ù]İš[™ÊœÙ\™\‹›Ø˜K]ÛÜ›‹ˆŠJKBˆ™]ÈØØ][Û“Ü[ÛœÊBˆÛÛ™šYË™Ù]İš[™ÊBˆ›Ø˜KœÜ]Û‹ÛÜ›‹BˆÛÛ™šYË™Ù]İš[™ÊBˆ›Ø˜KÛÜ›‹ÛÛ™šYË™Ù]İš[™ÊœÙ\™\‹›Ø˜K]ÛÜ›‹ˆŠJJKBˆÛÛ™šYË™Ù]İX›J›Ø˜KœÜ]Û‹‹JKBˆÛÛ™šYË™Ù]İX›J›Ø˜KœÜ]Û‹H‹KŒ
KBˆÛÛ™šYË™Ù]İX›J›Ø˜KœÜ]Û‹ˆ‹JKBˆ
›Ø]
HÛÛ™šYË™Ù]İX›J›Ø˜KœÜ]Û‹X]È‹Œ
KBˆ
›Ø]
HÛÛ™šYË™Ù]İX›J›Ø˜KœÜ]Û‹œ]Ú‹Œ
JJKBˆ™]È[œİ[˜ÙSÜ[ÛœÊBˆÛÛ™šYË™Ù]İš[™Êš[œİ[˜Ù\ËÛÜ›ËY\™XİÜH‹›ÛXšYWÚ[œİ[˜Ù\ÈŠKBˆÛÛ™šYË™Ù]İš[™Êš[œİ[˜Ù\Ë[\]\ËY\™XİÜH‹›ÛXšYWİ[\]\ÈŠKBˆÛÛ™šYË™Ù]›ÛÛX[Šš[œİ[˜Ù\Ë™[]K]ÛÜ›XY\‹YØ[YH‹YJKBˆÛÛ™šYË™Ù]›ÛÛX[Šš[œİ[˜Ù\Ëœ™\Ù\™KY˜Z[Y]ÛÜ›È‹YJKBˆÛÛ™šYË™Ù][
š[œİ[˜Ù\Ë[›ØYY[^K\ÙXÛÛ™È‹JKBˆÛÛ™šYË™Ù][
š[œİ[˜Ù\Ë˜Ü™X][Û‹][Y[İ]\ÙXÛÛ™È‹Œ
KBˆÛÛ™šYË™Ù][
š[œİ[˜Ù\Ë›X^[][KXÛÛ˜İ\œ™[YØ[Y\È‹LJKBˆÛÛ™šYË™Ù]›ÛÛX[Šš[œİ[˜Ù\Ëœ™]™[Y[K]Ú]İ]\Ù\ÜÚ[Ûˆ‹YJKBˆÛÛ™šYË™Ù][
š[œİ[˜Ù\Ë™Y˜][[X\[X^[][K\^Y\œÈ‹
JKBˆ™]ÈÚ]Ü[ÛœÊBˆÛÛ™šYË™Ù]›ÛÛX[Š˜Ú]š\ÛÛ][Û‹Y[˜X›Y‹YJKBˆÛÛ™šYË™Ù]›ÛÛX[Š˜Ú]›Ø˜KXÚ[›™[Y[˜X›Y‹YJKBˆÛÛ™šYË™Ù]›ÛÛX[Š˜Ú]š[œİ[˜ÙKXÚ[›™[Y[˜X›Y‹YJKBˆÛÛ™šYË™Ù]›ÛÛX[Š˜Ú]˜[İËYÛØ˜[XYZ[‹XÚ[›™[‹YJJKBˆ™]È™XÛÛ›™XİÜ[ÛœÊBˆÛÛ™šYË™Ù]›ÛÛX[Šœ™XÛÛ›™Xİ™[˜X›Y‹YJKBˆÛÛ™šYË™Ù][
œ™XÛÛ›™Xİ™Ü˜XÙK\\š[Ù\ÙXÛÛ™È‹N
KBˆÛÛ™šYË™Ù]›ÛÛX[Šœ™XÛÛ›™Xİœ™\Ù\™K\^Y\‹\Ûİ‹YJKBˆÛÛ™šYË™Ù]›ÛÛX[Šœ™XÛÛ›™Xİœ™]\›‹]Ë[Ø˜KXY\‹Y^\˜][Ûˆ‹YJJKBˆ™]ÈÛÜ›[SÜ[ÛœÊBˆÛÛ™šYË™Ù]›ÛÛX[ŠÛÜ›\[\Ë˜[İË[˜]\˜[[[Ø‹\Ü]Ûš[™È‹˜[ÙJKBˆÛÛ™šYË™Ù]›ÛÛX[ŠÛÜ›\[\Ë˜[İË]ÙX]\‹XŞXÛH‹˜[ÙJKBˆÛÛ™šYË™Ù]›ÛÛX[ŠÛÜ›\[\Ë˜[İË][YKXŞXÛH‹˜[ÙJKBˆÛÛ™šYË™Ù]›ÛÛX[ŠÛÜ›\[\Ë˜[İËX›ØÚËXœ™XZÚ[™È‹˜[ÙJKBˆÛÛ™šYË™Ù]›ÛÛX[ŠÛÜ›\[\Ë˜[İËX›ØÚË\XÚ[™È‹˜[ÙJKBˆÛÛ™šYË™Ù]›ÛÛX[ŠÛÜ›\[\Ë˜[İËZ][KY›Ü[™È‹˜[ÙJKBˆÛÛ™šYË™Ù]›ÛÛX[ŠÛÜ›\[\Ë˜[İËZ][K\XÚİ\‹YJKBˆÛÛ™šYË™Ù]›ÛÛX[ŠÛÜ›\[\Ë˜[İË\œ‹˜[ÙJKBˆÛÛ™šYË™Ù]›ÛÛX[ŠÛÜ›\[\ËšÙY\Z[™[ÜH‹YJKBˆÛÛ™šYË™Ù]›ÛÛX[ŠÛÜ›\[\Ë›ÚY\™\ØİYKY[˜X›Y‹YJJKBˆ™]ÈİZSÜ[ÛœÊBˆÛÛ™šYË™Ù]İš[™Ê™İZK™Y˜][][YH‹ˆŠKBˆÛÛ™šYË™Ù]›ÛÛX[Š™İZKœÛİ[™ËY[˜X›Y‹YJKBˆÛÛ™šYË™Ù]›ÛÛX[Š™İZK˜[š[X][ÛœËY[˜X›Y‹YJKBˆÛÛ™šYË™Ù][
™İZKœÙ\ÜÚ[Û‹][Y[İ]\ÙXÛÛ™È‹Ì
KBˆÛÛ™šYË™Ù][
™İZKš[œ]][Y[İ]\ÙXÛÛ™È‹Œ
KBˆÛÛ™šYË™Ù][
™İZK˜ÛÛ™š\›X][Û‹Y[^K]XÚÜÈ‹Œ
KBˆÛÛ™šYË™Ù][
™İZKœ™Yœ™\Ú›X\Ë[Y[K]XÚÜÈ‹L
KBˆÛÛ™šYË™Ù][
™İZKœ™Yœ™\Úš[œİ[˜Ù\Ë[Y[K]XÚÜÈ‹Œ
KBˆÛÛ™šYË™Ù][
™İZKœ™Yœ™\Ú™XYÛ›ÜİXÜË[Y[K]XÚÜÈ‹
JKBˆ™]ÈØİ[Y[][Û“Ü[ÛœÊBˆÛÛ™šYË™Ù]›ÛÛX[Š™Øİ[Y[][Û‹œ™\]Z\™KXÛÛ^]\]H‹YJJJNÃBˆX\İš[™Ëİš[™ÏˆY\™ÙYY\ÜØYÙ\ÈH[™YY\ÜØYÙ\Ê
NÃBˆY\™ÙYY\ÜØYÙ\Ëœ][
›][ŠY\ÜØYÙ\ÊJNÃBˆ™]\›ˆ™]ÈØ[™Y]JBˆ™]ÈÛÛ™šYİ\˜][Û”Û˜\Úİ
Ù][™ÜËY\™ÙYY\ÜØYÙ\ÊK˜[Y]Ü‹˜[Y]JÙ][™ÜÊJNÃBˆCBƒBˆš]˜]H›ÚY[œİ[Y˜][
İš[™È™\Ûİ\˜ÙS˜[YJH›İÜÈSÑ^Ù\[ÛˆÃBˆ]\™Ù]H]Q\™XİÜKœ™\ÛÛ™J™\Ûİ\˜ÙS˜[YJNÃBˆYˆ
š[\Ë™^\İÊ\™Ù]
JHÃBˆ™]\›ÃBˆCBˆH
[œ]İ™X[H[œ]H™\Ûİ\˜ÙSØY\‹™Ù]™\Ûİ\˜ÙP\Ôİ™X[J™\Ûİ\˜ÙS˜[YJJHÃBˆYˆ
[œ]OH[
HÃBˆ›İÈ™]ÈSÑ^Ù\[ÛŠ“Z\ÜÚ[™È[™Y™\Ûİ\˜ÙHˆ
È™\Ûİ\˜ÙS˜[YJNÃBˆCBˆš[\Ë˜ÛÜJ[œ]\™Ù]
NÃBˆCBˆCBƒBˆš]˜]HX\İš[™Ëİš[™Ïˆ[™YY\ÜØYÙ\Ê
HÃBˆH
[œ]İ™X[H[œ]H™\Ûİ\˜ÙSØY\‹™Ù]™\Ûİ\˜ÙP\Ôİ™X[J›Y\ÜØYÙ\Ë[[ŠJHÃBˆYˆ
[œ]OH[
HÃBˆ›İÈ™]È[YØ[İ]Q^Ù\[ÛŠ“Z\ÜÚ[™È[™Y™\Ûİ\˜ÙHY\ÜØYÙ\Ë[[ŠNÃBˆCBˆ™]\›ˆ™]È[šÙY\ÚX\ŠBˆ›][ŠBˆX[[ÛÛ™šYİ\˜][Û‹›ØYÛÛ™šYİ\˜][ÛŠBˆ™]È˜]˜Kš[Ë’[œ]İ™X[T™XY\Š[œ]˜]˜K›š[Ë˜Ú\œÙ]”İ[™\™Ú\œÙ]Ë•U—Î
JJJNÃBˆHØ]Ú
SÑ^Ù\[Ûˆ˜Z[\™JHÃBˆ›İÈ™]È[YØ[İ]Q^Ù\[ÛŠÛİ[›İ™XY[™YY\ÜØYÙ\Ë[[‹˜Z[\™JNÃBˆCBˆCBƒBˆš]˜]Hİ]XÈX\İš[™Ëİš[™Ïˆ›][ŠX[[ÛÛ™šYİ\˜][ÛˆX[[
HÃBˆX\İš[™Ëİš[™Ïˆ˜[Y\ÈH™]È[šÙY\ÚX\Š
NÃBˆÛÜTÙXİ[ÛŠX[[ˆ‹˜[Y\ÊNÃBˆ™]\›ˆ˜[Y\ÎÃBˆCBƒBˆš]˜]Hİ]XÈ›ÚYÛÜTÙXİ[ÛŠBˆÛÛ™šYİ\˜][Û”ÙXİ[ÛˆÙXİ[Û‹İš[™È™Yš^X\İš[™Ëİš[™Ïˆ\™Ù]
HÃBˆ›Üˆ
İš[™ÈÙ^HˆÙXİ[Û‹™Ù]Ù^\Ê˜[ÙJJHÃBˆİš[™È]H™Yš^š\Ñ[\J
HÈÙ^Hˆ™Yš^
È‹ˆˆ
ÈÙ^NÃBˆYˆ
ÙXİ[Û‹š\ĞÛÛ™šYİ\˜][Û”ÙXİ[ÛŠÙ^JJHÃBˆÛÛ™šYİ\˜][Û”ÙXİ[ÛˆÚ[HÙXİ[Û‹™Ù]ÛÛ™šYİ\˜][Û”ÙXİ[ÛŠÙ^JNÃBˆYˆ
Ú[OH[
HÃBˆÛÜTÙXİ[ÛŠÚ[]\™Ù]
NÃBˆCBˆH[ÙHYˆ
ÙXİ[Û‹š\Ôİš[™ÊÙ^JJHÃBˆ\™Ù]œ]
]ÙXİ[Û‹™Ù]İš[™ÊÙ^KˆŠJNÃBˆCBˆCBˆCBƒBˆš]˜]H™XÛÜ™Ø[™Y]JÛÛ™šYİ\˜][Û”Û˜\ÚİÛ˜\Úİ\İÛÛ™šYİ\˜][Û’\ÜİYOˆ\ÜİY\ÊHßCBŸCB