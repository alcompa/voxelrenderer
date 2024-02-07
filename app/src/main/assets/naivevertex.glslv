#version 300 es

layout(location = 1) in vec3 vPos;
layout(location = 2) in vec3 normal;

uniform mat4 MVP;
uniform mat4 modelMatrix;
uniform mat4 inverseModel;

out vec3 fragModel;
out vec3 transfNormal;

void main(){
    transfNormal = vec3(inverseModel * vec4(normal, 1));
    fragModel = vec3(modelMatrix * vec4(vPos, 1));
    gl_Position = MVP * vec4(vPos,1);
}