#version 150

in vec3 Position;

//#if MC>=12106
//$$ #moj_import <minecraft:dynamictransforms.glsl>
//$$ #moj_import <minecraft:projection.glsl>
//#else
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
//#endif

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
