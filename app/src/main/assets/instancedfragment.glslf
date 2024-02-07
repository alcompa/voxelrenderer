#version 300 es

precision mediump float;

// VERTEX LEVEL DATA
in vec3 fragModel;
// in vec3 transfNormal;

// INSTANCE LEVEL DATA
flat in int varyingPaletteIdx; // position in palette (not normalized in [0, 1]

// GLOBAL LEVEL DATA
uniform vec3 lightPos;
uniform vec3 eyePos;
uniform sampler2D tex; // the active texture is bound to this sampler object

out vec4 fragColor;

void main() {
    int textureSide = (textureSize(tex, 0)).x; // lod=0
    ivec2 texCoord = ivec2(varyingPaletteIdx % textureSide, varyingPaletteIdx / textureSide);
    vec4 color = vec4(1, 0, 0, 1); // texelFetch(tex, texCoord, 0); // lod=0 since no mipmaps are used // TODO: change
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