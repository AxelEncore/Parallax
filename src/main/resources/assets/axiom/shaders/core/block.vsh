#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;

//#if MC>=12106
//$$ #moj_import <minecraft:dynamictransforms.glsl>
//$$ #moj_import <minecraft:projection.glsl>
//#else
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
//#endif

out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
    texCoord0 = UV0;
}
