#version 150

uniform sampler2D Sampler0;
uniform float Progress;

in vec4 vertexColor;
in vec2 texCoord0;
in float vNormY;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;

    float threshold = 1.0 - Progress;
    if (vNormY > threshold) {
        color.a = 0.0;
    } else {
        float fadeStart = threshold - 0.1;
        if (vNormY > fadeStart) {
            color.a *= 1.0 - smoothstep(fadeStart, threshold, vNormY);
        }
    }

    fragColor = color;
}