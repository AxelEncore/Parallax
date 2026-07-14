#version 150

in vec3 Position;
in vec2 UV;

//#if MC>=12106
//$$ #moj_import <minecraft:dynamictransforms.glsl>
//$$ #moj_import <minecraft:projection.glsl>
//#else
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
//#endif

out vec2 texCoord;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = UV;
}
