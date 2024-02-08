#version 300 es

precision mediump float;

// VERTEX LEVEL DATA
in vec4 varyingColor;

// GLOBAL LEVEL DATA
uniform vec3 lightPos;
uniform vec3 eyePos;

out vec4 fragColor;

void main() {
    fragColor = varyingColor;
}