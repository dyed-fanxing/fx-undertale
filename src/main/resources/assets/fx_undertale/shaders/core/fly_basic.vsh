#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV1;
in vec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float Progress;
uniform float StripeHeight;

out vec4 vertexColor;
out vec2 texCoord0;
out vec2 texCoord1;
out vec2 texCoord2;
out vec4 normal;
out float vIsInWindow;

void main() {
    float v = UV0.y; // 假设0=底部，1=顶部
    float scanCenter = 1.0 - Progress; // 扫描中心从顶向下
    float halfHeight = StripeHeight / 2.0;
    float lower = scanCenter - halfHeight;
    float upper = scanCenter + halfHeight;

    // 判断顶点是否在窗口内
    vIsInWindow = (v >= lower && v <= upper) ? 1.0 : 0.0;

    // 不修改顶点位置
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    texCoord0 = UV0;
    texCoord1 = UV1;
    texCoord2 = UV2;
    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
}