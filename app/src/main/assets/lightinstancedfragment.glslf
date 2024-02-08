#version 300 es

precision mediump float;

// VERTEX LEVEL DATA
in vec4 varyingColor;
in vec3 norm;
in vec3 fragModel;

// GLOBAL LEVEL DATA
uniform vec3 lightPos;
uniform vec3 eyePos;

out vec4 fragColor;

void main() {
    vec4 diffuseComponent = varyingColor;
    vec4 specComponent = varyingColor;
    vec4 ambientComponent = 0.15 * varyingColor;
    vec3 lightDir = normalize(lightPos-fragModel);
    vec3 eyeDir = normalize(eyePos-fragModel);
    float diffuse = max(dot(lightDir,norm),0.0);
    vec3 halfWay = normalize(lightDir+eyeDir);
    float specular = pow(max(dot(halfWay,norm),0.0),1.0);
    fragColor = ambientComponent + diffuse*diffuseComponent + specular*specComponent;
}