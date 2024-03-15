# Voxel Renderer Technical Docs

## Model Selection
La classe `ModelSelectionActivity.java` permette di scegliere mediante uno `Spinner` quale voxel model visualizzare nel renderer.

L'elenco dei modelli viene generato filtrando i file `.vly` nella cartella `assets/`. 

La entry correntemente selezionata viene salvata in un `Bundle` per mantenere la scelta corrente anche in caso di rotazione del dispositivo o altri eventi che portino l'Activity ad essere ri-creata.

Scelto il modello da visualizzare, mediante un `Intent` esplicito viene avviata l'Activity di visualizzazione del modello. 
***

## Model Viewer

La classe `ModelViewerActivity.java` si occupa principalmente della creazione del GLES context e della `GLSurfaceView` (alla quale viene associato il `Renderer`).

Si noti che al crescere del crescere del numero di voxel da disegnare, il numero di frammenti potenzialmente sovrapposti che si contendono la scrittura del depth-buffer può essere non indifferente. Nel metodo `getConfig` viene quindi richiesta una depth bit size di 24.

***

## Instanced Voxel Renderer

La classe `InstancedVoxelRenderer.java` si occupa del rendering dei voxel models (`.vly`). Implementa i metodi del Renderer Life Cycle (`onSurfaceCreated`, `onSurfaceChanged`, `onDrawFrame`) e gestisce l'interazione con l'utente (in `setContextAndSurface`)

### `onSurfaceCreated`
1. Compilazione vertex e fragment shader

2. Parsing del modello `pcube.ply` (rappresenta un singolo voxel), da cui si ottengono vertici, normali e indici

3. Parsing del voxel model (`.vly`)

4. Resource allocation and initialization

***

#### 3. Parsing del voxel model - `VlyObject.java`
Si occupa del parsing di modelli nel formato `.vly`.
Permette di ottenere:
- `gridSize`: dimensioni dell'occupancy grid
- `voxelNum`: numero di voxel presenti
- `voxelsRaw`: array contenente $X_i, Y_i, Z_i, C_i, X_{i+1}, Y_{i+1}, Z_{i+1}, C_{i+1}, ...$
- `paletteRaw`: array contenente la palette dei colori, $R_0, G_0, B_0, R_1, G_1, B_1, ...$. Il colore corrispondente al palette index $C_i$ si trova in `paletteRaw[i*3 + 0], paletteRaw[i*3 + 1], paletteRaw[i*3 + 2]` 

***

#### 4. Resource allocation and initialization
Al fine di utilizzare una soluzione basata su **instanced rendering**, sono stati individuati 3 livelli di granularità dei dati:
- **vertex level data (V)**: dati relativi al singolo vertice (assumono valori diversi per ogni vertice)
- **instance level data (I)**: dati relativi a un singolo voxel (assumono valori diversi per ogni voxel/istanza)
- **global level data (G)**: dati comuni a tutti i voxel/istanze

Si noti che in questa soluzione l'istanza è un singolo voxel.

| Dato | Descrizione | Granularità | Java (*) | GLSL |
|-|-|-|-|-|
| vertex position | coordinate del vertice | V | `vertexData`, `indexData` | `vPos` |
| normal | normale | V | `vertexData`, `indexData` | `normal` |
| translation | traslazione che serve a posizionare il voxel nel punto corretto dell'occupancy grid | I | `voxelsData` | `translation` |
| palette index | indice $C_i$, serve per ottenere il colore corrispondente | I | `voxelsData` | `paletteIdx` |
axes matrix | trasformazione affine (rototraslazione) per passare dal "voxel reference frame" all' "opengl reference frame" | G | `axesM` | `axesM` |
| view-proj matrix | view-proj matrix | G | `VP` | `VP` |
| palette texture | texture creata a partire dalla palette | G | `paletteBitmap` | `tex` |
| eye | posizione dell'osservatore | G | `eyePos` | `eyePos` |
| light | posizione della fonte di illuminazione | G | `lightPos` | `lightPos` | 

(*) non vi è sempre un mapping 1:1, in alcuni oggetti sono presenti diversi dati

Premessa: la mesh, nella sua posizione finale, sarà centrata nell'origine. 

Per prima cosa viene calcolato il diametro del cilindro che l'oggetto forma ruotando su se stesso, per evitare che l'osservatore, quando raggiunge la minima distanza dall'origine (`minEyeDistance`), possa toccare la mesh o addirittura "passare attraverso" essa.

La massima distanza dell'osservatore dall'origine (`maxEyeDistance`) viene impostata a un multiplo della massima dimensione della occupancy grid.

La posizione iniziale dell'osservatore viene impostata a metà tra la distanza minima e la distanza massima.

Per ottimizzare la gestione dei colori, viene creata una bitmap a partire dalla palette, da utilizzare come texture. Per ottenere texture quadrate i cui lati sono potenze di 2, bisogna trovare $\text{paletteSide} = 2^x, x \in \N \mid 2^x 2^x \ge \text{numColors}$, quindi $x \ge \frac{\log_2(\text{numColors})}{2}$

Infine è stato necessario prestare particolare attenzione agli instance level data:
- siccome i dati relativi alle istanze (`voxelsData`) sono di tipo `IntBuffer`, è stato necessario utilizzare `glVertexAttribIPointer`[(ref)](https://registry.khronos.org/OpenGL-Refpages/gl4/html/glVertexAttribPointer.xhtml) per definire il loro attribute buffer layout
- per ottenere la granularità "per-instance data", è stato necessario utilizzare ` glVertexAttribDivisor` [(ref)](https://www.khronos.org/opengl/wiki/Vertex_Specification#Instanced_arrays) 

***

### `onSurfaceChanged`
Viene calcolato l'aspect ratio al fine di costruire la projection matrix

***

### `onDrawFrame`
Sfruttando l'equazione $\frac{eyeDistance-minEyeDistance}{maxEyeDistance-minEyeDistance} = \frac{maxZoom-zoom}{maxZoom-minZoom}$ posso calcolare la distanza dell'osservatore dall'origine.

Utilizzando l'angolo di rotazione rispetto all'asse $\mathbf{y}$ ed $eyeDistance$  vengono calcolate le componenti x e z di `eyePos`.

Si è scelto di far muovere la fonte di illuminazione allo stesso modo dell'osservatore.

Per la drawcall è stato utilizzato `glDrawElementsInstanced` [(ref)](https://www.khronos.org/opengl/wiki/Vertex_Rendering#Instancing).

A questo punto, nel vertex shader vengono calcolate le coordinate 2D a cui trovare il colore del vertice corrente: `ivec2(paletteIdx % textureSide, paletteIdx / textureSide)` 

Osservando a livello di voxel la sequenza di trasformazioni che ogni vertice subisce, vediamo che:
- un voxel (`pcube.ply`), inizialmente centrato nell'origine, viene traslato di una quantità pari alle sue coordinate (negate) nell'occupancy grid
- viene poi traslato per centrare **l'intero modello** nell'origine
- viene infine ruotato di 90° attorno all'asse x (il formato .vly considerava Z come up-axis) 

***

### `setContextAndSurface`
L'interazione con l'utente viene gestita all'interno di questo metodo.

Viene istanziato uno `ScaleGestureDetector` per poter tradurre il pinching in zoom in / zoom out. Lo zoom viene implementato muovendo la posizione dell'osservatore ed è vincolato ad assumere valori consoni grazie alla formula con cui viene impostata la posizione dell'utente.

Viene settato un `View.OnTouchListener` per rilevare i tocchi dell'utente, che può decidere la direzione di rotazione a seconda della parte dello schermo su cui clicca. La rotazione è stata implementata cambiando la posizione dell'osservatore, in alternativa si poteva ruotare la mesh tenendo fissa la posizione dell'osservatore.

E' stato infine istanziato un `GestureDetector` per permettere la rotazione tramite scroll e la visualizzazione "wireframe" tramite long press.

***

