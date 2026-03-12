#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec2 texCoord2;
in vec4 normal;

out vec4 fragColor;

void main() {
    // 采样纹理颜色
    vec4 textureColor = texture(Sampler0, texCoord0);

    // 关键点：输出白色，但alpha使用纹理的alpha
    // 完全忽略纹理的RGB，只保留其透明度
    vec4 whiteColor = vec4(1.0, 1.0, 1.0, textureColor.a);

    // 乘以顶点颜色（用于控制透明度渐变）
    fragColor = whiteColor * vertexColor * ColorModulator;
}