#version 300 es

// VERTEX LEVEL DATA
layout(location = 1) in vec3 vPos;
layout(location = 2) in vec3 normal;

// INSTANCE LEVEL DATA
layout(location = 3) in ivec3 translation;
layout(location = 4) in int paletteIdx;

// GLOBAL LEVEL DATA
uniform mat4 axesM;
uniform mat4 MVP; // TODO: rename, is just VP
uniform sampler2D tex; // the active texture is bound to this sampler object
// uniform mat4 modelMatrix;
// uniform mat4 inverseModel;

out vec4 varyingColor;
// out vec3 transfNormal;
// out vec3 fragModel;

void main(){
    // transfNormal = vec3(inverseModel * vec4(normal, 1));
    // fragModel = vec3(modelMatrix * vec4(vPos, 1));

    int textureSide = (textureSize(tex, 0)).x; // lod=0
    ivec2 texCoord = ivec2(paletteIdx % textureSide, paletteIdx / textureSide);
    varyingColor = texelFetch(tex, texCoord, 0); // lod=0 since no mipmaps are used

    mat4 translM = mat4(1); // identity
    translM[0][0] = float(translation.x); // col, row
    translM[1][1] = float(translation.y);
    translM[2][2] = float(translation.z);

    // translM = vec4(translation, 1) * translM; // TODO: check if it works

    mat4 modelMatrix = axesM * translM;
    gl_Position = MVP * modelMatrix * vec4(vPos,1);
}