#version 300 es

precision mediump float;

// VERTEX LEVEL DATA
// in vec3 fragModel;
// in vec3 transfNormal;
in vec4 varyingColor;

// GLOBAL LEVEL DATA
uniform vec3 lightPos;
uniform vec3 eyePos;

out vec4 fragColor;

void main() {
    vec4 color = varyingColor;
    /*
    vec4 specComponent = vec4(color); // vec4(0.92,0.94,0.69,1);
    vec4 diffuseComponent = vec4(color); // vec4(0.64,0.84,0.15,1);
    vec4 ambientComponent = 0.15 * vec4(color); // vec4(0.12,0.4,0.01,1);
    vec3 eyeDir = normalize(eyePos-fragModel);
    vec3 lightDir = normalize(lightPos-fragModel);
    float diff = max(dot(lightDir,transfNormal),0.0);
    vec3 refl = reflect(-lightDir,transfNormal);
    float spec =  pow( max(dot(eyeDir,refl),0.0), 32.0);
    fragColor = ambientComponent + diff*diffuseComponent + spec*specComponent;
    */
    fragColor = color;
}