#version 150

//#if MC>=12106
//$$ #moj_import <minecraft:dynamictransforms.glsl>
//#else
uniform vec4 ColorModulator;
//#endif

out vec4 fragColor;

void main() {
    fragColor = ColorModulator;
}
