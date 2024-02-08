#version 300 es

// VERTEX LEVEL DATA
layout(location = 1) in vec3 vPos;
layout(location = 2) in vec3 normal;

// INSTANCE LEVEL DATA
layout(location = 3) in ivec3 translation;
layout(location = 4) in int paletteIdx;

// GLOBAL LEVEL DATA
uniform mat4 axesM;
uniform mat4 VP;
uniform sampler2D tex; // the active texture is bound to this sampler object

out vec4 varyingColor;

void main(){
    int textureSide = (textureSize(tex, 0)).x; // lod=0
    ivec2 texCoord = ivec2(paletteIdx % textureSide, paletteIdx / textureSide);
    varyingColor = texelFetch(tex, texCoord, 0); // lod=0 since no mipmaps are used

    vec3 newPos = vPos + vec3(translation);

    gl_Position = VP * vec4(newPos,1);
}