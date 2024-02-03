#version 300 es

layout(location = 1) in vec3 vPos;
layout(location = 2) in vec3 normals;

uniform mat4 MVP;

void main(){
    gl_Position = MVP * vec4(vPos,1);
}