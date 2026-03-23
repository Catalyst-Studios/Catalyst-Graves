#version 150
in vec3 Position;
in vec2 UV0; // Recibimos la coordenada de la textura

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0; // Se la enviamos al .fsh

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
}