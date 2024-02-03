#version 300 es

precision mediump float;

uniform vec3 color;

out vec4 fragColor;

void main() {
    fragColor = vec4(color, 1);
}