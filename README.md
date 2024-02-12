# Voxel Renderer

## TODO
- [ ] zoom [0-1] save display height/width, scale ratio by height and width if they are in pixels
- [ ] data transfer optimization
- [ ] creazione della bitmap durante il parsing del vly, memorizza voxelraw, la palette e altre costanti in una classe diversa da vlyobject
- [ ] fai zoomare fino a sqrt(gs[0]**2 + gs[1]**2), la z non c'entra per il maxzoom. sistema zoom
- [x] zoom, rotate -> avoid interfering of touch and swipe (boolean), avoid fov issues
- [ ] (anche no) translate the model up by gridSizeOGL[1]/2 and see implications for other params
- [x] lightning
- [x] instanced rendering
- [x] change depth buffer
- [ ] view to select vly model
- [ ] file spiegazioni
- [ ] orto/persp switch
- [x] wireframe
- [ ] enable multisample
- [ ] make model rotation inversely proportional to eyeDist

