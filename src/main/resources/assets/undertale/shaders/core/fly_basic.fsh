#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float Progress;

in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    // 输出白色，alpha 使用纹理的 alpha
    vec4 whiteColor = vec4(1.0, 1.0, 1.0, texColor.a);
    // 乘以顶点颜色（用于控制淡出）和颜色调制器
    fragColor = whiteColor * vertexColor * ColorModulator;
}