#version 150

uniform sampler2D Sampler0;

//#if MC>=12106
//$$ #moj_import <minecraft:dynamictransforms.glsl>
//#else
uniform vec4 ColorModulator;
//#endif

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = color * ColorModulator;
}
