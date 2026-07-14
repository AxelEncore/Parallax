#version 150

uniform sampler2D Sampler0;

//#if MC>=12106
//$$ layout(std140) uniform MinBrightnessUBO {
//$$     float MinBrightness;
//$$ };
//#else
uniform float MinBrightness;
//#endif

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord);

    fragColor = vec4(
        color.r * (1 - MinBrightness) + MinBrightness,
        color.g * (1 - MinBrightness) + MinBrightness,
        color.b * (1 - MinBrightness) + MinBrightness,
        color.a
    );
}
