#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

uniform float FogStart;
uniform float FogEnd;
uniform float uEmissiveStrength;

in float vertexDistance;
in vec4 vertexColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec2 texCoord2;

out vec4 fragColor;

void main() {
vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }

    // 应用顶点颜色
    color *= vertexColor;

    // 应用覆盖层（受伤闪烁）
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);

    // 采样光照贴图（区块亮度）
    vec4 lightmapColor = texture(Sampler2, texCoord2);

    // 光照 + 自发光
    vec3 finalColor = color.rgb * lightmapColor.rgb + color.rgb * uEmissiveStrength;

    fragColor = vec4(finalColor, color.a) * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}