#version 150

in vec3 Position;
in vec2 UV0;

//#if MC>=12106
//$$ #moj_import <minecraft:dynamictransforms.glsl>
//$$ #moj_import <minecraft:projection.glsl>
//#else
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
//#endif

out vec2 vertexCoordinate;
out vec2 vertexScale;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    gl_Position.z += (gl_Position.z * 0.001) / gl_Position.w; // Slight bias to put behind terrain

//    vertexCoordinate = vec2(sign(UV0.x) * 0.5 + 0.5, sign(UV0.y) * 0.5 + 0.5);
    vertexCoordinate = vec2(sign(UV0.x), sign(UV0.y));
    vertexScale = abs(UV0);
}
