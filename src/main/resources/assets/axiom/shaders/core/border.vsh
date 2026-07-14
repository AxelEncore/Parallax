#version 150

in vec3 Position;
in vec4 Color;
in ivec2 UV2;

//#if MC>=12106
//$$ #moj_import <minecraft:dynamictransforms.glsl>
//$$ #moj_import <minecraft:projection.glsl>
//#else
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
//#endif

out vec4 vertexColor;
out vec2 vertexCoordinate;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    gl_Position.z -= (gl_Position.z * 0.001) / gl_Position.w; // Slight bias to prevent z-fighting with terrain

    vertexColor = Color;
    vertexCoordinate = vec2(float(UV2.x), float(UV2.y));
}
