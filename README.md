# Voxel Renderer

## TODO
- [] rotazione movendo eyex, eyez (sin(angley), cos(angley)), sposta eyePos e lightPos in onDraw
    - [] decouple eyepos e lightpos
- [ ] creazione della bitmap durante il parsing del vly, memorizza voxelraw, la palette e altre costanti in una classe diversa da vlyobject
    - [ ] test palette più piccola
- [ ] fai zoomare fino a sqrt(gs[0]**2 + gs[1]**2), la z non c'entra per il maxzoom
- [x] zoom, rotate -> avoid interfering of touch and swipe (boolean), avoid fov issues
- [ ] (anche no) translate the model up by gridSizeOGL[1]/2 and see implications for other params
- [ ] lightning
- [ ] instanced rendering
- [x] change depth buffer
- [ ] java palette recycle, data transfer optimization
- [ ] view to select vly model
- [ ] file spiegazioni
- [ ] orto/persp switch
- [ ] model selector
- [x] wireframe
- [ ] enable multisample
- [ ] make model rotation inversely proportional to maxGridSizeOgl
