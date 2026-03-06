#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float CameraY;
uniform float bottomY;
uniform float bbHeight;

out vec4 vertexColor;
out vec2 texCoord0;
out float vNormY;

void main() {
    float worldY = Position.y + CameraY;
    float normY = (worldY - bottomY) / max(bbHeight, 0.001);
    vNormY = clamp(normY, 0.0, 1.0);

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    texCoord0 = UV0;
}