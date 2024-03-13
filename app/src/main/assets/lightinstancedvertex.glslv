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
out vec3 norm;
out vec3 fragModel;

void main(){
    int textureSide = (textureSize(tex, 0)).x; // lod=0
    ivec2 texCoord = ivec2(paletteIdx % textureSide, paletteIdx / textureSide);
    varyingColor = texelFetch(tex, texCoord, 0); // lod=0 since no mipmaps are used

    mat4 translM = mat4(1);
    translM[3] = vec4(vec3(-translation), 1); // set the 4th col to translation

    mat4 modelM = axesM * translM;

    mat4 inverseModelT = transpose(inverse(modelM));

    norm = normalize(vec3(inverseModelT*vec4(normal,1)));
    fragModel = vec3(modelM*vec4(vPos,1));

    gl_Position = VP * modelM * vec4(vPos,1);
}