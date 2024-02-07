#version 300 es

// VERTEX LEVEL DATA
layout(location = 1) in vec3 vPos;
layout(location = 2) in vec3 normal;

// INSTANCE LEVEL DATA
layout(location = 3) in ivec3 translation; // TODO: check type conversion
layout(location = 4) in int paletteIdx;

// GLOBAL LEVEL DATA
uniform mat4 axesM;
uniform mat4 MVP; // TODO: rename, is just VP
// uniform mat4 modelMatrix;
// uniform mat4 inverseModel;

out vec3 fragModel;
// out vec3 transfNormal;
flat out int varyingPaletteIdx;

void main(){
    // transfNormal = vec3(inverseModel * vec4(normal, 1));
    // fragModel = vec3(modelMatrix * vec4(vPos, 1));

    mat4 temp = mat4(1); // identity
    temp[0][0] = float(translation.x); // col, row
    temp[1][1] = float(translation.y);
    temp[2][2] = float(translation.z);

    // temp = vec4(translation, 1) * temp; // TODO: check if it works

    mat4 modelMatrix = axesM * temp;
    varyingPaletteIdx = paletteIdx;
    gl_Position = MVP * modelMatrix * vec4(vPos,1);
}